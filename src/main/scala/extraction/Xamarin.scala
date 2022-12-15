package extraction

import com.typesafe.scalalogging.Logger
import play.api.libs.json._
import tools.Util.findFileInLib
import vulnerability.Xamarin.getVulnerabilities

import java.io.{BufferedReader, IOException, InputStreamReader}
import java.nio.file.Paths
import scala.util.control.Breaks.breakable

class Xamarin(var xamarinVersion: Array[String] = Array()) {

  var logger: Option[Logger] = None

  /**
   * Extract the Xamarin version from the given APK, if Xamarin is used.
   *
   * @param folderPath the path to the extracted APK folder
   * @return the mapping of the Xamarin version
   */
  def extractXamarinVersion(folderPath: String, logger: Logger): (String, JsValue) = {
    this.logger = Some(logger)
    logger.info("Starting Xamarin version extraction")

    try {
      // search for libxa-internal-api.so
      val fileName = "libxa-internal-api.so"
      val filePath = findFileInLib(folderPath, fileName)

      // no libxa-internal-api.so found
      if (filePath == null || filePath.isEmpty) {
        logger.warn(s"$fileName is not found in $folderPath lib directory")
        return null
      }
      logger.info("Xamarin implementation found")

      // check which lib is the returned libxa-internal-api.so in
      var libType = ""
      if (filePath.contains("arm64-v8a")) libType = "arm64-v8a"
      else if (filePath.contains("armeabi-v7a")) libType = "armeabi-v7a"

      // run certutil
      val processBuilder = new ProcessBuilder("certutil", "-hashfile", filePath, "SHA256")
      val process = processBuilder.start

      // prepare to read the output
      val stdout = process.getInputStream
      val reader = new BufferedReader(new InputStreamReader(stdout))

      // extract the Xamarin version
      extractXamarinVersion(reader, libType)
      logger.info("Xamarin version extraction finished")

      // return it as a JSON value
      createJson()
    } catch {
      case e: IOException => logger.error(e.getMessage)
        null
    }
  }

  /**
   * Extract the Xamarin version from a buffered reader
   *
   * @param reader the buffered reader of the output from certutil execution
   * @param libType the lib directory type arm64-v8a or armeabi-v7a
   */
  def extractXamarinVersion(reader: BufferedReader, libType: String): Unit = {
    try {
      var line = reader.readLine
      breakable {
        while (line != null) {
          if (!line.contains("SHA256") && !line.contains("CertUtil")) {
            val fileHash = line

            // check which version the hash belongs to
            val bufferedSource = io.Source.fromFile(
              Paths.get(".").toAbsolutePath + "/src/files/hashes/xamarin/" + libType + ".csv")
            for (csvLine <- bufferedSource.getLines) {
              val cols = csvLine.split(',').map(_.trim)
              if (cols(1).equals(fileHash) && !xamarinVersion.contains(cols(0))) {
                xamarinVersion = xamarinVersion :+ cols(0)
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
   * @return the mapping of the Xamarin version
   */
  def createJson(): (String, JsValue) = {
    var versions = Json.obj()
    var writeVersion = ""

    if (xamarinVersion.nonEmpty) {
      for (i <- 0 until xamarinVersion.length) {
        if (i == 0 ) {
          writeVersion = xamarinVersion(i)
        } else {
          writeVersion += ", " + xamarinVersion(i)
        }
        val links = getVersionVulnerability(xamarinVersion(i))
        versions = versions + (xamarinVersion(i) -> Json.toJson(links))
      }
    } else {
      val msg = "No Xamarin version found, perhaps too old or too new?"
      logger.get.warn(msg)
      writeVersion = msg
    }

    "Xamarin" -> Json.obj("Version" -> writeVersion, "Vulnerabilities" -> versions)
  }

  def getVersionVulnerability(version: String): Array[String] = {
    getVulnerabilities(version)
  }
}
