package extraction

import extraction.Main.{apkFilePath, cordovaFound, flutterFound, logger, qtFound, reactNativeFound, unityFound, xamarinFound}
import play.api.libs.json.{JsValue, Json}
import tools.Constants.{cordovaName, flutterName, qtName, reactNativeName, unityName, xamarinName}
import tools.VersionComparison.{olderThan, olderThanForUnity}
import vulnerability.VulnerabilityLinks.{getAndroidVulnerabilities, getFrameworksVulnerabilities, getUnityVulnerabilities}

import java.io.{BufferedWriter, File, FileWriter, IOException}
import scala.collection.mutable.ArrayBuffer

class JsonWriter {


  def writeJsonFile(android: AndroidAPI, frameworks: ExtractFrameworkVersions): Unit = {
    logger.info("Writing output file")
    val file = new File(apkFilePath.replaceFirst("[.][^.]+$", "") + ".json")
    try {
      val bw = new BufferedWriter(new FileWriter(file))

      // write versions and vulnerabilities to a JSON file
      val androidJSON = createAndroidAPIJson(android)
      var print = Json.obj(androidJSON)

      var versions: ArrayBuffer[String] = new ArrayBuffer()
      if (flutterFound) {
        if (frameworks.frameworkVersions.contains(flutterName)) {
          versions = frameworks.frameworkVersions(flutterName)
        }
        print += createJson(flutterName, versions)
      }
      if (reactNativeFound) {
        if (frameworks.frameworkVersions.contains(reactNativeName)) {
          versions = frameworks.frameworkVersions(reactNativeName)
        }
        print += createJson(reactNativeName, versions)
      }
      if (qtFound) {
        if (frameworks.frameworkVersions.contains(qtName)) {
          versions = frameworks.frameworkVersions(qtName)
        }
        print += createJson(qtName, versions)
      }
      if (xamarinFound) {
        if (frameworks.frameworkVersions.contains(xamarinName)) {
          versions = frameworks.frameworkVersions(xamarinName)
        }
        print += createJson(xamarinName, versions)
      }
      if (cordovaFound) {
        if (frameworks.frameworkVersions.contains(cordovaName)) {
          versions = frameworks.frameworkVersions(cordovaName)
        }
        print += createJson(cordovaName, versions)
      }
      if (unityFound) {
        if (frameworks.frameworkVersions.contains(unityName)) {
          versions = frameworks.frameworkVersions(unityName)
        }
        print += createJson(unityName, versions)
      }
      print += "inherit" -> Json.toJson(true)

      bw.write(Json.prettyPrint(print))
      bw.newLine()
      bw.close()
    } catch {
      case e: IOException => logger.error(e.getMessage)
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
    val minSdkVersion = android.minSdkVersion
    val targetSdkVersion = android.targetSdkVersion
    val compileSdkVersion = android.compileSdkVersion
    val withAndroidGeneral = android.withAndroidGeneral

    val range = targetSdkVersion - minSdkVersion

    var versions = Json.obj()
    if (targetSdkVersion != -1 && minSdkVersion == -1) {
      // only targetSdkVersion found
      val links = getAndroidVulnerabilities(targetSdkVersion, withAndroidGeneral)
      versions = versions + (targetSdkVersion.toString -> Json.toJson(links))
    } else if (targetSdkVersion == -1 && minSdkVersion != -1) {
      // only minSdkVersion found
      val links = getAndroidVulnerabilities(minSdkVersion, withAndroidGeneral)
      versions = versions + (minSdkVersion.toString -> Json.toJson(links))
    } else if (targetSdkVersion != -1 && minSdkVersion != -1) {
      // both versions found
      for (i <- 0 to range) {
        val currentVersion = minSdkVersion + i
        val links = getAndroidVulnerabilities(currentVersion, withAndroidGeneral)
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
   * @return the JSON object
   */
  def createJson(frameworkName: String, frameworkVersions: ArrayBuffer[String]): (String, JsValue) = {
    var versions = Json.obj()
    var writeVersion = ""

    if (frameworkVersions.nonEmpty) {
      // sort the versions from latest to oldest
      var sortedVersions: ArrayBuffer[String] = null
      if (frameworkName.equals(unityName)) {
        sortedVersions = frameworkVersions.sortWith((x, y) => olderThanForUnity(x, y) == -1)
      } else {
        sortedVersions = frameworkVersions.sortWith((x, y) => olderThan(x, y) == -1)
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
      logger.warn(msg)
      writeVersion = msg
    }

    frameworkName -> Json.obj("Version" -> writeVersion, "Vulnerabilities" -> versions)
  }
}
