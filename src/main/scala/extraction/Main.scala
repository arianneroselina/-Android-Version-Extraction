package extraction

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
        case "--aapt-exe-path" :: value :: tail => nextArg(map ++ Map("aaptPath" -> value), tail)
        case option => println(Console.RED + s"Unknown option: $option")
          sys.exit(1)
      }
    }

    // make sure apk file name is given correctly
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

    // open the apk file
    (new OpenApk).openApkFile(apkFilePath)

    // extract the android version information
    // TODO : find another way to use aapt.exe
    var aaptPath = ""
    nextArg(Map(), argList).get("aaptPath") match {
      case Some(path) => aaptPath = path
      case None => println(Console.RED + "aapt.exe path cannot be empty")
                   sys.exit(1)
    }

    (new ExtractAndroidVersion).extractAndroidAPIVersion(apkFilePath, aaptPath)
  }
}
