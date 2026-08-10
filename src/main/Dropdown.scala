// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import java.awt.{ Color, Component, Graphics }
import javax.swing.{ Icon, JPopupMenu, SwingConstants }

class Dropdown(title: String, items: Array[MenuItem]) extends Button(title, new DropdownArrow) with Zoomable {
  private val menu = new PopupMenu(items)

  setHorizontalTextPosition(SwingConstants.LEFT)
  setAction(() => menu.show(this, 0, getHeight))

  def count: Int =
    items.size

  override def zoom(): Unit = {
    setIconTextGap(Utils.zoom(Utils.GapSize))
  }
}

class ComboBox(options: Array[String]) extends Button(options.head, new DropdownArrow) {
  private var selectedOption: String = options.head

  private val menu = new PopupMenu(options.map(option => new MenuItem(option, () => select(option))))

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

  override def getIconWidth: Int = Utils.zoom(9)
  override def getIconHeight: Int = Utils.zoom(5)

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

class PopupMenu(items: Array[MenuItem]) extends JPopupMenu with ThemeSync {
  private var lastZoom: Float = Utils.getZoomLevel

  items.foreach(add)

  Utils.zoomComponents(this, Utils.getZoomLevel, 1)

  initTheme()

  override def setVisible(visible: Boolean): Unit = {
    if (visible) {
      Utils.zoomComponents(this, Utils.getZoomLevel, lastZoom)

      lastZoom = Utils.getZoomLevel
    }

    super.setVisible(visible)
  }

  override def syncTheme(theme: ColorTheme): Unit = {
    setBackground(theme.menuBackground)
  }
}
