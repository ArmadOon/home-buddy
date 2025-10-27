package com.armadoon.homebuddy.calendar.controller

import com.armadoon.homebuddy.calendar.security.SecurityUtils
import com.armadoon.homebuddy.calendar.service.EventService
import com.armadoon.homebuddy.dto.models.*
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import java.time.LocalDate

@Controller("/events")
@Secured("ROLE_USER")
class EventController(
    private val eventService: EventService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(EventController::class.java)
    }

    @Post
    fun createEvent(
        @Valid @Body request: CreateEventRequest,
        authentication: Authentication
    ): HttpResponse<EventResponse> {
        val userId = SecurityUtils.getUserId(authentication)
        val householdId = SecurityUtils.requireHouseholdId(authentication)

        logger.info("User $userId creating event: ${request.title}")

        val response = eventService.createEvent(request, householdId, userId)
        return HttpResponse.created(response)
    }

    @Get("{?startDate,endDate,eventType,assignedTo,status,onlyMine}")
    fun listEvents(
        @QueryValue startDate: LocalDate?,
        @QueryValue endDate: LocalDate?,
        @QueryValue eventType: EventType?,
        @QueryValue assignedTo: Long?,
        @QueryValue status: EventStatus?,
        @QueryValue onlyMine: Boolean?,
        authentication: Authentication,
        pageable: Pageable
    ): HttpResponse<Page<EventResponse>> {
        val userId = SecurityUtils.getUserId(authentication)
        val householdId = SecurityUtils.requireHouseholdId(authentication)

        logger.debug("User $userId listing events for household $householdId")

        val events = eventService.listEvents(
            householdId = householdId,
            userId = userId,
            startDate = startDate,
            endDate = endDate,
            eventType = eventType,
            assignedTo = assignedTo,
            status = status,
            onlyMine = onlyMine ?: false,
            pageable = pageable
        )

        return HttpResponse.ok(events)
    }

    @Get("/{id}")
    fun getEvent(
        @PathVariable id: Long,
        authentication: Authentication
    ): HttpResponse<EventResponse> {
        val householdId = SecurityUtils.requireHouseholdId(authentication)

        logger.debug("Fetching event $id")

        val event = eventService.getEvent(id, householdId)
        return HttpResponse.ok(event)
    }

    @Put("/{id}")
    fun updateEvent(
        @PathVariable id: Long,
        @Valid @Body request: UpdateEventRequest,
        authentication: Authentication
    ): HttpResponse<EventResponse> {
        val userId = SecurityUtils.getUserId(authentication)
        val householdId = SecurityUtils.requireHouseholdId(authentication)

        logger.info("User $userId updating event $id")

        val event = eventService.updateEvent(id, request, householdId, userId)
        return HttpResponse.ok(event)
    }

    @Delete("/{id}")
    fun deleteEvent(
        @PathVariable id: Long,
        authentication: Authentication
    ): HttpResponse<Unit> {
        val userId = SecurityUtils.getUserId(authentication)
        val householdId = SecurityUtils.requireHouseholdId(authentication)

        logger.info("User $userId deleting event $id")

        eventService.deleteEvent(id, householdId, userId)
        return HttpResponse.noContent()
    }

    @Patch("/{id}/status")
    fun updateEventStatus(
        @PathVariable id: Long,
        @Valid @Body request: UpdateEventStatusRequest,
        authentication: Authentication
    ): HttpResponse<EventResponse> {
        val userId = SecurityUtils.getUserId(authentication)
        val householdId = SecurityUtils.requireHouseholdId(authentication)

        logger.info("User $userId updating status of event $id to ${request.status}")

        val event = eventService.updateEventStatus(id, request.status, householdId, userId)
        return HttpResponse.ok(event)
    }
}
