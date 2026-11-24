package pl.miloszgilga.espidf

import org.gradle.api.attributes.Category
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Exec
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.io.File

class EspIdfPluginTest {
  @Test
  fun `plugin registers all expected firmware tasks and default conventions`() {
    // given
    val project = ProjectBuilder.builder().withName("esp-firmware").build()
    val expectedTasks = listOf(
      "attachUsbWsl",
      "checkEnv",
      "setChip",
      "buildFirmware",
      "buildAppOnly",
      "rebuildFirmware",
      "flashFirmware",
      "flashAppOnly",
      "monitorFirmware",
      "runFirmware",
      "eraseFlash",
      "cleanFirmware",
      "generateDocs"
    )

    // when
    project.pluginManager.apply("pl.miloszgilga.gradle.esp-idf")

    // then
    expectedTasks.forEach { taskName ->
      assertTrue(project.tasks.names.contains(taskName), "Task '$taskName' should be registered")
    }
    val ext = project.extensions.getByType(EspIdfExtension::class.java)
    assertEquals("esp32", ext.chip.get())
    assertEquals("921600", ext.baud.get())
    assertEquals("${project.name}.bin", ext.binaryName.get())
    assertFalse(ext.usbipdAutoAttach.get())
    assertTrue(ext.usbipdDevicePattern.get().contains("303a:"))

    val incomingConfig = project.configurations.findByName(EspComponentsContract.CONFIGURATION_NAME)
    assertNotNull(incomingConfig)
    assertFalse(incomingConfig!!.isCanBeConsumed)
    assertTrue(incomingConfig.isCanBeResolved)
  }

  @Test
  fun `serial tasks depend on attachUsbWsl while build tasks do not`() {
    // given
    val project = ProjectBuilder.builder().build()
    project.pluginManager.apply("pl.miloszgilga.gradle.esp-idf")

    val attachTask = project.tasks.named("attachUsbWsl").get()
    val serialTaskNames = listOf("flashFirmware", "flashAppOnly", "monitorFirmware", "runFirmware", "eraseFlash")

    // then
    serialTaskNames.forEach { taskName ->
      val taskDeps = project.tasks.named(taskName).get().taskDependencies.getDependencies(null)
      assertTrue(taskDeps.contains(attachTask), "Task '$taskName' should depend on 'attachUsbWsl'")
    }

    val buildDeps = project.tasks.named("buildFirmware").get().taskDependencies.getDependencies(null)
    assertFalse(buildDeps.contains(attachTask), "Task 'buildFirmware' should not depend on 'attachUsbWsl'")
  }

  @Test
  fun `extension overrides are reflected in task configuration`() {
    // given
    val project = ProjectBuilder.builder().build()
    project.pluginManager.apply("pl.miloszgilga.gradle.esp-idf")
    val ext = project.extensions.getByType(EspIdfExtension::class.java)

    // when
    ext.chip.set("esp32s3")
    ext.port.set("/dev/ttyUSB9")
    ext.usbipdAutoAttach.set(true)

    // then
    val setChipTask = project.tasks.named("setChip", Exec::class.java).get()
    assertEquals("firmware", setChipTask.group)
    assertEquals("esp32s3", ext.chip.get())
    assertEquals("/dev/ttyUSB9", ext.port.get())
    assertTrue(ext.usbipdAutoAttach.get())
  }

  @Test
  fun `components plugin registers consumable configuration and exports project directory`() {
    // given
    val project = ProjectBuilder.builder().withName("common-components").build()

    // when
    project.pluginManager.apply("pl.miloszgilga.gradle.esp-idf-components")

    // then
    val outgoingConfig = project.configurations.findByName(EspComponentsContract.CONFIGURATION_NAME)
    assertNotNull(outgoingConfig)
    assertTrue(outgoingConfig!!.isCanBeConsumed)
    assertFalse(outgoingConfig.isCanBeResolved)
    assertEquals(
      "esp-idf-components",
      outgoingConfig.attributes.getAttribute(Category.CATEGORY_ATTRIBUTE)?.name
    )
    assertTrue(outgoingConfig.artifacts.files.contains(project.projectDir))
  }

  @Test
  @DisabledOnOs(OS.WINDOWS)
  fun `WslUsbAttacher binds unshared device via UAC and attaches it to WSL2`(@TempDir tempDir: File) {
    // given
    val procVersion = File(tempDir, "proc_version").apply {
      writeText("Linux version 5.15.153.1-microsoft-standard-WSL2")
    }
    val fakeSerialPort = File(tempDir, "ttyACM0").apply { writeText("") }
    val callLog = File(tempDir, "calls.log")

    val mockUsbipd = File(tempDir, "usbipd.exe").apply {
      writeText(
        """
        #!/usr/bin/env bash
        echo "usbipd $*" >> "${callLog.absolutePath}"
        if [ "$1" = "list" ]; then
          cat << 'EOF'
        Connected:
        BUSID  VID:PID    DEVICE                                                        STATE
        1-2    046d:c52b  Logitech USB Input Device                                     Not shared
        16-2   303a:1001  USB JTAG/serial debug unit                                    Not shared
        EOF
        fi
        """.trimIndent()
      )
      setExecutable(true)
    }

    val mockPowershell = File(tempDir, "powershell.exe").apply {
      writeText(
        """
        #!/usr/bin/env bash
        echo "powershell $*" >> "${callLog.absolutePath}"
        """.trimIndent()
      )
      setExecutable(true)
    }

    val logger = Logging.getLogger(EspIdfPluginTest::class.java)

    // when
    WslUsbAttacher.attachMatchingDevice(
      devicePattern = "303a:",
      expectedPort = fakeSerialPort.absolutePath,
      logger = logger,
      procVersionFile = procVersion,
      usbipdOverride = mockUsbipd.absolutePath,
      powershellBin = mockPowershell.absolutePath
    )

    // then
    val logLines = callLog.readLines()
    assertEquals(3, logLines.size)
    assertEquals("usbipd list", logLines[0])
    assertTrue(logLines[1].contains("powershell") && logLines[1].contains("bind --busid 16-2"))
    assertEquals("usbipd attach --wsl --busid 16-2", logLines[2])
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
