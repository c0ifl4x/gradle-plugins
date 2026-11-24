package pl.miloszgilga.espidf

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin

/**
 * Gradle plugin for shared ESP-IDF component modules (`pl.miloszgilga.gradle.esp-idf-components`).
 *
 * Applies the Gradle [BasePlugin] and exposes the module's root directory as a consumable artifact under the
 * `espComponents` configuration, allowing firmware projects using [EspIdfPlugin] to depend on shared C/C++ components.
 *
 * @author Miłosz Gilga
 * @since 3.1.5
 */
class EspIdfComponentsPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.pluginManager.apply(BasePlugin::class.java)

    val outgoing = project.configurations.create(EspComponentsContract.CONFIGURATION_NAME) {
      isCanBeConsumed = true
      isCanBeResolved = false
    }
    EspComponentsContract.applyAttributes(outgoing, project.objects)
    project.artifacts.add(outgoing.name, project.projectDir)
  }
}
