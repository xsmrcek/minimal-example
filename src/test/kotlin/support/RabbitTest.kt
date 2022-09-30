package support

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Connection
import org.assertj.core.api.Assertions
import org.example.minimalexample.config.MessagingConfiguration
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils
import reactor.core.publisher.Mono
import reactor.rabbitmq.*

internal class RabbitTest(): BaseTest(){

    @Autowired
    lateinit var connectionMono: Mono<Connection>

    @Autowired
    lateinit var messagingConfiguration: MessagingConfiguration

    private val testSender by lazy { RabbitFlux.createSender(SenderOptions().connectionMono(connectionMono)) }

    private fun sendTestNotification(body: String) {
        val response =
                testSender
                        .sendWithPublishConfirms(
                                Mono.just(
                                        OutboundMessage(
                                                messagingConfiguration.queue.exchange,
                                                "routing_key",
                                                AMQP.BasicProperties.Builder()
                                                        .messageId(RandomStringUtils.randomAlphanumeric(7))
                                                        .build(),
                                                body.toByteArray()
                                        )
                                ),
                                SendOptions().trackReturned(true)
                        )
                        .blockFirst()
        Assertions.assertThat(response).isNotNull
        Assertions.assertThat(response!!.isAck).isTrue
        Assertions.assertThat(response.isReturned).isFalse

        Thread.sleep(300000)
    }

    @Test
    fun `success test`() {
        sendTestNotification("random body")

    }

    @Test
    fun `error test test`() {
        sendTestNotification("let's fail")

    }
}