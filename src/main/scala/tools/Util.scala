package tools

import java.nio.file.{Files, Paths}
import scala.reflect.io.Path._
import scala.util.control.Breaks.break

object Util {
  /**
   * Find the path of the given file in the given folder.
   *
   * @param folderPath the path to the folder
   * @param fileName   the filename
   * @return the path
   */
  def findFile(folderPath: String, fileName: String): String = {
    try {
      val filePath = folderPath.toDirectory.files
        .filter(file => file.name.equals(fileName))
        .map(_.path)
        .next()
      if (Files.exists(Paths.get(filePath))) {
        return filePath
      }
    } catch {
      case _: Exception => // do nothing
    }
    null // file not found
  }

  /**
   * Find the path of the given file name in lib directory.
   *
   * @param folderPath the path to the extracted APK folder
   * @param fileName   the filename (e.g. libflutter.so)
   * @return the path
   */
  def findFileInLib(folderPath: String, fileName: String): String = {
    val libDirs = folderPath.toDirectory.dirs.map(_.path).filter(name => name matches """.*lib""")
    for (libDir <- libDirs) {
      val inLibs = libDir.toDirectory.dirs.map(_.path)
      for (lib <- inLibs) {
        try {
          val filePath = lib.toDirectory.files
            .filter(file => file.name.equals(fileName))
            .map(_.path)
            .next()
          if (Files.exists(Paths.get(filePath))) {
            return filePath
          }
        } catch {
          case _: Exception => // do nothing
        }
      }
    }
    null // file not found
  }

  /**
   * Find the path of the given file name in assemblies directory.
   *
   * @param folderPath the path to the extracted APK folder
   * @param fileName   the filename (e.g. Java.Interop.dll)
   * @return the path
   */
  def findFileInAssemblies(folderPath: String, fileName: String): String = {
    val assembliesDirs = folderPath.toDirectory.dirs.map(_.path).filter(name => name matches """.*assemblies""")
    for (assemblies <- assembliesDirs) {
      try {
        val filePath = assemblies.toDirectory.files
          .filter(file => file.name.equals(fileName))
          .map(_.path)
          .next()
        if (Files.exists(Paths.get(filePath))) {
          return filePath
        }
      } catch {
        case _: Exception => // do nothing
      }
    }
    null // file not found
  }

  /**
   * Find the path of the given file regex name in lib directory.
   *
   * @param folderPath the path to the extracted APK folder
   * @param fileName   the filename in regex (e.g. libreact*,so)
   * @return the paths
   */
  def findFilesInLib(folderPath: String, fileName: String): Array[String] = {
    val libDirs = folderPath.toDirectory.dirs.map(_.path).filter(name => name matches """.*lib""")
    for (libDir <- libDirs) {
      val inLibs = libDir.toDirectory.dirs.map(_.path)
      for (lib <- inLibs) {
        try {
          val filePaths = lib.toDirectory.files
            .filter(file => file.name matches fileName)
            .map(_.path)
          val pathArray = filePaths.toArray

          var found = true
          for (filePath <- filePaths) {
            if (!Files.exists(Paths.get(filePath))) {
              found = false
              break
            }
          }
          if (found) {
            return pathArray
          }
        } catch {
          case _: Exception => // do nothing
        }
      }
    }
    null // file not found
  }
}
