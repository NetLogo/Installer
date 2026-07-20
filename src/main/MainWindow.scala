// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import java.awt.Image
import java.awt.image.BufferedImage
import java.io.{ ByteArrayOutputStream, File, InputStream }
import java.net.{ URI, URLConnection }
import java.nio.file.{ Files, Path, Paths, StandardOpenOption }
import java.nio.file.attribute.PosixFilePermission
import java.util.HashSet
import javax.imageio.ImageIO
import javax.swing.{ Box, BoxLayout, ImageIcon, JFileChooser, JFrame, JLabel, JPanel, ScrollPaneConstants,
                     WindowConstants }
import javax.swing.border.EmptyBorder

import org.apache.commons.compress.archivers.zip.ZipFile

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.matching.Regex

import ujson.Obj

class MainWindow extends JFrame with ThemeSync {
  private implicit val ec: ExecutionContext = ExecutionContext.global

  private var availableVersions = Map[String, String]()

  private val titleLabel = new JLabel("<html><b>Installed Versions</b></html>") {
    setFont(getFont.deriveFont(24f))
  }

  private var cards = Seq[AppCard]()

  private val addCard = new AddCard(this)

  private val cardPanel = new JPanel with Transparent {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS))
  }

  private val scanLabel = new JLabel("Scanning...")

  private val statusPanel = new JPanel with Transparent {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS))

    add(Box.createVerticalStrut(Utils.GapSize))
    add(new JPanel with Transparent {
      setLayout(new BoxLayout(this, BoxLayout.X_AXIS))

      add(scanLabel)
      add(Box.createHorizontalGlue)
    })
  }

  private val contents = new JPanel with Transparent {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS))
    setBorder(new EmptyBorder(Utils.GapSize, Utils.GapSize, Utils.GapSize, Utils.GapSize))

    add(new JPanel with Transparent {
      setLayout(new BoxLayout(this, BoxLayout.X_AXIS))

      add(titleLabel)
      add(Box.createHorizontalGlue)
    })

    add(statusPanel)
  }

  private val scrollPane = new ScrollPane(contents, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                          ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)

  locally {
    setTitle("NetLogo Installer")
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)

    add(scrollPane)

    setMinimumSize(scrollPane.getMinimumSize)
    setSize(700, Utils.IconSize * 4 + Utils.GapSize * 13)

    val screenSize = getToolkit.getScreenSize

    setLocation(screenSize.width / 2 - getWidth / 2, screenSize.height / 2 - getHeight / 2)

    Future {
      getAvailableVersions()
      findInstalled()

      statusPanel.removeAll()

      statusPanel.add(cardPanel)
      statusPanel.add(Box.createVerticalStrut(Utils.GapSize))
      statusPanel.add(addCard)

      revalidate()
      repaint()
    }

    initTheme()
  }

  def setDefault(default: AppCard): Unit = {
    if (Prefs.get("defaultVersion").contains(default.config.version) || Defaults.setDefault(default.config)) {
      Prefs.put("defaultVersion", default.config.version)

      cards.foreach(card => card.setDefault(card == default))
    } else {
      new OptionPane(this, "Error", "Failed to set default NetLogo version.", Array("OK"))
    }
  }

  def removeCard(card: AppCard): Unit = {
    cards = cards.filter(_ != card)

    if (card.isDefault)
      cards.headOption.foreach(setDefault)

    refreshCardPanel()
  }

  def addInstallation(): Unit = {
    new OptionPane(this, "Add Installation", "Add an existing installation or download a new version?",
                   Array("Download new", "Add existing")).getSelectedIndex match {
      case 0 =>
        if (availableVersions.isEmpty) {
          new OptionPane(this, "Error", "Error retrieving available versions from server.", Array("OK"))

          return
        }

        val optionPane = new ComboBoxOptionPane(
          this, "Select Version", "Select the version you would like to download.",
          availableVersions.keys.toArray.sortBy(Utils.numericVersion).reverse, Array("Download", "Cancel")
        )

        if (optionPane.getSelectedIndex == 0)
          install(optionPane.getSelectedOption)

      case 1 =>
        val dialog = new JFileChooser

        dialog.setDialogTitle("Select Root Directory")
        dialog.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY)

        if (dialog.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
          verifyRoot(dialog.getSelectedFile) match {
            case Some(config) if cards.exists(_.config.version == config.version) =>
              new OptionPane(this, "Already Exists", s"${config.name} is already installed.", Array("OK"))

            case Some(config) =>
              putExtraPaths(getExtraPaths :+ config.root)

              findInstalled()

            case _ =>
              new OptionPane(this, "Invalid", "The selected directory is not a valid NetLogo installation.",
                             Array("OK"))
          }
        }

      case _ =>
    }
  }

  private def getExtraPaths: Array[File] =
    Prefs.get("extraPaths").fold(Array[File]())(_.split("\n").map(new File(_)))

  private def putExtraPaths(paths: Array[File]): Unit = {
    Prefs.put("extraPaths", paths.map(_.getAbsolutePath).mkString("\n"))
  }

  private def install(version: String): Unit = {
    if (cards.exists(_.config.version == version)) {
      new OptionPane(this, "Error", s"NetLogo $version is already installed.", Array("OK"))
    } else {
      val root = Paths.get(Utils.appRoot, s"NetLogo $version")

      getVersionURL(version).flatMap(downloadVersion(version, _, root)).foreach { data =>
        if (installVersion("Install", s"Installing NetLogo $version...", data, root))
          findInstalled()
      }
    }
  }

  private def getVersionURL(version: String): Option[String] = {
    Request.json("get_version", Obj(
      "os" -> Utils.os.name,
      "arch" -> Utils.arch,
      "version" -> version
    )).map(_.str).toOption.orElse {
      new OptionPane(this, "Error", "Error downloading files from server.", Array("OK"))

      None
    }
  }

  private def downloadVersion(version: String, url: String, dest: Path): Option[Array[Byte]] = {
    val progress = new ProgressTracker

    val output = new ByteArrayOutputStream

    Future {
      val connection: URLConnection = new URI(url).toURL.openConnection
      val input: InputStream = connection.getInputStream
      val length: Int = connection.getContentLength

      while (output.size < length) {
        if (progress.abortRequested) {
          input.close()

          throw new InterruptedException
        }

        output.write(input.readNBytes(1024))

        progress.setProgress(output.size.toDouble / length)
      }

      input.close()
      output.close()

      progress.setProgress(1.0)
    }.recover(_ => progress.requestAbort())

    new ProgressDialog(this, "Install", s"Downloading NetLogo $version...", progress).getStatus match {
      case ProgressStatus.Completed =>
        Option(output.toByteArray)

      case ProgressStatus.Canceled =>
        progress.requestAbort()

        None

      case _ =>
        new OptionPane(this, "Error", "Error downloading files from server.", Array("OK"))

        None
    }
  }

  def installVersion(title: String, message: String, data: Array[Byte], dest: Path): Boolean = {
    val progress = new ProgressTracker

    Future(updateFromZip(data, dest, progress)).recover { _ =>
      progress.requestAbort()

      Utils.deleteRecursive(dest.toFile)
    }

    new ProgressDialog(this, title, message, progress).getStatus match {
      case ProgressStatus.Completed =>
        true

      case ProgressStatus.Canceled =>
        progress.requestAbort()

        false

      case _ =>
        new OptionPane(this, "Error", "Error installing files.", Array("OK"))

        false
    }
  }

  def installUpdate(title: String, message: String, updates: Seq[Update], dest: Path): Boolean = {
    if (updates.isEmpty) {
      new OptionPane(this, title, "Installation is already up to date.", Array("OK"))

      return true
    }

    val progress = new ProgressTracker

    val totalLength: Long = updates.map(_.length).sum + 1
    var processed = 0L

    Future {
      updates.foreach {
        case Update(path, url, length) =>
          if (progress.abortRequested)
            throw new InterruptedException

          val fullPath: Path = dest.resolve(path)
          val stream: InputStream = new URI(url).toURL.openStream

          Files.createDirectories(fullPath.getParent)
          Files.write(fullPath, stream.readAllBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)

          stream.close()

          processed += length

          progress.setProgress(processed.toDouble / totalLength)
      }

      progress.setProgress(1.0)
    }.recover(_ => progress.requestAbort())

    new ProgressDialog(this, title, message, progress).getStatus match {
      case ProgressStatus.Completed =>
        true

      case ProgressStatus.Canceled =>
        progress.requestAbort()

        false

      case _ =>
        new OptionPane(this, "Error", "Error downloading updated files from server.", Array("OK"))

        false
    }
  }

  private def updateFromZip(bytes: Array[Byte], dest: Path, progress: ProgressTracker): Unit = {
    val builder = new ZipFile.Builder

    builder.setByteArray(bytes)

    val input = builder.get

    var processed = 0L

    input.stream.forEach { entry =>
      if (progress.abortRequested) {
        input.close()

        throw new InterruptedException
      }

      if (!entry.isDirectory) {
        val relativePath = Paths.get(entry.getName)
        val localPath = dest.resolve(relativePath)

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

      progress.setProgress(processed.toDouble / bytes.size)
    }

    input.close()

    progress.setProgress(1.0)
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

  // this method looks in the standard platform-specific locations to find NetLogo installations,
  // extracting the version number and the relevant paths for the installation. (Isaac B 8/25/25)
  private def findInstalled(): Unit = {
    val configs: Array[AppConfig] = (Utils.os match {
      case OS.Windows =>
        File.listRoots.flatMap { file =>
          Option(file.toPath.resolve("Program Files").toFile.listFiles).getOrElse(Array[File]()) ++
            Option(file.toPath.resolve("Program Files (x86)").toFile.listFiles).getOrElse(Array[File]())
        }.flatMap(verifyRoot)

      case OS.Mac =>
        File.listRoots.flatMap { file =>
          Option(file.toPath.resolve("Applications").toFile.listFiles).getOrElse(Array[File]())
        }.flatMap(verifyRoot)

      case _ =>
        Array[AppConfig]()
    }) ++ getExtraPaths.flatMap(verifyRoot)

    cards = configs.sortBy(config => Integer.MAX_VALUE - Utils.numericVersion(config.version)).map { config =>
      val card = new AppCard(config, this)

      availableVersions.get(config.version) match {
        case Some(expected) =>
          val checksumPath = config.root.toPath.resolve(".checksum")

          if (Files.exists(checksumPath))
            card.setUpdatable(expected != Files.readString(checksumPath).trim)

          card.setReparable(true)

        case _ =>
          card.setReparable(false)
      }

      card
    }.toSeq

    Prefs.get("defaultVersion").flatMap(version => cards.find(_.config.version == version))
      .orElse(cards.headOption).foreach(setDefault)

    refreshCardPanel()
  }

  private def verifyRoot(root: File): Option[AppConfig] = {
    if (root.getName.startsWith("NetLogo")) {
      val versionExec: String = s"( [0-9\\.]+(-(beta|rc)\\d+)?)?${Utils.os.exec}"

      val regex: Regex = s"(?i)^NetLogo$versionExec$$".r
      val regexThreed: Regex = s"(?i)^NetLogo ?3D$versionExec$$".r
      val regexBsearch: Regex = s"(?i)^BehaviorSearch$versionExec$$".r
      val regexHubNet: Regex = s"(?i)^HubNet ?Client$versionExec$$".r

      val files: Array[File] = Utils.listFilesRecursive(root)

      def findMatch(regex: Regex): Option[File] = {
        Utils.os match {
          case OS.Mac =>
            files.find(f => regex.matches(f.getName))

          case _ =>
            files.find(f => f.isFile && regex.matches(f.getName))
        }
      }

      findMatch(regex).map { exec =>
        val name: String = root.getName
        val version: String = name.stripPrefix("NetLogo ")

        val image: Image = ImageIO.read(getClass.getResource({
          if (Utils.numericVersion(version) > Utils.numericVersion("6.4.0")) {
            "/images/NetLogoNew.png"
          } else {
            "/images/NetLogoOld.png"
          }
        }))

        AppConfig(name, version, resizeImage(image), root, exec, findMatch(regexThreed), findMatch(regexBsearch),
                  findMatch(regexHubNet))
      }
    } else {
      None
    }
  }

  private def getAvailableVersions(): Unit = {
    Request.json("get_available_versions", Obj(
      "os" -> Utils.os.name,
      "arch" -> Utils.arch
    )).foreach { json =>
      availableVersions = json.obj.map(_ -> _.str).toMap
    }
  }

  override def syncTheme(theme: ColorTheme): Unit = {
    scrollPane.syncTheme(theme)

    titleLabel.setForeground(theme.windowText)
    scanLabel.setForeground(theme.windowText)

    cards.foreach(_.syncTheme(theme))

    addCard.syncTheme(theme)
  }
}
