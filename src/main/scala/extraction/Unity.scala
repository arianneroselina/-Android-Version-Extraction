package extraction

import com.typesafe.scalalogging.Logger
import play.api.libs.json._
import tools.HexEditor.{openHexFile, toAscii}
import tools.Util.findFile
import vulnerability.Unity.getVulnerabilities

import java.io.IOException

class Unity(var unityVersion: String = "") {

  var logger: Option[Logger] = None

  /**
   * Extract the Unity version from the given APK, if Unity is used.
   *
   * @param folderPath the path to the extracted APK folder
   * @return the mapping of the Unity version
   */
  def extractUnityVersion(folderPath: String, logger: Logger): (String, JsValue) = {
    this.logger = Some(logger)
    logger.info("Starting Unity version extraction")

    try {
      // search for file 0000000000000000f000000000000000
      val fileName = """0000000000000000f000000000000000"""
      val filePath = findFile(folderPath + "\\assets\\bin\\Data", fileName)

      // The file is not found
      if (filePath == null || filePath.isEmpty) {
        logger.warn(s"$fileName is not found in $folderPath directory")
        return null
      }
      logger.info("Unity implementation found")

      // extract the Unity version
      extractUnityVersion(filePath)
      logger.info("Unity version extraction finished")

      // return it as a JSON value
      createJson()
    } catch {
      case e: IOException => logger.error(e.getMessage)
        null
    }
  }

  /**
   * Extract the Unity version from file 0000000000000000f000000000000000
   *
   * @param filePath the file path
   */
  def extractUnityVersion(filePath: String): Unit = {
    try {
      val hexString = openHexFile(filePath)
      unityVersion = toAscii(hexString.substring(40, 62))
    } catch {
      case e: Exception => logger.get.error(e.getMessage)
    }
  }

  /**
   * Create a JSON from this class' object
   *
   * @return the mapping of the Unity version
   */
  def createJson(): (String, JsValue) = {
    var versions = Json.obj()
    var writeVersion = ""

    if (unityVersion.nonEmpty) {
      writeVersion = unityVersion
      val links = getVersionVulnerability(unityVersion)
      versions = versions + (unityVersion -> Json.toJson(links))
    } else {
      val msg = "No Unity version found, perhaps too old or too new?"
      logger.get.warn(msg)
      writeVersion = msg
    }

    "Unity" -> Json.obj("Version" -> writeVersion, "Vulnerabilities" -> versions)
  }

  def getVersionVulnerability(version: String): Array[String] = {
    getVulnerabilities(version)
  }
}
