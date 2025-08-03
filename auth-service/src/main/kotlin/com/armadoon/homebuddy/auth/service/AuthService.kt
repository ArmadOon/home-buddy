package com.armadoon.homebuddy.auth.service

import com.armadoon.homebuddy.auth.entity.Household
import com.armadoon.homebuddy.auth.entity.User
import com.armadoon.homebuddy.auth.mapping.toDto
import com.armadoon.homebuddy.auth.mapping.toEntity
import com.armadoon.homebuddy.auth.repository.HouseholdRepository
import com.armadoon.homebuddy.auth.repository.UserRepository
import com.armadoon.homebuddy.dto.models.RegisterRequest
import com.armadoon.homebuddy.dto.models.RegisterResponse
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder

@Singleton
class AuthService(
    private val userRepository: UserRepository,
    private val householdRepository: HouseholdRepository,
    private val passwordEncoder: PasswordEncoder
) {

    companion object {
        private val logger = LoggerFactory.getLogger(AuthService::class.java)
    }

    @Transactional
    fun register(request: RegisterRequest): RegisterResponse {
        logger.info("Registration attempt for username: ${request.username}, email: ${request.email}")

        // Check if user already exists
        if (userRepository.existsByUsernameOrEmail(request.username, request.email)) {
            logger.warn("Registration failed - user already exists: ${request.username}")
            return RegisterResponse(
                success = false,
                error = "User with this username or email already exists"
            )
        }

        // Validate and find household if invite code provided
        var household: Household? = null
        if (!request.inviteCode.isNullOrBlank()) {
            val householdOpt = householdRepository.findByInviteCode(request.inviteCode)
            if (householdOpt.isEmpty) {
                logger.warn("Registration failed - invalid invite code: ${request.inviteCode}")
                return RegisterResponse(success = false, error = "Invalid invite code")
            }

            household = householdOpt.get()
            if (!household.isActive) {
                logger.warn("Registration failed - household not active: ${household.id}")
                return RegisterResponse(success = false, error = "Household is not active")
            }

            // Check member limit
            val currentMemberCount = householdRepository.countActiveMembers(household.id!!)
            if (currentMemberCount >= household.maxMembers) {
                logger.warn("Registration failed - household full: ${household.id} ($currentMemberCount/${household.maxMembers})")
                return RegisterResponse(success = false, error = "Household is full")
            }
        }

        try {
            // Create user using generated DTO mapping
            val user = userRepository.save(
                request.toEntity(
                    passwordHash = passwordEncoder.encode(request.password),
                    householdId = household?.id
                )
            )

            logger.info("User successfully registered: ${user.username} (id=${user.id})")

            val memberCount = household?.let {
                householdRepository.countActiveMembers(it.id!!).toInt()
            } ?: 0

            return RegisterResponse(
                success = true,
                user = user.toDto(),
                household = household?.toDto(memberCount),
                needsHousehold = household == null
            )

        } catch (e: Exception) {
            logger.error("Registration failed for username: ${request.username}", e)
            return RegisterResponse(
                success = false,
                error = "Registration failed due to server error"
            )
        }
    }

    fun authenticate(username: String, password: String): User? {
        logger.debug("Authentication attempt for: $username")

        try {
            // Try to find user by username first, then by email
            val user = userRepository.findByUsername(username).orElse(null)
                ?: userRepository.findByEmail(username).orElse(null)

            return if (user != null && user.isActive && passwordEncoder.matches(password, user.passwordHash)) {
                logger.debug("Authentication successful for: $username (id=${user.id})")
                user
            } else {
                logger.debug("Authentication failed for: $username - ${if (user == null) "user not found" else if (!user.isActive) "user not active" else "invalid password"}")
                null
            }

        } catch (e: Exception) {
            logger.error("Authentication error for username: $username", e)
            return null
        }
    }

    fun findUserById(userId: Long): User? {
        return try {
            userRepository.findById(userId).orElse(null)
        } catch (e: Exception) {
            logger.error("Error finding user by ID: $userId", e)
            null
        }
    }

    fun findUserByUsername(username: String): User? {
        return try {
            userRepository.findByUsername(username).orElse(null)
        } catch (e: Exception) {
            logger.error("Error finding user by username: $username", e)
            null
        }
    }

    fun findUserByEmail(email: String): User? {
        return try {
            userRepository.findByEmail(email).orElse(null)
        } catch (e: Exception) {
            logger.error("Error finding user by email: $email", e)
            null
        }
    }

    @Transactional
    fun updateUserHousehold(userId: Long, householdId: Long): Boolean {
        return try {
            val updated = userRepository.updateHouseholdId(userId, householdId)
            logger.info("Updated user $userId household to $householdId (affected rows: $updated)")
            updated > 0
        } catch (e: Exception) {
            logger.error("Error updating user $userId household to $householdId", e)
            false
        }
    }

    fun getUsersByHousehold(householdId: Long): List<User> {
        return try {
            userRepository.findByHouseholdIdAndIsActive(householdId, true)
        } catch (e: Exception) {
            logger.error("Error finding users for household: $householdId", e)
            emptyList()
        }
    }
}