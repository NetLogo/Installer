// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import scala.concurrent.duration.{ Duration, SECONDS }
import scala.util.{ Failure, Try }

import sttp.client4.{ DefaultSyncBackend, quickRequest, UriContext }

import ujson.Value

object Request {
  private val base = "https://releases.netlogo.org/"

  def json(path: String, body: Value, timeout: Int = 5): Try[Value] = {
    Try(quickRequest.post(uri"$base$path").contentType("application/json").body(ujson.write(body))
                    .readTimeout(Duration(timeout, SECONDS)).send(DefaultSyncBackend())).flatMap {
      case response if response.isSuccess =>
        Try(ujson.read(response.body))

      case _ =>
        Failure(new Exception)
    }
  }
}

case class Update(path: String, url: String, length: Long)
