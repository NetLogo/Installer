// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import java.io.File
import javax.swing.ImageIcon

case class AppConfig(name: String, version: String, icon: ImageIcon, root: File) {
  def exec: File =
    root.toPath.resolve(s"NetLogo $version${Utils.os.exec}").toFile

  def execThreed: File =
    root.toPath.resolve(s"NetLogo 3D $version${Utils.os.exec}").toFile
}
