// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import java.awt.{ Color, Cursor, Graphics, Graphics2D, RenderingHints }
import java.awt.event.{ MouseAdapter, MouseEvent }
import javax.swing.JComponent

object Utils {
  val GapSize = 12
  val CornerDiameter = 8
  val IconSize = 64

  def initGraphics2D(g: Graphics): Graphics2D = {
    val g2d = g.asInstanceOf[Graphics2D]

    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

    g2d
  }
}

trait Transparent extends JComponent {
  setOpaque(false)
  setBackground(new Color(0, 0, 0, 0))
}

trait MouseActions extends JComponent {
  protected var hover = false
  protected var pressed = false

  setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))

  addMouseListener(new MouseAdapter {
    override def mouseEntered(e: MouseEvent): Unit = {
      hover = true

      repaint()
    }

    override def mouseExited(e: MouseEvent): Unit = {
      hover = false

      repaint()
    }

    override def mousePressed(e: MouseEvent): Unit = {
      pressed = true

      repaint()
    }

    override def mouseReleased(e: MouseEvent): Unit = {
      pressed = false

      repaint()
    }
  })
}
