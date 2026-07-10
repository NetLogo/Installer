// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import java.util.prefs.Preferences

object Prefs {
  private val prefs: Preferences = Preferences.userRoot.node("/org/nlogo/NetLogoInstaller")

  def get(key: String): Option[String] =
    Option(prefs.get(key, null))

  def put(key: String, value: String): Unit = {
    prefs.put(key, value)
  }
}
