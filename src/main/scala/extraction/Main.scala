package extraction

import com.typesafe.scalalogging.Logger
import org.apache.commons.cli.{DefaultParser, Options}

import java.io.IOException
import java.nio.file.Paths

object Main {

  var _apkFilePath = ""
  var _apkFilePaths = ""
  var _withAndroidGeneral: Boolean = false
  val _options = new Options()

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
        (new VulnerabilityDetection).androidAppVulnerabilityDetection(Paths.get(_apkFilePath))
      }
      else if (commandline.hasOption("d")) {
        _apkFilePaths = commandline.getOptionValue("d")

        try {
          val bufferedSource = io.Source.fromFile(_apkFilePaths)
          var fileCount = 0
          for (apkFile <- bufferedSource.getLines) {
            _logger.info(s"FILE #$fileCount")
            (new VulnerabilityDetection).androidAppVulnerabilityDetection(Paths.get(apkFile))
            fileCount += 1
          }
          bufferedSource.close
        } catch {
          case e: IOException => _logger.error(s"Reading file ${_apkFilePaths} throws an error with message: " +
            s"${e.getMessage}")
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
        _logger.error(s"main() throws an error with message: ${e.getMessage}")
        val formatter = new HelpFormatter()
        formatter.printHelp("StringDecryption", _options)
    }
  }
}