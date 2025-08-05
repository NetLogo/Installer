// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import javax.swing.{ JFrame, WindowConstants }

class MainWindow extends JFrame {
  setTitle("NetLogo Installer")
  setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
  setSize(600, 400)

  val screenSize = getToolkit.getScreenSize

  setLocation(screenSize.width / 2 - getWidth / 2, screenSize.height / 2 - getHeight / 2)
}
