package com.armadoon.homebuddy.calendar.service

import com.armadoon.homebuddy.calendar.entity.EventStatus
import com.armadoon.homebuddy.calendar.entity.EventType
import com.armadoon.homebuddy.calendar.entity.Priority
import com.armadoon.homebuddy.calendar.repository.EventRepository
import com.armadoon.homebuddy.dto.models.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.micronaut.data.model.Pageable
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

@MicronautTest
class EventServiceTest(
    private val eventService: EventService,
    private val eventRepository: EventRepository
) : StringSpec({

    beforeTest {
        // Clean up database before each test
        eventRepository.deleteAll()
    }

    "should create event successfully" {
        val request = CreateEventRequest(
            title = "Team Meeting",
            description = "Monthly sync",
            eventType = com.armadoon.homebuddy.dto.models.EventType.APPOINTMENT,
            startDateTime = OffsetDateTime.now().plusDays(1),
            endDateTime = OffsetDateTime.now().plusDays(1).plusHours(1),
            allDayEvent = false,
            location = "Conference Room",
            priority = com.armadoon.homebuddy.dto.models.Priority.MEDIUM,
            assignedTo = null,
            isRecurring = false,
            recurrenceRule = null
        )

        val response = eventService.createEvent(request, householdId = 1L, userId = 100L)

        response.id shouldNotBe null
        response.householdId shouldBe 1L
        response.createdBy shouldBe 100L
        response.title shouldBe "Team Meeting"
        response.description shouldBe "Monthly sync"
        response.eventType shouldBe com.armadoon.homebuddy.dto.models.EventType.APPOINTMENT
        response.location shouldBe "Conference Room"
        response.priority shouldBe com.armadoon.homebuddy.dto.models.Priority.MEDIUM
        response.status shouldBe com.armadoon.homebuddy.dto.models.EventStatus.PENDING
        response.isRecurring shouldBe false
    }

    "should fail to create event when start date is after end date" {
        val request = CreateEventRequest(
            title = "Invalid Event",
            eventType = com.armadoon.homebuddy.dto.models.EventType.OTHER,
            startDateTime = OffsetDateTime.now().plusDays(2),
            endDateTime = OffsetDateTime.now().plusDays(1),
            allDayEvent = false
        )

        val exception = shouldThrow<HttpStatusException> {
            eventService.createEvent(request, householdId = 1L, userId = 100L)
        }

        exception.status shouldBe HttpStatus.BAD_REQUEST
        exception.message shouldBe "Start date must be before end date"
    }

    "should create event with assigned user" {
        val request = CreateEventRequest(
            title = "Clean Kitchen",
            eventType = com.armadoon.homebuddy.dto.models.EventType.CHORE,
            startDateTime = OffsetDateTime.now().plusDays(1),
            endDateTime = OffsetDateTime.now().plusDays(1).plusHours(2),
            assignedTo = 200L,
            priority = com.armadoon.homebuddy.dto.models.Priority.HIGH
        )

        val response = eventService.createEvent(request, householdId = 1L, userId = 100L)

        response.assignedTo shouldBe 200L
        response.createdBy shouldBe 100L
        response.priority shouldBe com.armadoon.homebuddy.dto.models.Priority.HIGH
    }

    "should get event by id and household" {
        // Create an event first
        val createRequest = CreateEventRequest(
            title = "Test Event",
            eventType = com.armadoon.homebuddy.dto.models.EventType.REMINDER,
            startDateTime = OffsetDateTime.now().plusDays(1),
            endDateTime = OffsetDateTime.now().plusDays(1).plusHours(1)
        )

        val created = eventService.createEvent(createRequest, householdId = 1L, userId = 100L)

        // Get the event
        val retrieved = eventService.getEvent(created.id, householdId = 1L)

        retrieved.id shouldBe created.id
        retrieved.title shouldBe "Test Event"
        retrieved.householdId shouldBe 1L
    }

    "should fail to get event from different household" {
        // Create an event in household 1
        val createRequest = CreateEventRequest(
            title = "Household 1 Event",
            eventType = com.armadoon.homebuddy.dto.models.EventType.SOCIAL,
            startDateTime = OffsetDateTime.now().plusDays(1),
            endDateTime = OffsetDateTime.now().plusDays(1).plusHours(1)
        )

        val created = eventService.createEvent(createRequest, householdId = 1L, userId = 100L)

        // Try to get from household 2
        val exception = shouldThrow<HttpStatusException> {
            eventService.getEvent(created.id, householdId = 2L)
        }

        exception.status shouldBe HttpStatus.NOT_FOUND
    }

    "should list all events for household" {
        // Create multiple events
        eventService.createEvent(
            CreateEventRequest(
                title = "Event 1",
                eventType = com.armadoon.homebuddy.dto.models.EventType.CHORE,
                startDateTime = OffsetDateTime.now().plusDays(1),
                endDateTime = OffsetDateTime.now().plusDays(1).plusHours(1)
            ),
            householdId = 1L,
            userId = 100L
        )

        eventService.createEvent(
            CreateEventRequest(
                title = "Event 2",
                eventType = com.armadoon.homebuddy.dto.models.EventType.APPOINTMENT,
                startDateTime = OffsetDateTime.now().plusDays(2),
                endDateTime = OffsetDateTime.now().plusDays(2).plusHours(1)
            ),
            householdId = 1L,
            userId = 100L
        )

        // Create event in different household
        eventService.createEvent(
            CreateEventRequest(
                title = "Other Household Event",
                eventType = com.armadoon.homebuddy.dto.models.EventType.OTHER,
                startDateTime = OffsetDateTime.now().plusDays(1),
                endDateTime = OffsetDateTime.now().plusDays(1).plusHours(1)
            ),
            householdId = 2L,
            userId = 200L
        )

        val events = eventService.listEvents(
            householdId = 1L,
            userId = 100L,
            startDate = null,
            endDate = null,
            eventType = null,
            assignedTo = null,
            status = null,
            onlyMine = false,
            pageable = Pageable.from(0, 10)
        )

        events.totalSize shouldBe 2L
        events.content.size shouldBe 2
        events.content.all { it.householdId == 1L } shouldBe true
    }

    "should filter events by event type" {
        eventService.createEvent(
            CreateEventRequest(
                title = "Chore Event",
                eventType = com.armadoon.homebuddy.dto.models.EventType.CHORE,
                startDateTime = OffsetDateTime.now().plusDays(1),
                endDateTime = OffsetDateTime.now().plusDays(1).plusHours(1)
            ),
            householdId = 1L,
            userId = 100L
        )

        eventService.createEvent(
            CreateEventRequest(
                title = "Appointment Event",
                eventType = com.armadoon.homebuddy.dto.models.EventType.APPOINTMENT,
                startDateTime = OffsetDateTime.now().plusDays(1),
                endDateTime = OffsetDateTime.now().plusDays(1).plusHours(1)
            ),
            householdId = 1L,
            userId = 100L
        )

        val choreEvents = eventService.listEvents(
            householdId = 1L,
            userId = 100L,
            startDate = null,
            endDate = null,
            eventType = com.armadoon.homebuddy.dto.models.EventType.CHORE,
            assignedTo = null,
            status = null,
            onlyMine = false,
            pageable = Pageable.from(0, 10)
        )

        choreEvents.totalSize shouldBe 1L
        choreEvents.content[0].eventType shouldBe com.armadoon.homebuddy.dto.models.EventType.CHORE
        choreEvents.content[0].title shouldBe "Chore Event"
    }

    "should filter events by date range" {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val nextWeek = today.plusDays(7)

        // Event tomorrow
        eventService.createEvent(
            CreateEventRequest(
                title = "Tomorrow Event",
                eventType = com.armadoon.homebuddy.dto.models.EventType.REMINDER,
                startDateTime = tomorrow.atStartOfDay().atOffset(ZoneOffset.UTC),
                endDateTime = tomorrow.atTime(10, 0).atOffset(ZoneOffset.UTC)
            ),
            householdId = 1L,
            userId = 100L
        )

        // Event next week
        eventService.createEvent(
            CreateEventRequest(
                title = "Next Week Event",
                eventType = com.armadoon.homebuddy.dto.models.EventType.REMINDER,
                startDateTime = nextWeek.atStartOfDay().atOffset(ZoneOffset.UTC),
                endDateTime = nextWeek.atTime(10, 0).atOffset(ZoneOffset.UTC)
            ),
            householdId = 1L,
            userId = 100L
        )

        // Filter for this week only
        val thisWeekEvents = eventService.listEvents(
            householdId = 1L,
            userId = 100L,
            startDate = today,
            endDate = today.plusDays(6),
            eventType = null,
            assignedTo = null,
            status = null,
            onlyMine = false,
            pageable = Pageable.from(0, 10)
        )

        thisWeekEvents.totalSize shouldBe 1L
        thisWeekEvents.content[0].title shouldBe "Tomorrow Event"
    }

    "should filter events assigned to specific user" {
        eventService.createEvent(
            CreateEventRequest(
                title = "Assigned to User 200",
                eventType = com.armadoon.homebuddy.dto.models.EventType.CHORE,
                startDateTime = OffsetDateTime.now().plusDays(1),
                endDateTime = OffsetDateTime.now().plusDays(1).plusHours(1),
                assignedTo = 200L
            ),
            householdId = 1L,
            userId = 100L
        )

        eventService.createEvent(
            CreateEventRequest(
                title = "Assigned to User 300",
                eventType = com.armadoon.homebuddy.dto.models.EventType.CHORE,
                startDateTime = OffsetDateTime.now().plusDays(1),
                endDateTime = OffsetDateTime.now().plusDays(1).plusHours(1),
                assignedTo = 300L
            ),
            householdId = 1L,
            userId = 100L
        )

        val user200Events = eventService.listEvents(
            householdId = 1L,
            userId = 100L,
            startDate = null,
            endDate = null,
            eventType = null,
            assignedTo = 200L,
            status = null,
            onlyMine = false,
            pageable = Pageable.from(0, 10)
        )

        user200Events.totalSize shouldBe 1L
        user200Events.content[0].assignedTo shouldBe 200L
    }

    "should filter only my events (created by or assigned to)" {
        // Event created by user 100
        eventService.createEvent(
            CreateEventRequest(
                title = "Created by 100",
                eventType = com.armadoon.homebuddy.dto.models.EventType.REMINDER,
                startDateTime = OffsetDateTime.now().plusDays(1),
                endDateTime = OffsetDateTime.now().plusDays(1).plusHours(1)
            ),
            householdId = 1L,
            userId = 100L
        )

        // Event assigned to user 100
        eventService.createEvent(
            CreateEventRequest(
                title = "Assigned to 100",
                eventType = com.armadoon.homebuddy.dto.models.EventType.CHORE,
                startDateTime = OffsetDateTime.now().plusDays(1),
                endDateTime = OffsetDateTime.now().plusDays(1).plusHours(1),
                assignedTo = 100L
            ),
            householdId = 1L,
            userId = 200L
        )

        // Event not related to user 100
        eventService.createEvent(
            CreateEventRequest(
                title = "Not related to 100",
                eventType = com.armadoon.homebuddy.dto.models.EventType.SOCIAL,
                startDateTime = OffsetDateTime.now().plusDays(1),
                endDateTime = OffsetDateTime.now().plusDays(1).plusHours(1),
                assignedTo = 300L
            ),
            householdId = 1L,
            userId = 200L
        )

        val myEvents = eventService.listEvents(
            householdId = 1L,
            userId = 100L,
            startDate = null,
            endDate = null,
            eventType = null,
            assignedTo = null,
            status = null,
            onlyMine = true,
            pageable = Pageable.from(0, 10)
        )

        myEvents.totalSize shouldBe 2L
    }

    "should update event successfully" {
        val created = eventService.createEvent(
            CreateEventRequest(
                title = "Original Title",
                eventType = com.armadoon.homebuddy.dto.models.EventType.CHORE,
                startDateTime = OffsetDateTime.now().plusDays(1),
                endDateTime = OffsetDateTime.now().plusDays(1).plusHours(1),
                priority = com.armadoon.homebuddy.dto.models.Priority.LOW
            ),
            householdId = 1L,
            userId = 100L
        )

        val updateRequest = UpdateEventRequest(
            title = "Updated Title",
            priority = com.armadoon.homebuddy.dto.models.Priority.HIGH,
            description = "Updated description"
        )

        val updated = eventService.updateEvent(
            eventId = created.id,
            request = updateRequest,
            householdId = 1L,
            userId = 100L
        )

        updated.id shouldBe created.id
        updated.title shouldBe "Updated Title"
        updated.priority shouldBe com.armadoon.homebuddy.dto.models.Priority.HIGH
        updated.description shouldBe "Updated description"
        updated.eventType shouldBe created.eventType // Unchanged
    }

    "should fail to update event by non-creator and non-assigned user" {
        val created = eventService.createEvent(
            CreateEventRequest(
                title = "Event",
                eventType = com.armadoon.homebuddy.dto.models.EventType.CHORE,
                startDateTime = OffsetDateTime.now().plusDays(1),
                endDateTime = OffsetDateTime.now().plusDays(1).plusHours(1),
                assignedTo = 200L
            ),
            householdId = 1L,
            userId = 100L
        )

        val exception = shouldThrow<HttpStatusException> {
            eventService.updateEvent(
                eventId = created.id,
                request = UpdateEventRequest(title = "Hacked"),
                householdId = 1L,
                userId = 999L // Different user
            )
        }

        exception.status shouldBe HttpStatus.FORBIDDEN
    }

    "should allow assigned user to update event" {
        val created = eventService.createEvent(
            CreateEventRequest(
                title = "Original",
                eventType = com.armadoon.homebuddy.dto.models.EventType.CHORE,
                startDateTime = OffsetDateTime.now().plusDays(1),
                endDateTime = OffsetDateTime.now().plusDays(1).plusHours(1),
                assignedTo = 200L
            ),
            householdId = 1L,
            userId = 100L
        )

        val updated = eventService.updateEvent(
            eventId = created.id,
            request = UpdateEventRequest(title = "Updated by assignee"),
            householdId = 1L,
            userId = 200L // Assigned user
        )

        updated.title shouldBe "Updated by assignee"
    }

    "should delete event successfully" {
        val created = eventService.createEvent(
            CreateEventRequest(
                title = "To Delete",
                eventType = com.armadoon.homebuddy.dto.models.EventType.OTHER,
                startDateTime = OffsetDateTime.now().plusDays(1),
                endDateTime = OffsetDateTime.now().plusDays(1).plusHours(1)
            ),
            householdId = 1L,
            userId = 100L
        )

        eventService.deleteEvent(created.id, householdId = 1L, userId = 100L)

        val exception = shouldThrow<HttpStatusException> {
            eventService.getEvent(created.id, householdId = 1L)
        }

        exception.status shouldBe HttpStatus.NOT_FOUND
    }

    "should fail to delete event by non-creator and non-assigned user" {
        val created = eventService.createEvent(
            CreateEventRequest(
                title = "Event",
                eventType = com.armadoon.homebuddy.dto.models.EventType.CHORE,
                startDateTime = OffsetDateTime.now().plusDays(1),
                endDateTime = OffsetDateTime.now().plusDays(1).plusHours(1)
            ),
            householdId = 1L,
            userId = 100L
        )

        val exception = shouldThrow<HttpStatusException> {
            eventService.deleteEvent(created.id, householdId = 1L, userId = 999L)
        }

        exception.status shouldBe HttpStatus.FORBIDDEN
    }

    "should update event status to COMPLETED" {
        val created = eventService.createEvent(
            CreateEventRequest(
                title = "Task",
                eventType = com.armadoon.homebuddy.dto.models.EventType.CHORE,
                startDateTime = OffsetDateTime.now().plusDays(1),
                endDateTime = OffsetDateTime.now().plusDays(1).plusHours(1),
                assignedTo = 200L
            ),
            householdId = 1L,
            userId = 100L
        )

        val completed = eventService.updateEventStatus(
            eventId = created.id,
            newStatus = com.armadoon.homebuddy.dto.models.EventStatus.COMPLETED,
            householdId = 1L,
            userId = 200L
        )

        completed.status shouldBe com.armadoon.homebuddy.dto.models.EventStatus.COMPLETED
        completed.completedBy shouldBe 200L
        completed.completedAt shouldNotBe null
    }

    "should update event status to CANCELLED" {
        val created = eventService.createEvent(
            CreateEventRequest(
                title = "Event",
                eventType = com.armadoon.homebuddy.dto.models.EventType.APPOINTMENT,
                startDateTime = OffsetDateTime.now().plusDays(1),
                endDateTime = OffsetDateTime.now().plusDays(1).plusHours(1)
            ),
            householdId = 1L,
            userId = 100L
        )

        val cancelled = eventService.updateEventStatus(
            eventId = created.id,
            newStatus = com.armadoon.homebuddy.dto.models.EventStatus.CANCELLED,
            householdId = 1L,
            userId = 100L
        )

        cancelled.status shouldBe com.armadoon.homebuddy.dto.models.EventStatus.CANCELLED
        cancelled.completedAt shouldBe null
        cancelled.completedBy shouldBe null
    }

    "should update event status back to PENDING" {
        val created = eventService.createEvent(
            CreateEventRequest(
                title = "Task",
                eventType = com.armadoon.homebuddy.dto.models.EventType.CHORE,
                startDateTime = OffsetDateTime.now().plusDays(1),
                endDateTime = OffsetDateTime.now().plusDays(1).plusHours(1)
            ),
            householdId = 1L,
            userId = 100L
        )

        // Complete it first
        eventService.updateEventStatus(
            eventId = created.id,
            newStatus = com.armadoon.homebuddy.dto.models.EventStatus.COMPLETED,
            householdId = 1L,
            userId = 100L
        )

        // Reopen it
        val pending = eventService.updateEventStatus(
            eventId = created.id,
            newStatus = com.armadoon.homebuddy.dto.models.EventStatus.PENDING,
            householdId = 1L,
            userId = 100L
        )

        pending.status shouldBe com.armadoon.homebuddy.dto.models.EventStatus.PENDING
        pending.completedAt shouldBe null
        pending.completedBy shouldBe null
    }

    "should filter events by status" {
        val event1 = eventService.createEvent(
            CreateEventRequest(
                title = "Pending Event",
                eventType = com.armadoon.homebuddy.dto.models.EventType.CHORE,
                startDateTime = OffsetDateTime.now().plusDays(1),
                endDateTime = OffsetDateTime.now().plusDays(1).plusHours(1)
            ),
            householdId = 1L,
            userId = 100L
        )

        val event2 = eventService.createEvent(
            CreateEventRequest(
                title = "Completed Event",
                eventType = com.armadoon.homebuddy.dto.models.EventType.CHORE,
                startDateTime = OffsetDateTime.now().plusDays(1),
                endDateTime = OffsetDateTime.now().plusDays(1).plusHours(1)
            ),
            householdId = 1L,
            userId = 100L
        )

        eventService.updateEventStatus(
            event2.id,
            com.armadoon.homebuddy.dto.models.EventStatus.COMPLETED,
            householdId = 1L,
            userId = 100L
        )

        val pendingEvents = eventService.listEvents(
            householdId = 1L,
            userId = 100L,
            startDate = null,
            endDate = null,
            eventType = null,
            assignedTo = null,
            status = com.armadoon.homebuddy.dto.models.EventStatus.PENDING,
            onlyMine = false,
            pageable = Pageable.from(0, 10)
        )

        pendingEvents.totalSize shouldBe 1L
        pendingEvents.content[0].title shouldBe "Pending Event"
        pendingEvents.content[0].status shouldBe com.armadoon.homebuddy.dto.models.EventStatus.PENDING
    }
})
