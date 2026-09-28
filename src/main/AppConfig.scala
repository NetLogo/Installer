// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import java.io.File
import java.nio.file.Files
import javax.swing.ImageIcon

import scala.util.Try

case class AppConfig(name: String, version: String, icon: ImageIcon, root: File, exec: File, threed: Option[File],
                     bsearch: Option[File], hubNet: Option[File]) {
  val numericVersion: Int = Utils.numericVersion(version)
  val checksum: Option[String] = Try(Files.readString(root.toPath.resolve(".checksum")).trim).toOption
}
