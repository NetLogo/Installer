// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import java.util.prefs.Preferences

object Prefs {
  private val prefs: Preferences = Preferences.userRoot.node("/org/nlogo/NetLogoInstaller")

  def get(key: String): Option[String] =
    Option(prefs.get(key, null))

  def getFloat(key: String, default: Float): Float =
    prefs.getFloat(key, default)

  def put(key: String, value: String): Unit = {
    prefs.put(key, value)
  }

  def put(key: String, value: Float): Unit = {
    prefs.putFloat(key, value)
  }
}
