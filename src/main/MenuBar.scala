// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import java.awt.event.{ InputEvent, KeyEvent }
import javax.swing.{ JMenuBar, KeyStroke }

class MenuBar(mainWindow: MainWindow) extends JMenuBar with ThemeSync {
  private val zoomMenu = new Menu("Zoom", Array(
    new MenuItem("Zoom In", () => mainWindow.zoom(0.5),
                 Option(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.META_DOWN_MASK))),
    new MenuItem("Zoom Out", () => mainWindow.zoom(-0.5),
                 Option(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.META_DOWN_MASK))),
    new MenuItem("Reset Zoom", () => mainWindow.setZoom(1),
                 Option(KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.META_DOWN_MASK)))
  ))

  add(zoomMenu)

  override def syncTheme(theme: ColorTheme): Unit = {
    zoomMenu.syncTheme(theme)
  }
}
