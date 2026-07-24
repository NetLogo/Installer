// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import java.awt.{ BasicStroke, Color, Dimension, Graphics }
import java.io.File
import javax.swing.{ Box, BoxLayout, JLabel, JPanel }
import javax.swing.border.EmptyBorder

import scala.sys.process.Process

class AppCard(val config: AppConfig, mainWindow: MainWindow) extends JPanel with Transparent with ThemeSync {
  private var backgroundColor: Color = Color.WHITE
  private var borderColor: Color = Color.WHITE
  private var borderHighlightColor: Color = Color.WHITE

  private val nameLabel = new JLabel(s"<html><b>${config.name}</b></html>") {
    setFont(getFont.deriveFont(14f))
  }

  private val defaultLabel = new JLabel("(current default)") {
    setVisible(false)
  }

  private val launchButton = new Button("Launch", () => launchApp(config.exec))
  private val updateButton = new Button("Update", () => update())

  private val updatePanel = new JPanel with Transparent {
    setLayout(new BoxLayout(this, BoxLayout.X_AXIS))
    setVisible(false)

    add(Box.createHorizontalStrut(Utils.GapSize))
    add(updateButton)

    override def getMaximumSize: Dimension =
      this.getMinimumSize

    override def getPreferredSize: Dimension =
      this.getMinimumSize
  }

  private val otherDropdown = new Dropdown("Other Apps", Array(
    config.threed.map(app => new MenuItem("NetLogo 3D", () => launchApp(app))),
    config.bsearch.map(app => new MenuItem("BehaviorSearch", () => launchApp(app))),
    config.hubNet.map(app => new MenuItem("HubNet Client", () => launchApp(app))),
  ).flatten)

  private val repairItem = new MenuItem("Repair", () => repair()) {
    setEnabled(false)
  }

  private val manageDropdown = new Dropdown("Manage", Array(
    new MenuItem("Set as Default", () => mainWindow.setDefault(this)),
    repairItem,
    new MenuItem("Uninstall", () => uninstall())
  ))

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
  add(updatePanel)

  if (otherDropdown.count > 0) {
    add(Box.createHorizontalStrut(Utils.GapSize))
    add(otherDropdown)
  }

  add(Box.createHorizontalStrut(Utils.GapSize))
  add(manageDropdown)

  initTheme()

  def setDefault(default: Boolean): Unit = {
    defaultLabel.setVisible(default)

    repaint()
  }

  def isDefault: Boolean =
    defaultLabel.isVisible

  def setUpdatable(updatable: Boolean): Unit = {
    updatePanel.setVisible(updatable)
  }

  def setReparable(reparable: Boolean): Unit = {
    repairItem.setEnabled(reparable)
  }

  private def launchApp(app: File): Unit = {
    try {
      Utils.os match {
        case OS.Windows =>
          // no matter how you try to launch the exe on Windows, it blocks until the application is closed,
          // so we just have to start it in the background and hope it works. (Isaac B 9/5/25)
          Process(Seq(app.getAbsolutePath), config.root).run()

        case OS.Mac =>
          Process(Seq("open", "-n", app.getAbsolutePath)).!!

        case OS.Linux =>
          // Linux behaves similarly to Windows, so we do the same thing here. (Isaac B 7/14/26)
          Process(Seq(app.getAbsolutePath)).run()
      }
    } catch {
      case _ =>
        new OptionPane(mainWindow, "Error", s"Unable to launch ${app.getName}.", Array("OK"))
    }
  }

  private def update(): Unit = {
    Install.verifyFiles(mainWindow, "Update", config.root).flatMap {
      Install.getUpdates(mainWindow, "Update", config.version, _)
    }.foreach { updates =>
      if (Install.updateFromFiles(mainWindow, "Update", "Downloading updated files...", updates, config.root.toPath)) {
        new OptionPane(mainWindow, "Update", "Update complete.", Array("OK"))

        setUpdatable(false)
      } else {
        setUpdatable(true)
      }
    }
  }

  private def repair(): Unit = {
    Install.verifyFiles(mainWindow, "Repair", config.root).flatMap {
      Install.getUpdates(mainWindow, "Repair", config.version, _)
    }.foreach { updates =>
      if (Install.updateFromFiles(mainWindow, "Repair", "Downloading repaired files...", updates, config.root.toPath))
        new OptionPane(mainWindow, "Repair", "Repair complete.", Array("OK"))
    }
  }

  private def uninstall(): Unit = {
    if (new OptionPane(mainWindow, "Uninstall", s"Are you sure you want to uninstall ${config.name}?",
                       Array("Uninstall", "Cancel")).getSelectedIndex == 0) {
      val success = {
        try {
          Utils.deleteRecursive(config.root)
        } catch {
          case _: SecurityException => false
        }
      }

      if (success) {
        mainWindow.removeCard(this)
      } else {
        new OptionPane(mainWindow, "Error", s"Unable to delete ${config.name}.", Array("OK"))
      }
    }
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
    updateButton.syncTheme(theme)

    manageDropdown.syncTheme(theme)
    otherDropdown.syncTheme(theme)
  }
}
