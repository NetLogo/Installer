// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import com.dynatrace.hash4j.hashing.Hashing

import java.awt.{ BasicStroke, Color, Dimension, Graphics }
import java.io.File
import java.nio.file.{ Files, Paths }
import javax.swing.{ Box, BoxLayout, JLabel, JPanel }
import javax.swing.border.EmptyBorder

import org.apache.commons.compress.archivers.zip.ZipFile

import scala.sys.process.Process
import scala.util.{ Success, Try }

import ujson.Obj

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

  private val launchButton = new Button("Launch", () => launchApp())
  private val updateButton = new Button("Update", () => update())
  private val defaultButton = new Button("Set as Default", () => mainWindow.setDefault(this))
  private val uninstallButton = new Button("Uninstall", () => uninstall())

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
  add(Box.createHorizontalStrut(Utils.GapSize))
  add(defaultButton)
  add(Box.createHorizontalStrut(Utils.GapSize))
  add(uninstallButton)

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
      new OptionPane(mainWindow, "Error", s"Unable to launch ${config.name}.", Seq("OK"))
  }

  private def update(): Unit = {
    val files = Utils.listFilesRecursive(config.root).filterNot(_.isDirectory)

    val total = files.foldLeft(0L)(_ + _.length)
    var processed = 0

    var checksums = Map[String, String]()

    new Thread {
      override def run(): Unit = {
        files.foreach { file =>
          val path = file.toPath
          val relativePath = config.root.toPath.relativize(path).toString.replace("\\", "/")

          val bytes = Files.readAllBytes(path)

          checksums = checksums + (relativePath -> Hashing.xxh3_64.hashBytesToLong(bytes).toString)

          processed += bytes.size
        }
      }
    }.start()

    if (!new ProgressDialog(mainWindow, "Update", "Preparing for update...",
                            () => processed.toDouble / total, false).isCompleted)
      return

    var progress = 0.0

    new Thread {
      override def run(): Unit = {
        Request.file("get_updated_files", Obj(
          "os" -> Utils.os.name,
          "arch" -> Utils.arch,
          "version" -> config.version,
          "checksums" -> checksums
        )) match {
          case Success(file) =>
            val builder = new ZipFile.Builder

            builder.setFile(file)

            val input = builder.get

            val enumeration = input.getEntries

            while (enumeration.hasMoreElements) {
              val entry = enumeration.nextElement

              if (!entry.isDirectory) {
                val relativePath = Paths.get(entry.getName)
                val localPath = config.root.toPath.resolve(relativePath)

                localPath.toFile.getParentFile.mkdirs()

                val stream = input.getInputStream(entry)

                Files.write(localPath, stream.readAllBytes())

                stream.close()
              }
            }

            input.close()

            file.delete()

            progress = 1.0

          case _ =>
            new OptionPane(mainWindow, "Error", "Error downloading updated files from server.", Seq("OK"))
        }
      }
    }.start()

    if (new ProgressDialog(mainWindow, "Update", "Downloading updated files...", () => progress, true).isCompleted)
      setUpdatable(false)
  }

  private def uninstall(): Unit = {
    if (new OptionPane(mainWindow, "Uninstall", s"Are you sure you want to uninstall ${config.name}?",
                       Seq("Uninstall", "Cancel")).getSelectedIndex == 0) {
      val success = {
        try {
          deleteRecursive(config.root)
        } catch {
          case _: SecurityException => false
        }
      }

      if (success) {
        mainWindow.removeCard(this)
      } else {
        new OptionPane(mainWindow, "Error", s"Unable to delete ${config.name}.", Seq("OK"))
      }
    }
  }

  private def deleteRecursive(file: File): Boolean = {
    if (file.isDirectory) {
      file.listFiles.forall(deleteRecursive) && file.delete()
    } else {
      file.delete()
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
    defaultButton.syncTheme(theme)
    uninstallButton.syncTheme(theme)
  }
}
