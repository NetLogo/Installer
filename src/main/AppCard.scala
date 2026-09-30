// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import java.awt.{ BasicStroke, Color, Dimension, Graphics }
import java.awt.event.{ MouseAdapter, MouseEvent }
import java.io.File
import java.nio.file.{ Path, Paths }
import javax.swing.{ Box, BoxLayout, JLabel, JPanel }

import scala.sys.process.Process

class AppCard(val config: AppConfig, mainWindow: MainWindow)
  extends JPanel with Transparent with ThemeSync with Zoomable {

  private val platformFiles: String = Utils.os match {
    case OS.Windows => "File Explorer"
    case OS.Mac => "Finder"
    case OS.Linux => "Files"
  }

  private var backgroundColor: Color = Color.WHITE
  private var borderColor: Color = Color.WHITE
  private var borderHighlightColor: Color = Color.WHITE

  private val iconLabel = new JLabel(config.icon)

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

    add(new HorizontalStrut(Utils.GapSize))
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

  private val defaultItem = new MenuItem("Set as Default", () => mainWindow.setDefault(this))

  private val repairItem = new MenuItem("Repair", () => repair()) {
    setEnabled(false)
  }

  private val managePopup = new PopupMenu(Array(
    defaultItem,
    repairItem,
    new MenuItem("Uninstall", () => uninstall()),
    new MenuItem(s"View in $platformFiles", () => viewFiles())
  ))

  private val manageDropdown = new Dropdown("Manage", managePopup)

  setLayout(new BoxLayout(this, BoxLayout.X_AXIS))
  setBorder(new ZoomableBorder(Utils.GapSize))

  add(iconLabel)
  add(new HorizontalStrut(Utils.GapSize))

  add(new JPanel with Transparent {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS))

    add(nameLabel)
    add(defaultLabel)
  })

  add(new HorizontalStrut(Utils.GapSize))
  add(Box.createHorizontalGlue)
  add(launchButton)
  add(updatePanel)

  if (otherDropdown.count > 0) {
    add(new HorizontalStrut(Utils.GapSize))
    add(otherDropdown)
  }

  add(new HorizontalStrut(Utils.GapSize))
  add(manageDropdown)

  addMouseListener(new MouseAdapter {
    override def mousePressed(e: MouseEvent): Unit = {
      if (e.isPopupTrigger)
        managePopup.show(AppCard.this, e.getX, e.getY)
    }

    override def mouseReleased(e: MouseEvent): Unit = {
      if (e.isPopupTrigger)
        managePopup.show(AppCard.this, e.getX, e.getY)
    }
  })

  initTheme()

  def setDefault(default: Boolean): Unit = {
    defaultLabel.setVisible(default)
    defaultItem.setEnabled(!default)

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
    val version: String = mainWindow.latestVersion

    val newRoot: Path = {
      if (Utils.os == OS.Linux) {
        Paths.get(Utils.appRoot, s"NetLogo-$version")
      } else {
        Paths.get(Utils.appRoot, s"NetLogo $version")
      }
    }

    Install.verifyFiles(mainWindow, "Update", config)
      .flatMap(Install.getUpdates(mainWindow, "Update", version, _))
      .flatMap(Install.updateFromFiles(mainWindow, "Update", "Downloading updated files...", _, newRoot)) match {

      case result @ Right(_) =>
        Utils.deleteRecursive(config.root)

        val default: Boolean = isDefault

        mainWindow.removeCard(this)
        mainWindow.refreshInstallation(newRoot.toFile)

        if (default)
          mainWindow.setDefault(version)

        Analytics.sendNetLogoEvent(NetLogoEvent.Update, version, mainWindow.getChecksum(version), result)

        new OptionPane(mainWindow, "Update", "Update complete.", Array("OK"))

      case result @ Left(kind) =>
        setUpdatable(kind != Result.Noop)

        Analytics.sendNetLogoEvent(NetLogoEvent.Update, version, mainWindow.getChecksum(version), result)

        kind match {
          case Result.Failed(message) =>
            new OptionPane(mainWindow, "Error", message, Array("OK"))

          case _ =>
        }
    }
  }

  private def repair(): Unit = {
    Install.verifyFiles(mainWindow, "Repair", config)
      .flatMap(Install.getUpdates(mainWindow, "Repair", config.version, _))
      .flatMap(Install.updateFromFiles(mainWindow, "Repair", "Downloading repaired files...", _,
                                       config.root.toPath)) match {

      case result @ Right(_) =>
        mainWindow.refreshInstallation(config.root)

        Analytics.sendNetLogoEvent(NetLogoEvent.Repair, config.version, config.checksum, result)

        new OptionPane(mainWindow, "Repair", "Repair complete.", Array("OK"))

      case result @ Left(kind) =>
        Analytics.sendNetLogoEvent(NetLogoEvent.Repair, config.version, config.checksum, result)

        kind match {
          case Result.Failed(message) =>
            new OptionPane(mainWindow, "Error", message, Array("OK"))

          case _ =>
        }
    }
  }

  private def viewFiles(): Unit = {
    val open: String = {
      if (Utils.os == OS.Windows) {
        "explorer"
      } else {
        "open"
      }
    }

    Process(Seq(open, config.root.getAbsolutePath)).!
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

        Analytics.sendNetLogoEvent(NetLogoEvent.Uninstall, config.version, config.checksum, Right({}))
      } else {
        Analytics.sendNetLogoEvent(NetLogoEvent.Uninstall, config.version, config.checksum, Left(Result.Failed("")))

        new OptionPane(mainWindow, "Error", s"Failed to delete ${config.name}.", Array("OK"))
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
    g2d.fillRoundRect(0, 0, getWidth, getHeight, Utils.zoom(Utils.CornerDiameter), Utils.zoom(Utils.CornerDiameter))

    val stroke = g2d.getStroke

    if (defaultLabel.isVisible) {
      g2d.setStroke(new BasicStroke(Utils.zoomFloat(2), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND))
      g2d.setColor(borderHighlightColor)
    } else {
      g2d.setColor(borderColor)
    }

    g2d.drawRoundRect(0, 0, getWidth - 1, getHeight - 1, Utils.zoom(Utils.CornerDiameter),
                      Utils.zoom(Utils.CornerDiameter))
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

  override def zoom(): Unit = {
    iconLabel.setIcon(Utils.scaleIcon(config.icon, Utils.zoom(Utils.IconSize)))
  }
}
