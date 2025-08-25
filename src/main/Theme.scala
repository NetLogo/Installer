// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import java.awt.Color

trait ColorTheme {
  def windowBackground: Color
  def windowText: Color
  def cardBackground: Color
  def cardBackgroundHover: Color
  def cardBackgroundPressed: Color
  def cardBorder: Color
  def cardBorderHighlight: Color
  def cardText: Color
  def buttonBackground: Color
  def buttonBackgroundHover: Color
  def buttonBackgroundPressed: Color
  def buttonBorder: Color
  def buttonText: Color
}

object LightTheme extends ColorTheme {
  override def windowBackground: Color = Color.WHITE
  override def windowText: Color = new Color(50, 50, 50)
  override def cardBackground: Color = new Color(255, 255, 255)
  override def cardBackgroundHover: Color = new Color(235, 235, 235)
  override def cardBackgroundPressed: Color = new Color(210, 210, 210)
  override def cardBorder: Color = new Color(100, 100, 100)
  override def cardBorderHighlight: Color = new Color(0, 0, 0)
  override def cardText: Color = new Color(50, 50, 50)
  override def buttonBackground: Color = new Color(255, 255, 255)
  override def buttonBackgroundHover: Color = new Color(230, 230, 230)
  override def buttonBackgroundPressed: Color = new Color(205, 205, 205)
  override def buttonBorder = new Color(100, 100, 100)
  override def buttonText: Color = new Color(50, 50, 50)
}

object DarkTheme extends ColorTheme {
  override def windowBackground: Color = new Color(50, 50, 50)
  override def windowText: Color = new Color(220, 220, 220)
  override def cardBackground: Color = new Color(75, 75, 75)
  override def cardBackgroundHover: Color = new Color(75, 75, 75)
  override def cardBackgroundPressed: Color = new Color(100, 100, 100)
  override def cardBorder: Color = new Color(150, 150, 150)
  override def cardBorderHighlight: Color = Color.WHITE
  override def cardText: Color = new Color(220, 220, 220)
  override def buttonBackground: Color = new Color(100, 100, 100)
  override def buttonBackgroundHover: Color = new Color(75, 75, 75)
  override def buttonBackgroundPressed: Color = new Color(50, 50, 50)
  override def buttonBorder: Color = new Color(150, 150, 150)
  override def buttonText: Color = new Color(220, 220, 220)
}

trait ThemeSync {
  def syncTheme(theme: ColorTheme): Unit
}
