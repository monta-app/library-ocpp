plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    alias(libs.plugins.kotlin.jvm) apply false
    // Linter
    alias(libs.plugins.ktlint)
    // Code coverage
    alias(libs.plugins.kover) apply false
    // Apply the java-library plugin for API and implementation separation.
    `java-library`
}

subprojects {
    apply(plugin = "org.jetbrains.kotlinx.kover")
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
