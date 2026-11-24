package pl.miloszgilga.espidf

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.io.File

class EspIdfFunctionalTest {
  @TempDir
  lateinit var testProjectDir: File

  @TempDir
  lateinit var mockBinDir: File

  private lateinit var customEnv: MutableMap<String, String>

  @BeforeEach
  fun setupMockEnvironment() {
    // create fake idf.py script that prints arguments and generates dummy output file
    File(mockBinDir, "idf.py").apply {
      writeText(
        """
        #!/usr/bin/env bash
        echo "MOCK_IDF_CALLED_WITH: $*"
        mkdir -p build
        touch build/test-fw.bin
        """.trimIndent()
      )
      setExecutable(true)
    }

    File(testProjectDir, "settings.gradle.kts").writeText("""rootProject.name = "test-fw"""")
    File(testProjectDir, "build.gradle.kts").writeText(
      """
      plugins {
        id("pl.miloszgilga.gradle.esp-idf")
      }
      espIdf {
        chip.set("esp32s3")
        appVersion.set("2.1.0-test")
      }
      """.trimIndent()
    )

    // create sample c source file to test incremental build caching
    File(testProjectDir, "main").mkdirs()
    File(testProjectDir, "main/main.c").writeText("void app_main(void) {}")

    customEnv = System.getenv().toMutableMap().apply {
      put("PATH", "${mockBinDir.absolutePath}:${get("PATH")}")
    }
  }

  @Test
  @DisabledOnOs(OS.WINDOWS)
  fun `setChip and buildFirmware execute idf commands with configured properties`() {
    // given
    val runner = GradleRunner.create()
      .withProjectDir(testProjectDir)
      .withPluginClasspath()
      .withEnvironment(customEnv)
      .withArguments("setChip", "buildFirmware", "--configuration-cache")

    // when
    val result = runner.build()

    // then
    assertEquals(TaskOutcome.SUCCESS, result.task(":setChip")?.outcome)
    assertEquals(TaskOutcome.SUCCESS, result.task(":buildFirmware")?.outcome)
    assertTrue(result.output.contains("MOCK_IDF_CALLED_WITH: set-target esp32s3"))
    assertTrue(result.output.contains("MOCK_IDF_CALLED_WITH: -DCMD_APP_VER=2.1.0-test build"))
  }

  @Test
  @DisabledOnOs(OS.WINDOWS)
  fun `buildFirmware supports incremental build and configuration cache reuse`() {
    // given
    val runner = GradleRunner.create()
      .withProjectDir(testProjectDir)
      .withPluginClasspath()
      .withEnvironment(customEnv)
      .withArguments("buildFirmware", "--configuration-cache")

    // when
    val firstRun = runner.build()
    val secondRun = runner.build()

    // then
    assertEquals(TaskOutcome.SUCCESS, firstRun.task(":buildFirmware")?.outcome)
    assertEquals(TaskOutcome.UP_TO_DATE, secondRun.task(":buildFirmware")?.outcome)
    assertTrue(
      secondRun.output.contains("Configuration cache entry reused") ||
        secondRun.output.contains("Reusing configuration cache")
    )
  }

  @Test
  @DisabledOnOs(OS.WINDOWS)
  fun `espIdfComponents resolves shared module paths and invalidates cache on component change`() {
    // given
    val commonDir = File(testProjectDir, "common-components").apply { mkdirs() }
    val sharedComponentFile = File(commonDir, "mqtt_bus/mqtt_bus.c").apply {
      parentFile.mkdirs()
      writeText("void mqtt_bus_init(void) {}")
    }
    File(commonDir, "build.gradle.kts").writeText(
      """
      plugins {
        id("pl.miloszgilga.gradle.esp-idf-components")
      }
      """.trimIndent()
    )

    val vendorDir = File(testProjectDir, "vendor-components").apply { mkdirs() }

    val appDir = File(testProjectDir, "app").apply { mkdirs() }
    File(appDir, "main").mkdirs()
    File(appDir, "main/main.c").writeText("void app_main(void) {}")
    File(appDir, "build.gradle.kts").writeText(
      """
      plugins {
        id("pl.miloszgilga.gradle.esp-idf")
      }
      espIdf {
        chip.set("esp32c3")
        appVersion.set("1.0.0")
        binaryName.set("test-fw.bin")
        extraComponentDirs.from(rootProject.file("vendor-components"))
      }
      dependencies {
        espComponents(project(":common-components"))
      }
      """.trimIndent()
    )

    File(testProjectDir, "build.gradle.kts").delete()
    File(testProjectDir, "settings.gradle.kts").writeText(
      """
      rootProject.name = "monorepo-test"
      include(":common-components", ":app")
      """.trimIndent()
    )

    val runner = GradleRunner.create()
      .withProjectDir(testProjectDir)
      .withPluginClasspath()
      .withEnvironment(customEnv)
      .withArguments(":app:buildFirmware", "--configuration-cache")

    // when - first build
    val firstRun = runner.build()

    // then - both Gradle module and manual extraComponentDirs are passed to CMake
    val expectedCommonPath = commonDir.absolutePath.replace('\\', '/')
    val expectedVendorPath = vendorDir.absolutePath.replace('\\', '/')
    assertEquals(TaskOutcome.SUCCESS, firstRun.task(":app:buildFirmware")?.outcome)
    assertTrue(
      firstRun.output.contains(
        "MOCK_IDF_CALLED_WITH: -DEXTRA_COMPONENT_DIRS=$expectedVendorPath;$expectedCommonPath -DCMD_APP_VER=1.0.0 build"
      ) || firstRun.output.contains(
        "MOCK_IDF_CALLED_WITH: -DEXTRA_COMPONENT_DIRS=$expectedCommonPath;$expectedVendorPath -DCMD_APP_VER=1.0.0 build"
      )
    )

    // when - second build without changes
    val secondRun = runner.build()
    assertEquals(TaskOutcome.UP_TO_DATE, secondRun.task(":app:buildFirmware")?.outcome)

    // when - third build after modifying file in shared component module
    sharedComponentFile.writeText("void mqtt_bus_init(void) { int v = 2; }")
    val thirdRun = runner.build()

    // then - cache is invalidated and firmware rebuilds
    assertEquals(TaskOutcome.SUCCESS, thirdRun.task(":app:buildFirmware")?.outcome)
  }

  @Test
  @DisabledOnOs(OS.WINDOWS)
  fun `flashFirmware skips attachUsbWsl when autoAttach is disabled`() {
    // given
    val runner = GradleRunner.create()
      .withProjectDir(testProjectDir)
      .withPluginClasspath()
      .withEnvironment(customEnv)
      .withArguments("flashFirmware", "--configuration-cache")

    // when
    val result = runner.build()

    // then
    assertEquals(TaskOutcome.SKIPPED, result.task(":attachUsbWsl")?.outcome)
    assertEquals(TaskOutcome.SUCCESS, result.task(":buildFirmware")?.outcome)
    assertEquals(TaskOutcome.SUCCESS, result.task(":flashFirmware")?.outcome)
    assertTrue(result.output.contains("MOCK_IDF_CALLED_WITH: -p /dev/ttyACM0 -b 921600 flash"))
  }
}
