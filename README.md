# Gradle plugins

A multi-module collection of custom Gradle plugins for embedded firmware development, platform development and build
automation in my Gradle monorepo projects.

## Table of content

- [Available plugins](#available-plugins)
- [Usage](#usage)
    - [Add plugins repository](#add-plugins-repository)
    - [Usage of `plugin-esp-idf`](#usage-of-plugin-esp-idf)
    - [Usage of `plugin-web-embed`](#usage-of-plugin-web-embed)
- [Contributing](#contributing)
- [Author](#author)
- [License](#license)

## Available plugins

| Module            | Plugin ID                                                                     | Description                                                                          | Docs                                                                                               | Repository                                                                               |
|:------------------|:------------------------------------------------------------------------------|:-------------------------------------------------------------------------------------|:---------------------------------------------------------------------------------------------------|:-----------------------------------------------------------------------------------------|
| `plugin-esp-idf`  | `pl.miloszgilga.gradle.esp-idf`<br>`pl.miloszgilga.gradle.esp-idf-components` | Cross-platform Gradle wrapper for ESP-IDF firmware projects (Windows, Linux, macOS). | [Javadoc](https://m2.miloszgilga.pl/javadoc/releases/pl/miloszgilga/gradle/plugin-esp-idf/latest)  | [Reposilite](https://m2.miloszgilga.pl/#/releases/pl/miloszgilga/gradle/plugin-esp-idf)  |
| `plugin-webembed` | `pl.miloszgilga.gradle.frontend-builder`<br>`pl.miloszgilga.gradle.web-embed` | Builds frontend assets via Node/Yarn and embeds them into JVM server resources.      | [Javadoc](https://m2.miloszgilga.pl/javadoc/releases/pl/miloszgilga/gradle/plugin-webembed/latest) | [Reposilite](https://m2.miloszgilga.pl/#/releases/pl/miloszgilga/gradle/plugin-webembed) |

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

### Usage of `plugin-esp-idf`

This module provides two complementary plugins for ESP-IDF monorepos:

- `pl.miloszgilga.gradle.esp-idf` - wraps `idf.py` and Doxygen workflows, manages incremental build caching, and exports
  the compiled firmware binary via the `firmwareBinary` configuration,
- `pl.miloszgilga.gradle.esp-idf-components` - exposes a Gradle subproject directory as a consumable C/C++ component
  artifact via the `espComponents` configuration, automatically passing `-DEXTRA_COMPONENT_DIRS` to CMake.

> [!NOTE]
> To use this plugin, you must still have the **ESP-IDF** framework installed and configured on your system. This plugin
> serves as a convenient cross-platform wrapper for monorepo projects using Gradle Kotlin DSL.

#### Project structure

```text
monorepo-root/
├── firmware/
│   ├── common-components/   # applies pl.miloszgilga.gradle.esp-idf-components
│   └── device-app/          # applies pl.miloszgilga.gradle.esp-idf
└── settings.gradle.kts
```

#### Shared components configuration

In your shared components module (`firmware/common-components/build.gradle.kts`):

```kotlin
plugins {
  id("pl.miloszgilga.gradle.esp-idf-components") version "X.Y.Z"
}
```

#### Firmware configuration

Apply `pl.miloszgilga.gradle.esp-idf`, customize the `espIdf` extension, and link shared component modules via
`espComponents` in your firmware project's `build.gradle.kts`:

```kotlin
plugins {
  id("pl.miloszgilga.gradle.esp-idf") version "X.Y.Z"
}

espIdf {
  chip.set("esp32c3") // default: "esp32"
  port.set("/dev/ttyACM0") // default: OS-specific serial port (COM3, /dev/ttyACM0, /dev/cu.usbmodem101)
  baud.set("921600") // default: "921600"
  appVersion.set("1.0.0") // default: project.version (or "0.0.1-local-dev" if unspecified)
  extraComponentDirs.from(rootProject.file("vendor/external-components")) // optional: additional local component dirs
  usbipdAutoAttach.set(true) // default: false (WSL2 only: auto-binds and attaches USB device via usbipd.exe)
  usbipdDevicePattern.set("(303a:|10c4:ea60|1a86:|0403:|USB JTAG|CP210|CH340|CH9102)") // optional: regex matching VID:PID or name
}

dependencies {
  espComponents(project(":firmware:common-components")) // optional: shared Gradle component modules
}
```

> [!TIP]
> When using `usbipdAutoAttach`, note that Windows does not include `usbipd` out of the box - install
> [usbipd-win](https://github.com/dorssel/usbipd-win) on the Windows host
> (`winget install --interactive --exact dorssel.usbipd-win`). When `usbipdAutoAttach` is enabled, serial tasks
> (`flashFirmware`, `monitorFirmware`, `runFirmware`, etc.) automatically bind (prompting for Windows UAC once if the
> device is `Not shared`) and attach the board to WSL2. On native Windows, Linux, or macOS, `attachUsbWsl` is skipped
> without affecting local serial port execution.

#### Available tasks (`firmware` group)

| Task              | Description                                                                      |
|:------------------|:---------------------------------------------------------------------------------|
| `attachUsbWsl`    | Binds and attaches a matching ESP32 USB device from Windows to WSL2 (`usbipd`).  |
| `checkEnv`        | Verifies that the ESP-IDF environment and `idf.py` are properly configured.      |
| `setChip`         | Sets the compilation target chip (e.g., `esp32c3`, `esp32s3`, `esp32`).          |
| `buildFirmware`   | Builds the full firmware (bootloader, partition table, and application).         |
| `buildAppOnly`    | Builds only the application binary (skips bootloader and partition images).      |
| `rebuildFirmware` | Forces CMake reconfiguration and performs a full firmware rebuild.               |
| `flashFirmware`   | Builds and flashes the full firmware to the board.                               |
| `flashAppOnly`    | Builds and flashes only the application partition to the board.                  |
| `monitorFirmware` | Opens the serial monitor with address-to-line backtrace decoding.                |
| `runFirmware`     | Builds, flashes the full firmware, and immediately starts the serial monitor.    |
| `eraseFlash`      | Erases the entire flash memory on the chip (including NVS and other partitions). |
| `cleanFirmware`   | Cleans build output files (`idf.py fullclean`).                                  |
| `generateDocs`    | Generates project documentation using Doxygen.                                   |

### Usage of `plugin-web-embed`

This module provides two complementary plugins designed for monorepo setups where a JVM backend embeds a compiled
Node/Yarn frontend application (such as Svelte, React, or Vite):

- `pl.miloszgilga.gradle.frontend-builder` - manages Node.js/Yarn toolchains, runs dev/build scripts, and exposes the
  compiled output directory via the `webResources` Gradle configuration,
- `pl.miloszgilga.gradle.web-embed` - resolves the `webResources` artifact from a frontend module and packages it into
  the JVM classpath resources during `processResources`.

#### Project structure

By default, `web-embed` automatically detects and wires a sibling `:ui` subproject under the same parent module:

```text
monorepo-root/
├── my-service/
│   ├── server/   # applies pl.miloszgilga.gradle.webembed
│   └── ui/       # applies pl.miloszgilga.gradle.frontend-builder
└── settings.gradle.kts
```

#### Frontend configuration

Apply `pl.miloszgilga.gradle.frontend-builder` and optionally customize the `frontendBuilder` extension:

```kotlin
plugins {
  id("pl.miloszgilga.gradle.frontend-builder") version "X.Y.Z"
}

frontendBuilder {
  nodeVersion.set("26.13.1") // default: "26.13.1"
  yarnVersion.set("1.22.22") // default: "1.22.22"
  downloadNode.set(true) // default: true
  buildDirName.set("dist") // default: "dist" (for standard Vite setups)
  devScript.set("dev") // default: "dev"
  buildScript.set("build") // default: "build"
  inputFiles.set(listOf("package.json", "tsconfig.json", "vite.config.ts", "yarn.lock"))
  additionalInputs.from("public") // optional extra files or directories tracked by buildWeb
  additionalDeletes.from(".vite") // optional extra files or directories removed during clean
}
```

#### Backend configuration

Apply `pl.miloszgilga.gradle.webembed` alongside any JVM plugin (`java`, `application`, `org.jetbrains.kotlin.jvm`,
etc.). If a sibling `:ui` project exists, zero configuration is required:

```kotlin
plugins {
  application
  id("pl.miloszgilga.gradle.web-embed") version "X.Y.Z"
}

// Optional customization:
webEmbed {
  uiProject.set(":my-service:ui") // optional: overrides automatic sibling :ui resolution
  targetDir.set("webapp") // default: "webapp" (maps to src/main/resources/webapp)
  autoWireSiblingUi.set(true) // default: true
  skip.set(false) // default: false (can also be toggled via -PskipWebEmbed)
}
```

#### Skipping frontend build

During local development when running the backend and UI dev server separately (`yarnDev`), you can skip building and
bundling frontend assets by passing the `-PskipWebEmbed` project property:

```bash
./gradlew :my-service:server:run -PskipWebEmbed
```

When active, `uiResources` is excluded from the task execution graph, so `yarnInstall` and `buildWeb` are not triggered.

#### Available tasks

| Task          | Plugin             | Description                                                           |
|:--------------|:-------------------|-----------------------------------------------------------------------|
| `yarnInstall` | `frontend-builder` | Installs frontend dependencies via Yarn.                              |
| `yarnDev`     | `frontend-builder` | Starts the local frontend development server (`yarn run dev`).        |
| `buildWeb`    | `frontend-builder` | Compiles the production frontend bundle and publishes `webResources`. |
| `clean`       | `frontend-builder` | Removes the generated build directory and optional cache (if absent). |

## Contributing

Contributions are welcome! See [CONTRIBUTING](CONTRIBUTING.md) for development setup, testing, and publishing
instructions.

## Author

Created by Miłosz Gilga. If you have any questions about this project, send a message to:
[miloszgilga@gmail.com](mailto:miloszgilga@gmail.com).

## License

This project is licensed under the Apache License 2.0.
