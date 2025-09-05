// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import com.jthemedetecor.OsThemeDetector

import java.awt.{ Font, GraphicsEnvironment, Window }
import javax.swing.UIManager

object Main {
  def main(args: Array[String]): Unit = {
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
  }
}
