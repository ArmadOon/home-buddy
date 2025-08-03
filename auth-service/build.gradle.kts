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
group = "com.armadoon.homebuddy.auth"

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
    packageName.set("com.armadoon.homebuddy.dto")
    configOptions.set(mapOf(
        "serializationLibrary" to "jackson",
        "enumPropertyNaming" to "UPPERCASE",
        "modelPropertyNaming" to "camelCase",
        "library" to "jvm-okhttp4",
        "useJakartaEe" to "true",                    // <-- pro Jakarta annotations
        "additionalModelTypeAnnotations" to "@io.micronaut.core.annotation.Introspected;@io.micronaut.serde.annotation.Serdeable"  // <-- Micronaut annotations
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
    // =========================
    // KSP processors (annotation processing)
    // =========================
    ksp("io.micronaut:micronaut-http-validation")
    ksp("io.micronaut.openapi:micronaut-openapi")
    ksp("io.micronaut.serde:micronaut-serde-processor")
    ksp("io.micronaut.data:micronaut-data-processor")
    ksp("io.micronaut.security:micronaut-security-annotations")

    // =========================
    // Main implementation dependencies
    // =========================
    implementation("io.micronaut:micronaut-management")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
    implementation("io.micronaut.serde:micronaut-serde-jackson")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("io.micronaut.beanvalidation:micronaut-hibernate-validator")
    implementation("io.micronaut.data:micronaut-data-hibernate-jpa")
    implementation("io.micronaut.sql:micronaut-jdbc-hikari")
    implementation("io.micronaut.security:micronaut-security-jwt")
    implementation("org.springframework.security:spring-security-crypto:6.4.4")
    implementation("org.springframework:spring-jcl:6.1.14")

    // =========================
    // Compile-only dependencies
    // =========================
    compileOnly("io.micronaut:micronaut-http-client")
    compileOnly("io.micronaut.openapi:micronaut-openapi-annotations")
    compileOnly("io.micronaut:micronaut-core-processor")

    // =========================
    // Runtime-only dependencies
    // =========================
    runtimeOnly("org.yaml:snakeyaml")
    runtimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin")
    runtimeOnly("ch.qos.logback:logback-classic")
    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql")

    // =========================
    // Test dependencies
    // =========================
    testImplementation("io.micronaut:micronaut-http-client")
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")

    // =========================
    // Optional/Advanced dependencies (uncomment as needed)
    // =========================
    // implementation("net.logstash.logback:logstash-logback-encoder:7.4")
    // testImplementation("io.mockk:mockk:1.13.8")
    // aotPlugins(platform("io.micronaut.platform:micronaut-platform:4.9.1"))
    // aotPlugins("io.micronaut.security:micronaut-security-aot")
}

application {
    mainClass = "com.armadoon.homebuddy.auth.ApplicationKt"
}

graalvmNative.toolchainDetection = false

micronaut {
    runtime("netty")
    testRuntime("kotest5")
    processing {
        incremental(true)
        annotations("com.armadoon.homebuddy.auth.*")
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
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
    annotation("jakarta.transaction.Transactional")  // <-- přidej toto
}