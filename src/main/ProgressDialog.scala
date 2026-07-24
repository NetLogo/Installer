// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import java.awt.{ BorderLayout, Color, Dimension, EventQueue, Frame, Graphics, LinearGradientPaint }
import javax.swing.{ Box, BoxLayout, JDialog, JLabel, JPanel }
import javax.swing.border.EmptyBorder

import scala.concurrent.{ ExecutionContext, Future }

sealed abstract trait ProgressResult

object ProgressResult {
  case object Completed extends ProgressResult
  case object Canceled extends ProgressResult
  case object Aborted extends ProgressResult
}

class ProgressDialog(parent: Frame, title: String, message: String)
  extends JDialog(parent, title, true) with ThemeSync {

  private implicit val ec: ExecutionContext = ExecutionContext.global

  private val label = new JLabel(message)

  private val progressBar = new ProgressBar

  private val cancelButton = new Button("Cancel", () => setVisible(false))

  private var progress = -1.0
  private var abort = false

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

  def setProgress(progress: Double): Unit = synchronized {
    this.progress = progress
  }

  def getProgress: Double = synchronized {
    progress
  }

  def requestAbort(): Unit = synchronized {
    abort = true
  }

  def abortRequested: Boolean = synchronized {
    abort
  }

  def trackProgress(): ProgressResult = {
    Future {
      while {
        progressBar.setValue(getProgress)

        getProgress < 1.0 && !abortRequested
      } do {
        Thread.sleep(100)
      }

      EventQueue.invokeLater(() => {
        setVisible(false)
      })
    }

    setVisible(true)

    if (abort) {
      ProgressResult.Aborted
    } else if (getProgress >= 1.0) {
      ProgressResult.Completed
    } else {
      ProgressResult.Canceled
    }
  }

  override def syncTheme(theme: ColorTheme): Unit = {
    getContentPane.setBackground(theme.windowBackground)

    label.setForeground(theme.windowText)

    progressBar.syncTheme(theme)
    cancelButton.syncTheme(theme)
  }

  private class ProgressBar extends JPanel with Transparent with ThemeSync {
    private var progress = -1.0

    private var backgroundColor = Color.WHITE
    private var foregroundColor = Color.WHITE

    private val start = System.currentTimeMillis

    def setValue(value: Double): Unit = {
      progress = value

      repaint()
    }

    override def getPreferredSize: Dimension =
      new Dimension(150, Utils.CornerDiameter)

    override def paintComponent(g: Graphics): Unit = {
      val g2d = Utils.initGraphics2D(g)

      g2d.setColor(backgroundColor)
      g2d.fillRoundRect(0, 0, getWidth, getHeight, Utils.CornerDiameter, Utils.CornerDiameter)

      if (progress == -1.0) {
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
