package com.armadoon.homebuddy.calendar.service

import com.armadoon.homebuddy.calendar.entity.Event
import com.armadoon.homebuddy.calendar.entity.EventStatus
import com.armadoon.homebuddy.calendar.entity.EventType
import com.armadoon.homebuddy.calendar.mapping.*
import com.armadoon.homebuddy.calendar.repository.EventRepository
import com.armadoon.homebuddy.dto.models.*
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Singleton
open class EventService(
    private val eventRepository: EventRepository
) {

    companion object {
        private val logger = LoggerFactory.getLogger(EventService::class.java)
    }

    @Transactional
    open fun createEvent(
        request: CreateEventRequest,
        householdId: Long,
        userId: Long
    ): EventResponse {
        logger.info("Creating event for household $householdId by user $userId: ${request.title}")

        // Validate dates
        if (request.startDateTime >= request.endDateTime) {
            throw HttpStatusException(HttpStatus.BAD_REQUEST, "Start date must be before end date")
        }

        // TODO: Validate assignedTo user belongs to household (requires auth-service integration)

        val event = request.toEntity(
            householdId = householdId,
            createdBy = userId
        )

        val savedEvent = eventRepository.save(event)
        logger.info("Event created successfully: id=${savedEvent.id}, title=${savedEvent.title}")

        return savedEvent.toDto()
    }

    open fun listEvents(
        householdId: Long,
        userId: Long,
        startDate: LocalDate?,
        endDate: LocalDate?,
        eventType: com.armadoon.homebuddy.dto.models.EventType?,
        assignedTo: Long?,
        status: com.armadoon.homebuddy.dto.models.EventStatus?,
        onlyMine: Boolean,
        pageable: Pageable
    ): Page<EventResponse> {
        logger.debug("Listing events for household $householdId with filters")

        val events: Page<Event> = when {
            // Only mine filter
            onlyMine -> {
                eventRepository.findByHouseholdIdAndAssignedToOrCreatedBy(
                    householdId, userId, userId, pageable
                )
            }
            // Date range filter
            startDate != null && endDate != null -> {
                val start = startDate.atStartOfDay()
                val end = endDate.atTime(LocalTime.MAX)

                if (status != null) {
                    eventRepository.findByHouseholdIdAndStartDateTimeBetweenAndStatus(
                        householdId, start, end, mapEventStatusFromDto(status), pageable
                    )
                } else {
                    eventRepository.findByHouseholdIdAndStartDateTimeBetween(
                        householdId, start, end, pageable
                    )
                }
            }
            // Event type filter
            eventType != null -> {
                eventRepository.findByHouseholdIdAndEventType(
                    householdId, mapEventTypeFromDto(eventType), pageable
                )
            }
            // Assigned to filter
            assignedTo != null -> {
                eventRepository.findByHouseholdIdAndAssignedTo(
                    householdId, assignedTo, pageable
                )
            }
            // Status filter
            status != null -> {
                eventRepository.findByHouseholdIdAndStatus(
                    householdId, mapEventStatusFromDto(status), pageable
                )
            }
            // No filters
            else -> {
                eventRepository.findByHouseholdId(householdId, pageable)
            }
        }

        return events.map { it.toDto() }
    }

    open fun getEvent(eventId: Long, householdId: Long): EventResponse {
        logger.debug("Fetching event $eventId for household $householdId")

        val event = eventRepository.findByIdAndHouseholdId(eventId, householdId)
            .orElseThrow {
                HttpStatusException(HttpStatus.NOT_FOUND, "Event not found")
            }

        return event.toDto()
    }

    @Transactional
    open fun updateEvent(
        eventId: Long,
        request: UpdateEventRequest,
        householdId: Long,
        userId: Long
    ): EventResponse {
        logger.info("Updating event $eventId by user $userId")

        val event = eventRepository.findByIdAndHouseholdId(eventId, householdId)
            .orElseThrow {
                HttpStatusException(HttpStatus.NOT_FOUND, "Event not found")
            }

        // Check authorization: only creator or assigned user can modify
        if (event.createdBy != userId && event.assignedTo != userId) {
            throw HttpStatusException(
                HttpStatus.FORBIDDEN,
                "You don't have permission to modify this event"
            )
        }

        // Validate dates if both are being updated
        val newStart = request.startDateTime?.toLocalDateTime() ?: event.startDateTime
        val newEnd = request.endDateTime?.toLocalDateTime() ?: event.endDateTime
        if (newStart >= newEnd) {
            throw HttpStatusException(HttpStatus.BAD_REQUEST, "Start date must be before end date")
        }

        val updatedEvent = event.applyUpdate(request)
        val savedEvent = eventRepository.update(updatedEvent)

        logger.info("Event updated successfully: id=${savedEvent.id}")
        return savedEvent.toDto()
    }

    @Transactional
    open fun deleteEvent(eventId: Long, householdId: Long, userId: Long) {
        logger.info("Deleting event $eventId by user $userId")

        val event = eventRepository.findByIdAndHouseholdId(eventId, householdId)
            .orElseThrow {
                HttpStatusException(HttpStatus.NOT_FOUND, "Event not found")
            }

        // Check authorization: only creator or assigned user can delete
        if (event.createdBy != userId && event.assignedTo != userId) {
            throw HttpStatusException(
                HttpStatus.FORBIDDEN,
                "You don't have permission to delete this event"
            )
        }

        eventRepository.deleteById(eventId)
        logger.info("Event deleted successfully: id=$eventId")
    }

    @Transactional
    open fun updateEventStatus(
        eventId: Long,
        newStatus: com.armadoon.homebuddy.dto.models.EventStatus,
        householdId: Long,
        userId: Long
    ): EventResponse {
        logger.info("Updating status of event $eventId to $newStatus by user $userId")

        val event = eventRepository.findByIdAndHouseholdId(eventId, householdId)
            .orElseThrow {
                HttpStatusException(HttpStatus.NOT_FOUND, "Event not found")
            }

        // Check authorization for status changes
        if (event.assignedTo != null && event.assignedTo != userId && event.createdBy != userId) {
            throw HttpStatusException(
                HttpStatus.FORBIDDEN,
                "Only assigned user or creator can change event status"
            )
        }

        val mappedStatus = mapEventStatusFromDto(newStatus)
        val now = LocalDateTime.now()

        val updatedEvent = when (mappedStatus) {
            EventStatus.COMPLETED -> event.copy(
                status = EventStatus.COMPLETED,
                completedAt = now,
                completedBy = userId,
                updatedAt = now
            )
            EventStatus.CANCELLED -> event.copy(
                status = EventStatus.CANCELLED,
                updatedAt = now
            )
            EventStatus.PENDING -> event.copy(
                status = EventStatus.PENDING,
                completedAt = null,
                completedBy = null,
                updatedAt = now
            )
        }

        val savedEvent = eventRepository.update(updatedEvent)
        logger.info("Event status updated successfully: id=${savedEvent.id}, status=$mappedStatus")

        return savedEvent.toDto()
    }
}
