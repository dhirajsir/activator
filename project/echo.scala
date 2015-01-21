import sbt._
import sbt.Keys._
import ActivatorBuild._
import com.typesafe.sbt.SbtGit
import com.typesafe.sbt.SbtCotest
import com.typesafe.sbt.SbtCotest.CotestKeys.cotestProjectName
import com.typesafe.sbt.SbtAspectj
import com.typesafe.sbt.SbtAspectj.{Aspectj, AspectjKeys}

object EchoBuild extends Build {

  import TraceProjects._
  import CollectProjects._

  lazy val echo = (
    Project("echo", file("echo"))
      .doNotPublish
      settings (LocalTemplateRepo.settings: _*)
      settings(
      Keys.resolvers += typesafeIvyReleases,
      parallelExecution in GlobalScope := false
      )
      aggregate(trace, collect, collect211, sigarLibs)
    )

  lazy val trace = (
    Project("echo-trace", file("echo/trace"))
      .doNotPublish
      aggregate(
      protocolProtobuf24, protocolProtobuf25,
      event210Protobuf24, event210Protobuf25,
      event211Protobuf24, event211Protobuf25,
      trace210Protobuf24, trace210Protobuf25,
      traceScala210Protobuf24, traceScala210Protobuf25,
      traceScala211Protobuf24, traceScala211Protobuf25,
      traceAkka22, traceAkka23Scala210, traceAkka23Scala211,
      tracePlayCommon, tracePlay23Scala210, tracePlay23Scala211
      )
    )

  lazy val sigarLibs =
    (Project("echo-sigar-libs", file("echo/sigar"))
      settings (defaultSettings: _*)
      settings(
      resourceDirectory in Compile <<= baseDirectory / "lib",
      autoScalaLibrary := false,
      pomIncludeRepository := { _ => false},
      publishArtifact in(Compile, packageDoc) := false,
      publishArtifact in(Compile, packageSrc) := false
      )
      )

  lazy val buildSettings = SbtGit.versionWithGit ++ Seq(
    organization := "com.typesafe.trace",
    scalaVersion := Dependencies.scala210Version,
    crossPaths := false,
    publishArtifact in packageSrc := false,
    publishArtifact in packageDoc := false,
    organizationName := "Typesafe Inc.",
    organizationHomepage := Some(url("http://www.typesafe.com")),
    pomIncludeRepository := { _ => false},
    // TODO: reenable once dependencies are aligned again
    // disable scala library conflict warnings
    conflictWarning := ConflictWarning.disable,
    // TODO: can be reenabled when all scala versions are final again
    // disable scala binary version warnings
    ivyScala ~= {
      _.map(_.copy(checkExplicit = false, overrideScalaVersion = false))
    },
    // reset these per project rather than globally
    scalaBinaryVersion <<= scalaVersion { v => if (v contains "-") v else CrossVersion.binaryScalaVersion(v)},
    crossScalaVersions <<= Seq(scalaVersion).join
  )

  lazy val projectSettings = buildSettings ++ Seq(
    resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
    // compile options
    scalacOptions <++= scalaVersion map { sv =>
      val opts = Seq("-encoding", "UTF-8", "-deprecation", "-unchecked")
      val opts210 = Seq("-feature", "-Xlint")
      if (sv.startsWith("2.10")) opts ++ opts210 else opts
    },
    javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation")
  )

  lazy val defaultSettings = projectSettings

  // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> COTESTS SETTINGS

  lazy val echoCotests = (
    Project("echo-cotests", file("echo/cotests"))
      .doNotPublish
      settings (defaultSettings: _*)
      settings (
      SbtCotest.cotestSettings(
        cotestsTraceAkka22, cotestsTraceAkka23Scala210, cotestsTraceAkka23Scala211,
        cotestsTracePlay23Scala210, cotestsTracePlay23Scala211,
        cotestsTrace2Akka22, cotestsTrace2Akka23Scala210, cotestsTrace2Akka23Scala211,
        cotestsCollect
      ): _*)
    )

