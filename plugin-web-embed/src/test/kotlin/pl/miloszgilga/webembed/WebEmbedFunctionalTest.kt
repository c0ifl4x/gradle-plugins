package pl.miloszgilga.webembed

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class WebEmbedFunctionalTest {
  @TempDir
  lateinit var projectDir: File

  @Test
  fun `automatically wires sibling ui buildWeb task to server processResources`() {
    // given
    writeFile(
      "settings.gradle.kts",
      """
      rootProject.name = "test-monorepo"
      include(":service:ui", ":service:server")
      """.trimIndent()
    )
    writeFile(
      "service/ui/build.gradle.kts",
      """
      plugins {
        id("pl.miloszgilga.gradle.frontend-builder")
      }
      """.trimIndent()
    )
    writeFile(
      "service/server/build.gradle.kts",
      """
      plugins {
        java
        id("pl.miloszgilga.gradle.web-embed")
      }
      """.trimIndent()
    )

    // when
    val result = GradleRunner.create()
      .withProjectDir(projectDir)
      .withPluginClasspath()
      .withArguments(":service:server:processResources", "--dry-run")
      .build()

    // then
    assertTrue(result.output.contains(":service:ui:yarnInstall SKIPPED"))
    assertTrue(result.output.contains(":service:ui:buildWeb SKIPPED"))
    assertTrue(result.output.contains(":service:server:processResources SKIPPED"))
  }

  @Test
  fun `skips sibling ui wiring when autoWireSiblingUi is disabled`() {
    // given
    writeFile(
      "settings.gradle.kts",
      """
      rootProject.name = "test-monorepo"
      include(":service:ui", ":service:server")
      """.trimIndent()
    )
    writeFile(
      "service/ui/build.gradle.kts",
      """
      plugins {
        id("pl.miloszgilga.gradle.frontend-builder")
      }
      """.trimIndent()
    )
    writeFile(
      "service/server/build.gradle.kts",
      """
      plugins {
        java
        id("pl.miloszgilga.gradle.web-embed")
      }

      webEmbed {
        autoWireSiblingUi.set(false)
      }
      """.trimIndent()
    )

    // when
    val result = GradleRunner.create()
      .withProjectDir(projectDir)
      .withPluginClasspath()
      .withArguments(":service:server:processResources", "--dry-run")
      .build()

    // then
    assertFalse(result.output.contains(":service:ui:buildWeb"))
    assertTrue(result.output.contains(":service:server:processResources SKIPPED"))
  }

  @Test
  fun `embeds frontend artifacts into custom targetDir for explicit uiProject`() {
    // given
    writeFile(
      "settings.gradle.kts",
      """
      rootProject.name = "test-monorepo"
      include(":custom-ui", ":backend")
      """.trimIndent()
    )
    writeFile(
      "custom-ui/build.gradle.kts",
      """
      val webResources by configurations.creating {
        isCanBeConsumed = true
        isCanBeResolved = false
      }

      val fakeBuildWeb = tasks.register("fakeBuildWeb") {
        val outDir = layout.buildDirectory.dir("dist")
        outputs.dir(outDir)
        doLast {
          val dir = outDir.get().asFile
          dir.mkdirs()
          dir.resolve("index.html").writeText("<h1>Hello HTML</h1>")
        }
      }

      artifacts {
        add(webResources.name, fakeBuildWeb)
      }
      """.trimIndent()
    )
    writeFile(
      "backend/build.gradle.kts",
      """
      plugins {
        java
        id("pl.miloszgilga.gradle.web-embed")
      }

      webEmbed {
        uiProject.set(":custom-ui")
        targetDir.set("static/admin")
      }
      """.trimIndent()
    )

    // when
    val result = GradleRunner.create()
      .withProjectDir(projectDir)
      .withPluginClasspath()
      .withArguments(":backend:processResources")
      .build()

    // then
    assertEquals(TaskOutcome.SUCCESS, result.task(":custom-ui:fakeBuildWeb")?.outcome)
    assertEquals(TaskOutcome.SUCCESS, result.task(":backend:processResources")?.outcome)

    val embeddedIndex = projectDir.resolve("backend/build/resources/main/static/admin/index.html")
    assertTrue(embeddedIndex.exists())
    assertEquals("<h1>Hello HTML</h1>", embeddedIndex.readText())
  }

  @Test
  fun `skips ui build and embedding when skipWebEmbed project property is passed`() {
    // given
    writeFile(
      "settings.gradle.kts",
      """
      rootProject.name = "test-monorepo"
      include(":service:ui", ":service:server")
      """.trimIndent()
    )
    writeFile(
      "service/ui/build.gradle.kts",
      """
      plugins {
        id("pl.miloszgilga.gradle.frontend-builder")
      }
      """.trimIndent()
    )
    writeFile(
      "service/server/build.gradle.kts",
      """
      plugins {
        java
        id("pl.miloszgilga.gradle.web-embed")
      }
      """.trimIndent()
    )

    // when
    val result = GradleRunner.create()
      .withProjectDir(projectDir)
      .withPluginClasspath()
      .withArguments(":service:server:processResources", "-PskipWebEmbed", "--dry-run")
      .build()

    // then
    assertFalse(result.output.contains(":service:ui:yarnInstall"))
    assertFalse(result.output.contains(":service:ui:buildWeb"))
    assertTrue(result.output.contains(":service:server:processResources SKIPPED"))
  }

  @Test
  fun `skips ui build and embedding when skip is enabled in extension DSL`() {
    // given
    writeFile(
      "settings.gradle.kts",
      """
      rootProject.name = "test-monorepo"
      include(":custom-ui", ":backend")
      """.trimIndent()
    )
    writeFile(
      "custom-ui/build.gradle.kts",
      """
      val webResources by configurations.creating {
        isCanBeConsumed = true
        isCanBeResolved = false
      }

      val fakeBuildWeb = tasks.register("fakeBuildWeb") {
        val outDir = layout.buildDirectory.dir("dist")
        outputs.dir(outDir)
        doLast {
          val dir = outDir.get().asFile
          dir.mkdirs()
          dir.resolve("index.html").writeText("<h1>Should not be built</h1>")
        }
      }

      artifacts {
        add(webResources.name, fakeBuildWeb)
      }
      """.trimIndent()
    )
    writeFile(
      "backend/build.gradle.kts",
      """
      plugins {
        java
        id("pl.miloszgilga.gradle.web-embed")
      }

      webEmbed {
        uiProject.set(":custom-ui")
        skip.set(true)
      }
      """.trimIndent()
    )

    // when
    val result = GradleRunner.create()
      .withProjectDir(projectDir)
      .withPluginClasspath()
      .withArguments(":backend:processResources")
      .build()

    // then
    assertNull(result.task(":custom-ui:fakeBuildWeb"))
    assertEquals(TaskOutcome.NO_SOURCE, result.task(":backend:processResources")?.outcome)
    assertFalse(projectDir.resolve("backend/build/resources/main/webapp/index.html").exists())
  }

  private fun writeFile(relativePath: String, content: String) {
    val file = projectDir.resolve(relativePath)
    file.parentFile.mkdirs()
    file.writeText(content)
  }
}
