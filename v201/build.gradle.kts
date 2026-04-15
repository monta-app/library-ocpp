val libraryVersion: String by project
val javaToolChainVersion: String by project

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
    id("java-library")
    id("maven-publish")
}

val libraryGroupId = "com.monta.library.ocpp"
val libraryArtifactId = "ocpp-v201"

group = "$libraryGroupId:$libraryArtifactId"
version = libraryVersion

dependencies {
    // Internal Libs
    implementation(project(":core"))
    // Main
    implementation(platform(libs.kotlin.bom))
    implementation(libs.bundles.kotlin)

    implementation(platform(libs.coroutines.bom))
    implementation(libs.bundles.coroutines)

    implementation(platform(libs.jackson.bom))
    implementation(libs.bundles.jackson)

    implementation(libs.bundles.logback)
    // Testing
    testImplementation(platform(libs.kotest.bom))
    testImplementation(libs.bundles.kotest.test)
    testRuntimeOnly(libs.bundles.kotest.test.runtime)
}

java {
    withJavadocJar()
    withSourcesJar()
}

kotlin {
    jvmToolchain(javaToolChainVersion.toInt())
}

tasks {
    test {
        useJUnitPlatform()
    }
    processResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
    processTestResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/monta-app/library-ocpp")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
    publications {
        register<MavenPublication>("gpr") {
            groupId = libraryGroupId
            artifactId = libraryArtifactId
            version = libraryVersion
            from(components["java"])
            pom {
                name.set("Monta OCPP Core Library")
                url.set("https://github.com/monta-app/library-ocpp")
                scm {
                    connection.set("git@github.com:monta-app/library-ocpp.git")
                    url.set("https://github.com/monta-app/library-ocpp")
                }
            }
        }
    }
}
