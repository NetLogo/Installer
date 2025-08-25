lazy val root = project.in(file(".")).settings(
  name := "netlogo-installer",
  version := "0.1.0",
  organization := "org.nlogo",
  licenses += ("GPL-2.0", url("http://opensource.org/licenses/GPL-2.0")),

  scalaVersion := "3.7.0",

  Compile / fork := true,

  Compile / scalaSource := baseDirectory.value / "src" / "main",
  Compile / mainClass := Some("org.nlogo.installer.Main"),

  scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-encoding", "us-ascii", "-release", "11",
                        "-Xfatal-warnings", "-Wunused:linted"),

  javaOptions += "-Dapple.awt.application.appearance=system",

  resolvers += "jitpack" at "https://jitpack.io",
  libraryDependencies += "com.github.Dansoftowner" % "jSystemThemeDetector" % "3.9.1"
)
