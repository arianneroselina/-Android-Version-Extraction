package extraction

import extraction.Main._logger
import tools.Constants.{cordovaName, reactNativeName, unityName}
import tools.HexEditor.{openHexFile, toAscii}

import java.io.{BufferedReader, IOException, InputStream, InputStreamReader}
import java.nio.file.Paths
import scala.collection.immutable.HashMap
import scala.collection.mutable.ArrayBuffer
import scala.util.control.Breaks.{break, breakable}

class ExtractFrameworkVersions() {
  var _frameworkVersions: HashMap[String, ArrayBuffer[String]] = HashMap()
  var _byDates: HashMap[String, Boolean] = HashMap()

  /**
   * Compare the given hash with the one in the generated tables.
   * This function is used by Flutter, Qt, Unity, and Xamarin.
   *
   * @param frameworkName the framework's name
   * @param hash          the file path
   * @param libType       the ABI directory type
   */
  def compareHashes(frameworkName: String, hash: String, libType: String): Unit = {
    try {
      // check which version the hash belongs to
      val bufferedSource = io.Source.fromFile(
        Paths.get(".").toAbsolutePath + s"/src/files/hashes/$frameworkName/$libType.csv")
      for (csvLine <- bufferedSource.getLines) {
        val cols = csvLine.split(',').map(_.trim)

        if (cols(1).equals(hash)) {
          // hashes match
          if (_frameworkVersions.contains(frameworkName)) {
            if (!_frameworkVersions(frameworkName).contains(cols(0))) {
              _frameworkVersions(frameworkName) += cols(0)
            }
          } else {
            _frameworkVersions += (frameworkName -> ArrayBuffer(cols(0)))
            _byDates += (frameworkName -> false)
          }
        }
      }
      bufferedSource.close
    } catch {
      case e: IOException => _logger.error(e.getMessage)
    }
  }

  /**
   * Compare the given hash with the one in the generated tables.
   * This function is used by React Native.
   *
   * @param hash          the file path
   * @param fileName      the filename
   * @param libType       the ABI directory type
   */
  def compareReactNativeHashes(hash: String, fileName: String, libType: String): Unit = {
    try {
      // check which version the hash belongs to
      val bufferedSource = io.Source.fromFile(
        Paths.get(".").toAbsolutePath + s"/src/files/hashes/$reactNativeName/$libType.csv")
      for (csvLine <- bufferedSource.getLines) {
        val cols = csvLine.split(',').map(_.trim)

        if (cols(1).equals(fileName) && cols(2).equals(hash)) {
          // hashes match
          if (_frameworkVersions.contains(reactNativeName)) {
            if (!_frameworkVersions(reactNativeName).contains(cols(0))) {
              _frameworkVersions(reactNativeName) += cols(0)
            }
          } else {
            _frameworkVersions += (reactNativeName -> ArrayBuffer(cols(0)))
            _byDates += (reactNativeName -> false)
          }
        }
      }
      bufferedSource.close
    } catch {
      case e: IOException => _logger.error(e.getMessage)
    }
  }

  /**
   * Extract the Cordova version from cordova.js.
   *
   * @param inputStream the input stream of cordova.js file
   */
  def extractCordovaVersion(inputStream: InputStream): Unit = {
    try {
      val keyword = "PLATFORM_VERSION_BUILD_LABEL"

      val reader = new BufferedReader(new InputStreamReader(inputStream))
      breakable {
        while (reader.ready()) {
          val line = reader.readLine()
          if (line.contains(keyword)) {
            val trimmed = line.replaceAll("\\s", "")
            val index = trimmed.indexOf("=")
            // there is only one cordova.js file
            _frameworkVersions += (cordovaName -> ArrayBuffer(trimmed.substring(index + 2, trimmed.length - 2)))
            _byDates += (cordovaName -> false)
            break
          }
        }
      }
      reader.close()
      inputStream.close()
    } catch {
      case _: IndexOutOfBoundsException => _logger.error("PLATFORM_VERSION_BUILD_LABEL is not specified in cordova.js")
      case e: Exception => _logger.error(e.getMessage)
    }
  }

  /**
   * Extract the Unity version from any numbered file found.
   *
   * @param inputStream the input stream of cordova.js file*
   */
  def extractUnityVersion(inputStream: InputStream): Unit = {
    try {
      val hexString = openHexFile(inputStream)
      _frameworkVersions += (unityName ->
        ArrayBuffer(toAscii(hexString.substring(40, 62)).filter(s => s.isLetterOrDigit || s.equals('.'))))
      _byDates += (unityName -> false)
      inputStream.close()
    } catch {
      case e: Exception => _logger.error(e.getMessage)
    }
  }

  /**
   * Extract the framework version by comparing the dates of the app creation and the released framework version.
   *
   * @param fileName     the filename
   * @param creationTime the file's date of creation
   */
  def extractFrameworkVersionByDate(fileName: String, creationTime: String): Unit = {

  }
}
