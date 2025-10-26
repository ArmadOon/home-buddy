package com.armadoon.homebuddy.auth.repository

import com.armadoon.homebuddy.auth.entity.User
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.*

@Repository
interface UserRepository : JpaRepository<User, Long> {

    fun findByUsername(username: String): Optional<User>

    fun findByEmail(email: String): Optional<User>

    fun findByHouseholdIdAndIsActive(householdId: Long, isActive: Boolean = true): List<User>

    fun existsByUsernameOrEmail(username: String, email: String): Boolean

    // We'll handle updateHouseholdId in the service layer using entity updates
}
