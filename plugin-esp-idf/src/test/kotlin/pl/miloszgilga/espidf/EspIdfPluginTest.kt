package pl.miloszgilga.espidf

import org.gradle.api.tasks.Exec
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EspIdfPluginTest {
  @Test
  fun `plugin registers all expected firmware tasks and default conventions`() {
    // given
    val project = ProjectBuilder.builder().withName("esp-firmware").build()
    val expectedTasks = listOf(
      "checkEnv",
      "setChip",
      "buildFirmware",
      "buildAppOnly",
      "rebuildFirmware",
      "flashFirmware",
      "flashAppOnly",
      "eraseFlash",
      "cleanFirmware",
      "generateDocs"
    )

    // when
    project.pluginManager.apply("pl.miloszgilga.gradle.espidf")

    // then
    expectedTasks.forEach { taskName ->
      assertTrue(project.tasks.names.contains(taskName), "Task '$taskName' should be registered")
    }
    val ext = project.extensions.getByType(EspIdfExtension::class.java)
    assertEquals("esp32", ext.chip.get())
    assertEquals("921600", ext.baud.get())
    assertEquals("${project.name}.bin", ext.binaryName.get())
  }

  @Test
  fun `extension overrides are reflected in task configuration`() {
    // given
    val project = ProjectBuilder.builder().build()
    project.pluginManager.apply("pl.miloszgilga.gradle.espidf")
    val ext = project.extensions.getByType(EspIdfExtension::class.java)

    // when
    ext.chip.set("esp32s3")
    ext.port.set("/dev/ttyUSB9")

    // then
    val setChipTask = project.tasks.named("setChip", Exec::class.java).get()
    assertEquals("firmware", setChipTask.group)
    assertEquals("esp32s3", ext.chip.get())
    assertEquals("/dev/ttyUSB9", ext.port.get())
  }

  @Test
  fun `strategies wrap commands properly for Windows and Unix`() {
    // given
    val windowsStrategy = WindowsStrategy()
    val linuxStrategy = LinuxStrategy()
    val macOsStrategy = MacOsStrategy()

    // when
    val winCmd = windowsStrategy.wrapCommand("idf.py build", useIdfEnv = true, idfPath = "C:\\esp")
    val linuxCmd = linuxStrategy.wrapCommand("idf.py build", useIdfEnv = true, idfPath = "/opt/esp")
    val macCmd = macOsStrategy.wrapCommand("idf.py build", useIdfEnv = false, idfPath = "/opt/esp")

    // then
    assertEquals("cmd", winCmd[0])
    assertEquals("/c", winCmd[1])
    assertTrue(winCmd[2].contains("export.bat"))
    assertTrue(winCmd[2].contains("idf.py build"))

    assertEquals("bash", linuxCmd[0])
    assertEquals("-c", linuxCmd[1])
    assertTrue(linuxCmd[2].contains("export.sh"))
    assertTrue(linuxCmd[2].contains("idf.py build"))

    assertEquals("bash", macCmd[0])
    assertEquals("-c", macCmd[1])
    assertEquals("idf.py build", macCmd[2])
  }
}
