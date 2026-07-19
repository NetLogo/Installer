// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

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
}
