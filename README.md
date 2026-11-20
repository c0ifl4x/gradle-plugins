# Gradle plugins

A multi-module collection of custom Gradle plugins for embedded firmware development, platform development and build
automation in my Gradle monorepo projects.

## Table of content

- [Available plugins](#available-plugins)
- [Usage](#usage)
  - [Add plugins repository](#add-plugins-repository)
  - [Usage of `esp-idf-plugin`](#usage-of-esp-idf-plugin)
- [Contributing](#contributing)
- [Author](#author)
- [License](#license)

## Available plugins

| Module | Plugin ID | Description | Docs | Repository |
| :--- | :--- | :--- | :--- | :--- |
| `esp-idf-plugin` | `pl.miloszgilga.gradle.espidf` | Cross-platform Gradle wrapper for ESP-IDF firmware projects (Windows, Linux, macOS). | [Javadoc](https://m2.miloszgilga.pl/javadoc/releases/pl/miloszgilga/gradle/esp-idf-plugin/latest) | [Reposilite](https://m2.miloszgilga.pl/#/releases/pl/miloszgilga/gradle/esp-idf-plugin) |

## Usage

### Add plugins repository

```kotlin
pluginManagement {
  repositories {
    maven {
      name = "ReposiliteReleases"
      url = uri("https://m2.miloszgilga.pl/releases")
    }
    // optional: required only if using snapshot versions
    maven {
      name = "ReposiliteSnapshots"
      url = uri("https://m2.miloszgilga.pl/snapshots")
    }
    gradlePluginPortal()
  }
}
```

### Usage of `esp-idf-plugin`

> [!NOTE]
> To use this plugin, you must still have the **ESP-IDF** framework installed and configured on your system. This plugin
> serves as a convenient cross-platform wrapper for monorepo projects using Gradle Kotlin DSL.

#### Configuration

Apply the plugin and customize the `espIdf` extension in your firmware project's `build.gradle.kts`:

```kotlin
plugins {
  id("pl.miloszgilga.gradle.espidf") version "1.0.0"
}

espIdf {
  chip.set("esp32c3") // default: "esp32c3"
  port.set("/dev/ttyACM0") // default: OS-specific serial port (COM3, /dev/ttyACM0, /dev/cu.usbmodem101)
  baud.set("921600") // default: "921600"
  appVersion.set("1.0.0") // default: project.version (or "0.0.1-local-dev" if unspecified)
}
```

#### Available tasks (`firmware` group)

| Task| Description|
| :--- | :--- |
| `checkEnv`| Verifies that the ESP-IDF environment and `idf.py` are properly configured. |
| `setChip`| Sets the compilation target chip (e.g., `esp32c3`, `esp32s3`, `esp32`). |
| `buildFirmware`| Builds the full firmware (bootloader, partition table, and application). |
| `buildAppOnly`| Builds only the application binary (skips bootloader and partition images). |
| `rebuildFirmware`| Forces CMake reconfiguration and performs a full firmware rebuild. |
| `flashFirmware`| Builds and flashes the full firmware to the board. |
| `flashAppOnly`| Builds and flashes only the application partition to the board. |
| `eraseFlash`| Erases the entire flash memory on the chip (including NVS and other partitions). |
| `cleanFirmware`| Cleans build output files (`idf.py fullclean`). |
| `generateDocs`| Generates project documentation using Doxygen. |

## Contributing

Contributions are welcome! See [CONTRIBUTING](CONTRIBUTING.md) for development setup, testing, and publishing
instructions.

## Author

Created by Miłosz Gilga. If you have any questions about this project, send a message to:
[miloszgilga@gmail.com](mailto:miloszgilga@gmail.com).

## License

This project is licensed under the Apache License 2.0.
