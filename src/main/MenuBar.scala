// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import java.awt.{ Color, Graphics }
import java.awt.event.KeyEvent
import javax.swing.{ JMenuBar, KeyStroke }

class MenuBar(mainWindow: MainWindow) extends JMenuBar with ThemeSync {
  private var backgroundColor: Color = Color.WHITE
  private var borderColor: Color = Color.WHITE

  private val zoomMenu = new Menu("Zoom", Array(
    new MenuItem("Zoom In", () => mainWindow.zoom(0.5),
                 Option(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, Utils.platformCtrl))),
    new MenuItem("Zoom Out", () => mainWindow.zoom(-0.5),
                 Option(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, Utils.platformCtrl))),
    new MenuItem("Reset Zoom", () => mainWindow.setZoom(1, Utils.getZoomLevel * Utils.getUIScale),
                 Option(KeyStroke.getKeyStroke(KeyEvent.VK_0, Utils.platformCtrl)))
  ))

  add(zoomMenu)

  override def paintComponent(g: Graphics): Unit = {
    val g2d = Utils.initGraphics2D(g)

    g2d.setColor(backgroundColor)
    g2d.fillRect(0, 0, getWidth, getHeight)
  }

  override def paintBorder(g: Graphics): Unit = {
    val g2d = Utils.initGraphics2D(g)

    g2d.setColor(borderColor)
    g2d.drawLine(0, getHeight - 1, getWidth, getHeight - 1)
  }

  override def syncTheme(theme: ColorTheme): Unit = {
    backgroundColor = theme.menuBackground
    borderColor = theme.menuBorder

    zoomMenu.syncTheme(theme)
  }
}
