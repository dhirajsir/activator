import sbt._
import SnapBuild._
import SnapDependencies._
import Packaging.localRepoArtifacts
// NOTE - This file is only used for SBT 0.12.x, in 0.13.x we'll use build.sbt and scala libraries.
// As such try to avoid putting stuff in here so we can see how good build.sbt is without build.scala.


object TheSnapBuild extends Build {

  // ADD sbt launcher support here.
  override def settings = super.settings ++ SbtSupport.buildSettings ++ baseVersions ++ Seq(
    // This is a hack, so the play application will have the right view of the template directory.
    Keys.baseDirectory <<= Keys.baseDirectory apply { bd =>
      sys.props("snap.home") = bd.getAbsoluteFile.getAbsolutePath
      bd
    }
  ) ++ play.Project.intellijCommandSettings(play.Project.SCALA) // workaround for #24

  val root = (
    Project("root", file("."))  // TODO - Oddities with clean..
    aggregate((publishedProjects.map(_.project) ++ Seq(dist.project, it.project)):_*)
    settings(
      // Stub out commands we run frequently but don't want them to really do anything.
      Keys.publish := {},
      Keys.publishLocal := {}
    )
  )

  // Theser are the projects we want in the local SNAP repository
  lazy val publishedProjects = Seq(ui, launcher, props, cache, sbtRemoteProbe, sbtDriver)

  // basic project that gives us properties to use in other projects.  
  lazy val props = (
    SnapJavaProject("props")
    settings(Properties.makePropertyClassSetting(SnapDependencies.sbtVersion,SnapDependencies.scalaVersion):_*)
  )

  lazy val cache = (
    SnapProject("cache")
    dependsOn(props)
    dependsOnRemote(junitInterface % "test")
  )

  // add sources from the given dir
  def dependsOnSource(dir: String): Seq[Setting[_]] = {
    import Keys._
    Seq(unmanagedSourceDirectories in Compile <<= (unmanagedSourceDirectories in Compile, baseDirectory) { (srcDirs, base) => (base / dir / "src/main/scala") +: srcDirs },
        unmanagedSourceDirectories in Test <<= (unmanagedSourceDirectories in Test, baseDirectory) { (srcDirs, base) => (base / dir / "src/test/scala") +: srcDirs })
  }

  // sbt-child process projects
  lazy val sbtRemoteProbe = (
    SbtChildProject("remote-probe")
    settings(dependsOnSource("../protocol"): _*)
    settings(Keys.scalaVersion := "2.9.2", Keys.scalaBinaryVersion <<= Keys.scalaVersion)
    dependsOnRemote(
      sbtMain % "provided",
      sbtTheSbt % "provided",
      sbtIo % "provided",
      sbtLogging % "provided",
      sbtProcess % "provided"
    )
  )
  
  lazy val sbtDriver = (
    SbtChildProject("parent")
    settings(Keys.libraryDependencies <+= (Keys.scalaVersion) { v => "org.scala-lang" % "scala-reflect" % v })
    settings(dependsOnSource("../protocol"): _*)
    dependsOn(props)
    dependsOnRemote(akkaActor, 
                    sbtLauncherInterface)
  )  
  
  lazy val ui = (
    SnapPlayProject("ui")
    dependsOnRemote(
      commonsIo,
      sbtLauncherInterface % "provided"
    )
    dependsOn(props, cache, sbtDriver)
  )

  // TODO - SBT plugin, or just SBT integration?

  lazy val launcher = (
    SnapProject("launcher")
    dependsOnRemote(sbtLauncherInterface)
    dependsOn(props)
  )
  
  // A hack project just for convenient IvySBT when resolving artifacts into new local repositories.
  lazy val dontusemeresolvers = (
    SnapProject("dontuseme")
    settings(
      // This hack removes the project resolver so we don't resolve stub artifacts.
      Keys.fullResolvers <<= (Keys.externalResolvers, Keys.sbtResolver) map (_ :+ _)
    )
  )
  lazy val it = (
      SnapProject("integration-tests")
      settings(integration.settings:_*)
      dependsOnRemote(sbtLauncherInterface)
      dependsOn(sbtDriver, props, cache)
      settings(
        // Note: we remve project resolver for IT stuff (lame, I know), so we require publishLocal from our dependencies to update...
        // Keys.update <<= (Keys.update.task, (Keys.publishLocal in sbtDriver).task) apply ((a, b) => b flatMapR (_ => a)),
        
        org.sbtidea.SbtIdeaPlugin.ideaIgnoreModule := true
      )
  )

  lazy val dist = (
    SnapProject("dist")
    settings(Packaging.settings:_*)
    settings(
      Keys.scalaBinaryVersion <<= Keys.scalaVersion,
      Keys.resolvers ++= Seq(
        "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
        Resolver.url("typesafe-ivy-releases", new URL("http://repo.typesafe.com/typesafe/releases/"))(Resolver.ivyStylePatterns),
        Resolver.url("sbt-plugin-releases", new URL("http://repo.scala-sbt.org/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)
      ),
      // TODO - Do this better - This is where we define what goes in the local repo cache.

      localRepoArtifacts <++= (publishedProjects map { ref =>
        // The annoyance caused by cross-versioning.
        (Keys.projectID in ref, Keys.scalaBinaryVersion in ref, Keys.scalaVersion in ref) apply {
          (id, sbv, sv) =>
            CrossVersion(sbv,sv)(id)
        }
      }).join,
      localRepoArtifacts ++= 
        Seq("org.scala-sbt" % "sbt" % SnapDependencies.sbtVersion,
            // For some reason, these are not resolving transitively correctly! 
            "org.scala-lang" % "scala-compiler" % "2.9.2",
            "org.scala-lang" % "scala-compiler" % "2.10.0-RC1",
            "net.java.dev.jna" % "jna" % "3.2.3",
            "commons-codec" % "commons-codec" % "1.3",
            "org.apache.httpcomponents" % "httpclient" % "4.0.1",
            "com.google.guava" % "guava" % "11.0.2",
            "xml-apis" % "xml-apis" % "1.0.b2"
        ),
      localRepoArtifacts ++= {
        val sbt = "0.12"
        val scala = "2.9.2"
        Seq(
          Defaults.sbtPluginExtra("com.typesafe.sbt" % "sbt-site" % "0.6.0", sbt, scala),
          Defaults.sbtPluginExtra("com.typesafe" % "sbt-native-packager" % "0.4.3", sbt, scala),
          Defaults.sbtPluginExtra("play" % "sbt-plugin" % "2.1-RC1", sbt, scala),
          Defaults.sbtPluginExtra("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.1.0", sbt, scala),
          Defaults.sbtPluginExtra("com.typesafe.sbt" % "sbt-pgp" % "0.7", sbt, scala)
        )
      }
    )
  )
}
