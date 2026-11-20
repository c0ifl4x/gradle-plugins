plugins {
  `kotlin-dsl` apply false
  alias(libs.plugins.dokka.javadoc) apply false
}

subprojects {
  group = "pl.miloszgilga.gradle"

  pluginManager.withPlugin("org.gradle.kotlin.kotlin-dsl") {
    plugins.apply("org.jetbrains.dokka-javadoc")

    extensions.configure<JavaPluginExtension> {
      toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
      }
      withSourcesJar()
      withJavadocJar()
    }

    tasks.named<Jar>("javadocJar") {
      dependsOn("dokkaGeneratePublicationJavadoc")
      from(layout.buildDirectory.dir("dokka/javadoc"))
    }

    dependencies {
      "testImplementation"(gradleTestKit())
      "testImplementation"(platform(rootProject.libs.junit.bom))
      "testImplementation"(rootProject.libs.junit.jupiter)
      "testRuntimeOnly"(rootProject.libs.junit.platform.launcher)
    }

    tasks.withType<Test>().configureEach {
      useJUnitPlatform()
      testLogging {
        events("passed", "skipped", "failed")
      }
    }
  }

  pluginManager.withPlugin("maven-publish") {
    extensions.configure<PublishingExtension> {
      repositories {
        maven {
          name = "Reposilite"

          val baseUrl = "https://m2.miloszgilga.pl"
          val isSnapshot = version.toString().endsWith("-SNAPSHOT")
          url = uri(if (isSnapshot) "$baseUrl/snapshots" else "$baseUrl/releases")

          credentials {
            username = System.getenv("REPOSILITE_USER")
            password = System.getenv("REPOSILITE_TOKEN")
          }
        }
      }

      publications.withType<MavenPublication>().configureEach {
        pom {
          url.set("https://github.com/c0ifl4x/gradle-plugins")
          licenses {
            license {
              name.set("The Apache License, Version 2.0")
              url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
          }
          developers {
            developer {
              id.set("c0ifl4x")
              name.set("Miłosz Gilga")
            }
          }
          scm {
            connection.set("scm:git:git://github.com/c0ifl4x/gradle-plugins.git")
            developerConnection.set("scm:git:ssh://github.com/c0ifl4x/gradle-plugins.git")
            url.set("https://github.com/c0ifl4x/gradle-plugins")
          }
        }
      }
    }
  }
}
