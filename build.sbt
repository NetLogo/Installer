// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

import org.nlogo.installer.Dist

lazy val root = project.in(file(".")).settings(
  name := "netlogo-installer",
  version := "0.1.0",
  organization := "org.nlogo",
  licenses += ("GPL-2.0", url("http://opensource.org/licenses/GPL-2.0")),

  scalaVersion := "3.7.0",

  Compile / fork := true,
  Compile / scalaSource := baseDirectory.value / "src" / "main",
  Compile / mainClass := Some("org.nlogo.installer.Main"),

  scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-encoding", "us-ascii", "-release", "21",
                        "-Xfatal-warnings", "-Wunused:linted"),

  javaOptions += "-Dapple.awt.application.appearance=system",

  resolvers += "jitpack" at "https://jitpack.io",

  libraryDependencies ++= Seq(
    "com.github.Dansoftowner" % "jSystemThemeDetector" % "3.9.1",
    "org.slf4j" % "slf4j-nop" % "2.0.13",
    "com.softwaremill.sttp.client4" %% "core" % "4.0.9",
    "com.softwaremill.sttp.client4" %% "upickle" % "4.0.9",
    "org.apache.commons" % "commons-compress" % "1.28.0",
    "com.dynatrace.hash4j" % "hash4j" % "0.28.0"
  )
).settings(Dist.settings: _*)
