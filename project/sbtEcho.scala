import sbt._
import sbt.Keys._
import com.typesafe.sbt.SbtGit
import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import net.virtualvoid.sbt.cross.CrossPlugin

object SbtEchoBuild extends Build {
  def baseVersions: Seq[Setting[_]] = SbtGit.versionWithGit

  lazy val sbtEcho = (
    Project("sbt-echo", base = file("sbt-echo"))
      settings (noPublishSettings: _*)
      settings (defaultSettings: _*)
      aggregate(sbtEchoAkka, sbtEchoPlay)
    )

  lazy val defaultSettings: Seq[Setting[_]] = crossBuildSettings ++ baseVersions ++ SbtScalariform.scalariformSettings ++ Seq(
    sbtPlugin := true,
    organization := "com.typesafe.sbt",
    version <<= version in ThisBuild,
    publishMavenStyle := false,
    publishTo <<= isSnapshot { snapshot =>
      if (snapshot) Some(Classpaths.sbtPluginSnapshots) else Some(Classpaths.sbtPluginReleases)
    },
    sbt.CrossBuilding.latestCompatibleVersionMapper ~= {
      original => {
        case "0.13" => Dependencies.sbtVersion
        case x => original(x)
      }
    },
    ScalariformKeys.preferences in Compile := formatPrefs,
    ScalariformKeys.preferences in Test := formatPrefs
  )

  def formatPrefs = {
    import scalariform.formatter.preferences._
    FormattingPreferences()
      .setPreference(IndentSpaces, 2)
  }

  lazy val crossBuildSettings: Seq[Setting[_]] = CrossPlugin.crossBuildingSettings ++ CrossBuilding.scriptedSettings ++ Seq(
    CrossBuilding.crossSbtVersions := Seq("0.12", "0.13")
  )

  lazy val noPublishSettings: Seq[Setting[_]] = Seq(
    publish := {},
    publishLocal := {}
  )

  lazy val sbtEchoAkka = (
    Project("sbt-echo-akka", file("sbt-echo/akka"))
      settings (defaultSettings: _*)
      settings(
      name := "sbt-echo",
      libraryDependencies ++= Seq(Dependencies.aspectjTools, Dependencies.sbtBackgroundRun)
      )
    )

  lazy val sbtEchoPlay = (
    Project("sbt-echo-play", file("sbt-echo/play"))
      dependsOn (sbtEchoAkka)
      settings (defaultSettings: _*)
      settings (Dependencies.playPlugin: _*)
      settings (
      name := "sbt-echo-play"
      )
    )
}
