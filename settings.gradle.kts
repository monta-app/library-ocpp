rootProject.name = "library-ocpp"

pluginManagement {
    val kotlinVersion: String by settings
    plugins {
        id("org.jetbrains.kotlin.jvm") version (kotlinVersion)
    }
}

include("server", "core", "v16", "v201")

dependencyResolutionManagement {
    versionCatalogs {
        // Kotlin
        create("kotlinlibs") {
            version("kotlin", providers.gradleProperty("kotlinVersion").get())
            library("platform", "org.jetbrains.kotlin", "kotlin-bom").versionRef("kotlin")
            library("stdlib", "org.jetbrains.kotlin", "kotlin-stdlib-jdk8").withoutVersion()
            bundle("implementation", listOf("stdlib"))
        }

        // Kotlin coroutines
        create("coroutines") {
            version("coroutines", "1.10.2")
            library("platform", "org.jetbrains.kotlinx", "kotlinx-coroutines-bom").versionRef("coroutines")
            library("core", "org.jetbrains.kotlinx", "kotlinx-coroutines-core").withoutVersion()
            library("jdk8", "org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8").withoutVersion()
            library("slf4j", "org.jetbrains.kotlinx", "kotlinx-coroutines-slf4j").withoutVersion()
            bundle("implementation", listOf("core", "jdk8", "slf4j"))
        }

        // Jackson serialization
        create("jackson") {
            version("jackson", "2.21.2")
            library("platform", "com.fasterxml.jackson", "jackson-bom").versionRef("jackson")
            library("core", "com.fasterxml.jackson.core", "jackson-core").withoutVersion()
            library("annotations", "com.fasterxml.jackson.core", "jackson-annotations").withoutVersion()
            library("databind", "com.fasterxml.jackson.core", "jackson-databind").withoutVersion()
            library("kotlin", "com.fasterxml.jackson.module", "jackson-module-kotlin").withoutVersion()
            library("datatypes", "com.fasterxml.jackson.datatype", "jackson-datatype-jsr310").withoutVersion()
            bundle("implementation", listOf("core", "annotations", "databind", "kotlin", "datatypes"))
        }

        // Kotest test framework
        create("kotest") {
            version("kotest", "5.9.1")
            version("strikt", "0.35.1")
            version("jupiter", "6.0.3")

            library("platform", "io.kotest", "kotest-bom").versionRef("kotest")
            library("junit5", "io.kotest", "kotest-runner-junit5-jvm").withoutVersion()
            library("assertions", "io.kotest", "kotest-assertions-core").withoutVersion()
            library("strikt", "io.strikt", "strikt-core").versionRef("strikt")
            library("junit-jupiter", "org.junit.jupiter", "junit-jupiter-engine").versionRef("jupiter")
            library("junit-jupiter-params", "org.junit.jupiter", "junit-jupiter-params").versionRef("jupiter")

            bundle("test-implementation", listOf("junit5", "junit-jupiter-params", "assertions", "strikt"))
            bundle("test-runtime", listOf("junit-jupiter"))
        }

        // Logback
        create("logback") {
            version("logback", "1.5.32")
            library("logback", "ch.qos.logback", "logback-classic").versionRef("logback")
            bundle("implementation", listOf("logback"))
        }
    }
}
