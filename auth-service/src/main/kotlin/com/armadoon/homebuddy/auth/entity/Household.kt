package com.armadoon.homebuddy.auth.entity

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import kotlin.random.Random

@Entity
@Table(name = "households")
data class Household(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    @NotBlank
    @Size(min = 2, max = 100)
    val name: String,

    @Column(unique = true, nullable = false)
    @NotBlank
    val inviteCode: String,

    @Column(nullable = false)
    val createdBy: Long,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val isActive: Boolean = true,

    @Column(nullable = false)
    val maxMembers: Int = 10
) {
    // JPA no-arg constructor
    constructor() : this(
        name = "",
        inviteCode = "",
        createdBy = 0L
    )

    companion object {
        fun generateInviteCode(): String {
            val letters = (1..4).map { ('A'..'Z').random() }.joinToString("")
            val numbers = Random.nextInt(1000, 10000)
            return "$letters-$numbers"
        }
    }
}
