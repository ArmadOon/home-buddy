package com.armadoon.homebuddy.auth.entity

import jakarta.persistence.*
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(unique = true, nullable = false)
    @NotBlank
    @Size(min = 3, max = 50)
    val username: String,

    @Column(unique = true, nullable = false)
    @NotBlank
    @Email
    @Size(max = 255)
    val email: String,

    @Column(nullable = false)
    @NotBlank
    val passwordHash: String,

    @Column(nullable = false)
    @NotBlank
    @Size(min = 1, max = 100)
    val displayName: String,

    @Column(name = "household_id")
    val householdId: Long? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val isActive: Boolean = true
) {
    // JPA requires no-arg constructor
    constructor() : this(
        username = "",
        email = "",
        passwordHash = "",
        displayName = ""
    )
}