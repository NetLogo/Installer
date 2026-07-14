// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import java.awt.{ Adjustable, Color, Dimension, Graphics, Rectangle }
import javax.swing.{ JComponent, JButton, JScrollBar }
import javax.swing.plaf.basic.BasicScrollBarUI

class ScrollBar(orientation: Int) extends JScrollBar(orientation) with MouseActions with ThemeSync {
  private var backgroundColor: Color = Color.WHITE
  private var foregroundColor: Color = Color.WHITE
  private var foregroundHoverColor: Color = Color.WHITE

  setUnitIncrement(5)
  setUI(new ScrollBarUI)

  override def getPreferredSize: Dimension = {
    orientation match {
      case Adjustable.HORIZONTAL =>
        new Dimension(super.getPreferredSize.width, 8)

      case Adjustable.VERTICAL =>
        new Dimension(8, super.getPreferredSize.height)
    }
  }

  override def syncTheme(theme: ColorTheme): Unit = {
    backgroundColor = theme.scrollBarBackground
    foregroundColor = theme.scrollBarForeground
    foregroundHoverColor = theme.scrollBarForegroundHover
  }

  private class ScrollBarUI extends BasicScrollBarUI {
    override def createDecreaseButton(orientation: Int): JButton = {
      new JButton {
        setFocusable(false)

        override def getPreferredSize: Dimension =
          new Dimension(0, 0)
      }
    }

    override def createIncreaseButton(orientation: Int): JButton = {
      new JButton {
        setFocusable(false)

        override def getPreferredSize: Dimension =
          new Dimension(0, 0)
      }
    }

    override def paintTrack(g: Graphics, c: JComponent, bounds: Rectangle): Unit = {
      val g2d = Utils.initGraphics2D(g)

      g2d.setColor(backgroundColor)
      g2d.fillRect(bounds.x, bounds.y, bounds.width, bounds.height)
    }

    override def paintThumb(g: Graphics, c: JComponent, bounds: Rectangle): Unit = {
      val g2d = Utils.initGraphics2D(g)

      if (hover || pressed) {
        g2d.setColor(foregroundHoverColor)
      } else {
        g2d.setColor(foregroundColor)
      }

      val radius: Int = bounds.width.min(bounds.height)

      g2d.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, radius, radius)
    }
  }
}
