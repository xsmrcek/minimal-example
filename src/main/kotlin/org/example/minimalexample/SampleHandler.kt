package org.example.minimalexample

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.awaitSingle
import mu.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import java.nio.charset.StandardCharsets

@Component
class SampleHandler(
        private val webClient: WebClient
) {

    private val logger = KotlinLogging.logger(javaClass.name)

    suspend fun handle(request: ServerRequest): ServerResponse =
        runCatching {
            logger.info { "processing request" }
            callGoogle()
            coroutineScope {
                async {
                    callGoogle()
                    callGoogle()
                }
                launch {
                    callGoogle()
                }
            }.join()
        }.fold(
                onSuccess = {
                    logger.info { "request processed successfully" }
                    ServerResponse.ok().build()
                            },
                onFailure = {
                    logger.info { "request failed" }
                    ServerResponse.status(500).build()
                }
        ).awaitSingle()

    private suspend fun callGoogle() =
            webClient.get()
            .uri("https://google.com")
            .accept(MediaType.APPLICATION_JSON)
            .acceptCharset(StandardCharsets.UTF_8)
            .retrieve()
            .toBodilessEntity()
            .awaitSingle()
}
