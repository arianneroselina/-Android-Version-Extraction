package extraction

import com.typesafe.scalalogging.Logger
import play.api.libs.json.Json

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import scala.annotation.tailrec

object Main {
  val usage =
    """
    Usage: main --apk-filepath filepath
    """

  val logger: Logger = Logger("AndroidVersionExtraction")

  def main(args: Array[String]): Unit = {
    logger.info("Starting android_version_extraction")
    val startTime = System.nanoTime

    if (args.length != 2) {
      println(usage)
      sys.exit(1)
    }

    // command line arguments parser
    // from https://stackoverflow.com/questions/2315912/best-way-to-parse-command-line-parameters
    val argList = args.toList
    type ArgsMap = Map[String, String]

    @tailrec
    def nextArg(map: ArgsMap, list: List[String]): ArgsMap = {
      list match {
        case Nil => map
        case "--apk-filepath" :: value :: tail => nextArg(map ++ Map("apkFilePath" -> value), tail)
        case option => println(Console.RED + s"Unknown option: $option" + Console.WHITE)
          sys.exit(1)
      }
    }

    // make sure APK file name is given correctly
    var apkFilePath = ""
    nextArg(Map(), argList).get("apkFilePath") match {
      case Some(path) => apkFilePath = path
      case None => println(Console.RED + "APK file path cannot be empty" + Console.WHITE)
                   sys.exit(1)
    }

    if (!apkFilePath.endsWith("apk")) {
      println(Console.RED + s"APK file does not have .apk file ending: $apkFilePath" + Console.WHITE)
      sys.exit(1)
    }
    logger.info("Got the .apk file " + apkFilePath)

    // open the APK file
    (new OpenApk).openApkFile(apkFilePath, logger)
    val paths = apkFilePath.split(Array('\\', '/')) // get rid of .apk
    val apkFileName = paths(paths.length - 1)
    val fileName = apkFileName.substring(0, apkFileName.length - 4) // get rid of .apk
    val folderPath = apkFilePath.substring(0, apkFilePath.length - apkFileName.length)

    // extract the Android version information
    val androidJSON = (new AndroidAPI).extractAndroidAPIVersion(apkFilePath, logger)

    // extract the Flutter version information
    val flutterJSON = (new Flutter).extractFlutterVersion(folderPath + fileName, logger)

    // extract the React Native version information
    val reactNativeJSON = (new ReactNative).extractReactNativeVersion(folderPath + fileName, logger)

    // extract the Apache Cordova version information
    val cordovaJSON = (new Cordova).extractCordovaVersion(folderPath + fileName, logger)

    // extract the Unity version information
    val unityJSON = (new Unity).extractUnityVersion(folderPath + fileName, logger)

    // write the JSON value to JSON file
    logger.info("Writing output file")
    val file = new File(folderPath + fileName + ".json")
    try {
      val bw = new BufferedWriter(new FileWriter(file))

      // write versions and vulnerabilities to a JSON file
      var print = Json.obj(androidJSON)
      if (flutterJSON != null) print += flutterJSON
      if (reactNativeJSON != null) print += reactNativeJSON
      if (cordovaJSON != null) print += cordovaJSON
      if (unityJSON != null) print += unityJSON
      print += "inherit" -> Json.toJson(true)

      bw.write(Json.prettyPrint(print))
      bw.newLine()
      bw.close()
    } catch {
      case e: IOException => println(Console.RED + e.getMessage + Console.WHITE)
                             sys.exit(-1)
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
  }

}
