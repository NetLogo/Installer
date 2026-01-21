// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import java.awt.{ BorderLayout, Component, Dimension, Frame }
import javax.swing.{ Box, BoxLayout, JDialog, JLabel, JPanel }
import javax.swing.border.EmptyBorder

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

  add(new JPanel(new BorderLayout(Utils.GapSize, Utils.GapSize)) with Transparent {
    setBorder(new EmptyBorder(Utils.GapSize, Utils.GapSize, Utils.GapSize, Utils.GapSize))

    add(getContent(), BorderLayout.CENTER)
    add(new JPanel with Transparent {
      setLayout(new BoxLayout(this, BoxLayout.X_AXIS))

      add(Box.createHorizontalGlue)
      add(buttons.head)

      buttons.drop(1).foreach { button =>
        add(Box.createHorizontalStrut(Utils.GapSize))
        add(button)
      }

      add(Box.createHorizontalGlue)
    }, BorderLayout.SOUTH)
  })

  initTheme()
  pack()

  Utils.center(this, parent)

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

class ComboBoxOptionPane(parent: Frame, title: String, message: String, options: Array[String], buttons: Array[String])
  extends OptionPane(parent, title, message, buttons) {

  private lazy val dropdown = new ComboBox(options)

  override def getContent(): Component = {
    new JPanel with Transparent {
      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS))

      add(ComboBoxOptionPane.super.getContent())
      add(Box.createVerticalStrut(Utils.GapSize))
      add(new JPanel with Transparent {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS))

        add(Box.createHorizontalGlue)
        add(dropdown)
        add(Box.createHorizontalGlue)
      })
    }
  }

  def getSelectedOption: String =
    dropdown.getSelectedOption

  override def syncTheme(theme: ColorTheme): Unit = {
    super.syncTheme(theme)

    dropdown.syncTheme(theme)
  }
}
