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

  var _apkFilePath = ""
  var _apkFilePaths = ""
  var _zipFile: Option[ZipFile] = None
  val _options = new Options()

  // keep track of found frameworks
  var _cordovaFound = false
  var _flutterFound = false
  var _reactNativeFound = false
  var _qtFound = false
  var _unityFound = false
  var _xamarinFound = false


  _options.addOption("f", "apk-filepath", true, "input file <apk file path>")
  _options.addOption("d", "apk-filepaths", true, "path to file containing <apk file paths>")
  _options.addOption("a", "android-general", false, "include general android vulnerability links")

  val _logger: Logger = Logger("AndroidVersionExtraction")

  def main(args: Array[String]): Unit = {
    val command = new DefaultParser()

    try {
      val commandline = command.parse(_options, args)

      _logger.info("Starting android_version_extraction")
      val startTime = System.nanoTime

      // exactly one of the options must be set
      if (!((commandline.hasOption("f") && !commandline.hasOption("d"))
        || (commandline.hasOption("d") && !commandline.hasOption("f")))) {
        _logger.error("Either filepath or directory must be specified!")
        sys.exit(1)
      }

      if (commandline.hasOption("f")) {
        _apkFilePath = commandline.getOptionValue("f")
        androidAppVulnerabilityDetection(Paths.get(_apkFilePath), commandline.hasOption("a"))
      }
      else if (commandline.hasOption("d")) {
        _apkFilePaths = commandline.getOptionValue("d")

        try {
          val bufferedSource = io.Source.fromFile(_apkFilePaths)
          for (apkFile <- bufferedSource.getLines) {
            androidAppVulnerabilityDetection(Paths.get(apkFile), commandline.hasOption("a"))
          }
          bufferedSource.close
        } catch {
          case e: IOException => _logger.error(e.getMessage)
        }
      }

      _logger.info("All done")

      // benchmark
      val duration = (System.nanoTime - startTime) / 1e9d
      _logger.info("Total Running Time: " + duration)
      val mb = 1024 * 1024
      val runtime = Runtime.getRuntime
      _logger.info("Used Memory:  " + (runtime.totalMemory - runtime.freeMemory) / mb + " mb")
      //logger.info("** Free Memory:  " + runtime.freeMemory / mb)
      //logger.info("** Total Memory: " + runtime.totalMemory / mb)
      //logger.info("** Max Memory:   " + runtime.maxMemory / mb)
    } catch {
      case e: Throwable => import org.apache.commons.cli.HelpFormatter
        _logger.error(e.getMessage)
        val formatter = new HelpFormatter()
        formatter.printHelp("StringDecryption", _options)
    }
  }

  def androidAppVulnerabilityDetection(apkFilePath: Path, withAndroidGeneral: Boolean): Unit = {
    // make sure APK file name is given correctly
    if (!apkFilePath.getFileName.toString.endsWith(".apk")) {
      _logger.error(s"APK file does not have .apk file ending: $apkFilePath")
      _logger.warn("Make sure that filename does not have blank spaces")
      sys.exit(1)
    }
    _logger.info("Got the .apk file " + apkFilePath)

    // extract the Android version information
    val android = new AndroidAPI()
    android.extractAndroidAPIVersion(apkFilePath.toString, withAndroidGeneral)

    val frameworks = new ExtractFrameworkVersions()
    var classDexEntry: ZipEntry = null

    // iterate through the zip file and run framework version extraction if a certain file is found
    _zipFile = Some(new ZipFile(apkFilePath.toFile))
    for (entry <- _zipFile.get.entries.asScala) {
      if (entry.getName.contains(classDexFile)) {
        classDexEntry = entry
      }

      if (entry.getName.contains(flutterFile)) {
        // extract the Flutter version information
        if (!_flutterFound) _logger.info(s"$flutterName implementation found")

        val hash = hashFile(entry)
        val libType = Paths.get(entry.getName).getParent.getFileName.toString
        frameworks.compareHashes(flutterName, hash, libType)

        if (!frameworks._frameworkVersions.contains(flutterName)) {
          frameworks.extractFrameworkVersionByDate(flutterName, classDexEntry.getCreationTime.toString)
        }

        if (!_flutterFound) _logger.info(s"$flutterName version extraction finished")
        _flutterFound = true
      }

      if (entry.getName.matches(""".*""" + reactNativeFile)) {
        // extract the React Native version information
        if (!_reactNativeFound) _logger.info(s"$reactNativeName implementation found")

        val hash = hashFile(entry)
        val entryName = Paths.get(entry.getName)
        val fileName = entryName.getFileName.toString
        val libType = entryName.getParent.getFileName.toString
        frameworks.compareReactNativeHashes(hash, fileName, libType)

        if (!_reactNativeFound) _logger.info(s"$reactNativeName version extraction finished")
        _reactNativeFound = true
      }

      if (entry.getName.matches(""".*""" + qtFile)) {
        // extract the Qt version information
        if (!_qtFound) _logger.info(s"$qtName implementation found")

        val hash = hashFile(entry)
        val libType = Paths.get(entry.getName).getParent.getFileName.toString
        frameworks.compareHashes(qtName, hash, libType)

        if (!_qtFound) _logger.info(s"$qtName version extraction finished")
        _qtFound = true
      }

      if (entry.getName.contains(xamarinSoFile) || entry.getName.contains(xamarinDllFile)) {
        // extract the Xamarin version information
        if (!_xamarinFound) _logger.info(s"$xamarinName implementation found")

        val hash = hashFile(entry)
        val libType = Paths.get(entry.getName).getParent.getFileName.toString
        frameworks.compareHashes(xamarinName, hash, libType)

        if (!_xamarinFound) _logger.info(s"$xamarinName version extraction finished")
        _xamarinFound = true
      }

      if (entry.getName.contains(cordovaFile)) {
        // extract the Cordova version information
        if (!_cordovaFound) _logger.info(s"$cordovaName implementation found")
        frameworks.extractCordovaVersion(_zipFile.get.getInputStream(entry))
        if (!_cordovaFound) _logger.info(s"$cordovaName version extraction finished")
        _cordovaFound = true
      }

      // extract the Unity version information using the two methods
      if (entry.getName.matches(""".*""" + unityNumberedFile)) {
        if (!_unityFound) _logger.info(s"$unityName implementation found")
        frameworks.extractUnityVersion(_zipFile.get.getInputStream(entry))
        if (!_unityFound) _logger.info(s"$unityName version extraction finished")
        _unityFound = true
      }

      if (entry.getName.contains(unitySoFile)) {
        if (!_unityFound) _logger.info(s"$unityName implementation found")

        val hash = hashFile(entry)
        val libType = Paths.get(entry.getName).getParent.getFileName.toString
        frameworks.compareHashes(unityName, hash, libType)

        if (!_unityFound) _logger.info(s"$unityName version extraction finished")
        _unityFound = true
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
    val is = _zipFile.get.getInputStream(entry)
    val hash = bytesToHex(MessageDigest.getInstance("SHA256").digest(is.readAllBytes())).mkString("")
    hash
  }
}