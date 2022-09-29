package extraction

import java.nio.file._
import scala.jdk.CollectionConverters.EnumerationHasAsScala
import scala.util.Try
import java.util.zip.ZipFile

class OpenApk {

  /**
   * Take care of opening the given APK file by converting it to zip and extracting it.
   *
   * @param apkFilePath the APK file path
   */
  def openApkFile(apkFilePath: String): Unit = {
    // copy file and rename it to have .zip file ending
    val zipFilePath = renameToZip(apkFilePath)
    val success = copyAndRenameFile(Paths.get(apkFilePath), Paths.get(zipFilePath))
    if (success == false) {
      println(Console.RED + s"Failed to copy and rename APK file: $apkFilePath")
      sys.exit(1)
    }

    // get folder delimiter index (if any)
    var i = zipFilePath.lastIndexOf("/")
    if (i < 0) {
      i = zipFilePath.lastIndexOf("""\""")
    }

    var fileName = zipFilePath
    if (i > 0) {
      // split the file's path and name
      val filePath = zipFilePath.substring(0, i + 1)
      fileName = zipFilePath.substring(i + 1)
      extractZip(filePath, fileName)
    } else {
      // only file name is given, it means file is in the same directory as main.scala
      extractZip("", fileName)
    }
  }

  def renameToZip(path: String): String = {
    path.replace("apk", "zip")
  }

  /**
   * Copies and renames the given .apk file as .zip.
   *
   * @param src  the original file path
   * @param dest the new file path
   * @return true if it succeeds, false otherwise
   */
  def copyAndRenameFile(src: Path, dest: Path): Any = {
    Try(Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING)).getOrElse(false)
  }

  def deleteNonEmptyDirectory(path: Path): Unit = {
    if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
      try {
        val entries = Files.newDirectoryStream(path)
        entries.forEach(entry => deleteNonEmptyDirectory(entry))
      } catch {
        case _: Exception => println(Console.RED + s"Failed to delete non empty directory: ${path.toString}")
          sys.exit(1)
      }
    }
    Files.delete(path)
  }

  /**
   * Extract the zip with the name fileName at the given path.
   *
   * @param path     the path of the file
   * @param fileName the zip file name
   */
  def extractZip(path: String, fileName: String): Unit = {
    val inputFilePath = Paths.get(path + fileName)
    val outputFilePath = Paths.get(path + fileName.substring(0, fileName.length - 4))

    // delete directory if exists
    if (Files.exists(outputFilePath))
      deleteNonEmptyDirectory(outputFilePath)

    val zipFile = new ZipFile(inputFilePath.toFile)
    for (entry <- zipFile.entries.asScala) {
      val path = outputFilePath.resolve(entry.getName)
      if (entry.isDirectory) {
        Files.createDirectories(path)
      } else {
        Files.createDirectories(path.getParent)
        Files.copy(zipFile.getInputStream(entry), path)
      }
    }
  }
}
