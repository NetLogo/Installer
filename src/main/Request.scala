// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import java.io.ByteArrayOutputStream
import java.net.{ HttpURLConnection, URL }

import scala.concurrent.duration.{ Duration, SECONDS }
import scala.util.{ Failure, Try }

import sttp.client4.{ DefaultSyncBackend, quickRequest, UriContext }

import ujson.Value

object Request {
  private val base = "http://localhost:5000/"

  def json(path: String, body: Value): Try[Value] = {
    Try(quickRequest.post(uri"$base$path").contentType("application/json").body(ujson.write(body))
                    .readTimeout(Duration(5, SECONDS)).send(DefaultSyncBackend())).flatMap {
      case response if response.isSuccess =>
        Try(ujson.read(response.body))

      case _ =>
        Failure(new Exception)
    }
  }

  def file(path: String, body: Value, setProgress: Double => Unit = _ => {}): Try[Array[Byte]] = {
    Try {
      val connection = new URL(base + path).openConnection.asInstanceOf[HttpURLConnection]

      connection.setRequestMethod("POST")
      connection.setRequestProperty("Content-Type", "application/json")
      connection.setDoOutput(true)

      val output = connection.getOutputStream

      output.write(ujson.write(body).getBytes("UTF-8"))
      output.flush()

      val input = connection.getInputStream
      val length = connection.getContentLength

      val stream = new ByteArrayOutputStream

      while (stream.size < length) {
        stream.write(input.readNBytes(1024))

        setProgress(stream.size.toDouble / length)
      }

      input.close()
      output.close()
      stream.close()

      stream.toByteArray
    }
  }
}