  def sigarDir = resourceDirectory in(sigarLibs, Compile)

  def weaveAgent210Protobuf24 = AspectjKeys.weaverOptions in Aspectj in traceScala210Protobuf24

  def weaveAgent210Protobuf25 = AspectjKeys.weaverOptions in Aspectj in traceScala210Protobuf25

  def weaveAgent211Protobuf24 = AspectjKeys.weaverOptions in Aspectj in traceScala211Protobuf24

  def weaveAgent211Protobuf25 = AspectjKeys.weaverOptions in Aspectj in traceScala211Protobuf25

  def fileProperty(property: String, file: File) = "-D%s=%s" format(property, file.absolutePath)

  lazy val tracedTestSettingsScala210Protobuf24 = Seq(
    Keys.fork in Test := true,
    javaOptions in Test <++= (weaveAgent210Protobuf24, sigarDir) map {
      (weaveOpts, sigar) => weaveOpts ++ Seq(fileProperty("java.library.path", sigar))
    }
  )

  lazy val tracedTestSettingsScala210Protobuf25 = Seq(
    Keys.fork in Test := true,
    javaOptions in Test <++= (weaveAgent210Protobuf25, sigarDir) map {
      (weaveOpts, sigar) => weaveOpts ++ Seq(fileProperty("java.library.path", sigar))
    }
  )

  lazy val tracedTestSettingsScala211Protobuf24 = Seq(
    Keys.fork in Test := true,
    javaOptions in Test <++= (weaveAgent211Protobuf24, sigarDir) map {
      (weaveOpts, sigar) => weaveOpts ++ Seq(fileProperty("java.library.path", sigar))
    }
  )

  lazy val tracedTestSettingsScala211Protobuf25 = Seq(
    Keys.fork in Test := true,
    javaOptions in Test <++= (weaveAgent211Protobuf25, sigarDir) map {
      (weaveOpts, sigar) => weaveOpts ++ Seq(fileProperty("java.library.path", sigar))
    }
  )

  lazy val cotestsCommon210 = (
    Project("echo-cotests-common210", file("echo/cotests/common"))
      settings (defaultSettings: _*)
      settings(
      name := "echo-cotests-common",
      scalaVersion := Dependencies.scala210Version,
      crossPaths := true,
      target <<= target / "210",
      scalaSource in Test := baseDirectory.value / "src" / "test" / "2.10" / "scala",
      libraryDependencies ++= Seq(Dependencies.scalaTest, Dependencies.junit, Dependencies.logback)
      )
    )

  lazy val cotestsCommon211 = (
    Project("echo-cotests-common211", file("echo/cotests/common"))
      settings (defaultSettings: _*)
      settings(
      name := "echo-cotests-common",
      scalaVersion := Dependencies.scalaVersion,
      crossPaths := true,
      target <<= target / "211",
      scalaSource in Test := baseDirectory.value / "src" / "test" / "2.11" / "scala",
      libraryDependencies ++= Seq(Dependencies.scalaTest, Dependencies.junit, Dependencies.logback)
      )
    )

  lazy val cotestsTraceAkka22 = (
    Project("echo-cotests-trace-akka22", file("echo/cotests/trace/akka/2.2"))
      dependsOn (cotestsCommon210 % "test->test")
      dependsOn (traceAkka22)
      settings (defaultSettings: _*)
      settings (tracedTestSettingsScala210Protobuf24: _*)
      settings(
      name := "echo-cotests-trace-akka-2.2",
      scalaVersion := Dependencies.scala210Version,
      cotestProjectName := "echo-trace",
      javaOptions in Test += "-Dactivator.trace.enabled=true"
      )
    )

