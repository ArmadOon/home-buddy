package com.armadoon.homebuddy.auth.service

import com.armadoon.homebuddy.auth.entity.Household
import com.armadoon.homebuddy.auth.entity.User
import com.armadoon.homebuddy.auth.repository.HouseholdRepository
import com.armadoon.homebuddy.auth.repository.UserRepository
import com.armadoon.homebuddy.dto.models.RegisterRequest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.*

@MicronautTest
class AuthServiceTest(

    private val authService: AuthService,
    private val userRepository: UserRepository,
    private val householdRepository: HouseholdRepository,
    private val passwordEncoder: PasswordEncoder
) : StringSpec({

    beforeTest {
        userRepository.deleteAll()
        householdRepository.deleteAll()
    }

    "should register user successfully" {
        val request = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "password123",
            displayName = "Test User"
        )

        val response = authService.register(request)

        response.success shouldBe true
        response.user shouldNotBe null
        response.user!!.username shouldBe "testuser"
        response.user!!.email shouldBe "test@example.com"
        response.needsHousehold shouldBe true
        response.household shouldBe null
    }

    "should hash password when registering" {
        val request = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "password123",
            displayName = "Test User"
        )

        authService.register(request)

        val user = userRepository.findByUsername("testuser").get()
        user.passwordHash shouldNotBe "password123"
        passwordEncoder.matches("password123", user.passwordHash) shouldBe true
    }

    "should fail to register duplicate username" {
        val request1 = RegisterRequest(
            username = "testuser",
            email = "test1@example.com",
            password = "password123",
            displayName = "Test User 1"
        )

        val request2 = RegisterRequest(
            username = "testuser",
            email = "test2@example.com",
            password = "password456",
            displayName = "Test User 2"
        )

        authService.register(request1)
        val response = authService.register(request2)

        response.success shouldBe false
        response.error shouldNotBe null
    }

    "should fail to register duplicate email" {
        val request1 = RegisterRequest(
            username = "testuser1",
            email = "test@example.com",
            password = "password123",
            displayName = "Test User 1"
        )

        val request2 = RegisterRequest(
            username = "testuser2",
            email = "test@example.com",
            password = "password456",
            displayName = "Test User 2"
        )

        authService.register(request1)
        val response = authService.register(request2)

        response.success shouldBe false
        response.error shouldNotBe null
    }

    "should register user with invite code and join household" {
        // Create household first
        val household = householdRepository.save(
            Household(
                name = "Test Household",
                inviteCode = "TEST-1234",
                createdBy = 1L,
                maxMembers = 5
            )
        )

        val request = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "password123",
            displayName = "Test User",
            inviteCode = "TEST-1234"
        )

        val response = authService.register(request)

        response.success shouldBe true
        response.user shouldNotBe null
        response.household shouldNotBe null
        response.household!!.name shouldBe "Test Household"
        response.needsHousehold shouldBe false

        // Verify user was added to household
        val user = userRepository.findByUsername("testuser").get()
        user.householdId shouldBe household.id
    }

    "should fail to register with invalid invite code" {
        val request = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "password123",
            displayName = "Test User",
            inviteCode = "INVALID-CODE"
        )

        val response = authService.register(request)

        response.success shouldBe false
        response.error shouldNotBe null
    }

    "should fail to register when household is full" {
        // Create household with max 2 members
        val household = householdRepository.save(
            Household(
                name = "Test Household",
                inviteCode = "TEST-1234",
                createdBy = 1L,
                maxMembers = 2
            )
        )

        // Add 2 users to the household
        userRepository.save(
            User(
                username = "user1",
                email = "user1@example.com",
                passwordHash = "hash1",
                displayName = "User 1",
                householdId = household.id
            )
        )

        userRepository.save(
            User(
                username = "user2",
                email = "user2@example.com",
                passwordHash = "hash2",
                displayName = "User 2",
                householdId = household.id
            )
        )

        // Try to register third user
        val request = RegisterRequest(
            username = "user3",
            email = "user3@example.com",
            password = "password123",
            displayName = "User 3",
            inviteCode = "TEST-1234"
        )

        val response = authService.register(request)

        response.success shouldBe false
        response.error shouldNotBe null
    }

    "should fail to register with inactive household" {
        // Create inactive household
        householdRepository.save(
            Household(
                name = "Test Household",
                inviteCode = "TEST-1234",
                createdBy = 1L,
                isActive = false
            )
        )

        val request = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "password123",
            displayName = "Test User",
            inviteCode = "TEST-1234"
        )

        val response = authService.register(request)

        response.success shouldBe false
        response.error shouldNotBe null
    }

    "should authenticate user with valid username and password" {
        val request = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "password123",
            displayName = "Test User"
        )

        authService.register(request)

        val user = authService.authenticate("testuser", "password123")

        user shouldNotBe null
        user!!.username shouldBe "testuser"
    }

    "should authenticate user with email and password" {
        val request = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "password123",
            displayName = "Test User"
        )

        authService.register(request)

        val user = authService.authenticate("test@example.com", "password123")

        user shouldNotBe null
        user!!.username shouldBe "testuser"
    }

    "should fail authentication with invalid password" {
        val request = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "password123",
            displayName = "Test User"
        )

        authService.register(request)

        val user = authService.authenticate("testuser", "wrongpassword")

        user shouldBe null
    }

    "should fail authentication with non-existent user" {
        val user = authService.authenticate("nonexistent", "password123")

        user shouldBe null
    }

    "should fail authentication for inactive user" {
        // Create inactive user
        userRepository.save(
            User(
                username = "inactiveuser",
                email = "inactive@example.com",
                passwordHash = passwordEncoder.encode("password123"),
                displayName = "Inactive User",
                isActive = false
            )
        )

        val user = authService.authenticate("inactiveuser", "password123")

        user shouldBe null
    }

    "should find user by ID" {
        val request = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "password123",
            displayName = "Test User"
        )

        val registerResponse = authService.register(request)
        val userId = registerResponse.user!!.id

        val user = authService.findUserById(userId)

        user shouldNotBe null
        user!!.username shouldBe "testuser"
    }

    "should return null when finding non-existent user by ID" {
        val user = authService.findUserById(999999L)

        user shouldBe null
    }

    "should find user by username" {
        val request = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "password123",
            displayName = "Test User"
        )

        authService.register(request)

        val user = authService.findUserByUsername("testuser")

        user shouldNotBe null
        user!!.email shouldBe "test@example.com"
    }

    "should find user by email" {
        val request = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "password123",
            displayName = "Test User"
        )

        authService.register(request)

        val user = authService.findUserByEmail("test@example.com")

        user shouldNotBe null
        user!!.username shouldBe "testuser"
    }

    "should update user household" {
        // Create user
        val user = userRepository.save(
            User(
                username = "testuser",
                email = "test@example.com",
                passwordHash = "hash",
                displayName = "Test User"
            )
        )

        // Create household
        val household = householdRepository.save(
            Household(
                name = "Test Household",
                inviteCode = "TEST-1234",
                createdBy = 1L
            )
        )

        // Update user's household
        val result = authService.updateUserHousehold(user.id!!, household.id!!)

        result shouldBe true

        val updatedUser = userRepository.findById(user.id!!).get()
        updatedUser.householdId shouldBe household.id
    }

    "should get users by household" {
        // Create household
        val household = householdRepository.save(
            Household(
                name = "Test Household",
                inviteCode = "TEST-1234",
                createdBy = 1L
            )
        )

        // Create users in household
        userRepository.save(
            User(
                username = "user1",
                email = "user1@example.com",
                passwordHash = "hash1",
                displayName = "User 1",
                householdId = household.id
            )
        )

        userRepository.save(
            User(
                username = "user2",
                email = "user2@example.com",
                passwordHash = "hash2",
                displayName = "User 2",
                householdId = household.id
            )
        )

        // Create user not in household
        userRepository.save(
            User(
                username = "user3",
                email = "user3@example.com",
                passwordHash = "hash3",
                displayName = "User 3"
            )
        )

        val users = authService.getUsersByHousehold(household.id!!)

        users.size shouldBe 2
    }
})
