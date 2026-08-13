// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import java.awt.{ Color, Component, Container, Cursor, Graphics, Graphics2D, Image, RenderingHints, Window }
import java.awt.event.{ ActionEvent, InputEvent, KeyEvent, MouseAdapter, MouseEvent }
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.{ Files, Path, StandardOpenOption }
import javax.swing.{ AbstractAction, Icon, ImageIcon, JComponent, KeyStroke }

object Utils {
  val GapSize = 12
  val CornerDiameter = 8
  val IconSize = 64

  val os: OS = {
    val name = System.getProperty("os.name").toLowerCase

    if (name.startsWith("win")) {
      OS.Windows
    } else if (name.startsWith("mac")) {
      OS.Mac
    } else {
      OS.Linux
    }
  }

  val arch: String = System.getProperty("os.arch")

  val appRoot: String = {
    os match {
      case OS.Windows =>
        "C:/Program Files"

      case OS.Mac =>
        "/Applications"

      case OS.Linux =>
        System.getProperty("user.home")
    }
  }

  val platformCtrl = {
    if (os == OS.Mac) {
      InputEvent.META_DOWN_MASK
    } else {
      InputEvent.CTRL_DOWN_MASK
    }
  }

  private val versionRegex = """(?i)^(\d+).(\d+).(\d+)(?:-(?:beta|rc)(\d+))?$""".r
  private val oldVersionRegex = """(?i)^(\d+).(\d+)(?:-(?:beta|rc)(\d+))?$""".r

  private var zoomLevel = 1f
  private var uiScale = 1f

  def getZoomLevel: Float =
    zoomLevel

  def setZoomLevel(zoomLevel: Float): Unit = {
    this.zoomLevel = zoomLevel

    Prefs.put("zoomLevel", zoomLevel)
  }

  def zoom(value: Int): Int =
    (value * zoomLevel * uiScale).toInt

  def zoomFloat(value: Float): Float =
    value * zoomLevel * uiScale

  def getUIScale: Float =
    uiScale

  def setUIScale(scale: Float): Unit = {
    uiScale = scale
  }

  def initGraphics2D(g: Graphics): Graphics2D = {
    val g2d = g.asInstanceOf[Graphics2D]

    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

    g2d
  }

  def standardizeVersion(version: String): String = {
    version match {
      case oldVersionRegex(major, minor, modifier) =>
        s"$major.$minor.0${Option(modifier).fold("")(mod => s"-$mod")}"

      case _ =>
        version
    }
  }

  def numericVersion(name: String): Int = {
    try {
      name match {
        case versionRegex(major, minor, patch, modifier) =>
          major.toInt * 1000000 + minor.toInt * 10000 + patch.toInt * 100 + Option(modifier).fold(0)(_.toInt - 100)

        case oldVersionRegex(major, minor, modifier) =>
          major.toInt * 1000000 + minor.toInt * 10000 + Option(modifier).fold(0)(_.toInt - 100)
      }
    } catch {
      case _: Throwable =>
        0
    }
  }

  def center(window: Window, parent: Window): Unit = {
    window.setLocation(parent.getX + parent.getWidth / 2 - window.getWidth / 2,
                       parent.getY + parent.getHeight / 2 - window.getHeight / 2)
  }

  def listFilesRecursive(file: File): Array[File] =
    Option(file.listFiles).getOrElse(Array[File]()).flatMap(file => file +: listFilesRecursive(file))

  def findFile(file: File, pred: File => Boolean): Option[File] =
    Option(file.listFiles).flatMap(files => files.find(pred).orElse(files.flatMap(findFile(_, pred)).headOption))

  def deleteRecursive(file: File): Boolean = {
    if (file.isDirectory) {
      file.listFiles.forall(deleteRecursive) && file.delete()
    } else {
      file.delete()
    }
  }

  def loadExecutable(path: String, ext: String): Option[Path] = {
    Option(getClass.getResourceAsStream(path)).map { stream =>
      val dest: Path = Files.createTempFile(null, ext)

      dest.toFile.setExecutable(true)
      dest.toFile.deleteOnExit()

      Files.write(dest, stream.readAllBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)

      stream.close()

      dest
    }
  }

  def zoomComponents(root: Component, oldZoom: Float): Unit = {
    root.setFont(root.getFont.deriveFont(root.getFont.getSize / oldZoom * zoomLevel * uiScale))

    root match {
      case zoomable: Zoomable =>
        zoomable.zoom()

      case _ =>
    }

    root match {
      case container: Container =>
        container.getComponents.foreach(zoomComponents(_, oldZoom))

      case _ =>
    }
  }

  def zoomMenu(menu: MenuBar, oldZoom: Float): Unit = {
    menu.getComponents.foreach {
      case menu: Menu =>
        menu.setFont(menu.getFont.deriveFont(menu.getFont.getSize / oldZoom * zoomLevel * uiScale))

        menu.getMenuComponents.foreach(Utils.zoomComponents(_, oldZoom))

      case _ =>
    }
  }

  def scaleIcon(icon: Icon, size: Int): Icon = {
    val image = new BufferedImage(icon.getIconWidth, icon.getIconHeight, BufferedImage.TYPE_INT_ARGB)

    icon.paintIcon(null, image.getGraphics, 0, 0)

    new ImageIcon(image.getScaledInstance(size, size, Image.SCALE_SMOOTH))
  }

  def addEscAction(component: JComponent, action: () => Unit): Unit = {
    component.getInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close")
    component.getActionMap.put("close", new AbstractAction {
      override def actionPerformed(e: ActionEvent): Unit = {
        action()
      }
    })
  }
}

sealed abstract trait OS(val name: String, val exec: String, val bin: String)

object OS {
  case object Windows extends OS("windows", ".exe", ".exe")
  case object Mac extends OS("mac", ".app", "")
  case object Linux extends OS("linux", "", ".sh")
}

trait Transparent extends JComponent {
  setOpaque(false)
  setBackground(new Color(0, 0, 0, 0))
}

trait MouseActions extends JComponent {
  protected var hover = false
  protected var pressed = false

  setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))

  addMouseListener(new MouseAdapter {
    override def mouseEntered(e: MouseEvent): Unit = {
      hover = true

      repaint()
    }

    override def mouseExited(e: MouseEvent): Unit = {
      hover = false

      repaint()
    }

    override def mousePressed(e: MouseEvent): Unit = {
      pressed = true

      repaint()
    }

    override def mouseReleased(e: MouseEvent): Unit = {
      pressed = false

      repaint()
    }
  })
}
