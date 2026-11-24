package pl.miloszgilga.webembed

import org.gradle.api.plugins.JavaPlugin
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.findByType
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull

class WebEmbedPluginTest {
  @Test
  fun `frontendBuilder plugin registers extension with default conventions`() {
    // given
    val project = ProjectBuilder.builder().build()

    // when
    project.pluginManager.apply(FrontendBuilderPlugin::class)

    // then
    val extension = project.extensions.findByType<FrontendBuilderExtension>()
    assertNotNull(extension)
    assertEquals("26.13.1", extension.nodeVersion.get())
    assertEquals("1.22.22", extension.yarnVersion.get())
    assertTrue(extension.downloadNode.get())
    assertEquals("dist", extension.buildDirName.get())
    assertEquals("dev", extension.devScript.get())
    assertEquals("build", extension.buildScript.get())
    assertEquals(
      listOf("package.json", "tsconfig.json", "vite.config.ts", "yarn.lock"),
      extension.inputFiles.get()
    )
  }

  @Test
  fun `frontendBuilder plugin registers tasks and consumable webResources configuration`() {
    // given
    val project = ProjectBuilder.builder().build()

    // when
    project.pluginManager.apply(FrontendBuilderPlugin::class)

    // then
    assertNotNull(project.tasks.findByName("yarnInstall"))
    assertNotNull(project.tasks.findByName("yarnDev"))
    assertNotNull(project.tasks.findByName("buildWeb"))
    assertNotNull(project.tasks.findByName("clean"))

    val webResources = project.configurations.findByName("webResources")
    assertNotNull(webResources)
    assertTrue(webResources.isCanBeConsumed)
    assertFalse(webResources.isCanBeResolved)
  }

  @Test
  fun `webEmbed plugin registers extension and resolvable uiResources configuration`() {
    // given
    val project = ProjectBuilder.builder().build()
    project.pluginManager.apply(JavaPlugin::class)

    // when
    project.pluginManager.apply(WebEmbedPlugin::class)

    // then
    val extension = project.extensions.findByType<WebEmbedExtension>()
    assertNotNull(extension)
    assertEquals("webapp", extension.targetDir.get())
    assertTrue(extension.autoWireSiblingUi.get())
    assertFalse(extension.uiProject.isPresent)
    assertFalse(extension.skip.get())

    val uiResources = project.configurations.findByName("uiResources")
    assertNotNull(uiResources)
    assertFalse(uiResources.isCanBeConsumed)
    assertTrue(uiResources.isCanBeResolved)
  }
}