  lazy val cotestsTraceAkka23Scala210 = (
    Project("echo-cotests-trace-akka23-scala210", file("echo/cotests/trace/akka/2.3/2.10"))
      dependsOn (cotestsCommon210 % "test->test")
      dependsOn (traceAkka23Scala210)
      settings (defaultSettings: _*)
      settings (tracedTestSettingsScala210Protobuf25: _*)
      settings(
      name := "echo-cotests-trace-akka-2.3-scala-2.10",
      scalaVersion := Dependencies.scala210Version,
      cotestProjectName := "echo-trace",
      javaOptions in Test += "-Dactivator.trace.enabled=true"
      )
    )

  lazy val cotestsTraceAkka23Scala211 = (
    Project("echo-cotests-trace-akka23-scala211", file("echo/cotests/trace/akka/2.3/2.11"))
      dependsOn (cotestsCommon211 % "test->test")
      dependsOn (traceAkka23Scala211)
      settings (defaultSettings: _*)
      settings (tracedTestSettingsScala211Protobuf25: _*)
      settings(
      name := "echo-cotests-trace-akka-2.3-scala-2.11",
      scalaVersion := Dependencies.scalaVersion,
      cotestProjectName := "echo-trace",
      javaOptions in Test += "-Dactivator.trace.enabled=true"
      )
    )

  lazy val cotestsTracePlayCommon23Scala210 = (
    Project("echo-cotests-trace-play-common23-scala210", file("echo/cotests/trace/play/common"))
      dependsOn (Seq(cotestsCommon210 % "test->test", tracePlay23Scala210 % "test->test"): _*)
      settings (defaultSettings: _*)
      settings (tracedTestSettingsScala210Protobuf25: _*)
      settings(
      name := "echo-cotests-trace-play-common23-scala210",
      scalaVersion := Dependencies.scala210Version,
      libraryDependencies += Dependencies.play23ws,
      scalaSource in Test := baseDirectory.value / "src" / "test" / "play-2.3" / "scala",
      crossPaths := true,
      target <<= target / "play-23/scala-2.10"
      )
    )

  lazy val cotestsTracePlayCommon23Scala211 = (
    Project("echo-cotests-trace-play-common23-scala211", file("echo/cotests/trace/play/common"))
      dependsOn (cotestsCommon211 % "test->test")
      dependsOn (tracePlay23Scala211 % "test->test")
      settings (defaultSettings: _*)
      settings (tracedTestSettingsScala211Protobuf25: _*)
      settings(
      name := "echo-cotests-trace-play-common23-scala211",
      scalaVersion := Dependencies.scalaVersion,
      libraryDependencies += Dependencies.play23ws,
      scalaSource in Test := baseDirectory.value / "src" / "test" / "play-2.3" / "scala",
      crossPaths := true,
      target <<= target / "play-23/scala-2.11"
      )
    )

  lazy val cotestsTracePlay23Scala210 = (
    Project("echo-cotests-trace-play23-scala210", file("echo/cotests/trace/play/2.3.x/2.10"))
      dependsOn (cotestsTraceAkka23Scala210 % "test->test")
      dependsOn (tracePlay23Scala210 % "test->test")
      dependsOn (cotestsTracePlayCommon23Scala210 % "test->test")
      settings (defaultSettings: _*)
      settings (tracedTestSettingsScala210Protobuf25: _*)
      settings(
      name := "echo-cotests-trace-play-2.3.x-scala-2.10",
      scalaVersion := Dependencies.scala210Version,
      cotestProjectName := "echo-trace",
      javaOptions in Test ++= Seq(
        "-Dactivator.trace.enabled=true",
        "-Dactivator.trace.futures=off",
        "-Dactivator.trace.iteratees=on",
        "-Dactivator.trace.events.futures=off",
        "-Dactivator.trace.events.iteratees=on",
        "-Dactivator.trace.use-dispatcher-monitor=off",
        "-Dactivator.trace.play.traceable./get/filtered/*=off",
        "-Dactivator.trace.play.sampling./getSampled=3",
        "-Dactivator.trace.use-system-metrics-monitor=off"
      ),
      javaOptions in Test += ("-Datmos.integrationtest=" + System.getProperty("atmos.integrationtest", "off")),
      // javaOptions in Test ++= Seq("-verbose","-Xms768M","-Xmx2048M", "-Xss1500K"),
      // ignore deprecation warnings (intended usage of deprecated api)
      scalacOptions ~= {
        _ diff Seq("-deprecation")
      }
      )
    )

