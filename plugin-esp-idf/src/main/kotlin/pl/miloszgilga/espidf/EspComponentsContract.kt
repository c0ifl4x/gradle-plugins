package pl.miloszgilga.espidf

import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Category
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.named

internal object EspComponentsContract {
  const val CONFIGURATION_NAME = "espComponents"
  private const val CATEGORY_VALUE = "esp-idf-components"

  fun applyAttributes(config: Configuration, objects: ObjectFactory) {
    config.attributes {
      attribute(Category.CATEGORY_ATTRIBUTE, objects.named(CATEGORY_VALUE))
    }
  }
}
