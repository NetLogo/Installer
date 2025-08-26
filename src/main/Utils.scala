// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import java.awt.{ Color, Cursor, Graphics, Graphics2D, RenderingHints }
import java.awt.event.{ MouseAdapter, MouseEvent }
import javax.swing.JComponent

object Utils {
  val GapSize = 12
  val CornerDiameter = 8
  val IconSize = 64

  val os: OS = {
    val name = System.getProperty("os.name").toLowerCase

    if (name.startsWith("win")) {
      OS.Windows
    } else if (name.startsWith("mac")) {
      OS.Mac
    } else {
      OS.Linux
    }
  }

  def initGraphics2D(g: Graphics): Graphics2D = {
    val g2d = g.asInstanceOf[Graphics2D]

    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

    g2d
  }

  def numericVersion(name: String): Int = {
    val versionRegex = """NetLogo (\d+).(\d+).(\d+)(?:-(?:beta|rc)(\d+))?""".r

    try {
      name match {
        case versionRegex(major, minor, patch, modifier) =>
          major.toInt * 1000000 + minor.toInt * 10000 * patch.toInt * 100 + Option(modifier).fold(0)(_.toInt - 100)
      }
    } catch {
      case _: Throwable =>
        0
    }
  }
}

sealed abstract trait OS

object OS {
  case object Windows extends OS
  case object Mac extends OS
  case object Linux extends OS
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