  lazy val cotestsTracePlay23Scala211 = (
    Project("echo-cotests-trace-play23-scala211", file("echo/cotests/trace/play/2.3.x/2.11"))
      dependsOn (cotestsTraceAkka23Scala211 % "test->test")
      dependsOn (tracePlay23Scala211 % "test->test")
      dependsOn (cotestsTracePlayCommon23Scala211 % "test->test")
      settings (defaultSettings: _*)
      settings (tracedTestSettingsScala211Protobuf25: _*)
      settings(
      name := "echo-cotests-trace-play-2.3.x-scala-2.11",
      scalaVersion := Dependencies.scalaVersion,
      cotestProjectName := "echo-trace",
      javaOptions in Test ++= Seq(
        "-Dactivator.trace.enabled=true",
        "-Dactivator.trace.futures=off",
        "-Dactivator.trace.iteratees=on",
        "-Dactivator.trace.events.futures=off",
        "-Dactivator.trace.events.iteratees=on",
        "-Dactivator.trace.use-dispatcher-monitor=off",
        "-Dactivator.trace.play.traceable./get/filtered/*=off",
        "-Dactivator.trace.play.sampling./getSampled=3",
        "-Dactivator.trace.use-system-metrics-monitor=off"
      ),
      javaOptions in Test += ("-Datmos.integrationtest=" + System.getProperty("atmos.integrationtest", "off")),
      // javaOptions in Test ++= Seq("-verbose","-Xms768M","-Xmx2048M", "-Xss1500K"),
      // ignore deprecation warnings (intended usage of deprecated api)
      scalacOptions ~= {
        _ diff Seq("-deprecation")
      }
      )
    )

  lazy val cotestsTrace2Akka22 = (
    Project("echo-cotests-trace2-akka22", file("echo/cotests/trace2/akka/2.2"))
      dependsOn (cotestsTraceAkka22 % "test->test")
      settings (defaultSettings: _*)
      settings (tracedTestSettingsScala210Protobuf24: _*)
      settings(
      name := "echo-cotests-trace2-akka-2.2",
      scalaVersion := Dependencies.scala210Version,
      cotestProjectName := "echo-trace2",
      javaOptions in Test += "-Dactivator.trace.enabled=true"
      )
    )

  lazy val cotestsTrace2Akka23Scala210 = (
    Project("echo-cotests-trace2-akka23-scala210", file("echo/cotests/trace2/akka/2.3/2.10"))
      dependsOn (cotestsTraceAkka23Scala210 % "test->test")
      settings (defaultSettings: _*)
      settings (tracedTestSettingsScala210Protobuf25: _*)
      settings(
      name := "echo-cotests-trace2-akka-2.3-scala-2.10",
      scalaVersion := Dependencies.scala210Version,
      cotestProjectName := "echo-trace2",
      javaOptions in Test += "-Dactivator.trace.enabled=true"
      )
    )

  lazy val cotestsTrace2Akka23Scala211 = (
    Project("echo-cotests-trace2-akka23-scala211", file("echo/cotests/trace2/akka/2.3/2.11"))
      dependsOn (cotestsTraceAkka23Scala211 % "test->test")
      settings (defaultSettings: _*)
      settings (tracedTestSettingsScala211Protobuf25: _*)
      settings(
      name := "echo-cotests-trace2-akka-2.3-scala-2.11",
      scalaVersion := Dependencies.scalaVersion,
      cotestProjectName := "echo-trace2",
      javaOptions in Test += "-Dactivator.trace.enabled=true"
      )
    )

