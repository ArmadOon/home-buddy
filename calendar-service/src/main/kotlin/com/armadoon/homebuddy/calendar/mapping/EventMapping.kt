package com.armadoon.homebuddy.calendar.mapping

import com.armadoon.homebuddy.calendar.entity.Event
import com.armadoon.homebuddy.calendar.entity.EventStatus
import com.armadoon.homebuddy.calendar.entity.EventType
import com.armadoon.homebuddy.calendar.entity.Priority
import com.armadoon.homebuddy.dto.models.*
import java.time.LocalDateTime
import java.time.ZoneOffset

// Event to DTO mappings
fun Event.toDto(): EventResponse = EventResponse(
    id = this.id!!,
    householdId = this.householdId,
    createdBy = this.createdBy,
    assignedTo = this.assignedTo,
    title = this.title,
    description = this.description,
    eventType = mapEventTypeToDto(this.eventType),
    startDateTime = this.startDateTime.atOffset(ZoneOffset.UTC),
    endDateTime = this.endDateTime.atOffset(ZoneOffset.UTC),
    allDayEvent = this.allDayEvent,
    location = this.location,
    priority = mapPriorityToDto(this.priority),
    isRecurring = this.isRecurring,
    recurrenceRule = this.recurrenceRule,
    status = mapEventStatusToDto(this.status),
    completedAt = this.completedAt?.atOffset(ZoneOffset.UTC),
    completedBy = this.completedBy,
    createdAt = this.createdAt.atOffset(ZoneOffset.UTC),
    updatedAt = this.updatedAt.atOffset(ZoneOffset.UTC)
)

// Create request to Entity
fun CreateEventRequest.toEntity(
    householdId: Long,
    createdBy: Long
): Event = Event(
    householdId = householdId,
    createdBy = createdBy,
    assignedTo = this.assignedTo,
    title = this.title,
    description = this.description,
    eventType = mapEventTypeFromDto(this.eventType),
    startDateTime = this.startDateTime.toLocalDateTime(),
    endDateTime = this.endDateTime.toLocalDateTime(),
    allDayEvent = this.allDayEvent ?: false,
    location = this.location,
    priority = this.priority?.let { mapPriorityFromDto(it) } ?: Priority.MEDIUM,
    isRecurring = this.isRecurring ?: false,
    recurrenceRule = this.recurrenceRule,
    status = EventStatus.PENDING,
    createdAt = LocalDateTime.now(),
    updatedAt = LocalDateTime.now()
)

// Update existing entity from update request
fun Event.applyUpdate(request: UpdateEventRequest): Event {
    return this.copy(
        title = request.title ?: this.title,
        description = request.description ?: this.description,
        eventType = request.eventType?.let { mapEventTypeFromDto(it) } ?: this.eventType,
        startDateTime = request.startDateTime?.toLocalDateTime() ?: this.startDateTime,
        endDateTime = request.endDateTime?.toLocalDateTime() ?: this.endDateTime,
        allDayEvent = request.allDayEvent ?: this.allDayEvent,
        location = request.location ?: this.location,
        priority = request.priority?.let { mapPriorityFromDto(it) } ?: this.priority,
        assignedTo = request.assignedTo ?: this.assignedTo,
        updatedAt = LocalDateTime.now()
    )
}

// Enum mappings - EventType
fun mapEventTypeToDto(eventType: EventType): com.armadoon.homebuddy.dto.models.EventType {
    return when (eventType) {
        EventType.CHORE -> com.armadoon.homebuddy.dto.models.EventType.CHORE
        EventType.APPOINTMENT -> com.armadoon.homebuddy.dto.models.EventType.APPOINTMENT
        EventType.REMINDER -> com.armadoon.homebuddy.dto.models.EventType.REMINDER
        EventType.SOCIAL -> com.armadoon.homebuddy.dto.models.EventType.SOCIAL
        EventType.OTHER -> com.armadoon.homebuddy.dto.models.EventType.OTHER
    }
}

fun mapEventTypeFromDto(eventType: com.armadoon.homebuddy.dto.models.EventType): EventType {
    return when (eventType) {
        com.armadoon.homebuddy.dto.models.EventType.CHORE -> EventType.CHORE
        com.armadoon.homebuddy.dto.models.EventType.APPOINTMENT -> EventType.APPOINTMENT
        com.armadoon.homebuddy.dto.models.EventType.REMINDER -> EventType.REMINDER
        com.armadoon.homebuddy.dto.models.EventType.SOCIAL -> EventType.SOCIAL
        com.armadoon.homebuddy.dto.models.EventType.OTHER -> EventType.OTHER
    }
}

// Enum mappings - Priority
fun mapPriorityToDto(priority: Priority): com.armadoon.homebuddy.dto.models.Priority {
    return when (priority) {
        Priority.LOW -> com.armadoon.homebuddy.dto.models.Priority.LOW
        Priority.MEDIUM -> com.armadoon.homebuddy.dto.models.Priority.MEDIUM
        Priority.HIGH -> com.armadoon.homebuddy.dto.models.Priority.HIGH
    }
}

fun mapPriorityFromDto(priority: com.armadoon.homebuddy.dto.models.Priority): Priority {
    return when (priority) {
        com.armadoon.homebuddy.dto.models.Priority.LOW -> Priority.LOW
        com.armadoon.homebuddy.dto.models.Priority.MEDIUM -> Priority.MEDIUM
        com.armadoon.homebuddy.dto.models.Priority.HIGH -> Priority.HIGH
    }
}

// Enum mappings - EventStatus
fun mapEventStatusToDto(status: EventStatus): com.armadoon.homebuddy.dto.models.EventStatus {
    return when (status) {
        EventStatus.PENDING -> com.armadoon.homebuddy.dto.models.EventStatus.PENDING
        EventStatus.COMPLETED -> com.armadoon.homebuddy.dto.models.EventStatus.COMPLETED
        EventStatus.CANCELLED -> com.armadoon.homebuddy.dto.models.EventStatus.CANCELLED
    }
}

fun mapEventStatusFromDto(status: com.armadoon.homebuddy.dto.models.EventStatus): EventStatus {
    return when (status) {
        com.armadoon.homebuddy.dto.models.EventStatus.PENDING -> EventStatus.PENDING
        com.armadoon.homebuddy.dto.models.EventStatus.COMPLETED -> EventStatus.COMPLETED
        com.armadoon.homebuddy.dto.models.EventStatus.CANCELLED -> EventStatus.CANCELLED
    }
}
