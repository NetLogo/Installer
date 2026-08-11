// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import java.awt.{ Dimension, EventQueue, Image }
import java.io.File
import java.nio.file.{ Files, Path, Paths }
import javax.imageio.ImageIO
import javax.swing.{ Box, BoxLayout, ImageIcon, JFileChooser, JFrame, JLabel, JPanel, ScrollPaneConstants,
                     WindowConstants }

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

  private val scanPanel = new JPanel with Transparent {
    setLayout(new BoxLayout(this, BoxLayout.X_AXIS))

    add(scanLabel)
    add(Box.createHorizontalGlue)
  }

  private val statusPanel = new JPanel with Transparent {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS))

    add(new VerticalStrut(Utils.GapSize))
    add(scanPanel)
  }

  private val contents = new JPanel with Transparent {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS))
    setBorder(new ZoomableBorder(Utils.GapSize))

    add(new JPanel with Transparent {
      setLayout(new BoxLayout(this, BoxLayout.X_AXIS))

      add(titleLabel)
      add(Box.createHorizontalGlue)
    })

    add(statusPanel)
  }

  private val scrollPane = new ScrollPane(contents, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                          ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED)

  private val menu = new MenuBar(this)

  locally {
    setTitle("NetLogo Installer")
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
    setJMenuBar(menu)

    add(scrollPane)

    setMinimumSize(scrollPane.getMinimumSize)
    setZoom(Prefs.getFloat("zoomLevel", 1))

    val screenSize = getToolkit.getScreenSize

    setLocation(screenSize.width / 2 - getWidth / 2, screenSize.height / 2 - getHeight / 2)

    Future {
      getAvailableVersions()
      findInstalled()
      checkForUpdate()
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

        val versions: Array[String] = availableVersions.keys.toArray.filterNot { version =>
          cards.exists(_.config.version == version)
        }.sortBy(Utils.numericVersion).reverse

        val optionPane = new ComboBoxOptionPane(
          this, "Select Version", "Select the version you would like to download.", versions,
          Array("Download", "Cancel")
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

              setCards(cards.map(_.config) :+ config)

              refreshCardPanel()

            case _ =>
              new OptionPane(this, "Invalid", "The selected directory is not a valid NetLogo installation.",
                             Array("OK"))
          }
        }

      case _ =>
    }
  }

  def refreshInstallation(root: File): Unit = {
    setCards(cards.map(_.config).filter(_.root != root) ++ verifyRoot(root))

    refreshCardPanel()
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
      val root: Path = {
        if (Utils.os == OS.Linux) {
          Paths.get(Utils.appRoot, s"NetLogo-$version")
        } else {
          Paths.get(Utils.appRoot, s"NetLogo $version")
        }
      }

      Install.installVersion(this, version, root)

      refreshInstallation(root.toFile)
    }
  }

  private def refreshCardPanel(): Unit = {
    statusPanel.removeAll()
    cardPanel.removeAll()

    cards.foreach { card =>
      cardPanel.add(new VerticalStrut(Utils.GapSize))
      cardPanel.add(card)
    }

    statusPanel.add(cardPanel)
    statusPanel.add(new VerticalStrut(Utils.GapSize))
    statusPanel.add(addCard)

    Utils.zoomComponents(statusPanel, Utils.getZoomLevel, 1)

    revalidate()
    repaint()
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
        new File(Utils.appRoot).listFiles.flatMap(verifyRoot)
    }) ++ getExtraPaths.flatMap(verifyRoot)

    setCards(configs.toSeq)

    EventQueue.invokeLater(() => {
      refreshCardPanel()
    })
  }

  private def setCards(configs: Seq[AppConfig]): Unit = {
    cards = configs.sortBy(config => Integer.MAX_VALUE - Utils.numericVersion(config.version)).map {
      new AppCard(_, this) {
        availableVersions.get(config.version) match {
          case Some(expected) =>
            val checksumPath = config.root.toPath.resolve(".checksum")

            if (Files.exists(checksumPath))
              setUpdatable(expected != Files.readString(checksumPath).trim)

            setReparable(true)

          case _ =>
            setReparable(false)
        }
      }
    }

    Prefs.get("defaultVersion").flatMap(version => cards.find(_.config.version == version))
      .orElse(cards.headOption).foreach(setDefault)
  }

  private def verifyRoot(root: File): Option[AppConfig] = {
    if (root.getName.startsWith("NetLogo")) {
      val versionExec: String = s"( [0-9\\.]+(-(beta|rc)\\d+)?)?${Utils.os.exec}"

      val regex: Regex = s"(?i)^NetLogo$versionExec$$".r
      val regexThreed: Regex = s"(?i)^NetLogo ?3D$versionExec$$".r
      val regexBsearch: Regex = s"(?i)^BehaviorSearch$versionExec$$".r
      val regexHubNet: Regex = s"(?i)^HubNet ?Client$versionExec$$".r

      def findMatch(regex: Regex): Option[File] = {
        Utils.os match {
          case OS.Mac =>
            Utils.findFile(root, f => regex.matches(f.getName))

          case _ =>
            Utils.findFile(root, f => f.isFile && regex.matches(f.getName))
        }
      }

      findMatch(regex).map { exec =>
        val name: String = root.getName
        val version: String = Utils.standardizeVersion(name.replaceAll("^NetLogo( |-)", ""))

        val image: Image = ImageIO.read(getClass.getResource({
          if (Utils.numericVersion(version) > Utils.numericVersion("6.4.0")) {
            "/images/NetLogoNew.png"
          } else {
            "/images/NetLogoOld.png"
          }
        }))

        AppConfig(s"NetLogo $version", version, new ImageIcon(image), root, exec, findMatch(regexThreed),
                  findMatch(regexBsearch), findMatch(regexHubNet))
      }
    } else {
      None
    }
  }

  private def getAvailableVersions(): Unit = {
    Request.json("versions", Obj(
      "os" -> Utils.os.name,
      "arch" -> Utils.arch
    )).foreach { json =>
      availableVersions = json.obj.map(_ -> _.str).toMap
    }
  }

  private def checkForUpdate(): Unit = {
    Request.json("installer", Obj(
      "os" -> Utils.os.name,
      "arch" -> Utils.arch
    )).map(_.obj).foreach { obj =>
      val version: String = obj("version").str
      val url: String = obj("url").str

      if (Utils.numericVersion(version) > Utils.numericVersion(System.getProperty("installer.version"))) {
        EventQueue.invokeLater(() => {
          val message: String = centerText("""|An update is available for the installer application.<br>
                                              |Would you like to update now?""".stripMargin)

          if (new OptionPane(this, "Update Available", message, Array("Update", "Later")).getSelectedIndex == 0)
            Install.updateInstaller(this, url)
        })
      }
    }
  }

  private def centerText(text: String): String =
    s"<html><body style=\"text-align: center\">$text</body></html>"

  def zoom(amount: Float): Unit = {
    setZoom((Utils.getZoomLevel + amount).max(1))
  }

  def setZoom(newZoom: Float): Unit = {
    val oldZoom: Float = Utils.getZoomLevel

    Utils.setZoomLevel(newZoom)
    Utils.zoomComponents(scrollPane, newZoom, oldZoom)
    Utils.zoomMenu(menu, newZoom, oldZoom)

    val screenSize: Dimension = getToolkit.getScreenSize

    setSize(Utils.zoom(700).min(screenSize.width),
            Utils.zoom(Utils.IconSize * 4 + Utils.GapSize * 13).min(screenSize.height))
  }

  override def syncTheme(theme: ColorTheme): Unit = {
    scrollPane.syncTheme(theme)

    titleLabel.setForeground(theme.windowText)
    scanLabel.setForeground(theme.windowText)

    cards.foreach(_.syncTheme(theme))

    addCard.syncTheme(theme)

    menu.syncTheme(theme)
  }
}
