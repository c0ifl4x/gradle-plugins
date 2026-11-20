plugins {
  `kotlin-dsl`
  `maven-publish`
}

version = "1.0.0"

gradlePlugin {
  website.set("https://github.com/c0ifl4x/gradle-plugins")
  vcsUrl.set("https://github.com/c0ifl4x/gradle-plugins.git")

  plugins {
    create("espIdfPlugin") {
      id = "pl.miloszgilga.gradle.espidf"
      implementationClass = "pl.miloszgilga.espidf.EspIdfPlugin"
      displayName = "ESP-IDF Gradle Plugin"
      description = "Cross-platform Gradle wrapper for ESP-IDF firmware projects (Windows, Linux, macOS)"
      tags.set(listOf("esp32", "esp-idf", "embedded", "firmware", "c"))
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
