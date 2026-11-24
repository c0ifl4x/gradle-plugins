package pl.miloszgilga.webembed

import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import javax.inject.Inject

/**
 * Configuration extension for the Web Embed Gradle plugin (`webEmbed { ... }`).
 *
 * Configures how compiled frontend assets (`webResources`) are resolved and packaged into the JVM application's
 * classpath resources during `processResources`.
 *
 * @author Miłosz Gilga
 * @since 1.0.0
 */
abstract class WebEmbedExtension @Inject constructor(providers: ProviderFactory) {
  /**
   * Explicit Gradle project path pointing to the frontend module exposing the `webResources` configuration
   * (e.g. `":some-service-project:ui"`).
   *
   * If not set and [autoWireSiblingUi] is enabled, the plugin attempts to resolve a sibling `:ui` project
   * automatically.
   */
  abstract val uiProject: Property<String>

  /**
   * Target subdirectory inside the JVM classpath resources (`src/main/resources/<targetDir>`)
   * where the frontend assets will be copied.
   *
   * Defaults to `"webapp"`.
   */
  abstract val targetDir: Property<String>

  /**
   * Whether to automatically wire a sibling `:ui` subproject under the same parent module when [uiProject] is not
   * explicitly configured.
   *
   * Defaults to `true`.
   */
  abstract val autoWireSiblingUi: Property<Boolean>

  /**
   * When `true`, skips resolving and embedding frontend assets from `webResources`.
   *
   * Can be enabled via `-PskipWebEmbed` CLI flag or configured directly in the DSL.
   */
  abstract val skip: Property<Boolean>

  init {
    targetDir.convention("webapp")
    autoWireSiblingUi.convention(true)
    skip.convention(
      providers.gradleProperty("skipWebEmbed")
        .map { it.isEmpty() || it.equals("true", ignoreCase = true) }
        .orElse(false)
    )
  }
}
