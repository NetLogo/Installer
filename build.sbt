// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

import java.nio.file.Paths

import org.nlogo.installer.Dist

lazy val root = project.in(file(".")).settings(
  name := "netlogo-installer",
  version := "1.0.0",
  organization := "org.nlogo",
  licenses += ("GPL-2.0", url("http://opensource.org/licenses/GPL-2.0")),

  scalaVersion := "3.7.0",

  Compile / fork := true,
  Compile / scalaSource := baseDirectory.value / "src" / "main",
  Compile / mainClass := Some("org.nlogo.installer.Main"),

  scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-encoding", "us-ascii", "-release", "21",
                        "-Xfatal-warnings", "-Wunused:linted"),

  // silence warnings from auto-generated ScalaPB code (Isaac B 9/28/26)
  scalacOptions += {
    val current = Paths.get(".").toAbsolutePath
    val managed = (Compile / sourceManaged).value.toPath
    val relative = current.relativize(managed).toString.replace("\\", "/")

    s"-Wconf:src=$relative/.*:silent"
  },

  javaOptions ++= Seq(
    "-Dapple.awt.application.appearance=system",
    "-Dapple.laf.useScreenMenuBar=true",
    s"-Dinstaller.version=${version.value}"
  ) ++ {
    if (System.getProperty("os.name").toLowerCase.startsWith("mac")) {
      Some("-Xdock:name=NetLogo Installer")
    } else {
      None
    }
  },

  Compile / PB.protoSources := Seq((Compile / resourceDirectory).value / "protobuf"),
  Compile / PB.targets := Seq(scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"),

  resolvers += "jitpack" at "https://jitpack.io",

  libraryDependencies ++= Seq(
    "com.github.Dansoftowner" % "jSystemThemeDetector" % "3.9.1",
    "org.slf4j" % "slf4j-nop" % "2.0.13",
    "com.softwaremill.sttp.client4" %% "core" % "4.0.9",
    "com.softwaremill.sttp.client4" %% "upickle" % "4.0.9",
    "org.apache.commons" % "commons-compress" % "1.28.0",
    "com.dynatrace.hash4j" % "hash4j" % "0.28.0",
    "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
  )
).settings(Dist.settings: _*)