  lazy val cotestsCollect = (
    Project("echo-cotests-collect", file("echo/cotests/collect"))
      dependsOn (cotestsCommon210 % "test->test")
      dependsOn (collect % "compile;test->test")
      settings (defaultSettings: _*)
      settings (
      cotestProjectName := "echo-collect"
      )
    )
}

object CollectProjects extends Build {

  import TraceProjects._
  import Dependencies._

  lazy val collect = (
    Project("echo-collect", file("echo/collect"))
      .doNotPublish
      dependsOn (event210Protobuf24)
      settings(
      sbt.Keys.scalaVersion := Dependencies.scala210Version,
      libraryDependencies ++= Seq(akkaSlf4j22, slf4j, logback, akkaTestKit22, scalaTest, junit)
      )
    )

  /**
  NOTE: This is an exact copy of the collect code from the project above.
  The only difference is that this project pulls in another version of Protobuf and Scala.
  Since we want to do both, Protobuf and Scala, we can't simply cross compile the project above.
    */
  lazy val collect211 = (
    Project("echo-collect211", file("echo/collect211"))
      dependsOn (event211Protobuf25)
      settings (defaultSettings: _*)
      settings(
      sbt.Keys.scalaVersion := Dependencies.scalaVersion,
      libraryDependencies ++= Seq(akkaSlf4j23, slf4j, logback, akkaTestKit23, scalaTest, junit)
      )
    )
}

object TraceProjects extends Build {

  lazy val aspectjSettings = SbtAspectj.aspectjSettings ++ inConfig(Aspectj)(Seq(
    AspectjKeys.compileOnly := true,
    AspectjKeys.lintProperties += "typeNotExposedToWeaver = ignore",
    products in Compile <++= products in Aspectj,
    AspectjKeys.ajc <<= AspectjKeys.ajc triggeredBy (compile in Compile)
  ))

  lazy val defaultSettings = EchoBuild.defaultSettings

  lazy val protocolProtobuf24 = (
    Project("echo-trace-protocol-protobuf24", file("echo/trace/protocol/2.4.x"))
      settings (defaultSettings: _*)
      settings(
      autoScalaLibrary := false,
      libraryDependencies += Dependencies.protobuf24
      )
    )

  lazy val protocolProtobuf25 = (
    Project("echo-trace-protocol-protobuf25", file("echo/trace/protocol/2.5.x"))
      settings (defaultSettings: _*)
      settings(
      autoScalaLibrary := false,
      libraryDependencies += Dependencies.protobuf25
      )
    )

  lazy val event210Protobuf24 = (
    Project("echo-event210-protobuf24", file("echo/trace/event"))
      dependsOn (protocolProtobuf24)
      settings (defaultSettings: _*)
      settings(
      scalaVersion := Dependencies.scala210Version,
      crossPaths := true,
      target <<= target / "210-protobuf24",
      libraryDependencies += Dependencies.config
      )
    )

  lazy val event210Protobuf25 = (
    Project("echo-event210-protobuf25", file("echo/trace/event"))
      dependsOn (protocolProtobuf25)
      settings (defaultSettings: _*)
      settings(
      scalaVersion := Dependencies.scala210Version,
      crossPaths := true,
      target <<= target / "210-protobuf25",
      libraryDependencies += Dependencies.config
      )
    )

  lazy val event211Protobuf24 = (
    Project("echo-event211-protobuf24", file("echo/trace/event"))
      dependsOn (protocolProtobuf24)
      settings (defaultSettings: _*)
      settings(
      scalaVersion := Dependencies.scalaVersion,
      crossPaths := true,
      target <<= target / "211-protobuf24",
      libraryDependencies += Dependencies.config
      )
    )

  lazy val event211Protobuf25 = (
    Project("echo-event211-protobuf25", file("echo/trace/event"))
      dependsOn (protocolProtobuf25)
      settings (defaultSettings: _*)
      settings(
      scalaVersion := Dependencies.scalaVersion,
      crossPaths := true,
      target <<= target / "211-protobuf25",
      libraryDependencies += Dependencies.config
      )
    )

