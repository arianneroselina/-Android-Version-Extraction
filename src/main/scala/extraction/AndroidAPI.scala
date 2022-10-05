package extraction

import vulnerability.AndroidAPI.getVulnerabilities

import java.io.{BufferedReader, IOException, InputStreamReader}
import java.nio.file.{Files, Paths}
import play.api.libs.json._

import reflect.io._
import Path._
import scala.util.control.Breaks.{break, breakable}

class AndroidAPI(var minSdkVersion: Int = -1, var targetSdkVersion: Int = -1, var compileSdkVersion: Int = -1) {

  /**
   * Extract minSdkVersion, targetSdkVersion, and compileSdkVersion from the given APK
   *
   * @param apkFilePath the APK file path
   * @return the JSON value
   */
  def extractAndroidAPIVersion(apkFilePath: String): JsValue = {
    try {
      // get the aapt.exe path
      val aaptPath = findAaptPath("aapt.exe")
      if (aaptPath == null) {
        println(Console.RED + s"aapt.exe command not found at path $aaptPath")
        sys.exit(1)
      }

      // run aapt.exe
      val processBuilder = new ProcessBuilder(aaptPath, "list", "-a", apkFilePath)
      val process = processBuilder.start

      // prepare to read the output
      val stdout = process.getInputStream
      val reader = new BufferedReader(new InputStreamReader(stdout))

      // extract the android versions
      extractSdkVersions(reader)
      // return it as a JSON value
      createJson()
    } catch {
      case e: IOException => println(Console.RED + e.getMessage)
                             null
    }
  }

  /**
   * Find the path of the given file anywhere in Android/Sdk/build-tools
   *
   * @param fileName the file (e.g. aapt.exe)
   * @return the path
   */
  def findAaptPath(fileName: String): String = {
    val searchDirs = List(
      System.getenv("LOCALAPPDATA"), // User/AppData/Local
      System.getenv("ProgramFiles"), // Program Files
      System.getenv("ProgramFiles(X86)"), // Program Files (x86)
      System.getenv("APPDATA") // User/AppData/Roaming
    )

    for (searchDir <- searchDirs) {
      val androidDirs = searchDir.toDirectory.dirs.map(_.path).filter(name => name matches """.*Android.*""")
      for (androidDir <- androidDirs) {
        val sdkDirs = androidDir.toDirectory.dirs.map(_.path).filter(name => name matches """.*Sdk.*""")
        for (sdkDir <- sdkDirs) {
          val buildToolsDirs = sdkDir.toDirectory.dirs.map(_.path).filter(name => name matches """.*build-tools.*""")
          for (buildToolsDir <- buildToolsDirs) {
            try {
              val aaptPath = buildToolsDir.toDirectory.deepFiles
                             .filter(file => file.name.equals(fileName))
                             .map(_.path)
                             .next()
              if (Files.exists(Paths.get(aaptPath))) {
                // found unique file at DIR/Android
                return aaptPath
              }
            } catch {
              case _: Exception => // do nothing
            }
          }
        }
      }
    }
    null // file not found
  }

  /**
   * Extract the minSdkVersion, targetSdkVersion, and compileSdkVersion from a buffered reader
   *
   * @param reader the buffered reader of the output from aapt execution
   */
  def extractSdkVersions(reader: BufferedReader): Unit = {
    // constant patterns
    val MinPattern = ".*minSdkVersion\\(.*".r
    val TargetPattern = ".*targetSdkVersion\\(.*".r
    val CompilePattern = ".*compileSdkVersion\\(.*".r

    try {
      var line = reader.readLine
      breakable {
        while (line != null) {
          if (line.contains("SdkVersion(")) {
            val versionStr = line.split(')')(2).substring(2)
            val version = Integer.parseInt(versionStr, 16) // convert hex to int

            line match {
              case MinPattern() => minSdkVersion = version
              case TargetPattern() => targetSdkVersion = version
              case CompilePattern() => compileSdkVersion = version
              case _ =>
            }
          }

          if (minSdkVersion >= 0 && targetSdkVersion >= 0 && compileSdkVersion >= 0) break
          line = reader.readLine
        }
      }
    } catch {
      case e: IOException => println(Console.RED + e.getMessage)
    }
  }

  /**
   * Create a JSON value from this class' object
   *
   * @return the JSON value
   */
  def createJson(): JsValue = {
    val range = targetSdkVersion - minSdkVersion

    var versions = Json.obj()
    for (i <- 0 to range) {
      val links = getVersionVulnerability(minSdkVersion + i)
      versions = versions + ((minSdkVersion + i).toString -> Json.toJson(links))
    }

    Json.obj(
      "AndroidAPI" -> Json.obj("minSdkVersion" -> minSdkVersion,
        "targetSdkVersion" -> targetSdkVersion,
        "compileSdkVersion" -> compileSdkVersion,
        "Vulnerabilities" -> versions
      ),
      "inherit" -> true
    )
  }

  def getVersionVulnerability(version: Int): Array[String] = {
    getVulnerabilities(version)
  }
}
