rootProject.name = "gradle-plugins"

dependencyResolutionManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
  }
}

include(
  "esp-idf-plugin"
)
