package extraction

import com.typesafe.scalalogging.Logger
import org.apache.commons.cli.{DefaultParser, Options}
import tools.Constants._
import tools.HexEditor.bytesToHex

import java.io.IOException
import java.nio.file.{Path, Paths}
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.zip.{ZipEntry, ZipFile}
import scala.jdk.CollectionConverters.EnumerationHasAsScala

object Main {

  var _apkFilePath = ""
  var _apkFilePaths = ""
  var _withAndroidGeneral: Boolean = false
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

      _withAndroidGeneral = commandline.hasOption("a")

      if (commandline.hasOption("f")) {
        _apkFilePath = commandline.getOptionValue("f")
        androidAppVulnerabilityDetection(Paths.get(_apkFilePath))
      }
      else if (commandline.hasOption("d")) {
        _apkFilePaths = commandline.getOptionValue("d")

        try {
          val bufferedSource = io.Source.fromFile(_apkFilePaths)
          for (apkFile <- bufferedSource.getLines) {
            androidAppVulnerabilityDetection(Paths.get(apkFile))
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

  /**
   * Get the Android API and used framework versions, then write a JSON file as output,
   *
   * @param apkFilePath the given APK file path
   */
  def androidAppVulnerabilityDetection(apkFilePath: Path): Unit = {
    // make sure APK file name is given correctly
    if (!apkFilePath.getFileName.toString.endsWith(".apk")) {
      _logger.error(s"APK file does not have .apk file ending: $apkFilePath")
      _logger.warn("Make sure that filename does not have blank spaces")
      sys.exit(1)
    }
    _logger.info("Got the .apk file " + apkFilePath)

    // extract the Android version information
    val android = new AndroidAPI()
    android.extractAndroidAPIVersion(apkFilePath.toString)

    val frameworks = new ExtractFrameworkVersions()
    var classDexLastModDate: String = null

    _zipFile = Some(new ZipFile(apkFilePath.toFile))

    // iterate through the zip file and note which framework is used
    for (entry <- _zipFile.get.entries.asScala) {
      val classDexEntry = checkForFrameworks(entry)
      try {
        if (classDexEntry != null) {
          val sdf = new SimpleDateFormat("dd.MM.yyyy")
          classDexLastModDate = sdf.format(classDexEntry.getLastModifiedTime.toMillis)
        }
      } catch {
        case _: Throwable => _logger.warn("APK last modified date is not found.")
      }
    }

    // iterate through the zip file again and extract the found framework's version
    for (entry <- _zipFile.get.entries.asScala) {
      callVersionExtractionFunctions(entry, frameworks)
    }

    // set version from last modified date, if not found
    callVersionExtractionByDate(classDexLastModDate, frameworks)

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

  /**
   * Set true if a specific framework is found and return the entry if it contains the classes.dex.
   *
   * @param entry the current ZipEntry
   * @return the entry back if it contains classes.dex, null otherwise
   */
  def checkForFrameworks(entry: ZipEntry): ZipEntry = {
    if (entry.getName.toLowerCase.contains(flutterName.toLowerCase)) {
      if (!_flutterFound)
        _logger.info(s"$flutterName implementation found")
      _flutterFound = true
    }
    if (entry.getName.toLowerCase.contains(reactNativeShortName.toLowerCase)) {
      if (!_reactNativeFound)
        _logger.info(s"$reactNativeName implementation found")
      _reactNativeFound = true
    }
    if (entry.getName.toLowerCase.contains(qtName.toLowerCase)) {
      if (!_qtFound)
        _logger.info(s"$qtName implementation found")
      _qtFound = true
    }
    if (entry.getName.toLowerCase.contains(xamarinName.toLowerCase)) {
      if (!_xamarinFound)
        _logger.info(s"$xamarinName implementation found")
      _xamarinFound = true
    }
    if (entry.getName.toLowerCase.contains(cordovaName.toLowerCase)) {
      if (!_cordovaFound)
        _logger.info(s"$cordovaName implementation found")
      _cordovaFound = true
    }
    if (entry.getName.toLowerCase.contains(unityName.toLowerCase)) {
      if (!_unityFound)
        _logger.info(s"$unityName implementation found")
      _unityFound = true
    }
    if (entry.getName.contains(classDexFile)) {
      return entry
    }
    null
  }

  /**
   * Call the corresponding function to extract framework's version.
   *
   * @param entry                the current ZipEntry
   * @param frameworks           the ExtractFrameworkVersions object
   */
  def callVersionExtractionFunctions(entry: ZipEntry, frameworks: ExtractFrameworkVersions): Unit = {
    val hash = hashFile(entry)
    try {
      val entryName = Paths.get(entry.getName)
      val libType = entryName.getParent.getFileName.toString

      if (entry.getName.contains(flutterFile)) {
        frameworks.compareHashes(flutterName, hash, libType)
      }
      if (entry.getName.matches(""".*""" + reactNativeFile)) {
        val fileName = entryName.getFileName.toString
        frameworks.compareReactNativeHashes(hash, fileName, libType)
      }
      if (entry.getName.matches(""".*""" + qtFile)) {
        frameworks.compareHashes(qtName, hash, libType)
      }
      if (entry.getName.contains(xamarinSoFile) || entry.getName.contains(xamarinDllFile)) {
        frameworks.compareHashes(xamarinName, hash, libType)
      }
      if (entry.getName.contains(cordovaFile)) {
        frameworks.extractCordovaVersion(_zipFile.get.getInputStream(entry))
      }
      if (entry.getName.contains(unitySoFile)) {
        frameworks.compareHashes(qtName, hash, libType)
      }
      if (entry.getName.matches(""".*""" + unityNumberedFile)) {
        frameworks.extractUnityVersion(_zipFile.get.getInputStream(entry))
      }
    } catch {
      case _: Throwable => // do nothing
    }
  }

  /**
   * Make sure versions are found, otherwise the version will be determined through the app last modified date.
   *
   * @param lastModDate the date of the APK last modified
   * @param frameworks  the ExtractFrameworkVersions object
   */
  def callVersionExtractionByDate(lastModDate: String, frameworks: ExtractFrameworkVersions): Unit = {
    if (_flutterFound && !frameworks._frameworkVersions.contains(flutterName)) {
      frameworks.byDate(flutterName, lastModDate)
    }
    if (_reactNativeFound && !frameworks._frameworkVersions.contains(reactNativeName)) {
      frameworks.byDate(reactNativeName, lastModDate)
    }
    if (_qtFound && !frameworks._frameworkVersions.contains(qtName)) {
      frameworks.byDate(qtName, lastModDate)
    }
    if (_xamarinFound && !frameworks._frameworkVersions.contains(xamarinName)) {
      frameworks.byDate(xamarinName, lastModDate)
    }
    if (_cordovaFound && !frameworks._frameworkVersions.contains(cordovaName)) {
      frameworks.byDate(cordovaName, lastModDate)
    }
    if (_unityFound && !frameworks._frameworkVersions.contains(unityName)) {
      frameworks.byDate(unityName, lastModDate)
    }
  }
}