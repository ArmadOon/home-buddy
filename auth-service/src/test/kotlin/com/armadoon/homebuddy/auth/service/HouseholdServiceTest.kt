package com.armadoon.homebuddy.auth.service

import com.armadoon.homebuddy.auth.entity.Household
import com.armadoon.homebuddy.auth.entity.User
import com.armadoon.homebuddy.auth.repository.HouseholdRepository
import com.armadoon.homebuddy.auth.repository.UserRepository
import com.armadoon.homebuddy.dto.models.CreateHouseholdRequest
import com.armadoon.homebuddy.dto.models.JoinHouseholdRequest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest

@MicronautTest
class HouseholdServiceTest(
    private val householdService: HouseholdService,
    private val userRepository: UserRepository,
    private val householdRepository: HouseholdRepository
) : StringSpec({

    beforeTest {
        userRepository.deleteAll()
        householdRepository.deleteAll()
    }

    "should create household successfully" {
        // Create a user first
        val user = userRepository.save(
            User(
                username = "testuser",
                email = "test@example.com",
                passwordHash = "hash",
                displayName = "Test User"
            )
        )

        val request = CreateHouseholdRequest(
            name = "Test Household"
        )

        val response = householdService.createHousehold(request, user.id!!)

        response.success shouldBe true
        response.household shouldNotBe null
        response.household!!.name shouldBe "Test Household"
        response.inviteCode shouldNotBe null
        response.inviteCode!! shouldMatch Regex("[A-Z]{4}-\\d{4}")
    }

    "should update user's household when creating household" {
        val user = userRepository.save(
            User(
                username = "testuser",
                email = "test@example.com",
                passwordHash = "hash",
                displayName = "Test User"
            )
        )

        val request = CreateHouseholdRequest(
            name = "Test Household"
        )

        val response = householdService.createHousehold(request, user.id!!)

        response.success shouldBe true

        val updatedUser = userRepository.findById(user.id!!).get()
        updatedUser.householdId shouldNotBe null
        updatedUser.householdId shouldBe response.household!!.id
    }

    "should generate unique invite codes" {
        val user = userRepository.save(
            User(
                username = "testuser",
                email = "test@example.com",
                passwordHash = "hash",
                displayName = "Test User"
            )
        )

        val request1 = CreateHouseholdRequest(
            name = "Household 1"
        )

        val request2 = CreateHouseholdRequest(
            name = "Household 2"
        )

        val response1 = householdService.createHousehold(request1, user.id!!)

        // Create a second user for the second household
        val user2 = userRepository.save(
            User(
                username = "testuser2",
                email = "test2@example.com",
                passwordHash = "hash",
                displayName = "Test User 2"
            )
        )

        val response2 = householdService.createHousehold(request2, user2.id!!)

        response1.inviteCode shouldNotBe response2.inviteCode
    }

    "should fail to create household if user already has one" {
        // Create household first
        val household = householdRepository.save(
            Household(
                name = "Existing Household",
                inviteCode = "TEST-1234",
                createdBy = 1L
            )
        )

        val user = userRepository.save(
            User(
                username = "testuser",
                email = "test@example.com",
                passwordHash = "hash",
                displayName = "Test User",
                householdId = household.id
            )
        )

        val request = CreateHouseholdRequest(
            name = "New Household"
        )

        val response = householdService.createHousehold(request, user.id!!)

        response.success shouldBe false
        response.error shouldNotBe null
    }

    "should fail to create household for non-existent user" {
        val request = CreateHouseholdRequest(
            name = "Test Household"
        )

        val response = householdService.createHousehold(request, 999999L)

        response.success shouldBe false
        response.error shouldNotBe null
    }

    "should join household successfully" {
        // Create household
        val household = householdRepository.save(
            Household(
                name = "Test Household",
                inviteCode = "TEST-1234",
                createdBy = 1L,
                maxMembers = 5
            )
        )

        // Create user
        val user = userRepository.save(
            User(
                username = "testuser",
                email = "test@example.com",
                passwordHash = "hash",
                displayName = "Test User"
            )
        )

        val request = JoinHouseholdRequest(inviteCode = "TEST-1234")

        val response = householdService.joinHousehold(request, user.id!!)

        response.success shouldBe true
        response.household shouldNotBe null
        response.household!!.name shouldBe "Test Household"
    }

    "should update user's household when joining" {
        val household = householdRepository.save(
            Household(
                name = "Test Household",
                inviteCode = "TEST-1234",
                createdBy = 1L,
                maxMembers = 5
            )
        )

        val user = userRepository.save(
            User(
                username = "testuser",
                email = "test@example.com",
                passwordHash = "hash",
                displayName = "Test User"
            )
        )

        val request = JoinHouseholdRequest(inviteCode = "TEST-1234")

        householdService.joinHousehold(request, user.id!!)

        val updatedUser = userRepository.findById(user.id!!).get()
        updatedUser.householdId shouldBe household.id
    }

    "should fail to join household with invalid invite code" {
        val user = userRepository.save(
            User(
                username = "testuser",
                email = "test@example.com",
                passwordHash = "hash",
                displayName = "Test User"
            )
        )

        val request = JoinHouseholdRequest(inviteCode = "INVALID-CODE")

        val response = householdService.joinHousehold(request, user.id!!)

        response.success shouldBe false
        response.error shouldNotBe null
    }

    "should fail to join if user already has household" {
        val household1 = householdRepository.save(
            Household(
                name = "Household 1",
                inviteCode = "TEST-1234",
                createdBy = 1L
            )
        )

        val household2 = householdRepository.save(
            Household(
                name = "Household 2",
                inviteCode = "TEST-5678",
                createdBy = 2L
            )
        )

        val user = userRepository.save(
            User(
                username = "testuser",
                email = "test@example.com",
                passwordHash = "hash",
                displayName = "Test User",
                householdId = household1.id
            )
        )

        val request = JoinHouseholdRequest(inviteCode = "TEST-5678")

        val response = householdService.joinHousehold(request, user.id!!)

        response.success shouldBe false
        response.error shouldNotBe null
    }

    "should fail to join inactive household" {
        val household = householdRepository.save(
            Household(
                name = "Test Household",
                inviteCode = "TEST-1234",
                createdBy = 1L,
                isActive = false
            )
        )

        val user = userRepository.save(
            User(
                username = "testuser",
                email = "test@example.com",
                passwordHash = "hash",
                displayName = "Test User"
            )
        )

        val request = JoinHouseholdRequest(inviteCode = "TEST-1234")

        val response = householdService.joinHousehold(request, user.id!!)

        response.success shouldBe false
        response.error shouldNotBe null
    }

    "should fail to join full household" {
        val household = householdRepository.save(
            Household(
                name = "Test Household",
                inviteCode = "TEST-1234",
                createdBy = 1L,
                maxMembers = 2
            )
        )

        // Fill household to capacity
        userRepository.save(
            User(
                username = "user1",
                email = "user1@example.com",
                passwordHash = "hash",
                displayName = "User 1",
                householdId = household.id
            )
        )

        userRepository.save(
            User(
                username = "user2",
                email = "user2@example.com",
                passwordHash = "hash",
                displayName = "User 2",
                householdId = household.id
            )
        )

        // Try to join with third user
        val user3 = userRepository.save(
            User(
                username = "user3",
                email = "user3@example.com",
                passwordHash = "hash",
                displayName = "User 3"
            )
        )

        val request = JoinHouseholdRequest(inviteCode = "TEST-1234")

        val response = householdService.joinHousehold(request, user3.id!!)

        response.success shouldBe false
        response.error shouldNotBe null
    }

    "should get household info successfully" {
        val household = householdRepository.save(
            Household(
                name = "Test Household",
                inviteCode = "TEST-1234",
                createdBy = 1L,
                maxMembers = 5
            )
        )

        userRepository.save(
            User(
                username = "user1",
                email = "user1@example.com",
                passwordHash = "hash",
                displayName = "User 1",
                householdId = household.id
            )
        )

        userRepository.save(
            User(
                username = "user2",
                email = "user2@example.com",
                passwordHash = "hash",
                displayName = "User 2",
                householdId = household.id
            )
        )

        val response = householdService.getHouseholdInfo(household.id!!)

        response shouldNotBe null
        response!!.household.name shouldBe "Test Household"
        response.memberCount shouldBe 2
        response.maxMembers shouldBe 5
        response.members.size shouldBe 2
    }

    "should return null for non-existent household info" {
        val response = householdService.getHouseholdInfo(999999L)

        response shouldBe null
    }

    "should validate invite code correctly" {
        householdRepository.save(
            Household(
                name = "Test Household",
                inviteCode = "TEST-1234",
                createdBy = 1L
            )
        )

        val valid = householdService.validateInviteCode("TEST-1234")
        val invalid = householdService.validateInviteCode("INVALID-CODE")

        valid shouldBe true
        invalid shouldBe false
    }

    "should return false for inactive household invite code" {
        householdRepository.save(
            Household(
                name = "Test Household",
                inviteCode = "TEST-1234",
                createdBy = 1L,
                isActive = false
            )
        )

        val valid = householdService.validateInviteCode("TEST-1234")

        valid shouldBe false
    }

    "should find household by invite code" {
        householdRepository.save(
            Household(
                name = "Test Household",
                inviteCode = "TEST-1234",
                createdBy = 1L
            )
        )

        val household = householdService.findHouseholdByInviteCode("TEST-1234")

        household shouldNotBe null
        household!!.name shouldBe "Test Household"
    }

    "should find household by ID" {
        val saved = householdRepository.save(
            Household(
                name = "Test Household",
                inviteCode = "TEST-1234",
                createdBy = 1L
            )
        )

        val household = householdService.findHouseholdById(saved.id!!)

        household shouldNotBe null
        household!!.name shouldBe "Test Household"
    }

    "should find household by creator" {
        val userId = 123L

        householdRepository.save(
            Household(
                name = "Test Household",
                inviteCode = "TEST-1234",
                createdBy = userId
            )
        )

        val household = householdService.findHouseholdByCreator(userId)

        household shouldNotBe null
        household!!.name shouldBe "Test Household"
        household.createdBy shouldBe userId
    }

    "should count household members correctly" {
        val household = householdRepository.save(
            Household(
                name = "Test Household",
                inviteCode = "TEST-1234",
                createdBy = 1L
            )
        )

        userRepository.save(
            User(
                username = "user1",
                email = "user1@example.com",
                passwordHash = "hash",
                displayName = "User 1",
                householdId = household.id
            )
        )

        userRepository.save(
            User(
                username = "user2",
                email = "user2@example.com",
                passwordHash = "hash",
                displayName = "User 2",
                householdId = household.id
            )
        )

        // Add inactive user (should not be counted)
        userRepository.save(
            User(
                username = "user3",
                email = "user3@example.com",
                passwordHash = "hash",
                displayName = "User 3",
                householdId = household.id,
                isActive = false
            )
        )

        val count = householdService.getHouseholdMemberCount(household.id!!)

        count shouldBe 2
    }

    "should remove user from household" {
        val household = householdRepository.save(
            Household(
                name = "Test Household",
                inviteCode = "TEST-1234",
                createdBy = 1L
            )
        )

        val user = userRepository.save(
            User(
                username = "testuser",
                email = "test@example.com",
                passwordHash = "hash",
                displayName = "Test User",
                householdId = household.id
            )
        )

        val result = householdService.removeUserFromHousehold(user.id!!)

        result shouldBe true

        val updatedUser = userRepository.findById(user.id!!).get()
        updatedUser.householdId shouldBe null
    }

    "should return false when removing non-existent user from household" {
        val result = householdService.removeUserFromHousehold(999999L)

        result shouldBe false
    }
})
