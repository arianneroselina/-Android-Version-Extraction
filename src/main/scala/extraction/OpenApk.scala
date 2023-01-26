package extraction

import com.typesafe.scalalogging.Logger
import tools.Constants._

import java.nio.file._
import scala.jdk.CollectionConverters.EnumerationHasAsScala
import java.util.zip.{ZipEntry, ZipFile}

class OpenApk {

  var logger: Option[Logger] = None
  var outputDirPath: Path = null

  var flutterUsed = false
  var reactNativeUsed = false
  var cordovaUsed = false
  var qtUsed = false
  var unityUsed = false
  var xamarinUsed = false

  /**
   * Take care of opening the given APK file by converting it to zip and extracting it.
   *
   * @param apkFilePath the APK file path
   */
  def openApkFile(apkFilePath: Path, logger: Logger): Unit = {
    this.logger = Some(logger)

    outputDirPath = Paths.get(apkFilePath.toString.replaceFirst("[.][^.]+$", ""))

    // delete directory if exists
    if (Files.exists(outputDirPath))
      deleteNonEmptyDirectory(outputDirPath)

    val zipFile = new ZipFile(apkFilePath.toFile)
    for (entry <- zipFile.entries.asScala) {
      // extract Flutter, React Native, Qt, and Xamarin so files
      if (entry.getName.contains(flutterFile + soExtension)) {
        if (!flutterUsed) {
          flutterUsed = true
          logger.info("Flutter implementation found")
        }
        createDirOrFile(outputDirPath, zipFile, entry)
      }
      if (entry.getName.matches(""".*""" + reactNativeFile + soExtension)) {
        if (!reactNativeUsed) {
          reactNativeUsed = true
          logger.info("React Native implementation found")
        }
        createDirOrFile(outputDirPath, zipFile, entry)
      }
      if (entry.getName.matches(""".*""" + qtFile + soExtension)) {
        if (!qtUsed) {
          qtUsed = true
          logger.info("Qt implementation found")
        }
        createDirOrFile(outputDirPath, zipFile, entry)
      }
      if (entry.getName.contains(xamarinSoFile + soExtension)) {
        if (!xamarinUsed) {
          xamarinUsed = true
          logger.info("Xamarin implementation found")
        }
        createDirOrFile(outputDirPath, zipFile, entry)
      }

      // extract Xamarin dll file
      if (entry.getName.contains("assemblies/" + xamarinDllFile + dllExtension)) {
        if (!xamarinUsed) {
          xamarinUsed = true
          logger.info("Xamarin implementation found")
        }
        createDirOrFile(outputDirPath, zipFile, entry)
      }
      // extract Unity file
      if (entry.getName.contains("assets/bin/Data/" + unityFile)) {
        if (!unityUsed) {
          unityUsed = true
          logger.info("Unity implementation found")
        }
        createDirOrFile(outputDirPath, zipFile, entry)
      }
      // extract Cordova file
      if (entry.getName.contains("assets/www/" + cordovaFile)) {
        if (!cordovaUsed) {
          cordovaUsed = true
          logger.info("Cordova implementation found")
        }
        createDirOrFile(outputDirPath, zipFile, entry)
      }
    }
  }

  def deleteNonEmptyDirectory(path: Path): Unit = {
    if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
      try {
        val entries = Files.newDirectoryStream(path)
        entries.forEach(entry => deleteNonEmptyDirectory(entry))
      } catch {
        case _: Exception => logger.get.error(s"Failed to delete non empty directory: ${path.toString}")
          sys.exit(1)
      }
    }
    Files.delete(path)
  }

  /**
   * Create the directory or file for zip file extraction
   *
   * @param outputFilePath the output path
   * @param apkFile        the apk file
   * @param entry          the zip entry to be created
   */
  def createDirOrFile(outputFilePath: Path, apkFile: ZipFile, entry: ZipEntry): Unit = {
    val path = outputFilePath.resolve(entry.getName)
    if (entry.isDirectory) {
      Files.createDirectories(path)
    } else {
      Files.createDirectories(path.getParent)
      try {
        Files.copy(apkFile.getInputStream(entry), path)
      } catch {
        case _: Exception => logger.get.error(s"Failed to copy file $entry to $path")
      }
    }
  }
}
