// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import java.awt.{ Color, Component, Cursor, Graphics }
import java.awt.event.ActionEvent
import javax.swing.{ AbstractAction, Icon, JMenuItem, JPopupMenu, SwingConstants }
import javax.swing.border.EmptyBorder
import javax.swing.plaf.basic.BasicMenuItemUI

class Dropdown(title: String, items: Array[MenuItem]) extends Button(title, new DropdownArrow) {
  private val menu = new PopupMenu {
    items.foreach(add)
  }

  setHorizontalTextPosition(SwingConstants.LEFT)
  setIconTextGap(Utils.GapSize)
  setAction(() => menu.show(this, 0, getHeight))

  def count: Int =
    items.size
}

class ComboBox(options: Array[String]) extends Button(options.head, new DropdownArrow) {
  private var selectedOption: String = options.head

  private val menu = new PopupMenu {
    options.foreach(option => add(new MenuItem(option, () => select(option))))
  }

  setHorizontalTextPosition(SwingConstants.LEFT)
  setIconTextGap(Utils.GapSize)
  setAction(() => menu.show(this, 0, getHeight))

  def getSelectedOption: String =
    selectedOption

  private def select(option: String): Unit = {
    selectedOption = option

    setText(option)
  }
}

class DropdownArrow extends Icon with ThemeSync {
  private var color: Color = Color.WHITE

  override def getIconWidth: Int = 9
  override def getIconHeight: Int = 5

  override def paintIcon(c: Component, g: Graphics, x: Int, y: Int): Unit = {
    val g2d = Utils.initGraphics2D(g)

    g2d.setColor(color)
    g2d.drawLine(x, y, x + getIconWidth / 2, y + getIconHeight - 1)
    g2d.drawLine(x + getIconWidth / 2, y + getIconHeight - 1, x + getIconWidth - 1, y)
  }

  override def syncTheme(theme: ColorTheme): Unit = {
    color = theme.buttonText
  }
}

class PopupMenu extends JPopupMenu with ThemeSync {
  initTheme()

  override def syncTheme(theme: ColorTheme): Unit = {
    setBackground(theme.menuBackground)
  }
}

class MenuItem(text: String, function: () => Unit) extends JMenuItem(new AbstractAction(text) {
  override def actionPerformed(e: ActionEvent): Unit = {
    function()
  }
}) with ThemeSync {

  private val menuUI = new MenuItemUI

  setUI(menuUI)
  setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
  setBorder(new EmptyBorder(Utils.GapSize / 2, 0, Utils.GapSize / 2, 0))

  initTheme()

  override def syncTheme(theme: ColorTheme): Unit = {
    setBackground(theme.menuBackground)
    setForeground(theme.menuText)

    menuUI.syncTheme(theme)
  }

  private class MenuItemUI extends BasicMenuItemUI with ThemeSync {
    initTheme()

    override def syncTheme(theme: ColorTheme): Unit = {
      selectionBackground = theme.menuBackgroundHover
    }
  }
}
