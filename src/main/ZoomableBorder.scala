// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import java.awt.{ Component, Graphics, Insets }
import javax.swing.border.Border

class ZoomableBorder(top: Int, left: Int, bottom: Int, right: Int) extends Border {
  def this(vertical: Int, horizontal: Int) = this(vertical, horizontal, vertical, horizontal)
  def this(size: Int) = this(size, size, size, size)

  override def getBorderInsets(c: Component): Insets = {
    new Insets((top * Utils.getZoomLevel).toInt, (left * Utils.getZoomLevel).toInt,
               (bottom * Utils.getZoomLevel).toInt, (right * Utils.getZoomLevel).toInt)
  }

  override def isBorderOpaque: Boolean =
    false

  override def paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int): Unit = {}
}
