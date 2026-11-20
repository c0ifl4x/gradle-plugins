package pl.miloszgilga.espidf

import org.gradle.api.Project
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Configuration extension for the ESP-IDF Gradle plugin (`espIdf { ... }`).
 *
 * Provides cross-platform defaults and properties for building, flashing, and documenting ESP32 firmware projects.
 */
abstract class EspIdfExtension @Inject constructor(project: Project) {
  /**
   * Target ESP32 chip model used by `idf.py set-target`.
   *
   * Common values: `"esp32"`, `"esp32c3"`, `"esp32c6"`, `"esp32s3"`. Defaults to `"esp32c3"`.
   */
  abstract val chip: Property<String>

  /**
   * Serial port used for flashing and erasing the device.
   *
   * Defaults to an OS-specific serial port:
   * - Windows: `"COM3"`
   * - Linux: `"/dev/ttyACM0"`
   * - macOS: `"/dev/cu.usbmodem101"`
   */
  abstract val port: Property<String>

  /**
   * Serial baud rate used during flashing operations (`flashFirmware`, `flashAppOnly`).
   *
   * Defaults to `"921600"`.
   */
  abstract val baud: Property<String>

  /**
   * Root directory of the local ESP-IDF framework installation.
   *
   * Resolved in order from:
   * 1. `IDF_PATH` environment variable
   * 2. `idf_path` environment variable
   * 3. Default OS installation path (C:\Espressif\frameworks\esp-idf on Windows, ~/esp/esp-idf on Unix)
   */
  abstract val idfPath: Property<String>

  /**
   * Firmware application version passed to CMake via `-DCMD_APP_VER` and to Doxygen via `VER`.
   *
   * Defaults to `project.version` if specified, otherwise `"0.0.1-local-dev"`.
   */
  abstract val appVersion: Property<String>

  /**
   * Output firmware binary filename located inside the `build/` directory, exposed via the `firmwareBinary` Gradle
   * configuration.
   *
   * Defaults to `"${project.name}.bin"`.
   */
  abstract val binaryName: Property<String>

  internal val platform = PlatformStrategyFactory.detect()

  init {
    chip.convention("esp32")
    port.convention(platform.defaultPort)
    baud.convention("921600")
    idfPath.convention(System.getenv("IDF_PATH") ?: platform.defaultIdfPath)
    appVersion.convention(
      project.version.toString().takeIf { it != "unspecified" } ?: "0.0.1-local-dev"
    )
    binaryName.convention("${project.name}.bin")
  }
}
