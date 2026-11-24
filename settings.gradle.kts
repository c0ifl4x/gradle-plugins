@file:Suppress("UnstableApiUsage")

rootProject.name = "gradle-plugins"

dependencyResolutionManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
  }
}

include("plugin-esp-idf")
include("plugin-web-embed")
