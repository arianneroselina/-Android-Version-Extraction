package extraction

import play.api.libs.json.Json

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.nio.file.Paths
import scala.annotation.tailrec

object Main {
  val usage =
    """
    Usage: main --apk-filepath filepath
  """

  def main(args: Array[String]): Unit = {
    if (args.length == 0) println(usage)

    // command line arguments parser
    // from https://stackoverflow.com/questions/2315912/best-way-to-parse-command-line-parameters
    val argList = args.toList
    type ArgsMap = Map[String, String]

    @tailrec
    def nextArg(map: ArgsMap, list: List[String]): ArgsMap = {
      list match {
        case Nil => map
        case "--apk-filepath" :: value :: tail => nextArg(map ++ Map("apkFilePath" -> value), tail)
        case option => println(Console.RED + s"Unknown option: $option")
          sys.exit(1)
      }
    }

    // make sure APK file name is given correctly
    var apkFilePath = ""
    nextArg(Map(), argList).get("apkFilePath") match {
      case Some(path) => apkFilePath = path
      case None => println(Console.RED + "APK file path cannot be empty")
                   sys.exit(1)
    }

    if (!apkFilePath.endsWith("apk")) {
      println(Console.RED + s"APK file does not have .apk file ending: $apkFilePath")
      sys.exit(1)
    }

    // open the APK file
    (new OpenApk).openApkFile(apkFilePath)
    val paths = apkFilePath.split(Array('\\', '/')) // get rid of .apk
    val apkFileName = paths(paths.length - 1)
    val fileName = apkFileName.substring(0, apkFileName.length - 4) // get rid of .apk
    val folderPath = apkFilePath.substring(0, apkFilePath.length - apkFileName.length)

    // extract the Android version information
    val androidJSON = (new AndroidAPI).extractAndroidAPIVersion(apkFilePath)

    // extract the Flutter version information
    val flutterJSON = (new Flutter).extractFlutterVersion(folderPath + fileName)

    // extract the React Native version information
    val reactNativeJSON = (new ReactNative).extractReactNativeVersion(folderPath + fileName)

    // write the JSON value to JSON file
    val file = new File(folderPath + fileName + ".json")
    try {
      val bw = new BufferedWriter(new FileWriter(file))

      // write versions and vulnerabilities to a JSON file
      var print = Json.obj(androidJSON)
      if (flutterJSON != null) print += flutterJSON
      if (reactNativeJSON != null) print += reactNativeJSON
      print += "inherit" -> Json.toJson(true)

      bw.write(Json.prettyPrint(print))
      bw.newLine()
      bw.close()
    } catch {
      case e: IOException => println(Console.RED + e.getMessage)
                             sys.exit(-1)
    }
  }

}
