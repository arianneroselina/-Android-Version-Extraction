package extraction

import com.typesafe.scalalogging.Logger
import play.api.libs.json._
import tools.Constants.{reactNativeFile, soExtension}
import tools.HexEditor.bytesToHex
import tools.Util.findFilesInLib
import vulnerability.VulnerabilityLinks.getFrameworksVulnerabilities

import scala.collection.immutable.HashMap
import java.io.IOException
import java.nio.file.{Files, Path, Paths}
import java.security.MessageDigest

class ReactNative(var reactNativeVersion: Array[String] = Array()) {

  var logger: Option[Logger] = None
  var frameworkName = "React Native"

  /**
   * Extract the React Native version from the given APK, if React Native is used.
   *
   * @param folderPath the path to the extracted APK folder
   * @return the mapping of the React Native version
   */
  def extractReactNativeVersion(folderPath: Path, logger: Logger): (String, JsValue) = {
    this.logger = Some(logger)
    logger.info(s"Starting $frameworkName version extraction")

    try {
      // search for libreact*.so
      val filePaths = findFilesInLib(folderPath, reactNativeFile + soExtension)

      // check which lib is the returned libreact*.so in
      var libType = ""
      if (filePaths(0).contains("arm64-v8a")) libType = "arm64-v8a"
      else if (filePaths(0).contains("armeabi-v7a")) libType = "armeabi-v7a"
      else if (filePaths(0).contains("x86")) libType = "x86"
      else if (filePaths(0).contains("x86_64")) libType = "x86_64"

      var versionWeight = new HashMap[String, Int]()

      for (filePath <- filePaths) {
        // extract the (most likely) React Native version
        versionWeight = compareReactNativeHashes(filePath, libType, versionWeight)
      }

      // find and filter versions by the maximum weight
      val maxValue = versionWeight.maxBy(item => item._2)
      val writeVersion = versionWeight.filter(item => item._2 == maxValue._2)
      writeVersion.foreach(v => {
        // only write the version if it has the maximum weight compared to the other versions
        reactNativeVersion = reactNativeVersion :+ v._1
      })
      logger.info(s"$frameworkName version extraction finished")

      // return it as a JSON value
      createJson()
    } catch {
      case e: IOException => logger.error(e.getMessage)
        null
    }
  }

  /**
   * Extract the React Native version from a buffered reader
   *
   * @param filePath the file path
   * @param libType  the lib directory type arm64-v8a, armeabi-v7a, x86, or x86_64
   * @param versionWeight the weight for each version
   */
  def compareReactNativeHashes(filePath: String, libType: String, versionWeight: HashMap[String, Int]): HashMap[String, Int] = {
    try {
      // hash the file
      val b = Files.readAllBytes(Paths.get(filePath))
      val hash = bytesToHex(MessageDigest.getInstance("SHA256").digest(b)).mkString("")

      val fileName = Paths.get(filePath).getFileName.toString
      var copiedVersionWeight = versionWeight
      // check which version the hash belongs to
      val bufferedSource = io.Source.fromFile(
        Paths.get(".").toAbsolutePath + s"/src/files/hashes/$frameworkName/$libType.csv")
      for (csvLine <- bufferedSource.getLines) {
        val cols = csvLine.split(',').map(_.trim)
        if (cols(1).equals(fileName) && cols(2).equals(hash) && !reactNativeVersion.contains(cols(0))) {
          // add version's weight by one if hashes match
          val value = if (copiedVersionWeight.contains(cols(0))) copiedVersionWeight(cols(0)) + 1 else 1
          copiedVersionWeight += (cols(0) -> value)
        }
      }
      bufferedSource.close

      copiedVersionWeight
    } catch {
      case e: IOException => logger.get.error(e.getMessage)
        versionWeight
    }
  }

  /**
   * Create a JSON from this class' object
   *
   * @return the mapping of the React Native version
   */
  def createJson(): (String, JsValue) = {
    var versions = Json.obj()
    var writeVersion = ""

    if (reactNativeVersion.nonEmpty) {
      for (i <- 0 until reactNativeVersion.length) {
        if (i == 0) {
          writeVersion = reactNativeVersion(i)
        } else {
          writeVersion += ", " + reactNativeVersion(i)
        }
        val links = getFrameworksVulnerabilities(frameworkName, reactNativeVersion(i))
        versions = versions + (reactNativeVersion(i) -> Json.toJson(links))
      }
    } else {
      val msg = s"No $frameworkName version found, perhaps too old or too new?"
      logger.get.warn(msg)
      writeVersion = msg
    }

    frameworkName -> Json.obj("Version" -> writeVersion, "Vulnerabilities" -> versions)
  }
}
