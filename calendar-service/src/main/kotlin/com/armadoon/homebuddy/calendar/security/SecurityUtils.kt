package com.armadoon.homebuddy.calendar.security

import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.security.authentication.Authentication

object SecurityUtils {

    /**
     * Extract user ID from JWT authentication
     */
    fun getUserId(authentication: Authentication): Long {
        return try {
            authentication.name.toLong()
        } catch (e: NumberFormatException) {
            throw HttpStatusException(HttpStatus.UNAUTHORIZED, "Invalid user ID in token")
        }
    }

    /**
     * Extract household ID from JWT authentication
     * Returns null if household ID is not set or is empty
     */
    fun getHouseholdId(authentication: Authentication): Long? {
        val householdIdStr = authentication.attributes["householdId"] as? String
        return if (householdIdStr.isNullOrBlank()) {
            null
        } else {
            try {
                householdIdStr.toLong()
            } catch (e: NumberFormatException) {
                null
            }
        }
    }

    /**
     * Extract household ID and throw exception if not present
     */
    fun requireHouseholdId(authentication: Authentication): Long {
        return getHouseholdId(authentication)
            ?: throw HttpStatusException(
                HttpStatus.BAD_REQUEST,
                "User must belong to a household to access calendar events"
            )
    }

    /**
     * Get username from authentication
     */
    fun getUsername(authentication: Authentication): String {
        return authentication.attributes["username"] as? String
            ?: throw HttpStatusException(HttpStatus.UNAUTHORIZED, "Invalid token")
    }

    /**
     * Get email from authentication
     */
    fun getEmail(authentication: Authentication): String? {
        return authentication.attributes["email"] as? String
    }

    /**
     * Get display name from authentication
     */
    fun getDisplayName(authentication: Authentication): String? {
        return authentication.attributes["displayName"] as? String
    }
}
