plugins {
    kotlin("jvm") version "1.9.25" apply false
    kotlin("plugin.allopen") version "1.9.25" apply false
    kotlin("plugin.jpa") version "1.9.25" apply false
    kotlin("kapt") version "1.9.25" apply false
    id("com.google.devtools.ksp") version "1.9.25-1.0.20" apply false
    id("io.micronaut.application") version "4.5.4" apply false
    id("com.gradleup.shadow") version "8.3.7" apply false
    id("io.micronaut.aot") version "4.5.4" apply false
    id("org.openapi.generator") version "7.2.0" apply false
}

allprojects {
    group = "com.armadoon.homebuddy"
    version = "1.0.0"

    repositories {
        mavenCentral()
    }
}