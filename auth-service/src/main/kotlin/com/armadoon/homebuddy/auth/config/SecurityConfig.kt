package com.armadoon.homebuddy.auth.config

import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

@Factory
class SecurityConfig {

    @Bean
    fun passwordEncoder(): PasswordEncoder {
       return BCryptPasswordEncoder(12)
    }

}