  lazy val trace210Protobuf24 = (
    Project("echo-trace210-protobuf24", file("echo/trace/core"))
      dependsOn (event210Protobuf24)
      settings (defaultSettings: _*)
      settings(
      scalaVersion := Dependencies.scala210Version,
      crossPaths := true,
      target <<= target / "210-protobuf24",
      scalaSource in Test := baseDirectory.value / "src" / "test" / "2.10" / "scala",
      libraryDependencies ++= Seq(Dependencies.scalaTest, Dependencies.junit)
      )
    )

  lazy val trace210Protobuf25 = (
    Project("echo-trace210-protobuf25", file("echo/trace/core"))
      dependsOn (event210Protobuf25)
      settings (defaultSettings: _*)
      settings(
      scalaVersion := Dependencies.scala210Version,
      crossPaths := true,
      target <<= target / "210-protobuf25",
      scalaSource in Test := baseDirectory.value / "src" / "test" / "2.10" / "scala",
      libraryDependencies ++= Seq(Dependencies.scalaTest, Dependencies.junit)
      )
    )

  lazy val trace211Protobuf24 = (
    Project("echo-trace211-protobuf24", file("echo/trace/core"))
      dependsOn (event211Protobuf24)
      settings (defaultSettings: _*)
      settings(
      scalaVersion := Dependencies.scalaVersion,
      crossPaths := true,
      target <<= target / "211-protobuf24",
      scalaSource in Test := baseDirectory.value / "src" / "test" / "2.11" / "scala",
      libraryDependencies ++= Seq(Dependencies.scalaTest, Dependencies.junit)
      )
    )

  lazy val trace211Protobuf25 = (
    Project("echo-trace211-protobuf25", file("echo/trace/core"))
      dependsOn (event211Protobuf25)
      settings (defaultSettings: _*)
      settings(
      scalaVersion := Dependencies.scalaVersion,
      crossPaths := true,
      target <<= target / "211-protobuf25",
      scalaSource in Test := baseDirectory.value / "src" / "test" / "2.11" / "scala",
      libraryDependencies ++= Seq(Dependencies.scalaTest, Dependencies.junit)
      )
    )

  lazy val traceScala210Protobuf24 = (
    Project("echo-trace-scala210-protobuf24", file("echo/trace/scala/2.10.x"))
      dependsOn (trace210Protobuf24)
      settings (defaultSettings: _*)
      settings (aspectjSettings: _*)
      settings(
      name := "echo-trace-scala-protobuf24-" + Dependencies.scala210Version,
      normalizedName <<= name,
      target <<= target / "210-protobuf24",
      scalaVersion := Dependencies.scala210Version
      )
    )

  lazy val traceScala210Protobuf25 = (
    Project("echo-trace-scala210-protobuf25", file("echo/trace/scala/2.10.x"))
      dependsOn (trace210Protobuf25)
      settings (defaultSettings: _*)
      settings (aspectjSettings: _*)
      settings(
      name := "echo-trace-scala-protobuf25-" + Dependencies.scala210Version,
      normalizedName <<= name,
      target <<= target / "210-protobuf25",
      scalaVersion := Dependencies.scala210Version
      )
    )

  lazy val traceScala211Protobuf24 = (
    Project("echo-trace-scala211-protobuf24", file("echo/trace/scala/2.11.x"))
      dependsOn (trace211Protobuf24)
      settings (defaultSettings: _*)
      settings (aspectjSettings: _*)
      settings(
      name := "echo-trace-scala-protobuf24-" + Dependencies.scalaVersion,
      normalizedName <<= name,
      target <<= target / "211-protobuf24",
      scalaVersion := Dependencies.scalaVersion
      )
    )

