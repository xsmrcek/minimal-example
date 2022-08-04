package org.example.minimalexample.messaging

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.Context
import kotlinx.coroutines.reactive.awaitFirstOrNull
import mu.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.nio.charset.StandardCharsets
import kotlin.random.Random


@Component
class RabbitProcessingSampleHandler(
        private val webClient: WebClient
) {

    private val logger = KotlinLogging.logger(javaClass.name)

    suspend fun handle(): Int {
            val tracer = GlobalOpenTelemetry.getTracerProvider().tracerBuilder("my-tracer").build()
            val parentSpan = Span.current()

            val newSpan = tracer.spanBuilder("my span with parent").apply { setParent(Context.current().with(parentSpan)) }.startSpan().apply { makeCurrent() }
            try {
                //just call google here :)
                callGoogle()
                logger.info {"just called google"}
            } finally {
                newSpan.end()
            }

            // here I would construct message
            val messageSpan = tracer.spanBuilder("my span with parent").apply { setParent(Context.current().with(parentSpan)) }.startSpan().apply { makeCurrent() }
            try {
                //and send it using azure service bus here, but let's just call google again
                callGoogle()
                logger.info {"just called google"}

            } finally {
                messageSpan.end()
            }
        return Random.nextInt()
    }

    private suspend fun callGoogle() =
            webClient.get()
                    .uri("https://google.com")
                    .accept(MediaType.APPLICATION_JSON)
                    .acceptCharset(StandardCharsets.UTF_8)
                    .retrieve()
                    .toBodilessEntity()
                    .awaitFirstOrNull()
}