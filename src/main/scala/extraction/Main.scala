package extraction

import com.typesafe.scalalogging.Logger
import org.apache.commons.cli.{DefaultParser, Options}
import play.api.libs.json.{JsValue, Json}

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.nio.file.{FileSystems, Files, Path, Paths}
import scala.jdk.CollectionConverters.IteratorHasAsScala

object Main {

  var apkFilePath = ""
  var apkFilesDir = ""
  val options = new Options()

  options.addOption("f", "apk-filepath", true, "input file <apk file path>")
  options.addOption("d", "apk-files-directory", true, "input files <apk files directory path>")
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
        apkFilesDir = commandline.getOptionValue("d")
        val dir = FileSystems.getDefault.getPath("apkFilesDir")
        Files.list(dir).iterator().asScala.foreach(file =>
          androidAppVulnerabilityDetection(file, commandline.hasOption("a")))
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

    // open the APK file
    val openedApk: OpenApk = new OpenApk
    openedApk.openApkFile(apkFilePath, logger)

    // extract the Android version information
    val androidJSON = (new AndroidAPI).extractAndroidAPIVersion(apkFilePath.toString, withAndroidGeneral, logger)

    // extract the Flutter version information
    var flutterJSON: (String, JsValue) = null
    if (openedApk.flutterUsed) {
      flutterJSON = (new Flutter).extractFlutterVersion(openedApk.outputDirPath, logger)
    }

    // extract the React Native version information
    var reactNativeJSON: (String, JsValue) = null
    if (openedApk.reactNativeUsed) {
      reactNativeJSON = (new ReactNative).extractReactNativeVersion(openedApk.outputDirPath, logger)
    }

    // extract the Apache Cordova version information
    var cordovaJSON: (String, JsValue) = null
    if (openedApk.cordovaUsed) {
      cordovaJSON = (new Cordova).extractCordovaVersion(openedApk.outputDirPath, logger)
    }

    // extract the Unity version information
    var unityJSON: (String, JsValue) = null
    if (openedApk.unityUsed) {
      unityJSON = (new Unity).extractUnityVersion(openedApk.outputDirPath, logger)
    }

    // extract the Xamarin.Android version information
    var xamarinJSON: (String, JsValue) = null
    if (openedApk.xamarinUsed) {
      xamarinJSON = (new Xamarin).extractXamarinVersion(openedApk.outputDirPath, logger)
    }

    // extract the Qt version information
    var qtJSON: (String, JsValue) = null
    if (openedApk.qtUsed) {
      qtJSON = (new Qt).extractQtVersion(openedApk.outputDirPath, logger)
    }

    // write the JSON value to JSON file
    logger.info("Writing output file")
    val file = new File(openedApk.outputDirPath + ".json")
    try {
      val bw = new BufferedWriter(new FileWriter(file))

      // write versions and vulnerabilities to a JSON file
      var print = Json.obj(androidJSON)
      if (flutterJSON != null) print += flutterJSON
      if (reactNativeJSON != null) print += reactNativeJSON
      if (cordovaJSON != null) print += cordovaJSON
      if (unityJSON != null) print += unityJSON
      if (xamarinJSON != null) print += xamarinJSON
      if (qtJSON != null) print += qtJSON
      print += "inherit" -> Json.toJson(true)

      bw.write(Json.prettyPrint(print))
      bw.newLine()
      bw.close()
    } catch {
      case e: IOException => logger.error(e.getMessage)
        sys.exit(-1)
    }
  }
}
