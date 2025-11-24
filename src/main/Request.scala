// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import java.io.File
import java.nio.file.Files

import scala.util.{ Failure, Try }

import sttp.client4.{ asFileAlways, basicRequest, DefaultSyncBackend, quickRequest, UriContext }

import ujson.Value

object Request {
  private val base = "http://localhost:5000/"

  def json(path: String, body: Value): Try[Value] = {
    Try(quickRequest.post(uri"$base$path").contentType("application/json").body(ujson.write(body))
                    .send(DefaultSyncBackend())).flatMap {
      case response if response.isSuccess =>
        Try(ujson.read(response.body))

      case _ =>
        Failure(new Exception)
    }
  }

  def file(path: String, body: Value): Try[File] = {
    Try(basicRequest.post(uri"$base$path").contentType("application/json").body(ujson.write(body))
                    .response(asFileAlways(Files.createTempFile(null, null).toFile))
                    .send(DefaultSyncBackend())).collect {
      case response if response.isSuccess =>
        response.body
    }
  }
}
