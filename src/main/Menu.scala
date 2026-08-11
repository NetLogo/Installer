// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import javax.swing.JMenu
import javax.swing.plaf.basic.BasicMenuUI

class Menu(name: String, items: Array[MenuItem]) extends JMenu(name) with ThemeSync {
  private val menuUI = new MenuUI(this)

  setFont(items(0).getFont)
  setUI(menuUI)

  items.foreach(add)

  override def syncTheme(theme: ColorTheme): Unit = {
    menuUI.syncTheme(theme)

    items.foreach(_.syncTheme(theme))
  }
}

class MenuUI(menu: Menu) extends BasicMenuUI with ThemeSync {
  override def syncTheme(theme: ColorTheme): Unit = {
    menu.setForeground(theme.menuText)

    selectionBackground = theme.menuBackgroundHover
    selectionForeground = theme.menuTextHover
    acceleratorForeground = theme.menuText
    acceleratorSelectionForeground = theme.menuTextHover
    disabledForeground = theme.menuTextDisabled
  }
}
