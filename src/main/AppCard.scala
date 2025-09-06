// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import java.awt.{ BasicStroke, Color, Dimension, Graphics }
import javax.swing.{ Box, BoxLayout, JLabel, JPanel }
import javax.swing.border.EmptyBorder

import scala.sys.process.Process
import scala.util.Try

class AppCard(config: AppConfig, mainWindow: MainWindow) extends JPanel with Transparent with ThemeSync {
  private var backgroundColor: Color = Color.WHITE
  private var borderColor: Color = Color.WHITE
  private var borderHighlightColor: Color = Color.WHITE

  private val nameLabel = new JLabel(s"<html><b>${config.name}</b></html>") {
    setFont(getFont.deriveFont(14f))
  }

  private val defaultLabel = new JLabel("(current default)") {
    setVisible(false)
  }

  private val launchButton = new Button("Launch", () => launchApp())
  private val defaultButton = new Button("Set as Default", () => mainWindow.setDefault(this))
  private val uninstallButton = new Button("Uninstall", () => {})

  setLayout(new BoxLayout(this, BoxLayout.X_AXIS))
  setBorder(new EmptyBorder(Utils.GapSize, Utils.GapSize, Utils.GapSize, Utils.GapSize))

  add(new JLabel(config.icon))
  add(Box.createHorizontalStrut(Utils.GapSize))

  add(new JPanel with Transparent {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS))

    add(nameLabel)
    add(defaultLabel)
  })

  add(Box.createHorizontalStrut(Utils.GapSize))
  add(Box.createHorizontalGlue)
  add(launchButton)
  add(Box.createHorizontalStrut(Utils.GapSize))
  add(defaultButton)
  add(Box.createHorizontalStrut(Utils.GapSize))
  add(uninstallButton)

  initTheme()

  def setDefault(default: Boolean): Unit = {
    defaultLabel.setVisible(default)

    repaint()
  }

  private def launchApp(): Unit = {
    val success = Try(Utils.os match {
      case OS.Windows =>
        // no matter how you try to launch the exe on Windows, it blocks until the application is closed,
        // so we just have to start it in the background and hope it works. (Isaac B 9/5/25)
        Process(Seq(config.exec.getAbsolutePath)).run()

        true

      case OS.Mac =>
        Process(Seq("open", config.exec.getAbsolutePath)).! == 0

      case _ =>
        true
    }).getOrElse(false)

    if (!success)
      new OptionPane(mainWindow, "Error", s"Unable to launch ${config.name}.")
  }

  override def getMinimumSize: Dimension =
    getPreferredSize

  override def getMaximumSize: Dimension =
    new Dimension(super.getMaximumSize.width, getPreferredSize.height)

  override def paintComponent(g: Graphics): Unit = {
    val g2d = Utils.initGraphics2D(g)

    g2d.setColor(backgroundColor)
    g2d.fillRoundRect(0, 0, getWidth, getHeight, Utils.CornerDiameter, Utils.CornerDiameter)

    val stroke = g2d.getStroke

    if (defaultLabel.isVisible) {
      g2d.setStroke(new BasicStroke(2))
      g2d.setColor(borderHighlightColor)
    } else {
      g2d.setColor(borderColor)
    }

    g2d.drawRoundRect(0, 0, getWidth - 1, getHeight - 1, Utils.CornerDiameter, Utils.CornerDiameter)
    g2d.setStroke(stroke)

    super.paintComponent(g)
  }

  override def syncTheme(theme: ColorTheme): Unit = {
    backgroundColor = theme.cardBackground
    borderColor = theme.cardBorder
    borderHighlightColor = theme.cardBorderHighlight

    nameLabel.setForeground(theme.cardText)
    defaultLabel.setForeground(theme.cardText)

    launchButton.syncTheme(theme)
    defaultButton.syncTheme(theme)
    uninstallButton.syncTheme(theme)
  }
}
