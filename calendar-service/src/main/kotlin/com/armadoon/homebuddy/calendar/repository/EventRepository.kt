package com.armadoon.homebuddy.calendar.repository

import com.armadoon.homebuddy.calendar.entity.Event
import com.armadoon.homebuddy.calendar.entity.EventStatus
import com.armadoon.homebuddy.calendar.entity.EventType
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import java.time.LocalDateTime
import java.util.*

@Repository
interface EventRepository : JpaRepository<Event, Long> {

    // Basic queries
    fun findByHouseholdId(householdId: Long, pageable: Pageable): Page<Event>

    fun findByIdAndHouseholdId(id: Long, householdId: Long): Optional<Event>

    // Filter by date range
    fun findByHouseholdIdAndStartDateTimeBetween(
        householdId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        pageable: Pageable
    ): Page<Event>

    // Filter by event type
    fun findByHouseholdIdAndEventType(
        householdId: Long,
        eventType: EventType,
        pageable: Pageable
    ): Page<Event>

    // Filter by assigned user
    fun findByHouseholdIdAndAssignedTo(
        householdId: Long,
        assignedTo: Long,
        pageable: Pageable
    ): Page<Event>

    // Filter by status
    fun findByHouseholdIdAndStatus(
        householdId: Long,
        status: EventStatus,
        pageable: Pageable
    ): Page<Event>

    // Filter by creator
    fun findByHouseholdIdAndCreatedBy(
        householdId: Long,
        createdBy: Long,
        pageable: Pageable
    ): Page<Event>

    // Combined filters - for events assigned to or created by user
    fun findByHouseholdIdAndAssignedToOrCreatedBy(
        householdId: Long,
        assignedTo: Long,
        createdBy: Long,
        pageable: Pageable
    ): Page<Event>

    // Filter by date range and status
    fun findByHouseholdIdAndStartDateTimeBetweenAndStatus(
        householdId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        status: EventStatus,
        pageable: Pageable
    ): Page<Event>

    // Count events by household
    fun countByHouseholdId(householdId: Long): Long

    // Count events by household and status
    fun countByHouseholdIdAndStatus(householdId: Long, status: EventStatus): Long

    // Count events by household and event type
    fun countByHouseholdIdAndEventType(householdId: Long, eventType: EventType): Long

    // Find upcoming events (for reminders and notifications)
    fun findByHouseholdIdAndStartDateTimeAfterAndStatus(
        householdId: Long,
        after: LocalDateTime,
        status: EventStatus,
        pageable: Pageable
    ): Page<Event>
}
