package pl.miloszgilga.webembed

import com.github.gradle.node.NodeExtension
import com.github.gradle.node.NodePlugin
import com.github.gradle.node.yarn.task.YarnTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

/**
 * Gradle plugin that configures Node.js and Yarn toolchains for building frontend applications and exposes the compiled
 * output directory via the `webResources` configuration.
 *
 * @author Miłosz Gilga
 * @since 1.0.0
 */
class FrontendBuilderPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    val extension = project.extensions.create<FrontendBuilderExtension>("frontendBuilder")

    project.pluginManager.apply(NodePlugin::class.java)

    project.extensions.configure<NodeExtension> {
      version.set(extension.nodeVersion)
      yarnVersion.set(extension.yarnVersion)
      download.set(extension.downloadNode)
    }

    val yarnInstall = project.tasks.register("yarnInstall") {
      group = "node"
      description = "Install frontend dependencies via Yarn"
      dependsOn("yarn")
    }
    project.tasks.register<YarnTask>("yarnDev") {
      group = "application"
      description = "Run frontend development server"
      dependsOn(yarnInstall)
      args.set(extension.devScript.map { listOf("run", it) })
    }
    val buildWeb = project.tasks.register<YarnTask>("buildWeb") {
      group = "build"
      description = "Build production frontend bundle"
      dependsOn(yarnInstall)
      args.set(extension.buildScript.map { listOf("run", it) })

      inputs.dir(project.file("src"))
      inputs.dir(project.file("static")).optional()
      inputs.files(extension.inputFiles.map { files -> files.map { project.file(it) } })
      inputs.files(extension.additionalInputs)

      outputs.dir(extension.buildDirName.map { project.file(it) })
    }
    val webResources = project.configurations.create("webResources") {
      isCanBeConsumed = true
      isCanBeResolved = false
    }
    project.artifacts.add(webResources.name, buildWeb)

    if (project.tasks.findByName("clean") == null) {
      project.tasks.register<Delete>("clean") {
        group = "build"
        description = "Delete frontend generated files"
        delete(extension.buildDirName, extension.additionalDeletes)
      }
    }
  }
}
