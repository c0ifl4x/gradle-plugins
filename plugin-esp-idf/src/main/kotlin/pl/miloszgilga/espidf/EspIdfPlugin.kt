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
 *
 * @author Miłosz Gilga
 * @since 1.0.0
 */
class EspIdfPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.plugins.apply(BasePlugin::class.java)
    val ext = project.extensions.create<EspIdfExtension>("espIdf", project)

    val incomingComponents = project.configurations.create(EspComponentsContract.CONFIGURATION_NAME) {
      isCanBeConsumed = false
      isCanBeResolved = true
    }
    EspComponentsContract.applyAttributes(incomingComponents, project.objects)
    ext.extraComponentDirs.from(incomingComponents)

    val attachUsbWsl = project.tasks.register("attachUsbWsl") {
      group = "firmware"
      description = "Binds and attaches a matching ESP32 USB device from Windows to WSL2 via usbipd.exe"
      onlyIf { WslUsbAttacher.isWsl() && ext.usbipdAutoAttach.get() }
      doLast {
        WslUsbAttacher.attachMatchingDevice(
          devicePattern = ext.usbipdDevicePattern.get(),
          expectedPort = ext.port.get(),
          logger = logger
        )
      }
    }
    val ctx = TaskRuntimeContext(
      project = project,
      ext = ext,
      extraDirsFlag = createExtraDirsFlag(ext)
    )
    val registeredTasks = FirmwareTaskSpec.entries.associateWith { spec ->
      project.tasks.register<Exec>(spec.taskName) {
        group = "firmware"
        description = spec.description
        workingDir = project.projectDir

        val cmdProvider = spec.command(ctx)
        spec.configure(this, ctx)

        doFirst {
          commandLine(
            ext.platform.wrapCommand(
              command = cmdProvider.get(),
              useIdfEnv = spec.useIdfEnv,
              idfPath = ext.idfPath.get(),
              interactive = spec.interactive
            )
          )
        }
      }
    }
    FirmwareTaskSpec.entries.forEach { spec ->
      registeredTasks.getValue(spec).configure {
        spec.dependency?.let { dep ->
          dependsOn(registeredTasks.getValue(dep))
        }
        if (spec.requiresSerial) {
          dependsOn(attachUsbWsl)
        }
      }
    }
    val buildFirmwareTask = registeredTasks.getValue(FirmwareTaskSpec.BUILD_FIRMWARE)
    val cleanFirmwareTask = registeredTasks.getValue(FirmwareTaskSpec.CLEAN_FIRMWARE)

    project.tasks.named("assemble") {
      dependsOn(buildFirmwareTask)
    }
    project.tasks.named("clean") {
      dependsOn(cleanFirmwareTask)
    }

    val firmwareBinary = project.configurations.create("firmwareBinary") {
      isCanBeConsumed = true
      isCanBeResolved = false
    }
    project.artifacts {
      add(firmwareBinary.name, ext.binaryName.map { name ->
        project.layout.projectDirectory.file("build/$name")
      }) {
        builtBy(buildFirmwareTask)
      }
    }
  }

  private fun createExtraDirsFlag(ext: EspIdfExtension): Provider<String> {
    return ext.extraComponentDirs.elements.map { locations ->
      val dirs = locations
        .map { it.asFile }
        .filter { it.exists() }
        .map { it.absolutePath.replace('\\', '/') }
      if (dirs.isEmpty()) "" else " -DEXTRA_COMPONENT_DIRS=\"${dirs.joinToString(";")}\""
    }
  }
}
