package extraction

import java.io.BufferedInputStream
import java.io.IOException
import java.lang.ProcessBuilder.Redirect;
import scala.collection.mutable

class ExtractAndroidVersion {

  /**
   * Extract minSdkVersion and targetSdkVersion from the given APK
   *
   * @param apkFilePath the APK file path
   * @param aaptPath    the aapt.exe path
   */
  def extractAndroidAPIVersion(apkFilePath: String, aaptPath: String): Unit = {
    try {
      val processBuilder = new ProcessBuilder(aaptPath, s"list -a $apkFilePath")
      processBuilder.redirectError(Redirect.INHERIT);
      processBuilder.redirectOutput(Redirect.INHERIT);

      val process = processBuilder.start
      process.waitFor() // wait to finish application execution
/*
      val sb = new mutable.StringBuilder("")
      val in = process.getInputStream.asInstanceOf[BufferedInputStream]
      val contents = new Array[Byte](1024)

      var bytesRead = in.read(contents)
      while (bytesRead != -1) {
        sb.append(new String(contents, 0, bytesRead))
        bytesRead = in.read(contents)
      }

      println("StringBuilder = " + sb.toString())*/
    } catch {
      case e: IOException => println(Console.RED + e.getMessage)
    }
  }
}
