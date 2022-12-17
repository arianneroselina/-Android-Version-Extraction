package extraction

import com.typesafe.scalalogging.Logger
import play.api.libs.json._
import tools.Util.findFilesInLib
import vulnerability.Qt.getVulnerabilities

import java.io.{BufferedReader, IOException, InputStreamReader}
import java.nio.file.Paths
import scala.util.control.Breaks.breakable

class Qt(var qtVersion: Array[String] = Array()) {

  var logger: Option[Logger] = None

  /**
   * Extract the Qt version from the given APK, if Qt is used.
   *
   * @param folderPath the path to the extracted APK folder
   * @return the mapping of the Qt version
   */
  def extractQtVersion(folderPath: String, logger: Logger): (String, JsValue) = {
    this.logger = Some(logger)
    logger.info("Starting Qt version extraction")

    try {
      // search for libQt*Core*.so
      val fileName = """libQt.*Core.*.so"""
      val filePaths = findFilesInLib(folderPath, fileName)

      // no libQt*Core*.so found
      if (filePaths == null || filePaths.isEmpty) {
        logger.warn(s"$fileName is not found in $folderPath lib directory")
        return null
      }
      logger.info("Qt implementation found")

      // check which lib is the returned libQt*Core*.so in
      var libType = ""
      if (filePaths(0).contains("arm64-v8a")) libType = "arm64-v8a"
      else if (filePaths(0).contains("armeabi-v7a")) libType = "armeabi-v7a"
      else if (filePaths(0).contains("x86")) libType = "x86"
      else if (filePaths(0).contains("x86_64")) libType = "x86_64"

      // run certutil
      val processBuilder = new ProcessBuilder("certutil", "-hashfile", filePaths(0), "SHA256")
      val process = processBuilder.start

      // prepare to read the output
      val stdout = process.getInputStream
      val reader = new BufferedReader(new InputStreamReader(stdout))

      // extract the Qt version
      extractQtVersion(reader, libType)
      logger.info("Qt version extraction finished")

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
   * @param reader the buffered reader of the output from certutil execution
   * @param libType the lib directory type arm64-v8a, armeabi-v7a, or x86_64
   */
  def extractQtVersion(reader: BufferedReader, libType: String): Unit = {
    try {
      var line = reader.readLine
      breakable {
        while (line != null) {
          if (!line.contains("SHA256") && !line.contains("CertUtil")) {
            val fileHash = line

            // check which version the hash belongs to
            val bufferedSource = io.Source.fromFile(
              Paths.get(".").toAbsolutePath + "/src/files/hashes/qt/" + libType + ".csv")
            for (csvLine <- bufferedSource.getLines) {
              val cols = csvLine.split(',').map(_.trim)
              if (cols(1).equals(fileHash) && !qtVersion.contains(cols(0))) {
                qtVersion = qtVersion :+ cols(0)
              }
            }
            bufferedSource.close
          }

          line = reader.readLine
        }
      }
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
        if (i == 0 ) {
          writeVersion = qtVersion(i)
        } else {
          writeVersion += ", " + qtVersion(i)
        }
        val links = getVersionVulnerability(qtVersion(i))
        versions = versions + (qtVersion(i) -> Json.toJson(links))
      }
    } else {
      val msg = "No Qt version found, perhaps too old or too new?"
      logger.get.warn(msg)
      writeVersion = msg
    }

    "Qt" -> Json.obj("Version" -> writeVersion, "Vulnerabilities" -> versions)
  }

  def getVersionVulnerability(version: String): Array[String] = {
    getVulnerabilities(version)
  }
}
