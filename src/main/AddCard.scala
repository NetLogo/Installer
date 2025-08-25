// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import java.awt.{ BasicStroke, Color, Dimension, Graphics }
import java.awt.event.{ MouseAdapter, MouseEvent }
import javax.swing.JPanel

class AddCard extends JPanel with Transparent with ThemeSync with MouseActions {
  private var backgroundHoverColor: Color = Color.WHITE
  private var backgroundPressedColor: Color = Color.WHITE
  private var borderColor: Color = Color.WHITE

  addMouseListener(new MouseAdapter {
    override def mouseClicked(e: MouseEvent): Unit = {}
  })

  override def paintComponent(g: Graphics): Unit = {
    val g2d = Utils.initGraphics2D(g)

    if (hover) {
      if (pressed) {
        g2d.setColor(backgroundPressedColor)
      } else {
        g2d.setColor(backgroundHoverColor)
      }

      g2d.fillRoundRect(0, 0, getWidth, getHeight, Utils.CornerDiameter, Utils.CornerDiameter)
    }

    val stroke = g2d.getStroke

    g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0, Array(Utils.GapSize / 2f),
                                  Utils.GapSize / 2f))
    g2d.setColor(borderColor)
    g2d.drawRoundRect(0, 0, getWidth - 1, getHeight - 1, Utils.CornerDiameter, Utils.CornerDiameter)
    g2d.setStroke(stroke)

    g2d.drawLine(getWidth / 2 - getHeight / 8, getHeight / 2, getWidth / 2 + getHeight / 8, getHeight / 2)
    g2d.drawLine(getWidth / 2, getHeight / 2 - getHeight / 8, getWidth / 2, getHeight / 2 + getHeight / 8)

    super.paintComponent(g)
  }

  override def getMinimumSize: Dimension =
    new Dimension(super.getMinimumSize.width, getPreferredSize.height)

  override def getPreferredSize: Dimension =
    new Dimension(super.getPreferredSize.width, Utils.IconSize + Utils.GapSize * 2)

  override def getMaximumSize: Dimension =
    new Dimension(super.getMaximumSize.width, getPreferredSize.height)

  override def syncTheme(theme: ColorTheme): Unit = {
    backgroundHoverColor = theme.cardBackgroundHover
    backgroundPressedColor = theme.cardBackgroundPressed
    borderColor = theme.cardBorder
  }
}
