// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import java.io.InputStream
import java.nio.file.{ Files, Path, StandardOpenOption }

import scala.sys.process.Process

object Defaults {
  def setDefault(config: AppConfig): Boolean = {
    val path: Path = Files.createTempFile(null, null)

    path.toFile.setExecutable(true)
    path.toFile.deleteOnExit()

    Files.write(path, platformBinary.readAllBytes, StandardOpenOption.TRUNCATE_EXISTING)

    Process(Seq(path.toString, config.exec.getAbsolutePath, config.execThreed.getAbsolutePath)).! == 0
  }

  private def platformBinary: InputStream = {
    Utils.os match {
      case OS.Windows => getClass.getResourceAsStream(s"/defaults/windows/${Utils.arch}/defaults.exe")
      case OS.Mac => getClass.getResourceAsStream(s"/defaults/mac/${Utils.arch}/defaults")
      case OS.Linux => getClass.getResourceAsStream(s"/defaults/linux/${Utils.arch}/defaults")
    }
  }
}
