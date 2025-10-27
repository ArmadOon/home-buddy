package com.armadoon.homebuddy.calendar.entity

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@Entity
@Table(name = "events")
data class Event(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val householdId: Long,

    @Column(nullable = false)
    val createdBy: Long,

    @Column(nullable = true)
    val assignedTo: Long? = null,

    @Column(nullable = false)
    @NotBlank
    @Size(min = 1, max = 200)
    val title: String,

    @Column(columnDefinition = "TEXT")
    @Size(max = 2000)
    val description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    val eventType: EventType,

    @Column(nullable = false)
    val startDateTime: LocalDateTime,

    @Column(nullable = false)
    val endDateTime: LocalDateTime,

    @Column(nullable = false)
    val allDayEvent: Boolean = false,

    @Column(length = 500)
    @Size(max = 500)
    val location: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val priority: Priority = Priority.MEDIUM,

    @Column(nullable = false)
    val isRecurring: Boolean = false,

    @Column(columnDefinition = "TEXT")
    val recurrenceRule: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val status: EventStatus = EventStatus.PENDING,

    @Column(nullable = true)
    val completedAt: LocalDateTime? = null,

    @Column(nullable = true)
    val completedBy: Long? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    // JPA requires no-arg constructor
    constructor() : this(
        householdId = 0,
        createdBy = 0,
        title = "",
        eventType = EventType.OTHER,
        startDateTime = LocalDateTime.now(),
        endDateTime = LocalDateTime.now()
    )
}
