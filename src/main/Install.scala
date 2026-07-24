// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.installer

import com.dynatrace.hash4j.hashing.Hashing

import java.awt.Frame
import java.io.{ ByteArrayOutputStream, File, InputStream }
import java.net.{ URI, URLConnection }
import java.nio.file.{ Files, Path, Paths, StandardOpenOption }
import java.nio.file.attribute.PosixFilePermission
import java.util.HashSet
import java.util.concurrent.Executors

import org.apache.commons.compress.archivers.zip.ZipFile

import scala.concurrent.{ Await, ExecutionContext, Future, Promise }
import scala.concurrent.duration.Duration
import scala.util.Try

import ujson.{ Obj, Value }

object Install {
  private implicit val ec: ExecutionContext = ExecutionContext.global

  def installVersion(frame: Frame, version: String): Unit = {
    val root = Paths.get(Utils.appRoot, s"NetLogo $version")

    Request.json("version", Obj(
      "os" -> Utils.os.name,
      "arch" -> Utils.arch,
      "version" -> version
    )).map(_.str).toOption.orElse {
      new OptionPane(frame, "Error", "Error downloading files from server.", Array("OK"))

      None
    }.flatMap(downloadVersion(frame, version, _, root)).foreach { data =>
      if (installFull(frame, "Install", s"Installing NetLogo $version...", data, root))
        new OptionPane(frame, "Install", "Installation complete.", Array("OK"))
    }
  }

  private def downloadVersion(frame: Frame, version: String, url: String, dest: Path): Option[Array[Byte]] = {
    val progress = new ProgressDialog(frame, "Install", s"Downloading NetLogo $version...")

    val output = new ByteArrayOutputStream

    Future {
      val connection: URLConnection = new URI(url).toURL.openConnection
      val input: InputStream = connection.getInputStream
      val length: Int = connection.getContentLength

      while (output.size < length) {
        if (progress.abortRequested) {
          input.close()

          throw new InterruptedException
        }

        output.write(input.readNBytes(1024))

        progress.setProgress(output.size.toDouble / length)
      }

      input.close()
      output.close()

      progress.setProgress(1.0)
    }.recover(_ => progress.requestAbort())

    progress.trackProgress() match {
      case ProgressResult.Completed =>
        Option(output.toByteArray)

      case ProgressResult.Canceled =>
        progress.requestAbort()

        None

      case _ =>
        new OptionPane(frame, "Error", "Error downloading files from server.", Array("OK"))

        None
    }
  }

  def getUpdates(frame: Frame, title: String, version: String, checksums: Map[String, String]): Option[Seq[Update]] = {
    val progress = new ProgressDialog(frame, title, "Requesting update from server...")

    val updates = Promise[Seq[Update]]()

    Future {
      Request.json("update", Obj(
        "os" -> Utils.os.name,
        "arch" -> Utils.arch,
        "version" -> version,
        "checksums" -> checksums
      ), timeout = 30).map(_.arr.map(parseUpdate).toSeq).foreach(updates.success)

      progress.setProgress(1.0)
    }.recover(_ => progress.setProgress(1.0))

    progress.trackProgress() match {
      case ProgressResult.Completed if updates.isCompleted =>
        Option(Await.result(updates.future, Duration.Inf))

      case ProgressResult.Canceled =>
        None

      case _ =>
        new OptionPane(frame, "Error", "Error requesting update from server.", Array("OK"))

        None
    }
  }

  private def parseUpdate(json: Value): Update = {
    val obj: Obj = json.obj

    Update(obj("path").str, obj("url").str, obj("length").num.toLong)
  }

  def installFull(frame: Frame, title: String, message: String, data: Array[Byte], dest: Path): Boolean = {
    val progress = new ProgressDialog(frame, title, message)

    Future(updateFromZip(data, dest, progress)).recover { ex =>
      println(ex)

      progress.requestAbort()

      Utils.deleteRecursive(dest.toFile)
    }

    progress.trackProgress() match {
      case ProgressResult.Completed =>
        true

      case ProgressResult.Canceled =>
        progress.requestAbort()

        false

      case _ =>
        new OptionPane(frame, "Error", "Error installing files.", Array("OK"))

        false
    }
  }

