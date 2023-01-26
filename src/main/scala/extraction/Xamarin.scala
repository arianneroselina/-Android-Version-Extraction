package extraction

import com.typesafe.scalalogging.Logger
import play.api.libs.json._
import tools.Constants.{dllExtension, soExtension, xamarinDllFile, xamarinSoFile, xamarinSoFolders}
import tools.HexEditor.bytesToHex
import tools.Util.findFileInAssemblies
import vulnerability.Xamarin.getVulnerabilities

import java.io.IOException
import java.nio.file.{Files, Path, Paths}
import java.security.MessageDigest
import scala.collection.mutable.ArrayBuffer

class Xamarin(var xamarinVersion: Array[String] = Array()) {

  var logger: Option[Logger] = None

  /**
   * Extract the Xamarin.Android version from the given APK, if Xamarin is used.
   *
   * @param folderPath the path to the extracted APK folder
   * @return the mapping of the Xamarin.Android version
   */
  def extractXamarinVersion(folderPath: Path, logger: Logger): (String, JsValue) = {
    this.logger = Some(logger)
    logger.info("Starting Xamarin.Android version extraction")

    try {
      // search for libxa-internal-api.so
      for (xamarinSoFolder <- xamarinSoFolders) {
        val filePath = Paths.get(folderPath + "/lib/" + xamarinSoFolder + "/" + xamarinSoFile + soExtension)
        if (Files.exists(filePath)) {
          // extract the Xamarin version
          compareXamarinHashes(filePath, xamarinSoFolder)
        }
      }

      // search for Java.Interop.dll
     val filePath = Paths.get(folderPath + "/assemblies/" + xamarinDllFile + dllExtension)
      if (Files.exists(filePath)) {
        // extract the Xamarin version
        compareXamarinHashes(filePath, "assemblies")
      }

      logger.info("Xamarin.Android version extraction finished")

      // return it as a JSON value
      createJson()
    } catch {
      case e: IOException => logger.error(e.getMessage)
        null
    }
  }

  /**
   * Extract the Xamarin.Android version from a buffered reader
   *
   * @param filePath the file path
   * @param libType  the lib directory type arm64-v8a or armeabi-v7a for libxa-internal-api.so
   *                 and/or assemblies for Java.Interop.dll
   */
  def compareXamarinHashes(filePath: Path, libType: String): Unit = {
    try {
      // hash the file
        val b = Files.readAllBytes(filePath)
        val hash = bytesToHex(MessageDigest.getInstance("SHA256").digest(b)).mkString("")

        // check which version the hash belongs to
        val bufferedSource = io.Source.fromFile(
          Paths.get(".").toAbsolutePath + "/src/files/hashes/xamarin/" + libType + ".csv")
        for (csvLine <- bufferedSource.getLines) {
          val cols = csvLine.split(',').map(_.trim)
          if (cols(1).equals(hash) && !xamarinVersion.contains(cols(0))) {
            xamarinVersion = xamarinVersion :+ cols(0)
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
   * @return the mapping of the Xamarin.Android version
   */
  def createJson(): (String, JsValue) = {
    var versions = Json.obj()
    var writeVersion = ""

    if (xamarinVersion.nonEmpty) {
      for (i <- 0 until xamarinVersion.length) {
        if (i == 0) {
          writeVersion = xamarinVersion(i)
        } else {
          writeVersion += ", " + xamarinVersion(i)
        }
        val links = getVersionVulnerability(xamarinVersion(i))
        versions = versions + (xamarinVersion(i) -> Json.toJson(links))
      }
    } else {
      val msg = "No Xamarin.Android version found, perhaps too old or too new?"
      logger.get.warn(msg)
      writeVersion = msg
    }

    "Xamarin.Android" -> Json.obj("Version" -> writeVersion, "Vulnerabilities" -> versions)
  }

  def getVersionVulnerability(version: String): Array[String] = {
    getVulnerabilities(version)
  }
}
