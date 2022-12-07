package extraction

import com.typesafe.scalalogging.Logger
import play.api.libs.json._
import vulnerability.Flutter.getVulnerabilities

import java.io.{BufferedReader, IOException, InputStreamReader}
import java.nio.file.{Files, Paths}
import scala.reflect.io.Path._
import scala.util.control.Breaks.{break, breakable}

class Flutter(var flutterVersion: Array[String] = Array()) {

  var logger: Option[Logger] = None

  /**
   * Extract the Flutter version from the given APK, if Flutter is used.
   *
   * @param folderPath the path to the extracted APK folder
   * @return the mapping of the Flutter version
   */
  def extractFlutterVersion(folderPath: String, logger: Logger): (String, JsValue) = {
    this.logger = Some(logger)
    logger.info("Starting Flutter version extraction")

    try {
      // search for libflutter.so
      val fileName = "libflutter.so"
      val filePath = findInLib(folderPath, fileName)

      // no libflutter.so found
      if (filePath == null || filePath.isEmpty) {
        logger.warn(s"$fileName is not found in $folderPath lib directory")
        return null
      }
      logger.info("Flutter implementation found")

      // check which lib is the returned libflutter.so in
      var libType = ""
      if (filePath.contains("arm64-v8a")) libType = "arm64-v8a"
      else if (filePath.contains("armeabi-v7a")) libType = "armeabi-v7a"
      else if (filePath.contains("x86_64")) libType = "x86_64"

      // run certutil
      val processBuilder = new ProcessBuilder("certutil", "-hashfile", filePath, "SHA256")
      val process = processBuilder.start

      // prepare to read the output
      val stdout = process.getInputStream
      val reader = new BufferedReader(new InputStreamReader(stdout))

      // extract the Flutter version
      extractFlutterVersion(reader, libType)
      logger.info("Flutter version extraction finished")

      // return it as a JSON value
      createJson()
    } catch {
      case e: IOException => logger.error(e.getMessage)
        null
    }
  }

  /**
   * Find the path of the given file in lib directory.
   *
   * @param folderPath the path to the extracted APK folder
   * @param fileName the filename (e.g. libflutter,so)
   * @return the path
   */
  def findInLib(folderPath: String, fileName: String): String = {
    val libDirs = folderPath.toDirectory.dirs.map(_.path).filter(name => name matches """.*lib""")
    for (libDir <- libDirs) {
      val inLibs = libDir.toDirectory.dirs.map(_.path)
      for (lib <- inLibs) {
        try {
          val filePath = lib.toDirectory.files
            .filter(file => file.name.equals(fileName))
            .map(_.path)
            .next()
          if (Files.exists(Paths.get(filePath))) {
            return filePath
          }
        } catch {
          case _: Exception => // do nothing
        }
      }
    }
    null // file not found
  }

  /**
   * Extract the Flutter version from a buffered reader
   *
   * @param reader the buffered reader of the output from certutil execution
   * @param libType the lib directory type arm64-v8a, armeabi-v7a, or x86_64
   */
  def extractFlutterVersion(reader: BufferedReader, libType: String): Unit = {
    try {
      var line = reader.readLine
      breakable {
        while (line != null) {
          if (!line.contains("SHA256") && !line.contains("CertUtil")) {
            val fileHash = line

            // check which version the hash belongs to
            val bufferedSource = io.Source.fromFile(
              Paths.get(".").toAbsolutePath + "/src/files/hashes/flutter/" + libType + ".csv")
            for (csvLine <- bufferedSource.getLines) {
              val cols = csvLine.split(',').map(_.trim)
              if (cols(1).equals(fileHash) && !flutterVersion.contains(cols(0))) {
                flutterVersion = flutterVersion :+ cols(0)
              }
            }
            bufferedSource.close
          }

          line = reader.readLine
        }
      }
    } catch {
      case e: IOException => logger.get.error(e.getMessage)
    }
  }

  /**
   * Create a JSON from this class' object
   *
   * @return the mapping of the Flutter version
   */
  def createJson(): (String, JsValue) = {
    var versions = Json.obj()
    var writeVersion = ""

    if (flutterVersion.nonEmpty) {
      for (i <- 0 until flutterVersion.length) {
        if (i == 0 ) {
          writeVersion = flutterVersion(i)
        } else {
          writeVersion += ", " + flutterVersion(i)
        }
        val links = getVersionVulnerability(flutterVersion(i))
        versions = versions + (flutterVersion(i) -> Json.toJson(links))
      }
    } else {
      val msg = "No Flutter version found, perhaps too old or too new?"
      logger.get.warn(msg)
      writeVersion = msg
    }

    "Flutter" -> Json.obj("Version" -> writeVersion, "Vulnerabilities" -> versions)
  }

  def getVersionVulnerability(version: String): Array[String] = {
    getVulnerabilities(version)
  }
}
