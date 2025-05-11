plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    kotlin("plugin.serialization") version libs.versions.kotlin.version.get()
    id("org.openapi.generator") version "7.2.0"
}

group = "com.mpluhar"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

openApiGenerate {
    generatorName.set("kotlin")
    inputSpec.set("${project.projectDir}/src/main/resources/openapi/documentation.yaml")
    outputDir.set("${project.layout.buildDirectory.get()}/generated")
    apiPackage.set("com.mpluhar.cz.homebuddy.api")
    modelPackage.set("com.mpluhar.cz.homebuddy.models")
    configOptions.set(mapOf(
        "dateLibrary" to "java8",
        "enumPropertyNaming" to "UPPERCASE",
        "serializationLibrary" to "kotlinx_serialization",
        "useCoroutines" to "true"
    ))
    globalProperties.set(mapOf(
        "models" to "",
        "modelDocs" to "false",
        "apis" to "false",
        "apiDocs" to "false"
    ))
}

kotlin {
    sourceSets {
        main {
            kotlin.srcDir("${project.layout.buildDirectory.get()}/generated/src/main/kotlin")
        }
    }
}

tasks.compileKotlin {
    dependsOn("openApiGenerate")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.swagger)
    implementation(libs.ktor.server.openapi)
    implementation(libs.ktor.server.host.common)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.config.yaml)

    // Serialization
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)

    // Database
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)
    implementation(libs.h2.database)
    implementation(libs.hikaricp)

    // Hashing
    implementation(libs.jbcrypt)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
}