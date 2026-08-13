// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import java.awt.{ Component, Dimension, Frame }
import javax.swing.{ Box, BoxLayout, JDialog, JLabel, JPanel }

class OptionPane(parent: Frame, title: String, message: String, options: Array[String])
  extends JDialog(parent, title, true) with ThemeSync {

  private var selectedIndex = -1

  protected val label = new JLabel(message)

  private val buttons = options.zipWithIndex.map { (option, index) =>
    new Button(option, () => {
      selectedIndex = index

      setVisible(false)
    }) {
      override def getMaximumSize: Dimension =
        getMinimumSize

      override def getPreferredSize: Dimension =
        getMinimumSize
    }
  }

  add(new JPanel with Transparent {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS))
    setBorder(new ZoomableBorder(Utils.GapSize))

    add(getContent())
    add(new VerticalStrut(Utils.GapSize))
    add(new JPanel with Transparent {
      setLayout(new BoxLayout(this, BoxLayout.X_AXIS))

      add(Box.createHorizontalGlue)
      add(buttons.head)

      buttons.drop(1).foreach { button =>
        add(new HorizontalStrut(Utils.GapSize))
        add(button)
      }

      add(Box.createHorizontalGlue)
    })
  })

  Utils.zoomComponents(this, 1)

  initTheme()
  pack()

  Utils.center(this, parent)
  Utils.addEscAction(getRootPane, () => setVisible(false))

  setResizable(false)
  setAlwaysOnTop(true)
  setVisible(true)

  def getSelectedIndex: Int =
    selectedIndex

  protected def getContent(): Component = {
    new JPanel with Transparent {
      setLayout(new BoxLayout(this, BoxLayout.X_AXIS))

      add(Box.createHorizontalGlue)
      add(label)
      add(Box.createHorizontalGlue)
    }
  }

  override def syncTheme(theme: ColorTheme): Unit = {
    getContentPane.setBackground(theme.windowBackground)

    label.setForeground(theme.windowText)

    buttons.foreach(_.syncTheme(theme))
  }
}

class CustomOptionPane(parent: Frame, title: String, message: String, content: Component, options: Array[String])
  extends OptionPane(parent, title, message, options) {

  override def getContent(): Component = {
    new JPanel with Transparent {
      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS))

      add(CustomOptionPane.super.getContent())
      add(new VerticalStrut(Utils.GapSize))
      add(content)
    }
  }
}
