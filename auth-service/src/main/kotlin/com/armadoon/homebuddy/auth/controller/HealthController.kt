package com.armadoon.homebuddy.auth.controller

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get

@Controller("/status")
class HealthController {

    @Get
    fun status(): HttpResponse<Map<String, String>> {
        return HttpResponse.ok(mapOf(
            "status" to "UP",
            "service" to "auth-service",
            "timestamp" to java.time.Instant.now().toString()
        ))
    }
}