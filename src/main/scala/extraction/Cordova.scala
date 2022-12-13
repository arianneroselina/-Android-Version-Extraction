package extraction

import com.typesafe.scalalogging.Logger
import play.api.libs.json._
import tools.Util.findFile
import vulnerability.Cordova.getVulnerabilities

import java.io.IOException
import scala.util.control.Breaks.{break, breakable}
import scala.io.Source

class Cordova(var cordovaVersion: String = "") {

  var logger: Option[Logger] = None

  /**
   * Extract the Cordova version from the given APK, if Cordova is used.
   *
   * @param folderPath the path to the extracted APK folder
   * @return the mapping of the Cordova version
   */
  def extractCordovaVersion(folderPath: String, logger: Logger): (String, JsValue) = {
    this.logger = Some(logger)
    logger.info("Starting Cordova version extraction")

    try {
      // search for cordova.js
      val fileName = """cordova.js"""
      val filePath = findFile(folderPath + "\\assets\\www", fileName)

      // cordova.js is not found
      if (filePath == null || filePath.isEmpty) {
        logger.warn(s"$fileName is not found in $folderPath directory")
        return null
      }
      logger.info("Cordova implementation found")

      // extract the Cordova version
      extractCordovaVersion(filePath)
      logger.info("Cordova version extraction finished")

      // return it as a JSON value
      createJson()
    } catch {
      case e: IOException => logger.error(e.getMessage)
        null
    }
  }

  /**
   * Extract the Cordova version from cordova.js
   *
   * @param filePath the file path
   */
  def extractCordovaVersion(filePath: String): Unit = {
    try {
      val keyword = "PLATFORM_VERSION_BUILD_LABEL"
      val source = Source.fromFile(filePath)
      breakable {
        for (line <- source.getLines) {
          if (line.contains(keyword)) {
            val trimmed = line.replaceAll("\\s", "")
            val index = trimmed.indexOf("=")
            cordovaVersion = trimmed.substring(index + 2, trimmed.length - 2)
            break
          }
        }
      }
      source.close()
    } catch {
      case _: IndexOutOfBoundsException => logger.get.error("PLATFORM_VERSION_BUILD_LABEL is not specified in cordova.js")
      case e: Exception => logger.get.error(e.getMessage)
    }
  }

  /**
   * Create a JSON from this class' object
   *
   * @return the mapping of the Cordova version
   */
  def createJson(): (String, JsValue) = {
    var versions = Json.obj()
    var writeVersion = ""

    if (cordovaVersion.nonEmpty) {
      writeVersion = cordovaVersion
      val links = getVersionVulnerability(cordovaVersion)
      versions = versions + (cordovaVersion -> Json.toJson(links))
    } else {
      val msg = "No Cordova version found, perhaps too old or too new?"
      logger.get.warn(msg)
      writeVersion = msg
    }

    "Cordova" -> Json.obj("Version" -> writeVersion, "Vulnerabilities" -> versions)
  }

  def getVersionVulnerability(version: String): Array[String] = {
    getVulnerabilities(version)
  }
}
