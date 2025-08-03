package com.armadoon.homebuddy.auth.service

import com.armadoon.homebuddy.auth.entity.Household
import com.armadoon.homebuddy.auth.mapping.toDto
import com.armadoon.homebuddy.auth.mapping.toEntity
import com.armadoon.homebuddy.auth.repository.HouseholdRepository
import com.armadoon.homebuddy.auth.repository.UserRepository
import com.armadoon.homebuddy.dto.*
import com.armadoon.homebuddy.dto.models.CreateHouseholdRequest
import com.armadoon.homebuddy.dto.models.CreateHouseholdResponse
import com.armadoon.homebuddy.dto.models.HouseholdInfoResponse
import com.armadoon.homebuddy.dto.models.JoinHouseholdRequest
import com.armadoon.homebuddy.dto.models.JoinHouseholdResponse
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory

@Singleton
open class HouseholdService(  // <-- přidej 'open'
    private val householdRepository: HouseholdRepository,
    private val userRepository: UserRepository
) {

    companion object {
        private val logger = LoggerFactory.getLogger(HouseholdService::class.java)
    }

    @Transactional
    open fun createHousehold(request: CreateHouseholdRequest, createdBy: Long): CreateHouseholdResponse {  // <-- přidej 'open'
        logger.info("Creating new household '${request.name}' for user $createdBy")

        try {
            // Check if user exists and doesn't have household
            val existingUser = userRepository.findById(createdBy).orElse(null)
                ?: return CreateHouseholdResponse(
                    success = false,
                    error = "User not found"
                )

            if (existingUser.householdId != null) {
                logger.warn("User $createdBy already has household ${existingUser.householdId}")
                return CreateHouseholdResponse(
                    success = false,
                    error = "User already belongs to a household"
                )
            }

            // Generate unique invite code
            var inviteCode: String
            do {
                inviteCode = Household.generateInviteCode()
            } while (householdRepository.findByInviteCode(inviteCode).isPresent)

            // Create household using generated DTO mapping
            val household = householdRepository.save(
                request.toEntity(inviteCode = inviteCode, createdBy = createdBy)
            )

            // Update user's household
            userRepository.updateHouseholdId(createdBy, household.id!!)

            logger.info("Successfully created household ${household.id} with invite code $inviteCode")

            return CreateHouseholdResponse(
                success = true,
                household = household.toDto(1), // Creator is first member
                inviteCode = inviteCode
            )

        } catch (e: Exception) {
            logger.error("Failed to create household for user $createdBy", e)
            return CreateHouseholdResponse(
                success = false,
                error = "Failed to create household due to server error"
            )
        }
    }

    @Transactional
    open fun joinHousehold(request: JoinHouseholdRequest, userId: Long): JoinHouseholdResponse {  // <-- přidej 'open'
        logger.info("User $userId attempting to join household with code ${request.inviteCode}")

        try {
            val user = userRepository.findById(userId).orElse(null)
                ?: return JoinHouseholdResponse(
                    success = false,
                    error = "User not found"
                )

            if (user.householdId != null) {
                logger.warn("User $userId already belongs to household ${user.householdId}")
                return JoinHouseholdResponse(
                    success = false,
                    error = "User already belongs to a household"
                )
            }

            val household = householdRepository.findByInviteCode(request.inviteCode).orElse(null)
                ?: return JoinHouseholdResponse(
                    success = false,
                    error = "Invalid invite code"
                )

            if (!household.isActive) {
                return JoinHouseholdResponse(
                    success = false,
                    error = "Household is not active"
                )
            }

            // Check member limit
            val currentMemberCount = householdRepository.countActiveMembers(household.id!!)
            if (currentMemberCount >= household.maxMembers) {
                logger.warn("Household ${household.id} is full ($currentMemberCount/${household.maxMembers})")
                return JoinHouseholdResponse(
                    success = false,
                    error = "Household is full"
                )
            }

            // Add user to household
            userRepository.updateHouseholdId(userId, household.id!!)

            logger.info("User $userId successfully joined household ${household.id}")

            val newMemberCount = householdRepository.countActiveMembers(household.id!!).toInt()

            return JoinHouseholdResponse(
                success = true,
                household = household.toDto(newMemberCount)
            )

        } catch (e: Exception) {
            logger.error("Failed to join household for user $userId", e)
            return JoinHouseholdResponse(
                success = false,
                error = "Failed to join household due to server error"
            )
        }
    }

    fun getHouseholdInfo(householdId: Long): HouseholdInfoResponse? {
        return try {
            val household = householdRepository.findById(householdId).orElse(null) ?: return null
            val members = userRepository.findByHouseholdIdAndIsActive(householdId)
            val memberCount = members.size

            HouseholdInfoResponse(
                household = household.toDto(memberCount),
                members = members.map { it.toDto() },
                memberCount = memberCount,
                maxMembers = household.maxMembers
            )
        } catch (e: Exception) {
            logger.error("Error getting household info for ID: $householdId", e)
            null
        }
    }

    fun validateInviteCode(inviteCode: String): Boolean {
        return try {
            householdRepository.findByInviteCode(inviteCode)
                .map { it.isActive }
                .orElse(false)
        } catch (e: Exception) {
            logger.error("Error validating invite code: $inviteCode", e)
            false
        }
    }

    fun findHouseholdByInviteCode(inviteCode: String): Household? {
        return try {
            householdRepository.findByInviteCode(inviteCode).orElse(null)
        } catch (e: Exception) {
            logger.error("Error finding household by invite code: $inviteCode", e)
            null
        }
    }

    fun findHouseholdById(householdId: Long): Household? {
        return try {
            householdRepository.findById(householdId).orElse(null)
        } catch (e: Exception) {
            logger.error("Error finding household by ID: $householdId", e)
            null
        }
    }

    fun findHouseholdByCreator(createdBy: Long): Household? {
        return try {
            householdRepository.findByCreatedBy(createdBy).orElse(null)
        } catch (e: Exception) {
            logger.error("Error finding household by creator: $createdBy", e)
            null
        }
    }

    fun getHouseholdMemberCount(householdId: Long): Long {
        return try {
            householdRepository.countActiveMembers(householdId)
        } catch (e: Exception) {
            logger.error("Error counting household members for ID: $householdId", e)
            0L
        }
    }

    @Transactional
    open fun removeUserFromHousehold(userId: Long): Boolean {  // <-- přidej 'open'
        return try {
            val updated = userRepository.updateHouseholdId(userId, null)
            logger.info("Removed user $userId from household (affected rows: $updated)")
            updated > 0
        } catch (e: Exception) {
            logger.error("Error removing user $userId from household", e)
            false
        }
    }

    @Transactional
    open fun deactivateHousehold(householdId: Long): Boolean {  // <-- přidej 'open'
        return try {
            val household = householdRepository.findById(householdId).orElse(null) ?: return false

            // Note: V reálné aplikaci bys měl implementovat update metodu v repository
            // Pro teď jen vracíme false, protože nemáme update metodu
            logger.warn("Deactivate household not implemented yet for ID: $householdId")
            false
        } catch (e: Exception) {
            logger.error("Error deactivating household $householdId", e)
            false
        }
    }
}