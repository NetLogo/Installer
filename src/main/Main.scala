// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import com.jthemedetecor.OsThemeDetector

import java.awt.{ Font, GraphicsEnvironment, Window }
import javax.swing.UIManager
import javax.swing.border.LineBorder

import scala.sys.process.Process

object Main {
  def main(args: Array[String]): Unit = {
    Option(System.getProperty("sun.java2d.uiScale")).flatMap(_.toFloatOption) match {
      case Some(scale) =>
        Utils.setUIScale(scale)

      case _ if System.getProperty("os.name").toLowerCase.startsWith("linux") =>
        try {
          val query: String = Process(Seq("xrdb", "-query")).!!

          """Xft\.dpi:\s*(\d+)""".r.findFirstMatchIn(query).flatMap(_.group(1).toIntOption).foreach { dpi =>
            Utils.setUIScale(dpi / 96f)
          }
        } catch {
          case _ =>
        }

      case _ =>
    }

    val font = {
      if (Utils.os == OS.Windows) {
        new Font("Segoe UI", Font.PLAIN, 12)
      } else {
        GraphicsEnvironment.getLocalGraphicsEnvironment.registerFont(
          Font.createFont(Font.TRUETYPE_FONT, getClass.getResourceAsStream("/fonts/OpenSans-Variable.ttf")))

        new Font("Open Sans", Font.PLAIN, 12)
      }
    }

    UIManager.put("Label.font", font)
    UIManager.put("Button.font", font)
    UIManager.put("MenuItem.font", font)

    val themeDetector = OsThemeDetector.getDetector

    syncTheme(themeDetector.isDark)

    themeDetector.registerListener(syncTheme)

    new MainWindow().setVisible(true)
  }

  private def syncTheme(dark: Boolean): Unit = {
    val theme: ColorTheme = {
      if (dark) {
        DarkTheme
      } else {
        LightTheme
      }
    }

    ThemeSync.currentTheme = theme

    Window.getWindows.foreach {
      case ts: ThemeSync =>
        ts.syncTheme(theme)

      case _ =>
    }

    UIManager.put("PopupMenu.border", new LineBorder(theme.buttonBorder))
  }
}
