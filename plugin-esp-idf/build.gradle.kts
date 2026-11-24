plugins {
  `kotlin-dsl`
  `maven-publish`
}

version = "3.1.5"

gradlePlugin {
  website.set("https://github.com/c0ifl4x/gradle-plugins")
  vcsUrl.set("https://github.com/c0ifl4x/gradle-plugins.git")

  plugins {
    create("espIdf") {
      id = "pl.miloszgilga.gradle.esp-idf"
      implementationClass = "pl.miloszgilga.espidf.EspIdfPlugin"
      displayName = "ESP-IDF Gradle Plugin"
      description = "Cross-platform Gradle wrapper for ESP-IDF firmware projects (Windows, Linux, macOS)"
      tags.set(listOf("esp32", "esp-idf", "embedded", "firmware", "c"))
    }
    create("espIdfComponents") {
      id = "pl.miloszgilga.gradle.esp-idf-components"
      implementationClass = "pl.miloszgilga.espidf.EspIdfComponentsPlugin"
      displayName = "ESP-IDF Components Gradle Plugin"
      description = "Exports shared C/C++ ESP-IDF components as a consumable Gradle module artifact"
      tags.set(listOf("esp32", "esp-idf", "embedded", "components", "c"))
    }
  }
}

publishing {
  publications.withType<MavenPublication>().configureEach {
    pom {
      name.set("ESP-IDF Gradle Plugin")
      description.set("Cross-platform Gradle wrapper for ESP-IDF firmware projects")
    }
  }
}
