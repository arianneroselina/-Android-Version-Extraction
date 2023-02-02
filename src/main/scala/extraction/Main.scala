package extraction

import com.typesafe.scalalogging.Logger
import org.apache.commons.cli.{DefaultParser, Options}
import tools.Constants._
import tools.HexEditor.bytesToHex

import java.io.IOException
import java.nio.file.{Path, Paths}
import java.security.MessageDigest
import java.util.zip.{ZipEntry, ZipFile}
import scala.jdk.CollectionConverters.EnumerationHasAsScala

object Main {

  var apkFilePath = ""
  var apkFilePaths = ""
  var zipFile: Option[ZipFile] = None
  val options = new Options()

  // keep track of found frameworks
  var cordovaFound = false
  var flutterFound = false
  var reactNativeFound = false
  var qtFound = false
  var unityFound = false
  var xamarinFound = false


  options.addOption("f", "apk-filepath", true, "input file <apk file path>")
  options.addOption("d", "apk-filepaths", true, "path to file containing <apk file paths>")
  options.addOption("a", "android-general", false, "include general android vulnerability links")

  val logger: Logger = Logger("AndroidVersionExtraction")

  def main(args: Array[String]): Unit = {
    val command = new DefaultParser()

    try {
      val commandline = command.parse(options, args)

      logger.info("Starting android_version_extraction")
      val startTime = System.nanoTime

      // exactly one of the options must be set
      if (!((commandline.hasOption("f") && !commandline.hasOption("d"))
        || (commandline.hasOption("d") && !commandline.hasOption("f")))) {
        logger.error("Either filepath or directory must be specified!")
        sys.exit(1)
      }

      if (commandline.hasOption("f")) {
        apkFilePath = commandline.getOptionValue("f")
        androidAppVulnerabilityDetection(Paths.get(apkFilePath), commandline.hasOption("a"))
      }
      else if (commandline.hasOption("d")) {
        apkFilePaths = commandline.getOptionValue("d")

        try {
          val bufferedSource = io.Source.fromFile(apkFilePaths)
          for (apkFile <- bufferedSource.getLines) {
            androidAppVulnerabilityDetection(Paths.get(apkFile), commandline.hasOption("a"))
          }
          bufferedSource.close
        } catch {
          case e: IOException => logger.error(e.getMessage)
        }
      }

      logger.info("All done")

      // benchmark
      val duration = (System.nanoTime - startTime) / 1e9d
      logger.info("Total Running Time: " + duration)
      val mb = 1024 * 1024
      val runtime = Runtime.getRuntime
      logger.info("Used Memory:  " + (runtime.totalMemory - runtime.freeMemory) / mb + " mb")
      //logger.info("** Free Memory:  " + runtime.freeMemory / mb)
      //logger.info("** Total Memory: " + runtime.totalMemory / mb)
      //logger.info("** Max Memory:   " + runtime.maxMemory / mb)
    } catch {
      case e: Throwable => import org.apache.commons.cli.HelpFormatter
        logger.error(e.getMessage)
        val formatter = new HelpFormatter()
        formatter.printHelp("StringDecryption", options)
    }
  }

  def androidAppVulnerabilityDetection(apkFilePath: Path, withAndroidGeneral: Boolean): Unit = {
    // make sure APK file name is given correctly
    if (!apkFilePath.getFileName.toString.endsWith(".apk")) {
      logger.error(s"APK file does not have .apk file ending: $apkFilePath")
      logger.warn("Make sure that filename does not have blank spaces")
      sys.exit(1)
    }
    logger.info("Got the .apk file " + apkFilePath)

    // extract the Android version information
    val android = new AndroidAPI
    android.extractAndroidAPIVersion(apkFilePath.toString, withAndroidGeneral, logger)

    val frameworks = new ExtractFrameworkVersions

    // iterate through the zip file and run framework version extraction if a certain file is found
    zipFile = Some(new ZipFile(apkFilePath.toFile))
    for (entry <- zipFile.get.entries.asScala) {

      if (entry.getName.contains(flutterFile)) {
        // extract the Flutter version information
        if (!flutterFound) logger.info(s"$flutterName implementation found")

        val hash = hashFile(entry)
        val libType = Paths.get(entry.getName).getParent.getFileName.toString
        frameworks.compareHashes(flutterName, hash, libType, logger)

        if (!flutterFound) logger.info(s"$flutterName version extraction finished")
        flutterFound = true
      }

      if (entry.getName.matches(""".*""" + reactNativeFile)) {
        // extract the React Native version information
        if (!reactNativeFound) logger.info(s"$reactNativeName implementation found")

        val hash = hashFile(entry)
        val entryName = Paths.get(entry.getName)
        val fileName = entryName.getFileName.toString
        val libType = entryName.getParent.getFileName.toString
        frameworks.compareReactNativeHashes(hash, fileName, libType, logger)

        if (!reactNativeFound) logger.info(s"$reactNativeName version extraction finished")
        reactNativeFound = true
      }

      if (entry.getName.matches(""".*""" + qtFile)) {
        // extract the Qt version information
        if (!qtFound) logger.info(s"$qtName implementation found")

        val hash = hashFile(entry)
        val libType = Paths.get(entry.getName).getParent.getFileName.toString
        frameworks.compareHashes(qtName, hash, libType, logger)

        if (!qtFound) logger.info(s"$qtName version extraction finished")
        qtFound = true
      }

      if (entry.getName.contains(xamarinSoFile) || entry.getName.contains(xamarinDllFile)) {
        // extract the Xamarin version information
        if (!xamarinFound) logger.info(s"$xamarinName implementation found")

        val hash = hashFile(entry)
        val libType = Paths.get(entry.getName).getParent.getFileName.toString
        frameworks.compareHashes(xamarinName, hash, libType, logger)

        if (!xamarinFound) logger.info(s"$xamarinName version extraction finished")
        xamarinFound = true
      }

      if (entry.getName.contains(cordovaFile)) {
        // extract the Cordova version information
        if (!cordovaFound) logger.info(s"$cordovaName implementation found")
        frameworks.extractCordovaVersion(zipFile.get.getInputStream(entry), logger)
        if (!cordovaFound) logger.info(s"$cordovaName version extraction finished")
        cordovaFound = true
      }

      // extract the Unity version information using the two methods
      if (entry.getName.matches(""".*""" + unityNumberedFile)) {
        if (!unityFound) logger.info(s"$unityName implementation found")
        frameworks.extractUnityVersion(zipFile.get.getInputStream(entry), logger)
        if (!unityFound) logger.info(s"$unityName version extraction finished")
        unityFound = true
      }

      if (entry.getName.contains(unitySoFile)) {
        if (!unityFound) logger.info(s"$unityName implementation found")

        val hash = hashFile(entry)
        val libType = Paths.get(entry.getName).getParent.getFileName.toString
        frameworks.compareHashes(unityName, hash, libType, logger)

        if (!unityFound) logger.info(s"$unityName version extraction finished")
        unityFound = true
      }
    }

    // write the JSON value to JSON file
    (new JsonWriter).writeJsonFile(android, frameworks)
  }

  /**
   * Hash the file at the given entry
   *
   * @param entry the current ZipEntry
   * @return the hash of the file at the entry
   */
  def hashFile(entry: ZipEntry): String = {
    val is = zipFile.get.getInputStream(entry)
    val hash = bytesToHex(MessageDigest.getInstance("SHA256").digest(is.readAllBytes())).mkString("")
    hash
  }
}