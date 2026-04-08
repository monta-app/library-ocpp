val javaToolChainVersion: String by project

plugins {
    application
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
}

version = "1.0.0"
group = "com.monta.ocpp"

dependencies {

    implementation(project(":core"))
    implementation(project(":v201"))

    implementation(platform(kotlinlibs.platform))
    implementation(kotlinlibs.bundles.implementation)

    implementation(platform(coroutines.platform))
    implementation(coroutines.bundles.implementation)

    // Core
    implementation(platform("io.ktor:ktor-bom:3.4.2"))
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-host-common-jvm")
    implementation("io.ktor:ktor-server-status-pages-jvm")
    implementation("io.ktor:ktor-server-websockets")
    implementation("io.ktor:ktor-server-cors")
    implementation("io.ktor:ktor-server-config-yaml")

    // Serialization
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-client-content-negotiation")
    implementation("io.ktor:ktor-serialization-jackson-jvm")

    // Jackson
    implementation(platform(jackson.platform))
    implementation(jackson.bundles.implementation)

    // Logging
    implementation("io.ktor:ktor-server-call-logging-jvm")
    implementation("io.ktor:ktor-server-call-id-jvm")
    implementation(logback.bundles.implementation)

    // HttpClient
    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-client-logging")
    implementation("io.ktor:ktor-client-content-negotiation")
    implementation("io.ktor:ktor-client-cio")
}

application {
    mainClass.set("com.monta.ocpp.application.MontaApplication")
}

sourceSets {
    main {
        java.srcDirs("src/main/kotlin")
        resources.srcDirs("src/main/resources")
    }
    test {
        java.srcDirs("src/test/kotlin")
        resources.srcDirs("src/test/resources")
    }
}

kotlin {
    jvmToolchain(javaToolChainVersion.toInt())
}

tasks {
    processResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
    processTestResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}