  def updateFromFiles(frame: Frame, title: String, message: String, updates: Seq[Update], dest: Path): Boolean = {
    if (updates.isEmpty) {
      new OptionPane(frame, title, "Installation is already up to date.", Array("OK"))

      return false
    }

    val progress = new ProgressDialog(frame, title, message)

    val totalLength: Long = updates.map(_.length).sum + 1
    var processed = 0L

    implicit val context: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(20))

    Future.traverse(updates) {
      case Update(path, url, length) =>
        Future {
          if (progress.abortRequested)
            throw new InterruptedException

          try {
            val fullPath: Path = dest.resolve(path)
            val stream: InputStream = new URI(url).toURL.openStream

            Files.createDirectories(fullPath.getParent)
            Files.write(fullPath, stream.readAllBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)

            stream.close()

            processed += length

            progress.setProgress(processed.toDouble / totalLength)
          } catch {
            case _ => progress.requestAbort()
          }
        }
    }.foreach(_ => progress.setProgress(1.0))

    progress.trackProgress() match {
      case ProgressResult.Completed =>
        true

      case ProgressResult.Canceled =>
        progress.requestAbort()

        false

      case _ =>
        new OptionPane(frame, "Error", "Error downloading files from server.", Array("OK"))

        false
    }
  }

  private def updateFromZip(bytes: Array[Byte], dest: Path, progress: ProgressDialog): Unit = {
    val builder = new ZipFile.Builder

    builder.setByteArray(bytes)

    val input = builder.get

    var processed = 0L

    input.stream.forEach { entry =>
      if (progress.abortRequested) {
        input.close()

        throw new InterruptedException
      }

      if (!entry.isDirectory) {
        val relativePath = Paths.get(entry.getName)
        val localPath = dest.resolve(relativePath)

        localPath.toFile.getParentFile.mkdirs()

        val stream = input.getInputStream(entry)

        Files.write(localPath, stream.readAllBytes())

        stream.close()

        if (Utils.os != OS.Windows) {
          val mode = entry.getUnixMode

          val perms = new HashSet[PosixFilePermission]()

          if ((mode & 0400) != 0) perms.add(PosixFilePermission.OWNER_READ)
          if ((mode & 0200) != 0) perms.add(PosixFilePermission.OWNER_WRITE)
          if ((mode & 0100) != 0) perms.add(PosixFilePermission.OWNER_EXECUTE)

          if ((mode & 0040) != 0) perms.add(PosixFilePermission.GROUP_READ)
          if ((mode & 0020) != 0) perms.add(PosixFilePermission.GROUP_WRITE)
          if ((mode & 0010) != 0) perms.add(PosixFilePermission.GROUP_EXECUTE)

          if ((mode & 0004) != 0) perms.add(PosixFilePermission.OTHERS_READ)
          if ((mode & 0002) != 0) perms.add(PosixFilePermission.OTHERS_WRITE)
          if ((mode & 0001) != 0) perms.add(PosixFilePermission.OTHERS_EXECUTE)

          Files.setPosixFilePermissions(localPath, perms)
        }
      }

      processed += entry.getSize

      progress.setProgress(processed.toDouble / bytes.size)
    }

    input.close()

    progress.setProgress(1.0)
  }

  def verifyFiles(frame: Frame, title: String, root: File): Option[Map[String, String]] = {
    val files: Array[File] = Utils.listFilesRecursive(root).filterNot { file =>
      file.isDirectory || file.getName == ".checksum"
    }

    val total = files.foldLeft(0L)(_ + _.length)
    var processed = 0

    var checksums = Try(Map(
      ".checksum" -> Files.readString(root.toPath.resolve(".checksum")).trim
    )).getOrElse(Map())

    val progress = new ProgressDialog(frame, title, "Verifying files...")

    Future {
      files.foreach { file =>
        if (progress.abortRequested)
          throw new InterruptedException

        val path = file.toPath
        val relativePath = root.toPath.relativize(path).toString.replace("\\", "/")

        val bytes = Files.readAllBytes(path)

        checksums = checksums + (relativePath -> Hashing.xxh3_64.hashBytesToLong(bytes).toString)

        processed += bytes.size

        progress.setProgress(processed.toDouble / total)
      }

      progress.setProgress(1.0)
    }.recover(_ => progress.requestAbort())

    progress.trackProgress() match {
      case ProgressResult.Completed =>
        Some(checksums)

      case ProgressResult.Canceled =>
        progress.requestAbort()

        None

      case _ =>
        new OptionPane(frame, "Error", "Error verifying installation.", Array("OK"))

        None
    }
  }
}
