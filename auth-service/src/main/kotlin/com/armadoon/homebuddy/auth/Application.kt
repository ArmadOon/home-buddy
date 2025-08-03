package com.armadoon.homebuddy.auth

import io.micronaut.runtime.Micronaut.run
import io.swagger.v3.oas.annotations.*
import io.swagger.v3.oas.annotations.info.*
import java.io.File

@OpenAPIDefinition(
    info = Info(
        title = "auth-service",
        version = "0.0"
    )
)
object Api {
}

fun main(args: Array<String>) {
    loadEnvFile()
    run(*args)
}

private fun loadEnvFile() {
    val envFile = listOf(File(".env"), File("../.env")).firstOrNull { it.exists() }

    envFile?.readLines()?.forEach { line ->
        val trimmed = line.trim()
        if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
            val equalIndex = trimmed.indexOf('=')
            if (equalIndex > 0) {
                val key = trimmed.substring(0, equalIndex).trim()
                val value = trimmed.substring(equalIndex + 1).trim()
                    .removeSurrounding("\"")
                    .removeSurrounding("'")

                if (System.getProperty(key) == null && System.getenv(key) == null) {
                    System.setProperty(key, value)
                }
            }
        }
    }
}