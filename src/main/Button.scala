// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import java.awt.{ Color, Graphics }
import java.awt.event.ActionEvent
import javax.swing.{ AbstractAction, JButton }
import javax.swing.border.EmptyBorder

class Button(text: String, function: () => Unit) extends JButton(new AbstractAction(text) {
  override def actionPerformed(e: ActionEvent): Unit = {
    function()
  }
}) with Transparent with ThemeSync with MouseActions {
  private var backgroundColor: Color = Color.WHITE
  private var backgroundHoverColor: Color = Color.WHITE
  private var backgroundPressedColor: Color = Color.WHITE
  private var borderColor: Color = Color.WHITE

  setBorder(new EmptyBorder(Utils.GapSize / 4, Utils.GapSize, Utils.GapSize / 4, Utils.GapSize))
  setFocusable(false)
  setContentAreaFilled(false)

  override def paintComponent(g: Graphics): Unit = {
    val g2d = Utils.initGraphics2D(g)

    if (hover) {
      if (pressed) {
        g2d.setColor(backgroundPressedColor)
      } else {
        g2d.setColor(backgroundHoverColor)
      }
    } else {
      g2d.setColor(backgroundColor)
    }

    g2d.fillRoundRect(0, 0, getWidth, getHeight, Utils.CornerDiameter, Utils.CornerDiameter)

    g2d.setColor(borderColor)
    g2d.drawRoundRect(0, 0, getWidth - 1, getHeight - 1, Utils.CornerDiameter, Utils.CornerDiameter)

    super.paintComponent(g)
  }

  override def syncTheme(theme: ColorTheme): Unit = {
    backgroundColor = theme.buttonBackground
    backgroundHoverColor = theme.buttonBackgroundHover
    backgroundPressedColor = theme.buttonBackgroundPressed
    borderColor = theme.buttonBorder

    setForeground(theme.buttonText)
  }
}
