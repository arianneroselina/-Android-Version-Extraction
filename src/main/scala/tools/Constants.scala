package tools

object Constants {
  val flutterFile = "libflutter"
  val reactNativeFile = """libreact.*"""
  val qtFile = """libQt.*Core.*"""
  val cordovaFile = """cordova.js"""
  val xamarinSoFile = "libxa-internal-api"
  val xamarinDllFile = "Java.Interop"
  val unityFile = """0000000000000000f000000000000000"""

  val soExtension = ".so"
  val dllExtension = ".dll"

  val flutterFolders: Array[String] = Array("arm64-v8a", "armeabi-v7a", "x86_64")
  val reactNativeFolders: Array[String] = Array("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
  val xamarinSoFolders: Array[String] = Array("arm64-v8a", "armeabi-v7a")
}
