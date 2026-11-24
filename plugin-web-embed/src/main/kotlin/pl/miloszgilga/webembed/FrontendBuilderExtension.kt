package pl.miloszgilga.webembed

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * Configuration extension for the Frontend Builder Gradle plugin (`frontendBuilder { ... }`).
 *
 * Provides defaults and properties for managing Node.js/Yarn toolchains, running development servers, and building
 * production frontend bundles exposed via the `webResources` configuration.
 *
 * @author Miłosz Gilga
 * @since 1.0.0
 */
abstract class FrontendBuilderExtension {
  /**
   * Node.js runtime version downloaded and used by the `gradle-node-plugin`.
   *
   * Defaults to `"26.13.1"`.
   */
  abstract val nodeVersion: Property<String>

  /**
   * Yarn package manager version used to install dependencies and execute scripts.
   *
   * Defaults to `"1.22.22"`.
   */
  abstract val yarnVersion: Property<String>

  /**
   * Whether Gradle should automatically download and manage the specified Node.js and Yarn binaries instead of relying
   * on globally installed system versions.
   *
   * Defaults to `true`.
   */
  abstract val downloadNode: Property<Boolean>

  /**
   * Output directory name relative to the project root where the production frontend bundle is generated and exposed as
   * an artifact.
   *
   * Defaults to `"dist"`.
   */
  abstract val buildDirName: Property<String>

  /**
   * Yarn script name executed by the `yarnDev` task to start the local development server.
   *
   * Defaults to `"dev"`.
   */
  abstract val devScript: Property<String>

  /**
   * Yarn script name executed by the `buildWeb` task to compile the production frontend bundle.
   *
   * Defaults to `"build"`.
   */
  abstract val buildScript: Property<String>

  /**
   * List of project configuration files tracked as Gradle task inputs for incremental builds of `buildWeb`.
   *
   * Defaults to `["package.json", "tsconfig.json", "vite.config.ts", "yarn.lock"]`.
   */
  abstract val inputFiles: ListProperty<String>

  /**
   * Additional files or directories tracked as task inputs for incremental builds of `buildWeb`
   * (e.g. `"public"`, `".env.production"`).
   */
  abstract val additionalInputs: ConfigurableFileCollection

  /**
   * Additional files or directories removed when running the `clean` task alongside [buildDirName]
   * (e.g. `".vite"`).
   */
  abstract val additionalDeletes: ConfigurableFileCollection

  init {
    nodeVersion.convention("26.13.1")
    yarnVersion.convention("1.22.22")
    downloadNode.convention(true)
    buildDirName.convention("dist")
    devScript.convention("dev")
    buildScript.convention("build")
    inputFiles.convention(
      listOf("package.json", "tsconfig.json", "vite.config.ts", "yarn.lock")
    )
  }
}
