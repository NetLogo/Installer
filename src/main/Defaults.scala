// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import java.nio.file.{ Files, Path, StandardOpenOption }

import scala.sys.process.Process

object Defaults {
  def setDefault(config: AppConfig): Boolean = {
    val platformPath: String = s"/defaults/${Utils.os.name}/${Utils.arch}/defaults${Utils.os.bin}"

    Option(getClass.getResourceAsStream(platformPath)).fold(false) { stream =>
      val path: Path = Files.createTempFile(null, Utils.os.bin)

      path.toFile.setExecutable(true)
      path.toFile.deleteOnExit()

      Files.write(path, stream.readAllBytes, StandardOpenOption.TRUNCATE_EXISTING)

      stream.close()

      Utils.os match {
        case OS.Windows =>
          Process(Seq(path.toString, config.root.getAbsolutePath, config.version)).! == 0

        case _ =>
          Process(Seq(path.toString, config.exec.getAbsolutePath, config.threed.fold("")(_.getAbsolutePath))).! == 0
      }
    }
  }
}
