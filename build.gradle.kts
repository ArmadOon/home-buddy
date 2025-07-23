plugins {
    kotlin("jvm") version "1.9.22" apply false
    kotlin("plugin.allopen") version "1.9.22" apply false
    kotlin("kapt") version "1.9.22" apply false
    id("io.micronaut.application") version "4.2.1" apply false
    id("org.openapi.generator") version "7.2.0" apply false
}

allprojects {
    group = "cz.homebuddy"
    version = "1.0.0"

    repositories {
        mavenCentral()
    }
}

// Subproject configuration will be added when services are created