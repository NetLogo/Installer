// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import java.awt.{ BorderLayout, Color, Dimension, Frame, Graphics, Graphics2D, LinearGradientPaint }
import javax.swing.{ Box, BoxLayout, JDialog, JLabel, JPanel }
import javax.swing.border.EmptyBorder

class ProgressDialog(parent: Frame, title: String, message: String, progress: () => Double, indeterminate: Boolean)
  extends JDialog(parent, title, true) with ThemeSync {

  private val label = new JLabel(message)

  private val progressBar = new ProgressBar(indeterminate)

  private val cancelButton = new Button("Cancel", () => setVisible(false))

  add(new JPanel(new BorderLayout(Utils.GapSize, Utils.GapSize)) with Transparent {
    setBorder(new EmptyBorder(Utils.GapSize, Utils.GapSize, Utils.GapSize, Utils.GapSize))

    add(label, BorderLayout.NORTH)
    add(progressBar, BorderLayout.CENTER)
    add(new JPanel with Transparent {
      setLayout(new BoxLayout(this, BoxLayout.X_AXIS))

      add(Box.createHorizontalGlue)
      add(cancelButton)
      add(Box.createHorizontalGlue)
    }, BorderLayout.SOUTH)
  })

  initTheme()
  pack()

  Utils.center(this, parent)

  setResizable(false)
  setAlwaysOnTop(true)

  new Thread {
    override def run(): Unit = {
      while {
        progressBar.setValue(progress())

        !isCompleted
      } do {
        Thread.sleep(100)
      }

      setVisible(false)
    }
  }.start()

  setVisible(true)

  def isCompleted: Boolean =
    progressBar.getProgress >= 1

  override def syncTheme(theme: ColorTheme): Unit = {
    getContentPane.setBackground(theme.windowBackground)

    label.setForeground(theme.windowText)

    progressBar.syncTheme(theme)
    cancelButton.syncTheme(theme)
  }

  private class ProgressBar(indeterminate: Boolean) extends JPanel with Transparent with ThemeSync {
    private var progress = 0.0

    private var backgroundColor = Color.WHITE
    private var foregroundColor = Color.WHITE

    private val start = System.currentTimeMillis

    def setValue(value: Double): Unit = {
      progress = value

      repaint()
    }

    def getProgress: Double =
      progress

    override def getPreferredSize: Dimension =
      new Dimension(150, Utils.CornerDiameter)

    override def paintComponent(g: Graphics): Unit = {
      val g2d = g.asInstanceOf[Graphics2D]

      g2d.setColor(backgroundColor)
      g2d.fillRoundRect(0, 0, getWidth, getHeight, Utils.CornerDiameter, Utils.CornerDiameter)

      if (indeterminate) {
        val offset = (((System.currentTimeMillis - start) % 2000) / 2000f) * getWidth * 2 - getWidth

        g2d.setPaint(new LinearGradientPaint(offset, getHeight / 2f, getWidth + offset, getHeight / 2f,
                                             Array(0f, 0.5f, 1f),
                                             Array(backgroundColor, foregroundColor, backgroundColor)))
        g2d.fillRoundRect(0, 0, getWidth, getHeight, Utils.CornerDiameter, Utils.CornerDiameter)
      } else {
        g2d.setColor(foregroundColor)
        g2d.fillRoundRect(0, 0, (getWidth * progress).toInt, getHeight, Utils.CornerDiameter, Utils.CornerDiameter)
      }
    }

    override def syncTheme(theme: ColorTheme): Unit = {
      backgroundColor = theme.progressBackground
      foregroundColor = theme.progressForeground
    }
  }
}
