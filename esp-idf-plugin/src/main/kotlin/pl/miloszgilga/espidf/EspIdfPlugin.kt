package pl.miloszgilga.espidf

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Exec
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

/**
 * Gradle plugin wrapping ESP-IDF (`idf.py`) and Doxygen workflows for embedded firmware projects.
 *
 * Registers the `espIdf` extension, lifecycle tasks under the `firmware` group, and exports the built binary via the
 * `firmwareBinary` consumable configuration.
 */
class EspIdfPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.plugins.apply(BasePlugin::class.java)
    val ext = project.extensions.create<EspIdfExtension>("espIdf", project)

    registerIdfTask(
      project,
      ext,
      taskName = "checkEnv",
      taskDescription = "Verifies that the ESP-IDF environment and idf.py are properly configured",
      commandProvider = project.provider { "idf.py --version" }
    )
    registerIdfTask(
      project,
      ext,
      taskName = "setChip",
      taskDescription = "Sets the compilation target to esp32c3",
      commandProvider = ext.chip.map { targetChip -> "idf.py set-target $targetChip" }
    )
    val buildFirmware = registerIdfTask(
      project,
      ext,
      taskName = "buildFirmware",
      taskDescription = "Builds the full firmware (bootloader, partition table, and application)",
      commandProvider = ext.appVersion.map { ver -> "idf.py build -DCMD_APP_VER=\"$ver\"" }
    ).apply {
      configure {
        inputs.files(project.fileTree(project.projectDir) {
          include("main/**/*.c", "main/**/*.h", "components/**/*.c", "components/**/*.h")
          include("**/CMakeLists.txt", "sdkconfig*", "partitions*.csv")
          exclude("build/**", ".gradle/**")
        })
        inputs.property("version", ext.appVersion)
        inputs.property("chip", ext.chip)
        outputs.dir(project.layout.projectDirectory.dir("build"))
      }
    }
    val buildAppOnly = registerIdfTask(
      project,
      ext,
      taskName = "buildAppOnly",
      taskDescription = "Builds only the application binary (skips bootloader and partition images)",
      commandProvider = ext.appVersion.map { ver -> "idf.py app -DCMD_APP_VER=\"$ver\"" }
    )
    registerIdfTask(
      project,
      ext,
      taskName = "rebuildFirmware",
      taskDescription = "Forces CMake reconfiguration and performs a full firmware rebuild",
      commandProvider = ext.appVersion.map { ver ->
        "idf.py reconfigure && idf.py build -DCMD_APP_VER=\"$ver\""
      }
    )
    registerIdfTask(
      project,
      ext,
      taskName = "flashFirmware",
      taskDescription = "Builds and flashes the full firmware to the ESP32-C3 board",
      commandProvider = ext.port.zip(ext.baud) { port, baud -> "idf.py -p $port -b $baud flash" }
    ).apply {
      configure { dependsOn(buildFirmware) }
    }
    registerIdfTask(
      project,
      ext,
      taskName = "flashAppOnly",
      taskDescription = "Builds and flashes only the application partition to the board",
      commandProvider = ext.port.zip(ext.baud) { port, baud -> "idf.py -p $port -b $baud app-flash" }
    ).apply {
      configure { dependsOn(buildAppOnly) }
    }
    registerIdfTask(
      project,
      ext,
      taskName = "eraseFlash",
      taskDescription = "Erases the entire flash memory on the chip (including NVS and other partitions)",
      commandProvider = ext.port.map { port -> "idf.py -p $port erase-flash" }
    )
    val cleanFirmware = registerIdfTask(
      project,
      ext,
      taskName = "cleanFirmware",
      taskDescription = "Cleans build output files (idf.py fullclean)",
      commandProvider = project.provider { "idf.py fullclean" }
    )
    registerIdfTask(
      project,
      ext,
      taskName = "generateDocs",
      taskDescription = "Generates project documentation using Doxygen",
      commandProvider = project.provider { "doxygen" },
      useIdfEnv = false,
      extraEnv = mapOf("VER" to ext.appVersion)
    ).apply {
      configure {
        inputs.files(project.fileTree(project.projectDir) {
          include(
            "main/**/*.c",
            "main/**/*.h",
            "components/**/*.c",
            "components/**/*.h",
            "Doxyfile"
          )
        })
        inputs.property("version", ext.appVersion)
        outputs.dir(project.layout.projectDirectory.dir("docs"))
      }
    }
    project.tasks.named("assemble") {
      dependsOn(buildFirmware)
    }
    project.tasks.named("clean") {
      dependsOn(cleanFirmware)
    }
    val firmwareBinary = project.configurations.create("firmwareBinary") {
      isCanBeConsumed = true
      isCanBeResolved = false
    }
    project.artifacts {
      add(firmwareBinary.name, ext.binaryName.map { name ->
        project.layout.projectDirectory.file("build/$name")
      }) {
        builtBy(buildFirmware)
      }
    }
  }

  private fun registerIdfTask(
    project: Project,
    ext: EspIdfExtension,
    taskName: String,
    taskDescription: String,
    commandProvider: Provider<String>,
    useIdfEnv: Boolean = true,
    extraEnv: Map<String, Provider<String>> = emptyMap()
  ) = project.tasks.register<Exec>(taskName) {
    group = "firmware"
    description = taskDescription
    workingDir = project.projectDir
    extraEnv.forEach { (key, valueProvider) ->
      environment(key, valueProvider.get())
    }
    doFirst {
      commandLine(
        ext.platform.wrapCommand(
          command = commandProvider.get(),
          useIdfEnv = useIdfEnv,
          idfPath = ext.idfPath.get()
        )
      )
    }
  }
}
