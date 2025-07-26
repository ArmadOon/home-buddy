plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.9.25"
    id("com.google.devtools.ksp") version "1.9.25-1.0.20"
    id("io.micronaut.application") version "4.5.4"
    id("com.gradleup.shadow") version "8.3.7"
    id("io.micronaut.aot") version "4.5.4"
    id("org.openapi.generator") version "7.2.0"
}

version = "1.0.0"
group = "cz.homebuddy.auth"

repositories {
    mavenCentral()
}

// Java/Kotlin toolchain configuration
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

// Kotlin compilation configuration
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "21"
        freeCompilerArgs = listOf("-Xjsr305=strict")
    }
    dependsOn("openApiGenerate")
}

// OpenAPI Generator configuration
openApiGenerate {
    generatorName.set("kotlin")
    inputSpec.set("${project.rootDir}/shared/openapi-specs/auth-service.yaml")
    outputDir.set("${layout.buildDirectory.get()}/generated")
    packageName.set("cz.homebuddy.dto")
    configOptions.set(mapOf(
        "serializationLibrary" to "jackson",
        "enumPropertyNaming" to "UPPERCASE",
        "modelPropertyNaming" to "camelCase",
        "library" to "jvm-okhttp4"
    ))

    globalProperties.set(mapOf(
        "models" to "",
        "modelDocs" to "false"
    ))
}

// Add generated sources
sourceSets {
    main {
        kotlin {
            srcDir("${layout.buildDirectory.get()}/generated/src/main/kotlin")
        }
    }
}

dependencies {
    // Minimal Micronaut KSP processors - only what we need for basic app
    ksp("io.micronaut:micronaut-http-validation")
    ksp("io.micronaut.openapi:micronaut-openapi")
    ksp("io.micronaut.serde:micronaut-serde-processor")

    // Minimal Micronaut Core - only basic functionality
    implementation("io.micronaut:micronaut-management")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
    implementation("io.micronaut.serde:micronaut-serde-jackson")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // YAML support for application.yml
    runtimeOnly("org.yaml:snakeyaml")

    // Jackson for JSON serialization
    runtimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Logging
    runtimeOnly("ch.qos.logback:logback-classic")

    // OpenAPI compile-only dependencies
    compileOnly("io.micronaut:micronaut-http-client")
    compileOnly("io.micronaut.openapi:micronaut-openapi-annotations")

    // Testing - minimal setup
    testImplementation("io.micronaut:micronaut-http-client")
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")

    // ========================================
    // COMMENTED OUT - Will be added when implementing features
    // ========================================

    // Database & JPA (uncomment when adding entities)
    // ksp("io.micronaut.data:micronaut-data-processor")
    // implementation("io.micronaut.beanvalidation:micronaut-hibernate-validator")
    // implementation("io.micronaut.data:micronaut-data-hibernate-jpa")
    // implementation("io.micronaut.sql:micronaut-jdbc-hikari")
    // runtimeOnly("com.h2database:h2")
    // runtimeOnly("org.postgresql:postgresql")

    // Security & JWT (uncomment when adding auth)
    // ksp("io.micronaut.security:micronaut-security-annotations")
    // implementation("io.micronaut.security:micronaut-security-jwt")
    // implementation("org.springframework.security:spring-security-crypto:6.2.1")

    // Advanced logging (uncomment when needed)
    // implementation("net.logstash.logback:logstash-logback-encoder:7.4")

    // Testing tools (uncomment when writing complex tests)
    // testImplementation("io.mockk:mockk:1.13.8")

    // AOT plugins (uncomment for production optimization)
    // aotPlugins(platform("io.micronaut.platform:micronaut-platform:4.9.1"))
    // aotPlugins("io.micronaut.security:micronaut-security-aot")
}

application {
    mainClass = "cz.homebuddy.auth.ApplicationKt"
}

graalvmNative.toolchainDetection = false

micronaut {
    runtime("netty")
    testRuntime("kotest5")
    processing {
        incremental(true)
        annotations("cz.homebuddy.auth.*")
    }
    // Simplified AOT configuration for basic build
    aot {
        optimizeServiceLoading = false
        convertYamlToJava = false
        precomputeOperations = false
        cacheEnvironment = false
        optimizeClassLoading = false
        deduceEnvironment = false
        optimizeNetty = false
        replaceLogbackXml = false
    }
}

tasks.named<io.micronaut.gradle.docker.NativeImageDockerfile>("dockerfileNative") {
    jdkVersion = "21"
}

// Test configuration
tasks.test {
    useJUnitPlatform()

    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = false
    }
}

// Basic AllOpen configuration - only what we need now
allOpen {
    annotation("io.micronaut.http.annotation.Controller")
    annotation("jakarta.inject.Singleton")

    // Commented out - will be needed when adding JPA
    // annotation("jakarta.persistence.Entity")
    // annotation("jakarta.persistence.MappedSuperclass")
    // annotation("jakarta.persistence.Embeddable")
}