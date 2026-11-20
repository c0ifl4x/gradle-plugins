package pl.miloszgilga.espidf

import java.io.File

internal interface PlatformStrategy {
  val defaultPort: String
  val defaultIdfPath: String
  fun wrapCommand(command: String, useIdfEnv: Boolean, idfPath: String): List<String>
}

internal class WindowsStrategy : PlatformStrategy {
  override val defaultPort = "COM3"
  override val defaultIdfPath = "C:\\Espressif\\frameworks\\esp-idf"

  override fun wrapCommand(command: String, useIdfEnv: Boolean, idfPath: String): List<String> {
    val exportBat = File(idfPath, "export.bat").absolutePath
    val script = if (useIdfEnv) {
      """
      where idf.py >nul 2>nul || (
        if exist "$exportBat" (
          call "$exportBat" >nul 2>&1
        ) else (
          echo Error: ESP-IDF export.bat not found at "$exportBat". Check IDF_PATH. 1>&2
          exit /b 1
        )
      )
      $command
      """.trimIndent().replace("\n", " & ")
    } else {
      command
    }
    return listOf("cmd", "/c", script)
  }
}

internal abstract class UnixStrategy : PlatformStrategy {
  override val defaultIdfPath = "${System.getProperty("user.home")}/esp/esp-idf"

  override fun wrapCommand(command: String, useIdfEnv: Boolean, idfPath: String): List<String> {
    val exportSh = File(idfPath, "export.sh").absolutePath
    val script = if (useIdfEnv) {
      """
      if ! command -v idf.py >/dev/null 2>&1; then
        if [ -f "$exportSh" ]; then
          . "$exportSh" >/dev/null 2>&1
        else
          echo "Error: ESP-IDF export.sh not found at $exportSh. Check IDF_PATH." >&2
          exit 1
        fi
      fi
      $command
      """.trimIndent()
    } else {
      command
    }
    return listOf("bash", "-c", script)
  }
}

internal class MacOsStrategy : UnixStrategy() {
  override val defaultPort = "/dev/cu.usbmodem101"
}

internal class LinuxStrategy : UnixStrategy() {
  override val defaultPort = "/dev/ttyACM0"
}

internal object PlatformStrategyFactory {
  fun detect(): PlatformStrategy {
    val osName = System.getProperty("os.name").lowercase()
    return when {
      osName.contains("win") -> WindowsStrategy()
      osName.contains("mac") || osName.contains("darwin") -> MacOsStrategy()
      else -> LinuxStrategy()
    }
  }
}
