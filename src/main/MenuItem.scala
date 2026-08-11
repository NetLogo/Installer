// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import java.awt.{ Cursor, Font }
import java.awt.event.ActionEvent
import javax.swing.{ AbstractAction, JMenuItem, KeyStroke }
import javax.swing.plaf.basic.BasicMenuItemUI

class MenuItem(text: String, function: () => Unit, accelerator: Option[KeyStroke] = None)
  extends JMenuItem(new AbstractAction(text) {
    override def actionPerformed(e: ActionEvent): Unit = {
      function()
    }
  }) with ThemeSync {

  private lazy val menuUI = new MenuItemUI

  setUI(menuUI)
  setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
  setBorder(new ZoomableBorder(Utils.GapSize / 2, 0))

  accelerator.foreach(setAccelerator)

  initTheme()

  override def setFont(font: Font): Unit = {
    super.setFont(font)

    menuUI.setAcceleratorFont(font)
  }

  override def syncTheme(theme: ColorTheme): Unit = {
    setBackground(theme.menuBackground)
    setForeground(theme.menuText)

    menuUI.syncTheme(theme)
  }

  private class MenuItemUI extends BasicMenuItemUI with ThemeSync {
    initTheme()

    def setAcceleratorFont(font: Font): Unit = {
      acceleratorFont = font
    }

    override def syncTheme(theme: ColorTheme): Unit = {
      setForeground(theme.menuText)

      selectionBackground = theme.menuBackgroundHover
      selectionForeground = theme.menuTextHover
      acceleratorForeground = theme.menuText
      acceleratorSelectionForeground = theme.menuTextHover
      disabledForeground = theme.menuTextDisabled
    }
  }
}
