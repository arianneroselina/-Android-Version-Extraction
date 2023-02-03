package extraction

import extraction.Main.{_logger, _withAndroidGeneral}
import play.api.libs.json.{JsValue, Json}
import tools.Constants._
import tools.Comparison.{versionOlderThan, versionOlderThanForUnity}
import vulnerability.VulnerabilityLinks.{getAndroidVulnerabilities, getFrameworksVulnerabilities, getUnityVulnerabilities}

import java.io.{BufferedWriter, File, FileWriter, IOException}
import java.nio.file.Path
import scala.collection.mutable.ArrayBuffer

class JsonWriter {


  /**
   * Write the output JSON file.
   *
   * @param apkFilePath the given APK file path
   * @param detection   the VulnerabilityDetection object
   * @param android     the AndroidAPI object
   * @param frameworks  the ExtractFrameworkVersions object
   */
  def writeJsonFile(apkFilePath: Path, detection: VulnerabilityDetection, android: AndroidAPI,
                    frameworks: ExtractFrameworkVersions): Unit = {
    _logger.info("Writing output file")
    val file = new File(apkFilePath.toString.replaceFirst("[.][^.]+$", "") + ".json")
    try {
      val bw = new BufferedWriter(new FileWriter(file))

      // write versions and vulnerabilities to a JSON file
      val androidJSON = createAndroidAPIJson(android)
      var print = Json.obj(androidJSON)

      var versions: ArrayBuffer[String] = new ArrayBuffer()
      var byDate: Boolean = false

      if (detection._flutterFound) {
        if (frameworks._frameworkVersions.contains(flutterName)) {
          versions = frameworks._frameworkVersions(flutterName)
          byDate = frameworks._byDates(flutterName)
        }
        print += createJson(flutterName, versions, byDate)
      }
      if (detection._reactNativeFound) {
        if (frameworks._frameworkVersions.contains(reactNativeName)) {
          versions = frameworks._frameworkVersions(reactNativeName)
          byDate = frameworks._byDates(reactNativeName)
        }
        print += createJson(reactNativeName, versions, byDate)
      }
      if (detection._qtFound) {
        if (frameworks._frameworkVersions.contains(qtName)) {
          versions = frameworks._frameworkVersions(qtName)
          byDate = frameworks._byDates(qtName)
        }
        print += createJson(qtName, versions, byDate)
      }
      if (detection._xamarinFound) {
        if (frameworks._frameworkVersions.contains(xamarinName)) {
          versions = frameworks._frameworkVersions(xamarinName)
          byDate = frameworks._byDates(xamarinName)
        }
        print += createJson(xamarinName, versions, byDate)
      }
      if (detection._cordovaFound) {
        if (frameworks._frameworkVersions.contains(cordovaName)) {
          versions = frameworks._frameworkVersions(cordovaName)
          byDate = frameworks._byDates(cordovaName)
        }
        print += createJson(cordovaName, versions, byDate)
      }
      if (detection._unityFound) {
        if (frameworks._frameworkVersions.contains(unityName)) {
          versions = frameworks._frameworkVersions(unityName)
          byDate = frameworks._byDates(unityName)
        }
        print += createJson(unityName, versions, byDate)
      }
      print += "inherit" -> Json.toJson(true)

      bw.write(Json.prettyPrint(print))
      bw.newLine()
      bw.close()

      _logger.info("Finished writing output file")
    } catch {
      case e: IOException => _logger.error(s"writeJsonFile() throws an error with message: ${e.getMessage}")
        sys.exit(-1)
    }
  }

  /**
   * Create a JSON from this class' object
   *
   * @param android the AndroidAPI object
   * @return the mapping of the Android version
   */
  def createAndroidAPIJson(android: AndroidAPI): (String, Json.JsValueWrapper) = {
    val minSdkVersion = android._minSdkVersion
    val targetSdkVersion = android._targetSdkVersion
    val compileSdkVersion = android._compileSdkVersion

    val range = targetSdkVersion - minSdkVersion

    var versions = Json.obj()
    if (targetSdkVersion != -1 && minSdkVersion == -1) {
      // only targetSdkVersion found
      val links = getAndroidVulnerabilities(targetSdkVersion, _withAndroidGeneral)
      versions = versions + (targetSdkVersion.toString -> Json.toJson(links))
    } else if (targetSdkVersion == -1 && minSdkVersion != -1) {
      // only minSdkVersion found
      val links = getAndroidVulnerabilities(minSdkVersion, _withAndroidGeneral)
      versions = versions + (minSdkVersion.toString -> Json.toJson(links))
    } else if (targetSdkVersion != -1 && minSdkVersion != -1) {
      // both versions found
      for (i <- 0 to range) {
        val currentVersion = minSdkVersion + i
        val links = getAndroidVulnerabilities(currentVersion, _withAndroidGeneral)
        versions = versions + (currentVersion.toString -> Json.toJson(links))
      }
    }

    "AndroidAPI" -> Json.obj("minSdkVersion" -> minSdkVersion,
      "targetSdkVersion" -> targetSdkVersion,
      "compileSdkVersion" -> compileSdkVersion,
      "Vulnerabilities" -> versions)
  }

  /**
   * Create a JSON for the given framework and version.
   *
   * @param frameworkName     the framework's name
   * @param frameworkVersions the found versions of the framework
   * @param byDate            true, if the version is got by the compare date method
   * @return the JSON object
   */
  def createJson(frameworkName: String, frameworkVersions: ArrayBuffer[String], byDate: Boolean): (String, JsValue) = {
    var versions = Json.obj()
    var writeVersion = ""

    if (frameworkVersions.nonEmpty) {
      // sort the versions from latest to oldest
      var sortedVersions: ArrayBuffer[String] = null
      if (frameworkName.equals(unityName)) {
        sortedVersions = frameworkVersions.sortWith((x, y) => versionOlderThanForUnity(x, y) == -1)
      } else {
        sortedVersions = frameworkVersions.sortWith((x, y) => versionOlderThan(x, y) == -1)
      }

      // get vulnerability links for each version
      for (i <- sortedVersions.indices) {
        if (i == 0) {
          writeVersion = sortedVersions(i)
        } else {
          writeVersion += ", " + sortedVersions(i)
        }

        var links: Array[String] = null
        if (frameworkName.equals(unityName)) {
          links = getUnityVulnerabilities(sortedVersions(i))
        } else {
          links = getFrameworksVulnerabilities(frameworkName, sortedVersions(i))
        }

        versions = versions + (sortedVersions(i) -> Json.toJson(links))
      }
    } else {
      val msg = s"No $frameworkName version found, perhaps too old or too new?"
      _logger.warn(msg)
      writeVersion = msg
    }

    if (byDate) writeVersion += " (found by APK last modified date)"

    frameworkName -> Json.obj("Version" -> writeVersion, "Vulnerabilities" -> versions)
  }
}
