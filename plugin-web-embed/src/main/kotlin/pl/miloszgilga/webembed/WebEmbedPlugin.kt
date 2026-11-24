package pl.miloszgilga.webembed

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.withType
import org.gradle.language.jvm.tasks.ProcessResources

/**
 * Gradle plugin that resolves compiled frontend assets from a `webResources` configuration and packages them into the
 * JVM project's classpath resources during `processResources`.
 *
 * @author Miłosz Gilga
 * @since 1.0.0
 */
class WebEmbedPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    val extension = project.extensions.create<WebEmbedExtension>("webEmbed")

    val uiResources = project.configurations.create("uiResources") {
      isCanBeConsumed = false
      isCanBeResolved = true
    }

    project.tasks.withType<ProcessResources>().configureEach {
      from(uiResources) {
        into(extension.targetDir)
      }
    }

    project.afterEvaluate {
      if (extension.skip.get()) {
        return@afterEvaluate
      }
      val targetUiPath = when {
        extension.uiProject.isPresent -> extension.uiProject.get()
        extension.autoWireSiblingUi.get() -> resolveSiblingUiPath(project)
        else -> null
      }
      if (targetUiPath != null) {
        project.dependencies.add(
          uiResources.name,
          project.dependencies.project(
            mapOf(
              "path" to targetUiPath,
              "configuration" to "webResources"
            )
          )
        )
      }
    }
  }

  private fun resolveSiblingUiPath(project: Project): String? {
    val parent = project.parent ?: return null
    val siblingUi = parent.findProject("ui") ?: return null
    return siblingUi.path
  }
}
