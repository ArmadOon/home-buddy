package com.armadoon.homebuddy.auth.controller

import com.armadoon.homebuddy.auth.mapping.toDto
import com.armadoon.homebuddy.auth.repository.UserRepository
import com.armadoon.homebuddy.auth.service.AuthService
import com.armadoon.homebuddy.auth.service.HouseholdService
import com.armadoon.homebuddy.auth.service.JwtService
import com.armadoon.homebuddy.dto.models.*
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.rules.SecurityRule
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime

@Controller("/auth")
class AuthController(
    private val authService: AuthService,
    private val householdService: HouseholdService,
    private val userRepository: UserRepository,
    private val jwtService: JwtService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(AuthController::class.java)
    }

    @Post("/register")
    @Secured(SecurityRule.IS_ANONYMOUS)
    fun register(@Valid @Body registerRequest: RegisterRequest): HttpResponse<RegisterResponse> {
        logger.info("Registration attempt for username: ${registerRequest.username}")

        val response = authService.register(registerRequest)

        return if (response.success) {
            logger.info("Successful registration for user: ${registerRequest.username}")
            HttpResponse.created(response)
        } else {
            logger.warn("Registration failed: ${response.error}")
            HttpResponse.badRequest(response)
        }
    }

    @Post("/login")
    @Secured(SecurityRule.IS_ANONYMOUS)
    fun login(@Valid @Body loginRequest: LoginRequest): HttpResponse<*> {
        logger.info("Login attempt for username: ${loginRequest.username}")

        val user = authService.authenticate(loginRequest.username, loginRequest.password)
            ?: return HttpResponse.unauthorized<ErrorResponse>().body(
                ErrorResponse("Invalid credentials", OffsetDateTime.now())
            )

        val household = user.householdId?.let {
            householdService.getHouseholdInfo(it)?.household
        }

        val token = jwtService.generateToken(
            userId = user.id!!,
            username = user.username,
            email = user.email,
            displayName = user.displayName,
            householdId = user.householdId
        )

        logger.info("Successful login for user: ${user.username} (id=${user.id})")

        return HttpResponse.ok(LoginResponse(
            token = token,
            user = user.toDto(),
            household = household
        ))
    }

    @Post("/household/create")
    @Secured("ROLE_USER")
    fun createHousehold(
        @Valid @Body request: CreateHouseholdRequest,
        authentication: Authentication
    ): HttpResponse<CreateHouseholdResponse> {
        val userId = authentication.name.toLong()
        logger.info("Creating household for user $userId: ${request.name}")

        val response = householdService.createHousehold(request, userId)

        return if (response.success) {
            HttpResponse.created(response)
        } else {
            HttpResponse.badRequest(response)
        }
    }

    @Post("/household/join")
    @Secured("ROLE_USER")
    fun joinHousehold(
        @Valid @Body request: JoinHouseholdRequest,
        authentication: Authentication
    ): HttpResponse<JoinHouseholdResponse> {
        val userId = authentication.name.toLong()
        logger.info("User $userId joining household with code: ${request.inviteCode}")

        val response = householdService.joinHousehold(request, userId)

        return if (response.success) {
            HttpResponse.ok(response)
        } else {
            HttpResponse.badRequest(response)
        }
    }

    @Get("/household/validate-invite/{inviteCode}")
    @Secured(SecurityRule.IS_ANONYMOUS)
    fun validateInviteCode(@PathVariable inviteCode: String): HttpResponse<ValidateInviteResponse> {
        val isValid = householdService.validateInviteCode(inviteCode)
        return HttpResponse.ok(ValidateInviteResponse(valid = isValid))
    }

    @Get("/household/info")
    @Secured("ROLE_USER")
    fun getHouseholdInfo(authentication: Authentication): HttpResponse<*> {
        val userId = authentication.name.toLong()

        val user = userRepository.findById(userId).orElse(null)
            ?: return HttpResponse.notFound<ErrorResponse>().body(
                ErrorResponse("User not found", OffsetDateTime.now())
            )

        val householdId = user.householdId
            ?: return HttpResponse.badRequest<ErrorResponse>().body(
                ErrorResponse("User is not part of any household", OffsetDateTime.now())
            )

        val householdInfo = householdService.getHouseholdInfo(householdId)
            ?: return HttpResponse.notFound<ErrorResponse>().body(
                ErrorResponse("Household not found", OffsetDateTime.now())
            )

        return HttpResponse.ok(householdInfo)
    }
}