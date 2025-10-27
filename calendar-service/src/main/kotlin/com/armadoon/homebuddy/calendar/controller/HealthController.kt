package com.armadoon.homebuddy.calendar.controller

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule

@Controller("/status")
class HealthController {

    @Get
    @Secured(SecurityRule.IS_ANONYMOUS)
    fun status(): HttpResponse<Map<String, String>> {
        return HttpResponse.ok(mapOf(
            "status" to "UP",
            "service" to "calendar-service",
            "timestamp" to java.time.Instant.now().toString()
        ))
    }
}
