package tools

import java.io.InputStream
import scala.collection.mutable

/**
 * A simpler scala-version Hex Editor from https://github.com/Alem/HexEditorZ.
 * @author Alem <info@alemcode.com>
 */
object HexEditor {

  /** Stores characters of the hexadecimal numeral system */
  private val _hexCharacters = Array('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')

  /**
   * Returns a hexadecimal string from a given file
   *
   * @param is the input stream
   * @return a string containing the hexadecimal
   */
  def openHexFile(is: InputStream): String = {
    val length = is.available()
    val file_bytes = new Array[Byte](length)

    var bytes_read = 0
    do bytes_read = is.read(file_bytes) while (bytes_read != -1)

    new String(bytesToHex(file_bytes))
  }

  /**
   * Returns hexadecimal equivalent of byte array
   *
   * Creates a char array with 2:1 ratio to byte array ( 2 hex digits : 1 byte )
   * Masks (and shifts) to get value of left and right bit quartet in byte
   *
   * @param bytes the bytes array
   * @return hexadecimal char array
   */
  def bytesToHex(bytes: Array[Byte]): Array[Char] = {
    val hexFormat = new Array[Char](bytes.length * 2)
    var i = 0
    var j = 0
    while (i < bytes.length) {
      hexFormat(j) = _hexCharacters((bytes(i) & 0xf0) >>> 4)
      hexFormat(j + 1) = _hexCharacters(bytes(i) & 0x0f)

      i += 1
      j = i * 2
    }
    hexFormat
  }

  /**
   * Convert a Hex string to Ascii string
   *
   * This function is our own implementation
   *
   * @param hex the hexadecimal as string
   * @return the Ascii string
   */
  def toAscii(hex: String): String = {
    require(hex.length % 2 == 0, "Hex must have an even number of characters. You had " + hex.length)
    val sb = new mutable.StringBuilder
    for (i <- 0 until hex.length by 2) {
      val str = hex.substring(i, i + 2)
      sb.append(Integer.parseInt(str, 16).toChar)
    }
    sb.toString
  }
}
