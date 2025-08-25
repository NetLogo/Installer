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
  private val title = new JLabel("<html><b>Installed Versions</b></html>") {
    setFont(getFont.deriveFont(24f))
  }

  private val cards = Seq(
    new AppCard(AppConfig("NetLogo 7.0.0", resizeImage(ImageIO.read(new File("NetLogo.png")), Utils.IconSize,
                                                       Utils.IconSize)),
                this),
    new AppCard(AppConfig("NetLogo 6.4.0", resizeImage(ImageIO.read(new File("NetLogoOld.png")), Utils.IconSize,
                                                       Utils.IconSize)),
                this)
  )

  private val addCard = new AddCard

  private val contents = new JPanel with Transparent {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS))
    setBorder(new EmptyBorder(Utils.GapSize, Utils.GapSize, Utils.GapSize, Utils.GapSize))

    add(new JPanel with Transparent {
      setLayout(new BoxLayout(this, BoxLayout.X_AXIS))

      add(title)
      add(Box.createHorizontalGlue)
    })

    cards.foreach { card =>
      add(Box.createVerticalStrut(Utils.GapSize))
      add(card)
    }

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

    val detector = OsThemeDetector.getDetector

    syncTheme(if (detector.isDark) DarkTheme else LightTheme)

    detector.registerListener(dark => syncTheme(if (dark) DarkTheme else LightTheme))

    cards(0).setDefault(true)
  }

  private def resizeImage(image: Image, width: Int, height: Int): ImageIcon = {
    // getScaledInstance runs asynchronously, but wrapping it with ImageIcon ensures that the scaling
    // operation fully completes before creating the BufferedImage (Isaac B 8/24/25)
    val scaledImage = new ImageIcon(image.getScaledInstance(width, height, Image.SCALE_SMOOTH)).getImage
    val bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

    bufferedImage.createGraphics.drawImage(scaledImage, 0, 0, width, height, null)

    new ImageIcon(bufferedImage)
  }

  def setDefault(default: AppCard): Unit = {
    cards.foreach(card => card.setDefault(card == default))
  }

  override def syncTheme(theme: ColorTheme): Unit = {
    scrollPane.setBackground(theme.windowBackground)
    scrollPane.getViewport.setBackground(theme.windowBackground)

    title.setForeground(theme.windowText)

    cards.foreach(_.syncTheme(theme))

    addCard.syncTheme(theme)
  }
}
