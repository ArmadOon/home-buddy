package com.armadoon.homebuddy.auth.service

import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Singleton
class JwtService(
    @Value("\${micronaut.security.token.jwt.signatures.secret.generator.secret}")
    private val secret: String
) {

    fun generateToken(userId: Long, username: String, email: String, displayName: String, householdId: Long?): String {
        val header = """{"alg":"HS256","typ":"JWT"}"""

        val now = Instant.now()
        val expiration = now.plus(24, ChronoUnit.HOURS)

        val payload = buildString {
            append("{")
            append("\"sub\":\"$userId\",")
            append("\"username\":\"$username\",")
            append("\"email\":\"$email\",")
            append("\"displayName\":\"$displayName\",")
            append("\"householdId\":\"${householdId ?: ""}\",")
            append("\"iat\":${now.epochSecond},")
            append("\"exp\":${expiration.epochSecond},")
            append("\"roles\":[\"ROLE_USER\"]")
            append("}")
        }

        val encodedHeader = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(header.toByteArray())

        val encodedPayload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(payload.toByteArray())

        val message = "$encodedHeader.$encodedPayload"
        val signature = hmacSha256(message, secret)

        return "$message.$signature"
    }

    private fun hmacSha256(data: String, secret: String): String {
        val secretKeySpec = SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(secretKeySpec)
        val signedBytes = mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signedBytes)
    }
}