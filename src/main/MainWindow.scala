// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import java.awt.{ Dimension, Image }
import java.awt.image.BufferedImage
import java.io.File
import java.nio.channels.ClosedByInterruptException
import java.nio.file.{ Files, Paths }
import java.nio.file.attribute.PosixFilePermission
import java.util.HashSet
import javax.imageio.ImageIO
import javax.swing.{ Box, BoxLayout, ImageIcon, JFrame, JLabel, JPanel, JScrollPane, ScrollPaneConstants,
                     WindowConstants }
import javax.swing.border.EmptyBorder

import org.apache.commons.compress.archivers.zip.ZipFile

import scala.util.Success

import ujson.Obj

class MainWindow extends JFrame with ThemeSync {
  private var availableVersions = Map[String, String]()

  private val title = new JLabel("<html><b>Installed Versions</b></html>") {
    setFont(getFont.deriveFont(24f))
  }

  private var cards = Seq[AppCard]()

  private val addCard = new AddCard(this)

  private val cardPanel = new JPanel with Transparent {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS))
  }

  private val contents = new JPanel with Transparent {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS))
    setBorder(new EmptyBorder(Utils.GapSize, Utils.GapSize, Utils.GapSize, Utils.GapSize))

    add(new JPanel with Transparent {
      setLayout(new BoxLayout(this, BoxLayout.X_AXIS))

      add(title)
      add(Box.createHorizontalGlue)
    })

    add(cardPanel)
    add(Box.createVerticalStrut(Utils.GapSize))
    add(addCard)
  }

  private val scrollPane = new JScrollPane(contents, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                           ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER) {
    setBorder(null)

    override def getMinimumSize: Dimension =
      new Dimension(contents.getMinimumSize.width, contents.getMinimumSize.height)
  }

  locally {
    setTitle("NetLogo Installer")
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)

    add(scrollPane)

    setMinimumSize(scrollPane.getMinimumSize)
    setSize(700, Utils.IconSize * 4 + Utils.GapSize * 13)

    val screenSize = getToolkit.getScreenSize

    setLocation(screenSize.width / 2 - getWidth / 2, screenSize.height / 2 - getHeight / 2)

    findInstalled()
    getAvailableVersions()

    cards.foreach { card =>
      availableVersions.get(card.config.version).foreach { expected =>
        val checksumPath = card.config.root.toPath.resolve(".checksum")

        if (Files.exists(checksumPath))
          card.setUpdatable(expected != Files.readString(checksumPath).trim)
      }
    }

    cards.headOption.foreach(_.setDefault(true))

    initTheme()
  }

  def setDefault(default: AppCard): Unit = {
    cards.foreach(card => card.setDefault(card == default))
  }

  def removeCard(card: AppCard): Unit = {
    cards = cards.filter(_ != card)

    if (card.isDefault)
      cards.headOption.foreach(_.setDefault(true))

    refreshCardPanel()
  }

  def addInstallation(): Unit = {
    new OptionPane(this, "Add Installation", "Add an existing installation or download a new version?",
                   Array("Download new", "Add existing")).getSelectedIndex match {
      case 0 =>
        val optionPane = new ComboBoxOptionPane(
          this, "Select Version", "Select the version you would like to download.",
          availableVersions.keys.toArray.sortBy(Utils.numericVersion), Array("Download", "Cancel")
        )

        if (optionPane.getSelectedIndex == 0)
          install(optionPane.getSelectedOption)

      case 1 =>
        println("existing")

      case _ =>
    }
  }

  private def install(version: String): Unit = {
    val root = Paths.get(Utils.appRoot, s"NetLogo $version").toFile

    if (root.exists) {
      new OptionPane(this, "Error", s"NetLogo $version is already installed.", Array("OK"))
    } else {
      var progress = -1.0
      var bytes = Array[Byte]()

      var thread = new Thread {
        override def run(): Unit = {
          try {
            Request.file("get_version", Obj(
              "os" -> Utils.os.name,
              "arch" -> Utils.arch,
              "version" -> version
            ), progress = _) match {
              case Success(result) =>
                bytes = result

                progress = 1.0

              case _ =>
                progress = 1.0

                new OptionPane(MainWindow.this, "Error", "Error downloading files from server.", Array("OK"))
            }
          } catch {
            case _: InterruptedException | _: ClosedByInterruptException =>
          }
        }
      }

      thread.start()

      if (!new ProgressDialog(this, "Install", s"Downloading NetLogo $version...",
                             () => progress).isCompleted) {
        thread.interrupt()
        thread.join()

        return
      }

      thread.join()

      progress = 0.0

      thread = new Thread {
        override def run(): Unit = {
          try {
            updateFromZip(bytes, root, value => {
              if (isInterrupted)
                throw new InterruptedException

              progress = value
            })

            progress = 1.0
          } catch {
            case _: InterruptedException =>
          }
        }
      }

      thread.start()

      if (new ProgressDialog(this, "Install", s"Installing NetLogo $version...", () => progress).isCompleted) {
        findInstalled()

        thread.join()
      } else {
        thread.interrupt()
        thread.join()

        Utils.deleteRecursive(root)
      }
    }
  }

  def updateFromZip(bytes: Array[Byte], dest: File, setProgress: Double => Unit = _ => {}): Unit = {
    val builder = new ZipFile.Builder

    builder.setByteArray(bytes)

    val input = builder.get

    var processed = 0L

    input.stream.forEach { entry =>
      if (!entry.isDirectory) {
        val relativePath = Paths.get(entry.getName)
        val localPath = dest.toPath.resolve(relativePath)

        localPath.toFile.getParentFile.mkdirs()

        val stream = input.getInputStream(entry)

        Files.write(localPath, stream.readAllBytes())

        stream.close()

        if (Utils.os != OS.Windows) {
          val mode = entry.getUnixMode

          val perms = new HashSet[PosixFilePermission]()

          if ((mode & 0400) != 0) perms.add(PosixFilePermission.OWNER_READ)
          if ((mode & 0200) != 0) perms.add(PosixFilePermission.OWNER_WRITE)
          if ((mode & 0100) != 0) perms.add(PosixFilePermission.OWNER_EXECUTE)

          if ((mode & 0040) != 0) perms.add(PosixFilePermission.GROUP_READ)
          if ((mode & 0020) != 0) perms.add(PosixFilePermission.GROUP_WRITE)
          if ((mode & 0010) != 0) perms.add(PosixFilePermission.GROUP_EXECUTE)

          if ((mode & 0004) != 0) perms.add(PosixFilePermission.OTHERS_READ)
          if ((mode & 0002) != 0) perms.add(PosixFilePermission.OTHERS_WRITE)
          if ((mode & 0001) != 0) perms.add(PosixFilePermission.OTHERS_EXECUTE)

          Files.setPosixFilePermissions(localPath, perms)
        }
      }

      processed += entry.getSize

      setProgress(processed.toDouble / bytes.size)
    }

    input.close()
  }

  private def refreshCardPanel(): Unit = {
    cardPanel.removeAll()

    cards.foreach { card =>
      cardPanel.add(Box.createVerticalStrut(Utils.GapSize))
      cardPanel.add(card)
    }

    revalidate()
    repaint()
  }

  private def resizeImage(image: Image): ImageIcon = {
    val size = Utils.IconSize

    // getScaledInstance runs asynchronously, but wrapping it with ImageIcon ensures that the scaling
    // operation fully completes before creating the BufferedImage (Isaac B 8/24/25)
    val scaledImage = new ImageIcon(image.getScaledInstance(size, size, Image.SCALE_SMOOTH)).getImage
    val bufferedImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)

    bufferedImage.createGraphics.drawImage(scaledImage, 0, 0, size, size, null)

    new ImageIcon(bufferedImage)
  }

  // this method looks in the standard platform-specific locations to find NetLogo installations. it
  // extracts both the version number and the app icon from each executable, which unfortunately
  // requires native code since Java doesn't provide tools to load image data from platform-specific
  // icon types. that code can be found in the `iconext` directory. (Isaac B 8/25/25)
  private def findInstalled(): Unit = {
    val paths: Seq[PathInfo] = Utils.os match {
      case OS.Windows =>
        File.listRoots.flatMap { file =>
          Option(file.toPath.resolve("Program Files").toFile.listFiles).getOrElse(Array[File]()) ++
            Option(file.toPath.resolve("Program Files (x86)").toFile.listFiles).getOrElse(Array[File]())
        }.collect {
          case file if file.getName.contains("NetLogo") =>
            Utils.listFilesRecursive(file).find { f =>
              """(?i)^NetLogo( [0-9\.]+(-(beta|rc)\d+)?)?.exe""".r.matches(f.getName)
            }.map(exe => PathInfo(file.getName, exe, file, exe))
        }.flatten.toSeq

      case OS.Mac =>
        File.listRoots.flatMap { file =>
          Option(file.toPath.resolve("Applications").toFile.listFiles).getOrElse(Array[File]())
        }.collect {
          case file if file.getName.contains("NetLogo") =>
            Utils.listFilesRecursive(file).find { f =>
              """(?i)^NetLogo( [0-9\.]+(-(beta|rc)\d+)?)?.app$""".r.matches(f.getName)
            }.flatMap { app =>
              app.toPath.resolve("Contents/Resources").toFile.listFiles.find { icon =>
                """^NetLogo.*\.icns$""".r.matches(icon.getName)
              }.map(icon => PathInfo(file.getName, icon, app.getParentFile, app))
            }
        }.flatten.toSeq

      case _ =>
        Seq()
    }

    val configs: Seq[AppConfig] = paths.map {
      case PathInfo(name, icon, root, exec) =>
        val image = IconExt.extractIcon(icon.getAbsolutePath) match {
          case ExtResult(pixels, width, height) if pixels.nonEmpty =>
            val buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

            buffer.setRGB(0, 0, width, height, pixels, 0, width)

            buffer

          case _ =>
            ImageIO.read(new File("NetLogo.png"))
        }

        AppConfig(name, name.stripPrefix("NetLogo "), resizeImage(image), root, exec)
    }

    cards = configs.sortBy(config => Integer.MAX_VALUE - Utils.numericVersion(config.version)).map(AppCard(_, this))

    refreshCardPanel()
  }

  private def getAvailableVersions(): Unit = {
    Request.json("get_available_versions", Obj(
      "os" -> Utils.os.name,
      "arch" -> Utils.arch
    )) match {
      case Success(json) =>
        availableVersions = json.obj.map((key, value) => (key -> value.str)).toMap

      case _ =>
        new OptionPane(this, "Error", "Error retrieving release information from server.", Array("OK"))
    }
  }

  override def syncTheme(theme: ColorTheme): Unit = {
    scrollPane.setBackground(theme.windowBackground)
    scrollPane.getViewport.setBackground(theme.windowBackground)

    title.setForeground(theme.windowText)

    cards.foreach(_.syncTheme(theme))

    addCard.syncTheme(theme)
  }

  private case class PathInfo(name: String, icon: File, root: File, exec: File)
}
