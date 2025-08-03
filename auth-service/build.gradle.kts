plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.9.25"
    id("com.google.devtools.ksp") version "1.9.25-1.0.20"
    id("io.micronaut.application") version "4.5.4"
    id("com.gradleup.shadow") version "8.3.7"
    id("io.micronaut.aot") version "4.5.4"
    id("org.openapi.generator") version "7.2.0"
    id("org.flywaydb.flyway") version "10.0.1"  // <-- Flyway plugin
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
        "useJakartaEe" to "true",
        "additionalModelTypeAnnotations" to "@io.micronaut.core.annotation.Introspected;@io.micronaut.serde.annotation.Serdeable"
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

// Version variables
val springSecurityVersion = "6.4.4"
val springJclVersion = "6.1.14"
val kotestVersion = "5.8.0"

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
    implementation("org.springframework.security:spring-security-crypto:$springSecurityVersion")
    implementation("org.springframework:spring-jcl:$springJclVersion")
    implementation("io.micronaut.flyway:micronaut-flyway")  // <-- Flyway pro Micronaut

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
    runtimeOnly("org.flywaydb:flyway-core")  // <-- Flyway core
    runtimeOnly("org.flywaydb:flyway-database-postgresql")  // <-- PostgreSQL support

    // =========================
    // Test dependencies
    // =========================
    testImplementation("io.micronaut:micronaut-http-client")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
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

// Flyway configuration pro gradle tasks
flyway {
    url = "jdbc:postgresql://localhost:5432/homebuddy"
    user = "homebuddy"
    password = "homebuddy"
    locations = arrayOf("classpath:db/migration")
}

tasks.named<io.micronaut.gradle.docker.NativeImageDockerfile>("dockerfileNative") {
    jdkVersion = "21"
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = false
    }
}

allOpen {
    annotation("io.micronaut.http.annotation.Controller")
    annotation("jakarta.inject.Singleton")
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
    annotation("jakarta.transaction.Transactional")
}