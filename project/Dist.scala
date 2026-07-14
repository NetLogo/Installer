// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import java.io.File
import java.nio.file.{ Files, Path }
import java.util.UUID

import sbt.{ Compile, InputKey, Runtime, Setting }
import sbt.Keys.{ baseDirectory, dependencyClasspath, mainClass, packageBin, version }
import sbt.complete.{ DefaultParsers, Parser }, DefaultParsers._

import scala.sys.process.Process

object Dist {
  private val packageInstaller = InputKey[Unit]("packageInstaller", "Package the installer app for the specified platform")

  private val packageParser: Parser[Platform] = {
    def platformParser(name: String, arches: Set[String]): Parser[Platform] = {
      (name ~ (" " ~> StringBasic.examples(arches))).map {
        case (os, arch) => Platform(os, arch)
      }
    }

    " " ~> (platformParser("windows", Set("x64", "x86")) |
            platformParser("mac", Set("aarch64", "x86_64")) |
            platformParser("linux", Set("amd64", "i386")))
  }

  val settings: Seq[Setting[?]] = {
    packageInstaller := {
      val platform: Platform = packageParser.parsed

      val appVersion: String = (Compile / version).value
      val dist: Path = baseDirectory.value.toPath.resolve("dist")
      val config: Path = dist.resolve(platform.os)
      val input: Path = dist.resolve("input")
      val output: Path = dist.resolve("output")
      val name: String = "NetLogo Installer"
      var jar: Path = (Compile / packageBin).value.toPath
      val main: String = (Compile / mainClass).value.get

      deleteRecursive(input.toFile)
      deleteRecursive(output.toFile)

      Files.createDirectories(input)
      Files.copy(jar, input.resolve(jar.getFileName))

      (Runtime / dependencyClasspath).value.map(_.data).filter(_.isFile).foreach { file =>
        Files.copy(file.toPath, input.resolve(file.getName))
      }

      run(Seq("jpackage", "--type", "app-image", "--app-version", appVersion, "--name", name,
              "--dest", output.toString, "--vendor", "CCL", "--input", input.toString, "--main-jar", jar.toString,
              "--main-class", main))

      platform match {
        case Platform("windows", arch) =>
          packageWindows(arch, appVersion, dist, config, output, name)

        case Platform("mac", arch) =>
          packageMac(arch, appVersion, dist, config, output, name)

        case Platform("linux", arch) =>
          packageLinux(arch, appVersion, dist, config, output, name)

        case _ => throw new Exception(s"Invalid platform: $platform")
      }
    }
  }

  private def packageWindows(arch: String, version: String, dist: Path, config: Path, output: Path,
                             name: String): Unit = {
    val vs = new File("C:/Program Files (x86)/Windows Kits/10/bin")

    if (!vs.exists)
      throw new Exception("Visual Studio dev tools are not installed.")

    val mt: Path = vs.listFiles.filter(_.getName.startsWith("10.")).sorted.last.toPath.resolve(arch).resolve("mt.exe")
    val exe: Path = dist.getParent.relativize(output).resolve(name).resolve(s"$name.exe")

    Files.setAttribute(exe, "dos:readonly", false)

    run(Seq(mt.toString, "-manifest", config.resolve("Installer.manifest").toString, s"-outputresource:$exe;1"))
    run(Seq("wix", "build", "-arch", arch, "-src", config.resolve("Installer.wxs").toString,
            "-bindpath", output.resolve(name).toString, "-out", output.resolve(s"$name.msi").toString,
            "-pdbtype", "none") ++
          Seq(
            "-define", s"arch=$arch",
            "-define", s"version=$version",
            "-define", s"upgradeCode=${UUID.randomUUID.toString.toUpperCase}",
            "-define", s"root=${output.resolve(name).toString.replaceAll("\\\\", "\\\\\\\\")}",
            "-define", s"name=$name"
          ))
  }

  private def packageMac(arch: String, version: String, dist: Path, config: Path, output: Path, name: String): Unit = {
    run(Seq("create-dmg", "--volname", name, "--background", config.resolve("background.png").toString,
            "--window-size", "500", "375", "--icon", s"$name.app", "125", "137", "--hide-extension", s"$name.app",
            "--app-drop-link", "375", "137", output.resolve(s"$name.dmg").toString, output.toString))
  }

  private def packageLinux(arch: String, version: String, dist: Path, config: Path, output: Path,
                           name: String): Unit = {}

  private def deleteRecursive(file: File): Unit = {
    if (file.isDirectory)
      file.listFiles.foreach(deleteRecursive)

    file.delete()
  }

  private def run(command: Seq[String]): Unit = {
    val result = Process(command).!

    if (result != 0)
      throw new Exception("Command \"" + command(0) + "\" failed with exit code " + result + ".")
  }

  private case class Platform(os: String, arch: String)
}
