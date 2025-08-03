package com.armadoon.homebuddy.auth.repository

import com.armadoon.homebuddy.auth.entity.User
import io.micronaut.data.annotation.Query
import io.micronaut.data.annotation.Repository
import io.micronaut.data.repository.CrudRepository
import java.util.*

@Repository
interface UserRepository : CrudRepository<User, Long> {

    fun findByUsername(username: String): Optional<User>

    fun findByEmail(email: String): Optional<User>

    fun findByHouseholdIdAndIsActive(householdId: Long, isActive: Boolean = true): List<User>

    fun existsByUsernameOrEmail(username: String, email: String): Boolean

    @Query("UPDATE User u SET u.householdId = :householdId WHERE u.id = :userId")
    fun updateHouseholdId(userId: Long, householdId: Long?): Int
}
