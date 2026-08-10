// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import java.awt.{ Component, Dimension }

class HorizontalStrut(size: Int) extends Component {
  override def getPreferredSize: Dimension =
    new Dimension((size * Utils.getZoomLevel).toInt, 0)

  override def getMinimumSize: Dimension =
    getPreferredSize

  override def getMaximumSize: Dimension =
    getPreferredSize
}

class VerticalStrut(size: Int) extends Component {
  override def getPreferredSize: Dimension =
    new Dimension(0, (size * Utils.getZoomLevel).toInt)

  override def getMinimumSize: Dimension =
    getPreferredSize

  override def getMaximumSize: Dimension =
    getPreferredSize
}
