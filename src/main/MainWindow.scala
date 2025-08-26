// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import com.jthemedetecor.OsThemeDetector

import java.awt.{ Dimension, Image }
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.{ Box, BoxLayout, ImageIcon, JFrame, JLabel, JPanel, JScrollPane, ScrollPaneConstants,
                     WindowConstants }
import javax.swing.border.EmptyBorder

class MainWindow extends JFrame with ThemeSync {
  private val themeDetector = OsThemeDetector.getDetector

  private val title = new JLabel("<html><b>Installed Versions</b></html>") {
    setFont(getFont.deriveFont(24f))
  }

  private var cards = Seq[AppCard]()

  private val addCard = new AddCard

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
    setSize(600, Utils.IconSize * 4 + Utils.GapSize * 13)

    val screenSize = getToolkit.getScreenSize

    setLocation(screenSize.width / 2 - getWidth / 2, screenSize.height / 2 - getHeight / 2)

    syncTheme(if (themeDetector.isDark) DarkTheme else LightTheme)

    themeDetector.registerListener(dark => syncTheme(if (dark) DarkTheme else LightTheme))

    findInstalled()

    if (cards.nonEmpty)
      cards(0).setDefault(true)
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

  def setDefault(default: AppCard): Unit = {
    cards.foreach(card => card.setDefault(card == default))
  }

  // this method looks in the standard platform-specific locations to find NetLogo installations. it
  // extracts both the version number and the app icon from each executable, which unfortunately
  // requires native code since Java doesn't provide tools to load image data from platform-specific
  // icon types. that code can be found in the `iconext` directory. (Isaac B 8/25/25)
  private def findInstalled(): Unit = {
    val paths: Seq[(String, String)] = Utils.os match {
      case OS.Windows =>
        File.listRoots.flatMap { file =>
          Option(file.toPath.resolve("Program Files").toFile.listFiles).getOrElse(Array[File]()) ++
            Option(file.toPath.resolve("Program Files (x86)").toFile.listFiles).getOrElse(Array[File]())
        }.collect {
          case file if file.getName.contains("NetLogo") =>
            listFilesRecursive(file).find(f => """NetLogo( [0-9\.]+)?.exe""".r.matches(f.getName)).map { exe =>
              (file.getName, exe.toString)
            }
        }.flatten.toSeq

      case _ =>
        Seq()
    }

    val configs: Seq[AppConfig] = paths.map {
      case (name, exec) =>
        val image = IconExt.extractIcon(exec) match {
          case ExtResult(pixels, width, height) if pixels.nonEmpty =>
            val buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

            buffer.setRGB(0, 0, width, height, pixels, 0, width)

            buffer

          case _ =>
            ImageIO.read(new File("NetLogo.png"))
        }

        AppConfig(name, resizeImage(image), exec)
    }

    cards = configs.sortBy(config => Integer.MAX_VALUE - Utils.numericVersion(config.name)).map(AppCard(_, this))

    cardPanel.removeAll()

    cards.foreach { card =>
      cardPanel.add(Box.createVerticalStrut(Utils.GapSize))
      cardPanel.add(card)

      card.syncTheme(if (themeDetector.isDark) DarkTheme else LightTheme)
    }
  }

  private def listFilesRecursive(file: File): Array[File] =
    Option(file.listFiles).getOrElse(Array[File]()).flatMap(file => file +: listFilesRecursive(file))

  override def syncTheme(theme: ColorTheme): Unit = {
    scrollPane.setBackground(theme.windowBackground)
    scrollPane.getViewport.setBackground(theme.windowBackground)

    title.setForeground(theme.windowText)

    cards.foreach(_.syncTheme(theme))

    addCard.syncTheme(theme)
  }
}
