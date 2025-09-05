// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import java.io.File

object IconExt {
  Utils.os match {
    case OS.Windows =>
      System.load(new File("lib/iconext.dll").getAbsolutePath)

    case OS.Mac =>
      System.load(new File("lib/libiconext.dylib").getAbsolutePath)

    case _ =>
  }

  def extractIcon(path: String): ExtResult =
    new IconExt().extractIcon(path)
}

class IconExt {
  @native def extractIcon(path: String): ExtResult
}

case class ExtResult(pixels: Array[Int], width: Int, height: Int)
