// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import scala.concurrent.duration.{ Duration, SECONDS }
import scala.util.{ Success, Try }

import sttp.client4.{ DefaultSyncBackend, quickRequest, UriContext }

import ujson.Value

object Request {
  private val base = "https://releases.netlogo.org/"

  def json(path: String, body: Value, timeout: Int = 5): Either[Result, Value] = {
    Try(quickRequest.post(uri"$base$path").contentType("application/json").body(ujson.write(body))
                    .readTimeout(Duration(timeout, SECONDS)).send(DefaultSyncBackend())) match {
      case Success(response) if response.isSuccess =>
        try {
          Right(ujson.read(response.body))
        } catch {
          case _ =>
            Left(Result.Failed("Failed to download files from server."))
        }

      case _ =>
        Left(Result.Failed("Failed to download files from server."))
    }
  }
}

case class Update(path: String, url: String, length: Long, executable: Boolean)
