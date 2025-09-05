// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import java.awt.{ BorderLayout, Frame }
import javax.swing.{ Box, BoxLayout, JDialog, JLabel, JPanel }
import javax.swing.border.EmptyBorder

class OptionPane(parent: Frame, title: String, message: String) extends JDialog(parent, title, true) with ThemeSync {
  private val label = new JLabel(message)

  private val button = new Button("OK", () => {
    setVisible(false)
  }) {
    override def getMaximumSize: java.awt.Dimension =
      getMinimumSize

    override def getPreferredSize: java.awt.Dimension =
      getMinimumSize
  }

  add(new JPanel(new BorderLayout(Utils.GapSize, Utils.GapSize)) with Transparent {
    setBorder(new EmptyBorder(Utils.GapSize, Utils.GapSize, Utils.GapSize, Utils.GapSize))

    add(label, BorderLayout.CENTER)
    add(new JPanel with Transparent {
      setLayout(new BoxLayout(this, BoxLayout.X_AXIS))

      add(Box.createHorizontalGlue)
      add(button)
      add(Box.createHorizontalGlue)
    }, BorderLayout.SOUTH)
  })

  initTheme()
  pack()

  setLocation(parent.getX + parent.getWidth / 2 - getWidth / 2, parent.getY + parent.getHeight / 2 - getHeight / 2)

  setResizable(false)
  setAlwaysOnTop(true)
  setVisible(true)

  override def syncTheme(theme: ColorTheme): Unit = {
    getContentPane.setBackground(theme.windowBackground)

    label.setForeground(theme.windowText)

    button.syncTheme(theme)
  }
}
