package tools

object Comparison {

  /**
   * Check if the first version is older than the second one.
   *
   * @param version1 the first version (x.x.x)
   * @param version2 the second version (x.x.x)
   * @return 1 if version1 is older than version2
   *         0 if version1 == version2
   *         -1 if version1 is newer than version2
   */
  def versionOlderThan(version1: String, version2: String): Int = {
    val version1Array = version1.split('.')
    val version2Array = version2.split('.')

    if (version1Array(0).toInt < version2Array(0).toInt) {
      1
    } else if (version1Array(0).toInt == version2Array(0).toInt) {
      if (version1Array(1).toInt < version2Array(1).toInt) {
        1
      } else if (version1Array(1).toInt == version2Array(1).toInt) {
        if (version1Array(2).toInt < version2Array(2).toInt) {
          1
        } else if (version1Array(2).toInt == version2Array(2).toInt) {
          0
        } else -1
      } else -1
    } else -1
  }

  /**
   * Check if the first Unity version is older than the second one.
   *
   * @param version1 the first version (x.x.xfx)
   * @param version2 the second version (x.x.xfx)
   * @return 1 if version1 is older than version2
   *         0 if version1 == version2
   *         -1 if version1 is newer than version2
   */
  def versionOlderThanForUnity(version1: String, version2: String): Int = {
    val version1Array = version1.split('.')
    val version2Array = version2.split('.')

    if (version1Array(0) < version2Array(0)) {
      1
    } else if (version1Array(0) == version2Array(0)) {
      if (version1Array(1) < version2Array(1)) {
        1
      } else if (version1Array(1) == version2Array(1)) {
        val pattern = "([0-9]+)([A-Za-z]+)([0-9]+)".r
        val pattern(v1a, a1, v1b) = version1Array(2)
        val pattern(v2a, a2, v2b) = version2Array(2)
        if (v1a < v2a) {
          1
        } else if (v1a == v2a) {
          if (a1.compareToIgnoreCase(a2) == -1) {
            1
          } else if (a1.compareToIgnoreCase(a2) == 0) {
            if (v1b < v2b) {
              1
            } else if (v1b == v2b) {
              0
            } else -1
          } else -1
        } else -1
      } else -1
    } else -1
  }

  /**
   * Check if the first date is later than the second date.
   *
   * @param date1 the first version (x.x.x)
   * @param date2 the second version (x.x.x)
   * @return true if date1 is later or equal date2
   */
  def dateLaterThan(date1: String, date2: String): Boolean = {
    val date1Array = date1.split('.')
    val date2Array = date2.split('.')

    if (date1Array(2).toInt > date2Array(2).toInt) {
      true
    } else if (date1Array(2).toInt == date2Array(2).toInt) {
      if (date1Array(1).toInt > date2Array(1).toInt) {
        true
      } else if (date1Array(1).toInt == date2Array(1).toInt) {
        if (date1Array(0).toInt >= date2Array(0).toInt) {
          true
        } else false
      } else false
    } else false
  }
}
