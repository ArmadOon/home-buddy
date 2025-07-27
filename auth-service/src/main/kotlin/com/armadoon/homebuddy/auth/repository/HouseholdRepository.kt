package com.armadoon.homebuddy.auth.repository

import com.armadoon.homebuddy.auth.entity.Household
import io.micronaut.data.annotation.Query
import io.micronaut.data.annotation.Repository
import io.micronaut.data.repository.CrudRepository
import java.util.*

@Repository
interface HouseholdRepository : CrudRepository<Household, Long> {

    fun findByInviteCode(inviteCode: String): Optional<Household>

    fun findByCreatedBy(createdBy: Long): Optional<Household>

    @Query("SELECT COUNT(u) FROM User u WHERE u.householdId = :householdId AND u.isActive = true")
    fun countActiveMembers(householdId: Long): Long
}