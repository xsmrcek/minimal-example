package org.example.minimalexample

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.mono
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.function.server.router

@EnableWebFlux
@SpringBootApplication
class Application {
    @Autowired
    private lateinit var sampleHandler: SampleHandler

    @Bean
    fun route() = router {
        GET("/sample"){
            mono(context = Dispatchers.IO) { sampleHandler.handle(it) }
        }
    }
}

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}