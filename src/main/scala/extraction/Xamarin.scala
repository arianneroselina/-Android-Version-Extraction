package extraction

import com.typesafe.scalalogging.Logger
import play.api.libs.json._
import tools.Util.{findFileInAssemblies, findFileInLib}
import vulnerability.Xamarin.getVulnerabilities

import java.io.{BufferedReader, IOException, InputStreamReader}
import java.nio.file.Paths
import scala.collection.mutable.ArrayBuffer
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
      val soFileName = "libxa-internal-api.so"
      val soFilePath = findFileInLib(folderPath, soFileName)

      // search for Java.Interop.dll
      val dllFileName = "Java.Interop.dll"
      val dllFilePath = findFileInAssemblies(folderPath, dllFileName)

      // Both files are not found
      if ((soFilePath == null || soFilePath.isEmpty) && (dllFilePath == null || dllFilePath.isEmpty)) {
        logger.warn(s"Neither $soFileName nor $dllFileName is found in $folderPath lib and assemblies directory")
        return null
      }
      logger.info("Xamarin implementation found")

      // check if both or only one are/is found
      val paths = ArrayBuffer[String]()
      val types = ArrayBuffer[String]()
      if (soFilePath != null && soFilePath.nonEmpty) {
        paths += soFilePath
        if (soFilePath.contains("arm64-v8a")) types += "arm64-v8a"
        else if (soFilePath.contains("armeabi-v7a")) types += "armeabi-v7a"
      }
      if (dllFilePath != null && dllFilePath.nonEmpty){
        paths += dllFilePath
        types += "assemblies"
      }

      // run certutil
      val bufferedReaders = new Array[BufferedReader](paths.length)
      for (i <- paths.indices) {
        val processBuilder = new ProcessBuilder("certutil", "-hashfile", paths(i), "SHA256")
        val process = processBuilder.start

        // prepare to read the output
        val stdout = process.getInputStream
          bufferedReaders(i) = new BufferedReader(new InputStreamReader(stdout))
      }

      // extract the Xamarin version
      extractXamarinVersion(bufferedReaders, types)
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
   * @param readers the buffered reader(s) of the output from certutil execution(s)
   * @param types the lib directory type arm64-v8a or armeabi-v7a for libxa-internal-api.so
   *              and/or assemblies for Java.Interop.dll
   */
  def extractXamarinVersion(readers: Array[BufferedReader], types: ArrayBuffer[String]): Unit = {
    try {
      for (i <- readers.indices) {
        var line = readers(i).readLine
        breakable {
          while (line != null) {
            if (!line.contains("SHA256") && !line.contains("CertUtil")) {
              val fileHash = line

              // check which version the hash belongs to
              val bufferedSource = io.Source.fromFile(
                Paths.get(".").toAbsolutePath + "/src/files/hashes/xamarin/" + types(i) + ".csv")
              for (csvLine <- bufferedSource.getLines) {
                val cols = csvLine.split(',').map(_.trim)
                if (cols(1).equals(fileHash) && !xamarinVersion.contains(cols(0))) {
                  xamarinVersion = xamarinVersion :+ cols(0)
                }
              }
              bufferedSource.close
            }

            line = readers(i).readLine
          }
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
