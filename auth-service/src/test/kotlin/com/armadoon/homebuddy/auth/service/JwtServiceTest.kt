package com.armadoon.homebuddy.auth.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldMatch
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.matchers.string.shouldNotContain
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.util.*

@MicronautTest
class JwtServiceTest(
    private val jwtService: JwtService
) : StringSpec({

    "should generate valid JWT token" {
        val token = jwtService.generateToken(
            userId = 1L,
            username = "testuser",
            email = "test@example.com",
            displayName = "Test User",
            householdId = 123L
        )

        token shouldNotBe null
        token.shouldNotBeBlank()

        // JWT should have 3 parts separated by dots
        val parts = token.split(".")
        parts.size shouldBe 3
    }

    "should include correct header in JWT" {
        val token = jwtService.generateToken(
            userId = 1L,
            username = "testuser",
            email = "test@example.com",
            displayName = "Test User",
            householdId = 123L
        )

        val parts = token.split(".")
        val headerJson = String(Base64.getUrlDecoder().decode(parts[0]))

        headerJson shouldContain "\"alg\":\"HS256\""
        headerJson shouldContain "\"typ\":\"JWT\""
    }

    "should include user data in JWT payload" {
        val token = jwtService.generateToken(
            userId = 42L,
            username = "testuser",
            email = "test@example.com",
            displayName = "Test User",
            householdId = 123L
        )

        val parts = token.split(".")
        val payloadJson = String(Base64.getUrlDecoder().decode(parts[1]))

        payloadJson shouldContain "\"sub\":\"42\""
        payloadJson shouldContain "\"username\":\"testuser\""
        payloadJson shouldContain "\"email\":\"test@example.com\""
        payloadJson shouldContain "\"displayName\":\"Test User\""
        payloadJson shouldContain "\"householdId\":\"123\""
    }

    "should include ROLE_USER in JWT payload" {
        val token = jwtService.generateToken(
            userId = 1L,
            username = "testuser",
            email = "test@example.com",
            displayName = "Test User",
            householdId = 123L
        )

        val parts = token.split(".")
        val payloadJson = String(Base64.getUrlDecoder().decode(parts[1]))

        payloadJson shouldContain "\"roles\":[\"ROLE_USER\"]"
    }

    "should include issued at and expiration timestamps" {
        val token = jwtService.generateToken(
            userId = 1L,
            username = "testuser",
            email = "test@example.com",
            displayName = "Test User",
            householdId = 123L
        )

        val parts = token.split(".")
        val payloadJson = String(Base64.getUrlDecoder().decode(parts[1]))

        payloadJson shouldContain "\"iat\":"
        payloadJson shouldContain "\"exp\":"
    }

    "should handle null householdId" {
        val token = jwtService.generateToken(
            userId = 1L,
            username = "testuser",
            email = "test@example.com",
            displayName = "Test User",
            householdId = null
        )

        val parts = token.split(".")
        val payloadJson = String(Base64.getUrlDecoder().decode(parts[1]))

        payloadJson shouldContain "\"householdId\":\"\""
    }

    "should generate different tokens for different users" {
        val token1 = jwtService.generateToken(
            userId = 1L,
            username = "user1",
            email = "user1@example.com",
            displayName = "User 1",
            householdId = null
        )

        val token2 = jwtService.generateToken(
            userId = 2L,
            username = "user2",
            email = "user2@example.com",
            displayName = "User 2",
            householdId = null
        )

        token1 shouldNotBe token2
    }

    "should generate different tokens for same user at different times" {
        // Wait a tiny bit to ensure different timestamps
        val token1 = jwtService.generateToken(
            userId = 1L,
            username = "testuser",
            email = "test@example.com",
            displayName = "Test User",
            householdId = null
        )

        Thread.sleep(1001) // Sleep for more than 1 second to ensure different iat

        val token2 = jwtService.generateToken(
            userId = 1L,
            username = "testuser",
            email = "test@example.com",
            displayName = "Test User",
            householdId = null
        )

        token1 shouldNotBe token2
    }

    "should have signature part in token" {
        val token = jwtService.generateToken(
            userId = 1L,
            username = "testuser",
            email = "test@example.com",
            displayName = "Test User",
            householdId = null
        )

        val parts = token.split(".")

        // Signature should be base64 encoded and non-empty
        parts[2].shouldNotBeBlank()
        parts[2].length shouldBe 43 // HS256 signature is 32 bytes = 43 base64 chars without padding
    }

    "should generate URL-safe base64 encoded token" {
        val token = jwtService.generateToken(
            userId = 1L,
            username = "testuser",
            email = "test@example.com",
            displayName = "Test User",
            householdId = null
        )

        // URL-safe base64 should not contain +, /, or =
        token shouldNotContain "+"
        token shouldNotContain "/"
        token shouldNotContain "="
    }

    "should set expiration to 24 hours from issuance" {
        val token = jwtService.generateToken(
            userId = 1L,
            username = "testuser",
            email = "test@example.com",
            displayName = "Test User",
            householdId = null
        )

        val parts = token.split(".")
        val payloadJson = String(Base64.getUrlDecoder().decode(parts[1]))

        // Parse JSON to extract iat and exp
        val mapper = ObjectMapper()
        val payload = mapper.readTree(payloadJson)

        val iat = payload.get("iat").asLong()
        val exp = payload.get("exp").asLong()

        // Expiration should be 24 hours (86400 seconds) after issuance
        val expectedExp = iat + 86400
        exp shouldBe expectedExp
    }

    "should handle special characters in user data" {
        val token = jwtService.generateToken(
            userId = 1L,
            username = "test.user+123",
            email = "test+user@example.com",
            displayName = "Test User (Admin)",
            householdId = null
        )

        val parts = token.split(".")
        val payloadJson = String(Base64.getUrlDecoder().decode(parts[1]))

        payloadJson shouldContain "test.user+123"
        payloadJson shouldContain "test+user@example.com"
        payloadJson shouldContain "Test User (Admin)"
    }

    "should create consistent token structure" {
        val token = jwtService.generateToken(
            userId = 1L,
            username = "testuser",
            email = "test@example.com",
            displayName = "Test User",
            householdId = 123L
        )

        // Verify it matches JWT pattern (header.payload.signature)
        token shouldMatch Regex("^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$")
    }
})
