package com.armadoon.homebuddy.auth.controller

import com.armadoon.homebuddy.auth.repository.HouseholdRepository
import com.armadoon.homebuddy.auth.repository.UserRepository
import com.armadoon.homebuddy.dto.models.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest

@MicronautTest
class AuthControllerTest(
    @Client("/") private val client: HttpClient,
    private val userRepository: UserRepository,
    private val householdRepository: HouseholdRepository
) : StringSpec({

    beforeTest {
        // Clean up database before each test
        userRepository.deleteAll()
        householdRepository.deleteAll()
    }

    "should register a new user successfully" {
        val request = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "password123",
            displayName = "Test User"
        )

        val response = client.toBlocking().exchange(
            HttpRequest.POST("/auth/register", request),
            RegisterResponse::class.java
        )

        response.status shouldBe HttpStatus.CREATED
        response.body()!!.success shouldBe true
        response.body()!!.user shouldNotBe null
        response.body()!!.user!!.username shouldBe "testuser"
        response.body()!!.user!!.email shouldBe "test@example.com"
        response.body()!!.needsHousehold shouldBe true
    }

    "should fail registration with duplicate username" {
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

        // First registration should succeed
        client.toBlocking().exchange(
            HttpRequest.POST("/auth/register", request1),
            RegisterResponse::class.java
        )

        // Second registration should fail
        try {
            client.toBlocking().exchange(
                HttpRequest.POST("/auth/register", request2),
                RegisterResponse::class.java
            )
            throw AssertionError("Expected HttpClientResponseException but request succeeded")
        } catch (e: HttpClientResponseException) {
            e.status shouldBe HttpStatus.BAD_REQUEST
            val response = e.response.getBody(RegisterResponse::class.java).get()
            response.success shouldBe false
            response.error shouldNotBe null
        }
    }

    "should fail registration with duplicate email" {
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

        // First registration should succeed
        client.toBlocking().exchange(
            HttpRequest.POST("/auth/register", request1),
            RegisterResponse::class.java
        )

        // Second registration should fail
        try {
            client.toBlocking().exchange(
                HttpRequest.POST("/auth/register", request2),
                RegisterResponse::class.java
            )
            throw AssertionError("Expected HttpClientResponseException but request succeeded")
        } catch (e: HttpClientResponseException) {
            e.status shouldBe HttpStatus.BAD_REQUEST
            val response = e.response.getBody(RegisterResponse::class.java).get()
            response.success shouldBe false
        }
    }

    "should login successfully with valid credentials" {
        // First register a user
        val registerRequest = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "password123",
            displayName = "Test User"
        )

        client.toBlocking().exchange(
            HttpRequest.POST("/auth/register", registerRequest),
            RegisterResponse::class.java
        )

        // Then try to login
        val loginRequest = LoginRequest(
            username = "testuser",
            password = "password123"
        )

        val response = client.toBlocking().exchange(
            HttpRequest.POST("/auth/login", loginRequest),
            LoginResponse::class.java
        )

        response.status shouldBe HttpStatus.OK
        response.body()!!.token.shouldNotBeBlank()
        response.body()!!.user shouldNotBe null
        response.body()!!.user.username shouldBe "testuser"
    }

    "should login successfully with email instead of username" {
        // First register a user
        val registerRequest = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "password123",
            displayName = "Test User"
        )

        client.toBlocking().exchange(
            HttpRequest.POST("/auth/register", registerRequest),
            RegisterResponse::class.java
        )

        // Login with email
        val loginRequest = LoginRequest(
            username = "test@example.com",
            password = "password123"
        )

        val response = client.toBlocking().exchange(
            HttpRequest.POST("/auth/login", loginRequest),
            LoginResponse::class.java
        )

        response.status shouldBe HttpStatus.OK
        response.body()!!.token.shouldNotBeBlank()
    }

    "should fail login with invalid password" {
        // First register a user
        val registerRequest = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "password123",
            displayName = "Test User"
        )

        client.toBlocking().exchange(
            HttpRequest.POST("/auth/register", registerRequest),
            RegisterResponse::class.java
        )

        // Try to login with wrong password
        val loginRequest = LoginRequest(
            username = "testuser",
            password = "wrongpassword"
        )

        try {
            client.toBlocking().exchange(
                HttpRequest.POST("/auth/login", loginRequest),
                ErrorResponse::class.java
            )
            throw AssertionError("Expected HttpClientResponseException but request succeeded")
        } catch (e: HttpClientResponseException) {
            e.status shouldBe HttpStatus.UNAUTHORIZED
        }
    }

    "should fail login with non-existent user" {
        val loginRequest = LoginRequest(
            username = "nonexistent",
            password = "password123"
        )

        try {
            client.toBlocking().exchange(
                HttpRequest.POST("/auth/login", loginRequest),
                ErrorResponse::class.java
            )
            throw AssertionError("Expected HttpClientResponseException but request succeeded")
        } catch (e: HttpClientResponseException) {
            e.status shouldBe HttpStatus.UNAUTHORIZED
        }
    }

    "should create household successfully" {
        // First register and login a user
        val registerRequest = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "password123",
            displayName = "Test User"
        )

        client.toBlocking().exchange(
            HttpRequest.POST("/auth/register", registerRequest),
            RegisterResponse::class.java
        )

        val loginRequest = LoginRequest(
            username = "testuser",
            password = "password123"
        )

        val loginResponse = client.toBlocking().exchange(
            HttpRequest.POST("/auth/login", loginRequest),
            LoginResponse::class.java
        )

        val token = loginResponse.body()!!.token

        // Create household
        val createHouseholdRequest = CreateHouseholdRequest(
            name = "Test Household"
        )

        val response = client.toBlocking().exchange(
            HttpRequest.POST("/auth/household/create", createHouseholdRequest)
                .bearerAuth(token),
            CreateHouseholdResponse::class.java
        )

        response.status shouldBe HttpStatus.CREATED
        response.body()!!.success shouldBe true
        response.body()!!.household shouldNotBe null
        response.body()!!.household!!.name shouldBe "Test Household"
        response.body()!!.inviteCode.shouldNotBeBlank()
    }

    "should fail to create household if user already has one" {
        // Register and login
        val registerRequest = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "password123",
            displayName = "Test User"
        )

        client.toBlocking().exchange(
            HttpRequest.POST("/auth/register", registerRequest),
            RegisterResponse::class.java
        )

        val loginRequest = LoginRequest(
            username = "testuser",
            password = "password123"
        )

        val loginResponse = client.toBlocking().exchange(
            HttpRequest.POST("/auth/login", loginRequest),
            LoginResponse::class.java
        )

        val token = loginResponse.body()!!.token

        // Create first household
        val createHouseholdRequest = CreateHouseholdRequest(
            name = "Test Household",
        )

        client.toBlocking().exchange(
            HttpRequest.POST("/auth/household/create", createHouseholdRequest)
                .bearerAuth(token),
            CreateHouseholdResponse::class.java
        )

        // Try to create second household
        val secondRequest = CreateHouseholdRequest(
            name = "Second Household"
        )

        try {
            client.toBlocking().exchange(
                HttpRequest.POST("/auth/household/create", secondRequest)
                    .bearerAuth(token),
                CreateHouseholdResponse::class.java
            )
            throw AssertionError("Expected HttpClientResponseException but request succeeded")
        } catch (e: HttpClientResponseException) {
            e.status shouldBe HttpStatus.BAD_REQUEST
            val response = e.response.getBody(CreateHouseholdResponse::class.java).get()
            response.success shouldBe false
        }
    }

    "should join household with valid invite code" {
        // Create first user and household
        val user1Register = RegisterRequest(
            username = "user1",
            email = "user1@example.com",
            password = "password123",
            displayName = "User 1"
        )

        client.toBlocking().exchange(
            HttpRequest.POST("/auth/register", user1Register),
            RegisterResponse::class.java
        )

        val user1Login = LoginRequest(username = "user1", password = "password123")
        val user1LoginResponse = client.toBlocking().exchange(
            HttpRequest.POST("/auth/login", user1Login),
            LoginResponse::class.java
        )
        val user1Token = user1LoginResponse.body()!!.token

        val createHouseholdRequest = CreateHouseholdRequest(
            name = "Test Household",
        )

        val createResponse = client.toBlocking().exchange(
            HttpRequest.POST("/auth/household/create", createHouseholdRequest)
                .bearerAuth(user1Token),
            CreateHouseholdResponse::class.java
        )

        val inviteCode = createResponse.body()!!.inviteCode!!

        // Create second user
        val user2Register = RegisterRequest(
            username = "user2",
            email = "user2@example.com",
            password = "password123",
            displayName = "User 2"
        )

        client.toBlocking().exchange(
            HttpRequest.POST("/auth/register", user2Register),
            RegisterResponse::class.java
        )

        val user2Login = LoginRequest(username = "user2", password = "password123")
        val user2LoginResponse = client.toBlocking().exchange(
            HttpRequest.POST("/auth/login", user2Login),
            LoginResponse::class.java
        )
        val user2Token = user2LoginResponse.body()!!.token

        // Join household
        val joinRequest = JoinHouseholdRequest(inviteCode = inviteCode)
        val response = client.toBlocking().exchange(
            HttpRequest.POST("/auth/household/join", joinRequest)
                .bearerAuth(user2Token),
            JoinHouseholdResponse::class.java
        )

        response.status shouldBe HttpStatus.OK
        response.body()!!.success shouldBe true
        response.body()!!.household shouldNotBe null
    }

    "should fail to join household with invalid invite code" {
        // Register and login a user
        val registerRequest = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "password123",
            displayName = "Test User"
        )

        client.toBlocking().exchange(
            HttpRequest.POST("/auth/register", registerRequest),
            RegisterResponse::class.java
        )

        val loginRequest = LoginRequest(username = "testuser", password = "password123")
        val loginResponse = client.toBlocking().exchange(
            HttpRequest.POST("/auth/login", loginRequest),
            LoginResponse::class.java
        )
        val token = loginResponse.body()!!.token

        // Try to join with invalid code
        val joinRequest = JoinHouseholdRequest(inviteCode = "INVALID-CODE")
        try {
            client.toBlocking().exchange(
                HttpRequest.POST("/auth/household/join", joinRequest)
                    .bearerAuth(token),
                JoinHouseholdResponse::class.java
            )
            throw AssertionError("Expected HttpClientResponseException but request succeeded")
        } catch (e: HttpClientResponseException) {
            e.status shouldBe HttpStatus.BAD_REQUEST
            val response = e.response.getBody(JoinHouseholdResponse::class.java).get()
            response.success shouldBe false
        }
    }

    "should validate invite code correctly" {
        // Create user and household
        val registerRequest = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "password123",
            displayName = "Test User"
        )

        client.toBlocking().exchange(
            HttpRequest.POST("/auth/register", registerRequest),
            RegisterResponse::class.java
        )

        val loginRequest = LoginRequest(username = "testuser", password = "password123")
        val loginResponse = client.toBlocking().exchange(
            HttpRequest.POST("/auth/login", loginRequest),
            LoginResponse::class.java
        )
        val token = loginResponse.body()!!.token

        val createHouseholdRequest = CreateHouseholdRequest(
            name = "Test Household",
        )

        val createResponse = client.toBlocking().exchange(
            HttpRequest.POST("/auth/household/create", createHouseholdRequest)
                .bearerAuth(token),
            CreateHouseholdResponse::class.java
        )

        val inviteCode = createResponse.body()!!.inviteCode!!

        // Validate the invite code
        val response = client.toBlocking().exchange(
            HttpRequest.GET<Any>("/auth/household/validate-invite/$inviteCode"),
            ValidateInviteResponse::class.java
        )

        response.status shouldBe HttpStatus.OK
        response.body()!!.valid shouldBe true
    }

    "should return false for invalid invite code validation" {
        val response = client.toBlocking().exchange(
            HttpRequest.GET<Any>("/auth/household/validate-invite/INVALID-CODE"),
            ValidateInviteResponse::class.java
        )

        response.status shouldBe HttpStatus.OK
        response.body()!!.valid shouldBe false
    }

    "should get household info successfully" {
        // Create user and household
        val registerRequest = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "password123",
            displayName = "Test User"
        )

        client.toBlocking().exchange(
            HttpRequest.POST("/auth/register", registerRequest),
            RegisterResponse::class.java
        )

        val loginRequest = LoginRequest(username = "testuser", password = "password123")
        val loginResponse = client.toBlocking().exchange(
            HttpRequest.POST("/auth/login", loginRequest),
            LoginResponse::class.java
        )
        val token = loginResponse.body()!!.token

        val createHouseholdRequest = CreateHouseholdRequest(
            name = "Test Household",
        )

        client.toBlocking().exchange(
            HttpRequest.POST("/auth/household/create", createHouseholdRequest)
                .bearerAuth(token),
            CreateHouseholdResponse::class.java
        )

        // Get household info
        val response = client.toBlocking().exchange(
            HttpRequest.GET<Any>("/auth/household/info")
                .bearerAuth(token),
            HouseholdInfoResponse::class.java
        )

        response.status shouldBe HttpStatus.OK
        response.body()!!.household shouldNotBe null
        response.body()!!.household.name shouldBe "Test Household"
        response.body()!!.memberCount shouldBe 1
        response.body()!!.members.size shouldBe 1
    }

    "should require authentication for protected endpoints" {
        val createHouseholdRequest = CreateHouseholdRequest(
            name = "Test Household",
        )

        try {
            client.toBlocking().exchange(
                HttpRequest.POST("/auth/household/create", createHouseholdRequest),
                CreateHouseholdResponse::class.java
            )
        } catch (e: HttpClientResponseException) {
            e.status shouldBe HttpStatus.UNAUTHORIZED
        }
    }

    "should register user and auto-join household with invite code" {
        // Create first user and household
        val user1Register = RegisterRequest(
            username = "user1",
            email = "user1@example.com",
            password = "password123",
            displayName = "User 1"
        )

        client.toBlocking().exchange(
            HttpRequest.POST("/auth/register", user1Register),
            RegisterResponse::class.java
        )

        val user1Login = LoginRequest(username = "user1", password = "password123")
        val user1LoginResponse = client.toBlocking().exchange(
            HttpRequest.POST("/auth/login", user1Login),
            LoginResponse::class.java
        )
        val user1Token = user1LoginResponse.body()!!.token

        val createHouseholdRequest = CreateHouseholdRequest(
            name = "Test Household",
        )

        val createResponse = client.toBlocking().exchange(
            HttpRequest.POST("/auth/household/create", createHouseholdRequest)
                .bearerAuth(user1Token),
            CreateHouseholdResponse::class.java
        )

        val inviteCode = createResponse.body()!!.inviteCode!!

        // Register second user with invite code
        val user2Register = RegisterRequest(
            username = "user2",
            email = "user2@example.com",
            password = "password123",
            displayName = "User 2",
            inviteCode = inviteCode
        )

        val response = client.toBlocking().exchange(
            HttpRequest.POST("/auth/register", user2Register),
            RegisterResponse::class.java
        )

        response.status shouldBe HttpStatus.CREATED
        response.body()!!.success shouldBe true
        response.body()!!.household shouldNotBe null
        response.body()!!.needsHousehold shouldBe false
    }
})
