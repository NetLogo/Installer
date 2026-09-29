// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import java.util.UUID

import scala.concurrent.duration.DurationInt

import sttp.client4.DefaultFutureBackend
import sttp.client4.quick.{ quickRequest, UriContext }

import telemetry.TelemetryEventV1

import ujson.{ Obj, Str, write }

enum NetLogoEvent { case
  DownloadNew,
  AddExisting,
  Update,
  Repair,
  Uninstall,
  SetDefault
}

enum InstallerEvent { case
  Update
}

sealed abstract trait Result

object Result {
  case object Canceled extends Result
  case object Noop extends Result
  case class Failed(message: String) extends Result
}

object Analytics {
  private val uuid: UUID = Prefs.get("uuid").fold {
    val newUUID: UUID = UUID.randomUUID

    Prefs.put("uuid", newUUID.toString)

    newUUID
  }(UUID.fromString)

  private val developer: Boolean = System.getProperty("installer.release") != "true"

  def sendNetLogoEvent[T](event: NetLogoEvent, version: String, checksum: Option[String], result: Either[Result, T]): Unit = {
    sendEvent(event.ordinal, "netlogo", Obj("version" -> version, ("result" -> resultString(result)) +:
                                            checksum.map("checksum" -> Str(_)).toSeq*))
  }

  def sendInstallerEvent[T](event: InstallerEvent, version: String, result: Either[Result, T]): Unit = {
    sendEvent(event.ordinal, "installer", Obj("version" -> version, "result" -> resultString(result)))
  }

  def sendEvent(event: Int, endpoint: String, payload: Obj): Unit = {
    val pbEvent = TelemetryEventV1(
      formatVersion = 1,
      uuid1 = uuid.getMostSignificantBits,
      uuid2 = uuid.getLeastSignificantBits,
      isDeveloper = developer,
      eventType = event,
      payload = write(payload)
    )

    quickRequest.post(uri"https://telemetry.netlogo.org/telemetry/v2/installer/$endpoint/upload")
                .body(pbEvent.toByteArray)
                .contentType("application/x-protobuf")
                .readTimeout(15.seconds)
                .send(DefaultFutureBackend())
  }

  private def resultString[T](result: Either[Result, T]): String = {
    result.fold(_ match {
      case Result.Canceled => "canceled"
      case Result.Noop => "noop"
      case Result.Failed(_) => "failed"
    }, _ => "succeeded")
  }
}
