package org.example.minimalexample.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain


@Configuration
class SecurityConfiguration {

    @Bean
    fun securityFilterChain(serverHttpSecurity: ServerHttpSecurity): SecurityWebFilterChain? {
        return serverHttpSecurity
                .csrf().disable()
                .formLogin().disable()
                .build()
    }
}