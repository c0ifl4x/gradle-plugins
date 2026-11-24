plugins {
  `kotlin-dsl`
  `maven-publish`
}

version = "1.0.1"

dependencies {
  implementation(libs.node.gradle.plugin)
}

gradlePlugin {
  website.set("https://github.com/c0ifl4x/gradle-plugins")
  vcsUrl.set("https://github.com/c0ifl4x/gradle-plugins.git")

  plugins {
    create("frontendBuilder") {
      id = "pl.miloszgilga.gradle.frontend-builder"
      implementationClass = "pl.miloszgilga.webembed.FrontendBuilderPlugin"
      displayName = "Frontend Builder Plugin"
      description = "Configures Node/Yarn build and exposes webResources artifact"
      tags.set(listOf("frontend", "node", "yarn", "vite", "webapp"))
    }
    create("webEmbed") {
      id = "pl.miloszgilga.gradle.web-embed"
      implementationClass = "pl.miloszgilga.webembed.WebEmbedPlugin"
      displayName = "Web Embed Plugin"
      description = "Embeds frontend webResources artifact into JVM processResources"
      tags.set(listOf("webapp", "frontend", "resources", "jvm", "embed"))
    }
  }
}

publishing {
  publications.withType<MavenPublication>().configureEach {
    pom {
      name.set("WebEmbed Gradle Plugin")
      description.set("Gradle plugins for building frontend assets and embedding them into JVM server resources.")
    }
  }
}
