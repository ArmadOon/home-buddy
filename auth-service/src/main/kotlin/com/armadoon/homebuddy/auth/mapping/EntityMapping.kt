package com.armadoon.homebuddy.auth.mapping

import com.armadoon.homebuddy.auth.entity.Household
import com.armadoon.homebuddy.auth.entity.User
import com.armadoon.homebuddy.dto.models.CreateHouseholdRequest
import com.armadoon.homebuddy.dto.models.HouseholdDto
import com.armadoon.homebuddy.dto.models.RegisterRequest
import com.armadoon.homebuddy.dto.models.UserDto
import java.time.ZoneOffset


// User mappings
fun User.toDto(): UserDto = UserDto(
    id = this.id!!,
    username = this.username,
    email = this.email,
    displayName = this.displayName,
    householdId = this.householdId,
    createdAt = this.createdAt.atOffset(ZoneOffset.UTC),
    updatedAt = this.updatedAt.atOffset(ZoneOffset.UTC)
)

fun RegisterRequest.toEntity(passwordHash: String, householdId: Long? = null): User = User(
    username = this.username,
    email = this.email,
    passwordHash = passwordHash,
    displayName = this.displayName,
    householdId = householdId
)

// Household mappings
fun Household.toDto(memberCount: Int = 0): HouseholdDto = HouseholdDto(
    id = this.id!!,
    name = this.name,
    inviteCode = this.inviteCode,
    memberCount = memberCount,
    createdAt = this.createdAt.atOffset(ZoneOffset.UTC),
    updatedAt = this.updatedAt.atOffset(ZoneOffset.UTC)
)

fun CreateHouseholdRequest.toEntity(inviteCode: String, createdBy: Long): Household = Household(
    name = this.name,
    inviteCode = inviteCode,
    createdBy = createdBy
)

