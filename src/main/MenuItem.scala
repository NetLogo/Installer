// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import java.awt.Cursor
import java.awt.event.ActionEvent
import javax.swing.{ AbstractAction, JMenuItem, KeyStroke }
import javax.swing.border.EmptyBorder
import javax.swing.plaf.basic.BasicMenuItemUI

class MenuItem(text: String, function: () => Unit, accelerator: Option[KeyStroke] = None)
  extends JMenuItem(new AbstractAction(text) {
    override def actionPerformed(e: ActionEvent): Unit = {
      function()
    }
  }) with ThemeSync {

  private val menuUI = new MenuItemUI

  setUI(menuUI)
  setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
  setBorder(new EmptyBorder(Utils.GapSize / 2, 0, Utils.GapSize / 2, 0))

  accelerator.foreach(setAccelerator)

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
