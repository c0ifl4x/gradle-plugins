rootProject.name = "gradle-plugins"

dependencyResolutionManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
  }
}

include("plugin-esp-idf")
