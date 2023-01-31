package tools

import tools.Constants.unityFile

import java.io.File
import java.nio.file.{Files, Path, Paths}
import scala.collection.immutable.HashMap
import scala.collection.mutable.ArrayBuffer
import scala.reflect.io.Path._

object Util {

  /**
   * Find the path of the given file regex name in lib directory and map them into the corresponding ABI type.
   *
   * @param folderPath the path to the extracted APK folder
   * @param fileName   the filename in regex (e.g. libreact*,so)
   * @return the mapping from ABI types to file paths
   */
  def findFilesInLib(folderPath: Path, fileName: String): HashMap[String, ArrayBuffer[String]] = {
    var libsToPaths = new HashMap[String, ArrayBuffer[String]]()

    val libDirs = folderPath.toFile.toDirectory.dirs.map(_.path).filter(name => name matches """.*lib""")
    for (libDir <- libDirs) {
      val inLibs = libDir.toDirectory.dirs.map(_.path)
      for (lib <- inLibs) {
        try {
          val libType = new File(lib).getName
          val filePaths = lib.toDirectory.files
            .filter(file => file.name matches fileName)
            .map(_.path)

          for (filePath <- filePaths) {
            if (Files.exists(Paths.get(filePath))) {
              val value = if (libsToPaths.contains(libType)) libsToPaths(libType) :+ filePath else ArrayBuffer(filePath)
              libsToPaths += (libType -> value)
            }
          }
        } catch {
          case _: Exception => // do nothing
        }
      }
    }
    libsToPaths
  }

  /**
   * Find the path of one file with numbers in its name containing 32 chars inside the given directory.
   *
   * @param folderPath the path to the directory to be searched
   * @return the file path
   */
  def findUnityFileInAssets(folderPath: Path): String = {
    try {
      val filePaths = folderPath.toFile.toDirectory.files
        .filter(file => file.name matches unityFile)
        .map(_.path)

      for (filePath <- filePaths) {
        if (Files.exists(Paths.get(filePath))) {
          return filePath
        }
      }
    } catch {
      case _: Exception => //
    }
    ""
  }
}
