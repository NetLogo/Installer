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
import scala.sys.process.Process

import ujson.{ Obj, Value }

object Install {
  private implicit val ec: ExecutionContext = ExecutionContext.global

  def installVersion(frame: Frame, version: String, root: Path): Either[Result, Unit] = {
    Request.json("version", Obj(
      "os" -> Utils.os.name,
      "arch" -> Utils.arch,
      "version" -> version
    )).map(_.str).orElse(Left(Result.Failed("Failed to download files from server.")))
      .flatMap(downloadVersion(frame, _, "Install", s"Downloading NetLogo $version..."))
      .flatMap(installFull(frame, "Install", s"Installing NetLogo $version...", _, root, version))
      .map(_ => {})
  }

  private def downloadVersion(frame: Frame, url: String, title: String,
                              message: String): Either[Result, Array[Byte]] = {
    val progress = new ProgressDialog(frame, title, message)

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
        Right(output.toByteArray)

      case ProgressResult.Canceled =>
        progress.requestAbort()

        Left(Result.Canceled)

      case _ =>
        Left(Result.Failed("Failed to download files from server."))
    }
  }

  def getUpdates(frame: Frame, title: String, version: String,
                 checksums: Map[String, String]): Either[Result, Seq[Update]] = {
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
        Right(Await.result(updates.future, Duration.Inf))

      case ProgressResult.Canceled =>
        Left(Result.Canceled)

      case _ =>
        Left(Result.Failed("Failed to request update from server."))
    }
  }

  private def parseUpdate(json: Value): Update = {
    val obj: Obj = json.obj

    Update(obj("path").str, obj("url").str, obj("length").num.toLong, obj("exec").bool)
  }

  private def installFull(frame: Frame, title: String, message: String, data: Array[Byte], dest: Path,
                  version: String): Either[Result, Unit] = {

    val progress = new ProgressDialog(frame, title, message)

    Future(updateFromZip(data, dest, progress)).recover { _ =>
      progress.requestAbort()

      Utils.deleteRecursive(dest.toFile)
    }

    progress.trackProgress() match {
      case ProgressResult.Completed =>
        if (Utils.os == OS.Linux) {
          Utils.loadExecutable("/install/linux/install.sh", ".sh").filterOrElse(helper => {
            Process(Seq("pkexec", "sh", helper.toString, dest.toString, version)).! == 0
          }, Result.Failed("Failed to execute update.")).map(_ => {})
        } else {
          Right({})
        }

      case ProgressResult.Canceled =>
        progress.requestAbort()

        Left(Result.Canceled)

      case _ =>
        Left(Result.Failed("Failed to install files."))
    }
  }

  def updateFromFiles(frame: Frame, title: String, message: String, updates: Seq[Update],
                      dest: Path): Either[Result, Unit] = {
    if (updates.isEmpty) {
      new OptionPane(frame, title, "Installation is already up to date.", Array("OK"))

      return Left(Result.Noop)
    }

    val progress = new ProgressDialog(frame, title, message)

    val totalLength: Long = updates.map(_.length).sum + 1
    var processed = 0L

    implicit val context: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(20))

    Future.traverse(updates) {
      case Update(path, url, length, exec) =>
        Future {
          if (progress.abortRequested)
            throw new InterruptedException

          try {
            val fullPath: Path = dest.resolve(path)
            val stream: InputStream = new URI(url).toURL.openStream

            Files.createDirectories(fullPath.getParent)
            Files.write(fullPath, stream.readAllBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)

            stream.close()

            if (exec && Utils.os != OS.Windows) {
              Files.setPosixFilePermissions(fullPath, new HashSet[PosixFilePermission] {
                add(PosixFilePermission.OWNER_READ)
                add(PosixFilePermission.OWNER_WRITE)
                add(PosixFilePermission.OWNER_EXECUTE)
              })
            }

            processed += length

            progress.setProgress(processed.toDouble / totalLength)
          } catch {
            case _ => progress.requestAbort()
          }
        }
    }.foreach(_ => progress.setProgress(1.0))

    progress.trackProgress() match {
      case ProgressResult.Completed =>
        Right({})

      case ProgressResult.Canceled =>
        progress.requestAbort()

        Left(Result.Canceled)

      case _ =>
        Left(Result.Failed("Failed to download files from server."))
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
        val stream = input.getInputStream(entry)

        Files.createDirectories(localPath.getParent)
        Files.write(localPath, stream.readAllBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)

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

  def verifyFiles(frame: Frame, title: String, config: AppConfig): Either[Result, Map[String, String]] = {
    val files: Array[File] = Utils.listFilesRecursive(config.root).filterNot { file =>
      file.isDirectory || file.getName == ".checksum"
    }

    val total = files.foldLeft(0L)(_ + _.length)
    var processed = 0

    var checksums = config.checksum.fold(Map())(checksum => Map(".checksum" -> checksum))

    val progress = new ProgressDialog(frame, title, "Verifying files...")

    Future {
      files.foreach { file =>
        if (progress.abortRequested)
          throw new InterruptedException

        val path = file.toPath
        val relativePath = config.root.toPath.relativize(path).toString.replace("\\", "/")

        val bytes = Files.readAllBytes(path)

        checksums = checksums + (relativePath -> Hashing.xxh3_64.hashBytesToLong(bytes).toString)

        processed += bytes.size

        progress.setProgress(processed.toDouble / total)
      }

      progress.setProgress(1.0)
    }.recover(_ => progress.requestAbort())

    progress.trackProgress() match {
      case ProgressResult.Completed =>
        Right(checksums)

      case ProgressResult.Canceled =>
        progress.requestAbort()

        Left(Result.Canceled)

      case _ =>
        Left(Result.Failed("Failed to verify installed files."))
    }
  }

  def updateInstaller(frame: Frame, url: String, version: String): Either[Result, Unit] = {
    downloadVersion(frame, url, "Update", "Downloading latest version...").flatMap(unzipInstaller(frame, _))
      .flatMap { path =>

      val ext: String = {
        if (Utils.os == OS.Windows) {
          ".bat"
        } else {
          ".sh"
        }
      }

      Utils.loadExecutable(s"/update/${Utils.os.name}/update$ext", ext).filterOrElse(exec => {
        Process(Seq(exec.toString, ProcessHandle.current.pid.toString, path.toString)).! == 0
      }, Result.Failed("Failed to execute update.")).map(_ => {})
    }
  }

  def unzipInstaller(frame: Frame, data: Array[Byte]): Either[Result, Path] = {
    val path: Path = Files.createTempDirectory(null)

    val progress = new ProgressDialog(frame, "Update", "Installing latest version...")

    Future(updateFromZip(data, path, progress)).recover { _ =>
      progress.requestAbort()

      Utils.deleteRecursive(path.toFile)
    }

    progress.trackProgress() match {
      case ProgressResult.Completed =>
        Right(path)

      case ProgressResult.Canceled =>
        progress.requestAbort()

        Left(Result.Canceled)

      case _ =>
        Left(Result.Failed("Failed to install latest version."))
    }
  }
}
