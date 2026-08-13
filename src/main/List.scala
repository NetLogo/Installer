// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import java.awt.Component
import javax.swing.{ Box, BoxLayout, JLabel, JList, JPanel, ListCellRenderer }

class List[T <: AnyRef](items: Array[T]) extends JList[T](items) with ThemeSync {
  private val renderer = new CellRenderer

  setCellRenderer(renderer)

  initTheme()

  override def syncTheme(theme: ColorTheme): Unit = {
    setBackground(theme.menuBackground)
    setSelectionBackground(theme.menuBackgroundHover)

    renderer.syncTheme(theme)

    items.foreach {
      case ts: ThemeSync =>
        ts.syncTheme(theme)

      case _ =>
    }
  }

  private class CellRenderer extends JPanel with ThemeSync with ListCellRenderer[T] {
    private val label = new JLabel

    setLayout(new BoxLayout(this, BoxLayout.X_AXIS))
    setBorder(new ZoomableBorder(Utils.GapSize / 2))

    add(label)
    add(Box.createHorizontalGlue)

    override def getListCellRendererComponent(list: JList[? <: T], value: T, index: Int, selected: Boolean,
                                              focus: Boolean): Component = {
      label.setText(value.toString)

      if (selected) {
        setBackground(list.getSelectionBackground)
      } else {
        setBackground(list.getBackground)
      }

      this
    }

    override def syncTheme(theme: ColorTheme): Unit = {
      label.setForeground(theme.menuText)
    }
  }
}
