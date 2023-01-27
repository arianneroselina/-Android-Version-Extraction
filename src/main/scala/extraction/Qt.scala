package extraction

import com.typesafe.scalalogging.Logger
import play.api.libs.json._
import tools.Constants.{qtFile, soExtension}
import tools.HexEditor.bytesToHex
import tools.Util.findFilesInLib
import vulnerability.VulnerabilityLinks.getFrameworksVulnerabilities

import java.io.IOException
import java.nio.file.{Files, Path, Paths}
import java.security.MessageDigest

class Qt(var qtVersion: Array[String] = Array()) {

  var logger: Option[Logger] = None
  var frameworkName = "Qt"

  /**
   * Extract the Qt version from the given APK, if Qt is used.
   *
   * @param folderPath the path to the extracted APK folder
   * @return the mapping of the Qt version
   */
  def extractQtVersion(folderPath: Path, logger: Logger): (String, JsValue) = {
    this.logger = Some(logger)
    logger.info(s"Starting $frameworkName version extraction")

    try {
      // search for libQt*Core*.so
      val filePaths = findFilesInLib(folderPath, qtFile + soExtension)

      // check which lib is the returned libQt*Core*.so in
      var libType = ""
      if (filePaths(0).contains("arm64-v8a")) libType = "arm64-v8a"
      else if (filePaths(0).contains("armeabi-v7a")) libType = "armeabi-v7a"
      else if (filePaths(0).contains("x86")) libType = "x86"
      else if (filePaths(0).contains("x86_64")) libType = "x86_64"

      // extract the Qt version
      extractQtVersion(filePaths(0), libType)
      logger.info(s"$frameworkName version extraction finished")

      // return it as a JSON value
      createJson()
    } catch {
      case e: IOException => logger.error(e.getMessage)
        null
    }
  }

  /**
   * Extract the Qt version from a buffered reader
   *
   * @param filePath the file path
   * @param libType  the lib directory type arm64-v8a, armeabi-v7a, or x86_64
   */
  def extractQtVersion(filePath: String, libType: String): Unit = {
    try {
      // hash the file
      val b = Files.readAllBytes(Paths.get(filePath))
      val hash = bytesToHex(MessageDigest.getInstance("SHA256").digest(b)).mkString("")

      // check which version the hash belongs to
      val bufferedSource = io.Source.fromFile(
        Paths.get(".").toAbsolutePath + s"/src/files/hashes/$frameworkName/$libType.csv")
      for (csvLine <- bufferedSource.getLines) {
        val cols = csvLine.split(',').map(_.trim)
        if (cols(1).equals(hash) && !qtVersion.contains(cols(0))) {
          qtVersion = qtVersion :+ cols(0)
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
   * @return the mapping of the Qt version
   */
  def createJson(): (String, JsValue) = {
    var versions = Json.obj()
    var writeVersion = ""

    if (qtVersion.nonEmpty) {
      for (i <- 0 until qtVersion.length) {
        if (i == 0) {
          writeVersion = qtVersion(i)
        } else {
          writeVersion += ", " + qtVersion(i)
        }
        val links = getFrameworksVulnerabilities(frameworkName, qtVersion(i))
        versions = versions + (qtVersion(i) -> Json.toJson(links))
      }
    } else {
      val msg = s"No $frameworkName version found, perhaps too old or too new?"
      logger.get.warn(msg)
      writeVersion = msg
    }

    frameworkName -> Json.obj("Version" -> writeVersion, "Vulnerabilities" -> versions)
  }
}
