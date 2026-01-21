// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import java.awt.{ Color, Graphics }
import java.awt.event.ActionEvent
import javax.swing.{ AbstractAction, Action, Icon, JButton }
import javax.swing.border.EmptyBorder

class Button(action: Action) extends JButton(action) with Transparent with ThemeSync with MouseActions {
  def this(text: String, function: () => Unit) = this(new AbstractAction(text) {
    override def actionPerformed(e: ActionEvent): Unit = {
      function()
    }
  })

  def this(text: String, icon: Icon) = this(new AbstractAction(text, icon) {
    override def actionPerformed(e: ActionEvent): Unit = {}
  })

  private var backgroundColor: Color = Color.WHITE
  private var backgroundHoverColor: Color = Color.WHITE
  private var backgroundPressedColor: Color = Color.WHITE
  private var borderColor: Color = Color.WHITE

  setBorder(new EmptyBorder(Utils.GapSize / 4, Utils.GapSize, Utils.GapSize / 4, Utils.GapSize))
  setFocusable(false)
  setContentAreaFilled(false)

  initTheme()

  def setAction(function: () => Unit): Unit = {
    setAction(new AbstractAction(getText, getIcon) {
      override def actionPerformed(e: ActionEvent): Unit = {
        function()
      }
    })
  }

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
