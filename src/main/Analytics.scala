// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import java.util.UUID

import scala.concurrent.duration.DurationInt

import sttp.client4.DefaultFutureBackend
import sttp.client4.quick.{ quickRequest, UriContext }

import telemetry.TelemetryEventV1

import ujson.{ Obj, Str, write }

enum AnalyticsNetLogoEvent { case
  DownloadNew,
  AddExisting,
  Update,
  Repair,
  Uninstall,
  SetDefault
}

enum AnalyticsInstallerEvent { case
  Update
}

object Analytics {
  private val uuid: UUID = Prefs.get("uuid").fold {
    val newUUID: UUID = UUID.randomUUID

    Prefs.put("uuid", newUUID.toString)

    newUUID
  }(UUID.fromString)

  private val developer: Boolean = System.getProperty("installer.release") != "true"

  def sendNetLogoEvent(event: AnalyticsNetLogoEvent, version: String, checksum: Option[String]): Unit = {
    sendEvent(event.ordinal, "netlogo", Obj("version" -> version, checksum.map("checksum" -> Str(_)).toSeq*))
  }

  def sendInstallerEvent(event: AnalyticsInstallerEvent, version: String): Unit = {
    sendEvent(event.ordinal, "installer", Obj("version" -> version))
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
}
