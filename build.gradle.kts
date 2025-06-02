plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") apply false
    // Linter
    id("org.jlleitschuh.gradle.ktlint") version "12.3.0"
    // Apply the java-library plugin for API and implementation separation.
    `java-library`
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/monta-app/library-micronaut")
            credentials {
                username = System.getenv("GHL_USERNAME") ?: project.findProperty("gpr.user") as String?
                password = System.getenv("GHL_PASSWORD") ?: project.findProperty("gpr.key") as String?
            }
        }
    }
}
