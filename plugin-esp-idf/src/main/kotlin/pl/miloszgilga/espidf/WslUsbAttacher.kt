package pl.miloszgilga.espidf

import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import java.io.File
import java.util.concurrent.TimeUnit

internal object WslUsbAttacher {
  private const val DEFAULT_WIN_USBIPD_WSL = "/mnt/c/Program Files/usbipd-win/usbipd.exe"
  private const val DEFAULT_WIN_USBIPD_NATIVE = "C:\\Program Files\\usbipd-win\\usbipd.exe"

  private val BUSID_LINE_REGEX =
    Regex("""^(\d+-\d+)\s+([0-9a-fA-F]{4}:[0-9a-fA-F]{4})\s+(.+?)\s+(Not shared|Shared|Attached)\s*$""")

  fun isWsl(procVersionFile: File = File("/proc/version")): Boolean {
    if (!procVersionFile.exists()) return false
    val content = procVersionFile.readText()
    return content.contains("microsoft", ignoreCase = true) || content.contains("WSL", ignoreCase = true)
  }

  fun attachMatchingDevice(
    devicePattern: String,
    expectedPort: String,
    logger: Logger,
    procVersionFile: File = File("/proc/version"),
    usbipdOverride: String? = null,
    powershellBin: String = "powershell.exe"
  ) {
    if (!isWsl(procVersionFile)) {
      logger.debug("Not running inside WSL2, skipping usbipd attach")
      return
    }
    val usbipdBin = usbipdOverride ?: resolveUsbipdBinary()
    val listOutput = runCommand(listOf(usbipdBin, "list"))
    val matcher = Regex(devicePattern, RegexOption.IGNORE_CASE)

    val matchedDevice = listOutput.lines()
      .mapNotNull { line -> BUSID_LINE_REGEX.find(line.trim()) }
      .firstOrNull { match ->
        val vidPid = match.groupValues[2]
        val description = match.groupValues[3]
        matcher.containsMatchIn("$vidPid $description")
      }
      ?: throw GradleException(
        "No USB device matching pattern '$devicePattern' found in `usbipd.exe list`\nRaw output:\n$listOutput"
      )

    val busId = matchedDevice.groupValues[1]
    val description = matchedDevice.groupValues[3]
    val state = matchedDevice.groupValues[4]

    when (state) {
      "Attached" -> {
        logger.lifecycle("USB device [$busId] ($description) is already attached to WSL2")
        return
      }

      "Not shared" -> {
        logger.lifecycle(
          "USB device [$busId] ($description) is not shared. Requesting Windows UAC elevation to bind..."
        )
        bindWithUacElevation(busId, powershellBin)
      }
    }

    logger.lifecycle("Attaching USB device [$busId] ($description) to WSL2...")
    runCommand(listOf(usbipdBin, "attach", "--wsl", "--busid", busId))

    waitForSerialDevice(expectedPort, logger)
  }

  private fun bindWithUacElevation(busId: String, powershellBin: String) {
    val winExe = if (File(DEFAULT_WIN_USBIPD_WSL).exists()) DEFAULT_WIN_USBIPD_NATIVE else "usbipd"
    val psCommand = "Start-Process '$winExe' -ArgumentList 'bind --busid $busId' -Verb RunAs -Wait"
    val cmd = listOf(powershellBin, "-NoProfile", "-NonInteractive", "-Command", psCommand)

    val process = ProcessBuilder(cmd)
      .redirectErrorStream(true)
      .start()
    val output = process.inputStream.bufferedReader().readText()

    if (!process.waitFor(30, TimeUnit.SECONDS) || process.exitValue() != 0) {
      throw GradleException(
        "Failed to bind USB device [$busId] via UAC prompt (prompt cancelled or usbipd not found):\n$output"
      )
    }
  }

  private fun resolveUsbipdBinary(): String {
    val defaultWinPath = File(DEFAULT_WIN_USBIPD_WSL)
    if (defaultWinPath.exists()) return defaultWinPath.absolutePath

    val inPath = runCatching {
      ProcessBuilder("which", "usbipd.exe").start().waitFor() == 0
    }.getOrDefault(false)

    if (inPath) return "usbipd.exe"

    throw GradleException(
      "usbipd-win is not installed on the Windows host. " +
        "Install it once via PowerShell: `winget install --interactive --exact dorssel.usbipd-win` " +
        "(or disable `usbipdAutoAttach` in `espIdf { ... }`)."
    )
  }

  private fun waitForSerialDevice(expectedPort: String, logger: Logger) {
    val portFile = File(expectedPort)
    repeat(20) {
      if (portFile.exists()) {
        logger.lifecycle("Serial port $expectedPort is ready")
        return
      }
      Thread.sleep(150)
    }
    logger.warn("USB device attached, but $expectedPort did not appear within 3s")
  }

  private fun runCommand(cmd: List<String>): String {
    val process = ProcessBuilder(cmd)
      .redirectErrorStream(true)
      .start()
    val output = process.inputStream.bufferedReader().readText()
    if (!process.waitFor(10, TimeUnit.SECONDS) || process.exitValue() != 0) {
      throw GradleException("Command '${cmd.joinToString(" ")}' failed:\n$output")
    }
    return output
  }
}
