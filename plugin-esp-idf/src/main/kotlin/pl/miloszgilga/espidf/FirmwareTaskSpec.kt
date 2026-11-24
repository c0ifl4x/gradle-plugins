package pl.miloszgilga.espidf

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.PathSensitivity

internal data class TaskRuntimeContext(
  val project: Project,
  val ext: EspIdfExtension,
  val extraDirsFlag: Provider<String>
)

internal enum class FirmwareTaskSpec(
  val taskName: String,
  val description: String,
  val useIdfEnv: Boolean = true,
  val interactive: Boolean = false,
  val dependency: FirmwareTaskSpec? = null
) {
  CHECK_ENV(
    taskName = "checkEnv",
    description = "Verifies that the ESP-IDF environment and idf.py are properly configured"
  ) {
    override fun command(ctx: TaskRuntimeContext): Provider<String> =
      ctx.project.provider { "idf.py --version" }
  },

  SET_CHIP(
    taskName = "setChip",
    description = "Sets the compilation target"
  ) {
    override fun command(ctx: TaskRuntimeContext): Provider<String> =
      ctx.ext.chip.zip(ctx.extraDirsFlag) { chip, extra -> "idf.py$extra set-target $chip" }
  },

  BUILD_FIRMWARE(
    taskName = "buildFirmware",
    description = "Builds the full firmware (bootloader, partition table, and application)"
  ) {
    override fun command(ctx: TaskRuntimeContext): Provider<String> =
      ctx.ext.appVersion.zip(ctx.extraDirsFlag) { ver, extra -> "idf.py$extra -DCMD_APP_VER=\"$ver\" build" }

    override fun configure(task: Exec, ctx: TaskRuntimeContext) {
      task.inputs.files(ctx.project.fileTree(ctx.project.projectDir) {
        include("main/**/*.c", "main/**/*.h", "components/**/*.c", "components/**/*.h")
        include("**/CMakeLists.txt", "sdkconfig*", "partitions*.csv")
        exclude("build/**", ".gradle/**")
      })
      task.inputs.files(
        ctx.ext.extraComponentDirs.asFileTree.matching {
          exclude("build/**", ".gradle/**")
        }
      )
        .withPropertyName("extraComponentDirs")
        .withPathSensitivity(PathSensitivity.RELATIVE)
      task.inputs.property("version", ctx.ext.appVersion)
      task.inputs.property("chip", ctx.ext.chip)
      task.outputs.dir(ctx.project.layout.projectDirectory.dir("build"))
    }
  },

  BUILD_APP_ONLY(
    taskName = "buildAppOnly",
    description = "Builds only the application binary (skips bootloader and partition images)"
  ) {
    override fun command(ctx: TaskRuntimeContext): Provider<String> =
      ctx.ext.appVersion.zip(ctx.extraDirsFlag) { ver, extra -> "idf.py$extra -DCMD_APP_VER=\"$ver\" app" }
  },

  REBUILD_FIRMWARE(
    taskName = "rebuildFirmware",
    description = "Forces CMake reconfiguration and performs a full firmware rebuild"
  ) {
    override fun command(ctx: TaskRuntimeContext): Provider<String> =
      ctx.ext.appVersion.zip(ctx.extraDirsFlag) { ver, extra ->
        "idf.py$extra -DCMD_APP_VER=\"$ver\" reconfigure && idf.py$extra -DCMD_APP_VER=\"$ver\" build"
      }
  },

  FLASH_FIRMWARE(
    taskName = "flashFirmware",
    description = "Builds and flashes the full firmware to the board",
    dependency = BUILD_FIRMWARE
  ) {
    override fun command(ctx: TaskRuntimeContext): Provider<String> =
      ctx.ext.port.zip(ctx.ext.baud) { port, baud -> "idf.py -p $port -b $baud flash" }
  },

  FLASH_APP_ONLY(
    taskName = "flashAppOnly",
    description = "Builds and flashes only the application partition to the board",
    dependency = BUILD_APP_ONLY
  ) {
    override fun command(ctx: TaskRuntimeContext): Provider<String> =
      ctx.ext.port.zip(ctx.ext.baud) { port, baud -> "idf.py -p $port -b $baud app-flash" }
  },

  MONITOR_FIRMWARE(
    taskName = "monitorFirmware",
    description = "Opens the serial monitor with address-to-line backtrace decoding",
    interactive = true
  ) {
    override fun command(ctx: TaskRuntimeContext): Provider<String> =
      ctx.ext.port.map { port -> "idf.py -p $port monitor" }
  },

  RUN_FIRMWARE(
    taskName = "runFirmware",
    description = "Builds, flashes the full firmware, and immediately starts the serial monitor",
    interactive = true,
    dependency = BUILD_FIRMWARE
  ) {
    override fun command(ctx: TaskRuntimeContext): Provider<String> =
      ctx.ext.port.zip(ctx.ext.baud) { port, baud -> "idf.py -p $port -b $baud flash monitor" }
  },

  ERASE_FLASH(
    taskName = "eraseFlash",
    description = "Erases the entire flash memory on the chip (including NVS and other partitions)"
  ) {
    override fun command(ctx: TaskRuntimeContext): Provider<String> =
      ctx.ext.port.map { port -> "idf.py -p $port erase-flash" }
  },

  CLEAN_FIRMWARE(
    taskName = "cleanFirmware",
    description = "Cleans build output files (idf.py fullclean)"
  ) {
    override fun command(ctx: TaskRuntimeContext): Provider<String> =
      ctx.project.provider { "idf.py fullclean" }
  },

  GENERATE_DOCS(
    taskName = "generateDocs",
    description = "Generates project documentation using Doxygen",
    useIdfEnv = false
  ) {
    override fun command(ctx: TaskRuntimeContext): Provider<String> =
      ctx.project.provider { "doxygen" }

    override fun configure(task: Exec, ctx: TaskRuntimeContext) {
      task.environment("VER", ctx.ext.appVersion.get())
      task.inputs.files(ctx.project.fileTree(ctx.project.projectDir) {
        include(
          "main/**/*.c",
          "main/**/*.h",
          "components/**/*.c",
          "components/**/*.h",
          "Doxyfile"
        )
      })
      task.inputs.property("version", ctx.ext.appVersion)
      task.outputs.dir(ctx.project.layout.projectDirectory.dir("docs"))
    }
  };

  abstract fun command(ctx: TaskRuntimeContext): Provider<String>

  open fun configure(task: Exec, ctx: TaskRuntimeContext) {}
}
