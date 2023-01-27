package extraction

import com.typesafe.scalalogging.Logger
import play.api.libs.json._
import tools.Constants.{flutterFile, flutterFolders, soExtension}
import tools.HexEditor.bytesToHex
import vulnerability.VulnerabilityLinks.getFrameworksVulnerabilities

import java.io.IOException
import java.nio.file.{Files, Path, Paths}
import java.security.MessageDigest

class Flutter(var flutterVersion: Array[String] = Array()) {

  var logger: Option[Logger] = None
  var frameworkName = "Flutter"

  /**
   * Extract the Flutter version from the given APK, if Flutter is used.
   *
   * @param folderPath the path to the extracted APK folder
   * @return the mapping of the Flutter version
   */
  def extractFlutterVersion(folderPath: Path, logger: Logger): (String, JsValue) = {
    this.logger = Some(logger)
    logger.info(s"Starting $frameworkName version extraction")

    try {
      // search for libflutter.so
      for (flutterFolder <- flutterFolders) {
        val filePath = Paths.get(folderPath + "/lib/" + flutterFolder + "/" + flutterFile + soExtension)
        if (Files.exists(filePath)) {
          // extract the Flutter version
          compareFlutterHashes(filePath, flutterFolder)
        }
      }

      logger.info(s"$frameworkName version extraction finished")

      // return it as a JSON value
      createJson()
    } catch {
      case e: IOException => logger.error(e.getMessage)
        null
    }
  }

  /**
   * Extract the Flutter version from a buffered reader
   *
   * @param filePath the file path
   * @param libType the lib directory type arm64-v8a, armeabi-v7a, or x86_64
   */
  def compareFlutterHashes(filePath: Path, libType: String): Unit = {
    try {
      // hash the file
      val b = Files.readAllBytes(filePath)
      val hash = bytesToHex(MessageDigest.getInstance("SHA256").digest(b)).mkString("")

      // check which version the hash belongs to
      val bufferedSource = io.Source.fromFile(
        Paths.get(".").toAbsolutePath + s"/src/files/hashes/$frameworkName/$libType.csv")
      for (csvLine <- bufferedSource.getLines) {
        val cols = csvLine.split(',').map(_.trim)
        if (cols(1).equals(hash) && !flutterVersion.contains(cols(0))) {
          flutterVersion = flutterVersion :+ cols(0)
        }
      }
      bufferedSource.close
    } catch {
      case e: IOException => logger.get.error(e.getMessage)
    }
  }

  /**
   * Create a JSON from this class' object
   *
   * @return the mapping of the Flutter version
   */
  def createJson(): (String, JsValue) = {
    var versions = Json.obj()
    var writeVersion = ""

    if (flutterVersion.nonEmpty) {
      for (i <- 0 until flutterVersion.length) {
        if (i == 0) {
          writeVersion = flutterVersion(i)
        } else {
          writeVersion += ", " + flutterVersion(i)
        }
        val links = getFrameworksVulnerabilities(frameworkName, flutterVersion(i))
        versions = versions + (flutterVersion(i) -> Json.toJson(links))
      }
    } else {
      val msg = s"No $frameworkName version found, perhaps too old or too new?"
      logger.get.warn(msg)
      writeVersion = msg
    }

    frameworkName -> Json.obj("Version" -> writeVersion, "Vulnerabilities" -> versions)
  }
}
