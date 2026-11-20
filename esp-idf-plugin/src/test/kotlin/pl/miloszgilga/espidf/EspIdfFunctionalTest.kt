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
        id("pl.miloszgilga.gradle.espidf")
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
    assertTrue(result.output.contains("MOCK_IDF_CALLED_WITH: build -DCMD_APP_VER=2.1.0-test"))
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
}
