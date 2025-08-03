package com.armadoon.homebuddy.auth.security

import com.armadoon.homebuddy.auth.service.AuthService
import io.micronaut.http.HttpRequest
import io.micronaut.security.authentication.AuthenticationRequest
import io.micronaut.security.authentication.AuthenticationResponse
import io.micronaut.security.authentication.provider.HttpRequestAuthenticationProvider
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

@Singleton
class AuthenticationProvider(
    private val authService: AuthService
) : HttpRequestAuthenticationProvider<HttpRequest<*>> {

    companion object {
        private val logger = LoggerFactory.getLogger(AuthenticationProvider::class.java)
    }

    override fun authenticate(
        requestContext: HttpRequest<HttpRequest<*>?>?,
        authRequest: AuthenticationRequest<String?, String?>
    ): AuthenticationResponse {

        val username = authRequest.identity ?: return AuthenticationResponse.failure("Missing username")
        val password = authRequest.secret ?: return AuthenticationResponse.failure("Missing password")

        logger.debug("Authentication attempt for username: $username")

        val user = authService.authenticate(username, password)

        return if (user != null) {
            logger.debug("Authentication successful for user: ${user.username}")
            AuthenticationResponse.success(
                user.id.toString(),
                listOf("ROLE_USER"),
                mapOf(
                    "username" to user.username,
                    "email" to user.email,
                    "displayName" to user.displayName,
                    "householdId" to (user.householdId?.toString() ?: "")
                )
            )
        } else {
            logger.debug("Authentication failed for username: $username")
            AuthenticationResponse.failure("Invalid credentials")
        }
    }
}