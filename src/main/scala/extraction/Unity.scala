package extraction

import com.typesafe.scalalogging.Logger
import play.api.libs.json._
import tools.Constants.unityFile
import tools.HexEditor.{openHexFile, toAscii}
import vulnerability.Unity.getVulnerabilities

import java.io.IOException
import java.nio.file.{Files, Path, Paths}

class Unity(var unityVersion: String = "") {

  var logger: Option[Logger] = None

  /**
   * Extract the Unity version from the given APK, if Unity is used.
   *
   * @param folderPath the path to the extracted APK folder
   * @return the mapping of the Unity version
   */
  def extractUnityVersion(folderPath: Path, logger: Logger): (String, JsValue) = {
    this.logger = Some(logger)
    logger.info("Starting Unity version extraction")

    try {
      // search for file 0000000000000000f000000000000000
      val filePath = Paths.get(folderPath + "/assets/bin/Data/" + unityFile)
      if (Files.exists(filePath)) {
        // extract the Unity version
        extractUnityVersionFromFile(filePath)
      }

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
  def extractUnityVersionFromFile(filePath: Path): Unit = {
    try {
      val hexString = openHexFile(filePath.toString)
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
