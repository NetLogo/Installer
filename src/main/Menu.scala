// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import javax.swing.JMenu

class Menu(name: String, items: Array[MenuItem]) extends JMenu(name) with ThemeSync {
  items.foreach(add)

  override def syncTheme(theme: ColorTheme): Unit = {
    items.foreach(_.syncTheme(theme))
  }
}
