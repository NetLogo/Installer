// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import java.awt.{ Adjustable, Dimension }
import javax.swing.{ JComponent, JScrollPane }

import org.nlogo.installer.{ ScrollBar => NLScrollBar }

class ScrollPane(contents: JComponent, verticalPolicy: Int, horizontalPolicy: Int)
  extends JScrollPane(contents, verticalPolicy, horizontalPolicy) with ThemeSync {

  private val horizontalScrollBar: NLScrollBar = new NLScrollBar(Adjustable.HORIZONTAL)
  private val verticalScrollBar: NLScrollBar = new NLScrollBar(Adjustable.VERTICAL)

  setHorizontalScrollBar(horizontalScrollBar)
  setVerticalScrollBar(verticalScrollBar)
  setBorder(null)

  override def getMinimumSize: Dimension =
    new Dimension(contents.getMinimumSize.width, contents.getMinimumSize.height)

  override def syncTheme(theme: ColorTheme): Unit = {
    setBackground(theme.windowBackground)
    getViewport.setBackground(theme.windowBackground)

    horizontalScrollBar.syncTheme(theme)
    verticalScrollBar.syncTheme(theme)
  }
}