  lazy val traceScala211Protobuf25 = (
    Project("echo-trace-scala211-protobuf25", file("echo/trace/scala/2.11.x"))
      dependsOn (trace211Protobuf25)
      settings (defaultSettings: _*)
      settings (aspectjSettings: _*)
      settings(
      name := "echo-trace-scala-protobuf25-" + Dependencies.scalaVersion,
      normalizedName <<= name,
      target <<= target / "211-protobuf25",
      scalaVersion := Dependencies.scalaVersion
      )
    )

  lazy val traceAkka22 = (
    Project("echo-trace-akka22", file("echo/trace/akka/2.2.x"))
      dependsOn (traceScala210Protobuf24)
      settings (defaultSettings: _*)
      settings (aspectjSettings: _*)
      settings(
      name := "echo-trace-akka-" + Dependencies.akka22Version,
      normalizedName <<= name,
      scalaVersion := Dependencies.scala210Version,
      crossPaths := true,
      libraryDependencies ++= Dependencies.traceAkka(Dependencies.akka22Version, CrossVersion.binary),
      ivyXML := Dependencies.traceAkkaExcludes
      )
    )

  lazy val traceAkka23Scala210 = (
    Project("echo-trace-akka23-scala210", file("echo/trace/akka/2.3.x"))
      dependsOn (traceScala210Protobuf25)
      settings (defaultSettings: _*)
      settings (aspectjSettings: _*)
      settings(
      name := "echo-trace-akka-" + Dependencies.akka23Version,
      normalizedName <<= name,
      scalaVersion := Dependencies.scala210Version,
      crossPaths := true,
      target <<= target / "210",
      libraryDependencies ++= Dependencies.traceAkka(Dependencies.akka23Version, CrossVersion.binary),
      ivyXML := Dependencies.traceAkkaExcludes
      )
    )

  lazy val traceAkka23Scala211 = (
    Project("echo-trace-akka23-scala211", file("echo/trace/akka/2.3.x"))
      dependsOn (traceScala211Protobuf25)
      settings (defaultSettings: _*)
      settings (aspectjSettings: _*)
      settings(
      name := "echo-trace-akka-" + Dependencies.akka23Version,
      normalizedName <<= name,
      scalaVersion := Dependencies.scalaVersion,
      crossPaths := true,
      target <<= target / "211",
      libraryDependencies ++= Dependencies.traceAkka(Dependencies.akka23Version, CrossVersion.binary),
      ivyXML := Dependencies.traceAkkaExcludes
      )
    )

  lazy val tracePlayCommon = (
    Project("echo-trace-play-common", file("echo/trace/play/common"))
      settings (defaultSettings: _*)
      settings (
      scalaVersion := Dependencies.scala210Version
      )
    )

  lazy val tracePlay23Scala210 = (
    Project("echo-trace-play23-scala210", file("echo/trace/play/2.3.x"))
      dependsOn(traceAkka23Scala210, tracePlayCommon)
      settings (defaultSettings: _*)
      settings (aspectjSettings: _*)
      settings(
      name := "echo-trace-play-" + Dependencies.play23Version,
      normalizedName <<= name,
      scalaVersion := Dependencies.scala210Version,
      crossPaths := true,
      target <<= target / "210",
      libraryDependencies ++= Seq(Dependencies.play23, Dependencies.playTest23),
      ivyXML := Dependencies.traceAkkaExcludes
      )
    )

  lazy val tracePlay23Scala211 = (
    Project("echo-trace-play23-scala211", file("echo/trace/play/2.3.x"))
      dependsOn(traceAkka23Scala211, tracePlayCommon)
      settings (defaultSettings: _*)
      settings (aspectjSettings: _*)
      settings(
      name := "echo-trace-play-" + Dependencies.play23Version,
      normalizedName <<= name,
      scalaVersion := Dependencies.scalaVersion,
      crossPaths := true,
      target <<= target / "211",
      libraryDependencies ++= Seq(Dependencies.play23, Dependencies.playTest23),
      ivyXML := Dependencies.traceAkkaExcludes
      )
    )
}
