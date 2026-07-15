// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import java.io.File
import javax.swing.ImageIcon

case class AppConfig(name: String, version: String, icon: ImageIcon, root: File) {
  val exec: File = {
    if (Utils.os == OS.Mac) {
      root.toPath.resolve(s"NetLogo $version.app").toFile
    } else {
      root.toPath.resolve(s"NetLogo${Utils.os.exec}").toFile
    }
  }

  val execThreed: File = {
    if (Utils.os == OS.Mac) {
      root.toPath.resolve(s"NetLogo 3D $version.app").toFile
    } else {
      root.toPath.resolve(s"NetLogo 3D${Utils.os.exec}").toFile
    }
  }
}
