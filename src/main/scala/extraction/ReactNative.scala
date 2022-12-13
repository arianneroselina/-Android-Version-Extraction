package extraction

import com.typesafe.scalalogging.Logger
import play.api.libs.json._
import tools.Util.findFilesInLib
import vulnerability.ReactNative.getVulnerabilities

import scala.collection.immutable.HashMap
import java.io.{BufferedReader, IOException, InputStreamReader}
import java.nio.file.Paths
import scala.util.control.Breaks.breakable

class ReactNative(var reactNativeVersion: Array[String] = Array()) {

  var logger: Option[Logger] = None

  /**
   * Extract the React Native version from the given APK, if React Native is used.
   *
   * @param folderPath the path to the extracted APK folder
   * @return the mapping of the React Native version
   */
  def extractReactNativeVersion(folderPath: String, logger: Logger): (String, JsValue) = {
    this.logger = Some(logger)
    logger.info("Starting React Native version extraction")

    try {
      // search for libreact*.so
      val fileName = """libreact.*.so"""
      val filePaths = findFilesInLib(folderPath, fileName)

      // no libreact*.so found
      if (filePaths == null || filePaths.isEmpty) {
        logger.warn(s"$fileName is not found in $folderPath lib directory")
        return null
      }
      logger.info("React Native implementation found")

      // check which lib is the returned libreact*.so in
      var libType = ""
      if (filePaths(0).contains("arm64-v8a")) libType = "arm64-v8a"
      else if (filePaths(0).contains("armeabi-v7a")) libType = "armeabi-v7a"
      else if (filePaths(0).contains("x86")) libType = "x86"
      else if (filePaths(0).contains("x86_64")) libType = "x86_64"

      var versionWeight = new HashMap[String, Int]()

      // run certutil
      for (filePath <- filePaths) {
        val f = filePath.split(Array('\\', '/'))
        val fileName = f(f.length-1)

        val processBuilder = new ProcessBuilder("certutil", "-hashfile", filePath, "SHA256")
        val process = processBuilder.start

        // prepare to read the output
        val stdout = process.getInputStream
        val reader = new BufferedReader(new InputStreamReader(stdout))

        // extract the (most likely) React Native version
        versionWeight = extractReactNativeVersion(reader, libType, fileName, versionWeight)
      }

      // find and filter versions by the maximum weight
      val maxValue = versionWeight.maxBy(item => item._2)
      val writeVersion = versionWeight.filter(item => item._2 == maxValue._2)
      writeVersion.foreach(v => {
        // only write the version if it has the maximum weight compared to the other versions
        reactNativeVersion = reactNativeVersion :+ v._1
      })
      logger.info("React Native version extraction finished")

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
   * @param reader the buffered reader of the output from certutil execution
   * @param libType the lib directory type arm64-v8a, armeabi-v7a, x86, or x86_64
   * @param fileName the filename
   */
  def extractReactNativeVersion(reader: BufferedReader, libType: String, fileName: String,
                                versionWeight: HashMap[String, Int]): HashMap[String, Int] = {
    try {
      var copiedVersionWeight = versionWeight
      var line = reader.readLine
      breakable {
        while (line != null) {
          if (!line.contains("SHA256") && !line.contains("CertUtil")) {
            val fileHash = line

            // check which version the hash belongs to
            val bufferedSource = io.Source.fromFile(
              Paths.get(".").toAbsolutePath + "/src/files/hashes/react_native/" + libType + ".csv")
            for (csvLine <- bufferedSource.getLines) {
              val cols = csvLine.split(',').map(_.trim)
              if (cols(1).equals(fileName) && cols(2).equals(fileHash) && !reactNativeVersion.contains(cols(0))) {
                // add version's weight by one if hashes match
                val value = if (copiedVersionWeight.contains(cols(0))) copiedVersionWeight(cols(0))+1 else 1
                copiedVersionWeight += (cols(0) -> value)
              }
            }
            bufferedSource.close
          }

          line = reader.readLine
        }
      }

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
        val links = getVersionVulnerability(reactNativeVersion(i))
        versions = versions + (reactNativeVersion(i) -> Json.toJson(links))
      }
    } else {
      val msg = "No React Native version found, perhaps too old or too new?"
      logger.get.warn(msg)
      writeVersion = msg
    }

    "React Native" -> Json.obj("Version" -> writeVersion, "Vulnerabilities" -> versions)
  }

  def getVersionVulnerability(version: String): Array[String] = {
    getVulnerabilities(version)
  }
}
