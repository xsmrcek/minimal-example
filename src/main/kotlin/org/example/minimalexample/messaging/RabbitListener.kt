package org.example.minimalexample.messaging

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.*
import io.opentelemetry.context.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.example.minimalexample.config.MessagingConfiguration
import org.springframework.amqp.core.*
import org.springframework.amqp.core.Queue
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.rabbitmq.*
import java.time.Duration
import kotlin.random.Random

object RabbitConstants {
    const val QUEUE = "queue"
}

@Configuration
class RabbitListenersConfiguration(
        private val messagingConfiguration: MessagingConfiguration,
        @Value("\${rabbit-messaging.subscription-delay}") private val subscriptionDelay: Long,
        private val handler: RabbitProcessingSampleHandler
) {

    @Bean
    fun rabbitListener(customRabbitAmqpAdmin: AmqpAdmin, receiver: Receiver) = RabbitListeners(customRabbitAmqpAdmin, messagingConfiguration, subscriptionDelay, receiver, handler)

}

class RabbitListeners(
        private val customRabbitAmqpAdmin: AmqpAdmin,
        private val messagingConfiguration: MessagingConfiguration,
        @Value("\${rabbit-messaging.subscription-delay}") private val subscriptionDelay: Long,
        private val receiver: Receiver,
        private val handler: RabbitProcessingSampleHandler
) {
    private val logger = KotlinLogging.logger { }

    init {
        runBlocking {
            runCatching {
                declare(messagingConfiguration.queue)
                subscribe(messagingConfiguration.queue)
            }
                    .onSuccess { _ -> logger.info() { "declared  queue ${messagingConfiguration.queue.name}" } }
                    .onFailure { error -> logger.error(error) { "failed to declare  queue ${messagingConfiguration.queue.name}" } }
        }
    }

    private fun subscribe(queueConfiguration: MessagingConfiguration.QueueConfiguration) {
        receiver
                .consumeManualAck("${queueConfiguration.name}.${RabbitConstants.QUEUE}", ConsumeOptions().qos(10))
                .delaySubscription(Duration.ofMillis(subscriptionDelay))
                .subscribe { delivery ->
                    mono(context = Dispatchers.IO) {
                        val b3 = delivery.properties.headers["b3"]?.toString()?.split('-') ?: emptyList()
                        val currentSpan = createSpan(b3).startSpan().apply { makeCurrent() }
                        setSpanCustomDimensions()
                        try {
                            logger.info { "Received notification with body: '${String(delivery.body)}' and properties '${delivery.properties}'" }
                            val number = handler.handle()
                            logger.info { "Notification processed successfully with number $number" }
                            delivery.ack()
                        } catch (exception: Exception) {
                            logger.error(exception) { "failed to process notification" }
                            delivery.nack(false)
                        } finally {
                            currentSpan.end()
                        }
                    }.subscribe()
                }
    }

    private fun declare(queueConfiguration: MessagingConfiguration.QueueConfiguration) {
        val queue = Queue("${queueConfiguration.name}.${RabbitConstants.QUEUE}", queueConfiguration.durable, queueConfiguration.exclusive, queueConfiguration.autoDelete)

        val exchange = TopicExchange(queueConfiguration.exchange)
        val binding = BindingBuilder.bind(queue).to(exchange).with(queueConfiguration.routingKey)

        customRabbitAmqpAdmin.declareQueue(queue)
        customRabbitAmqpAdmin.declareExchange(exchange)
        customRabbitAmqpAdmin.declareBinding(binding)
    }

    private fun createSpan(b3: List<String>): SpanBuilder {
        val tracer = OpenTelemetry.noop().getTracer("")
        return if (b3.size == 3) {
            val traceId = if (b3[0].length < 32) {
                "0000000000000000${b3[0]}"
            } else b3[0]
            val spanId = b3[1]
            val sampled = if ("1" == b3[2]) {
                TraceFlags.getSampled()
            } else {
                TraceFlags.getDefault()
            }
            val parentSpanContext = SpanContext.create(traceId, spanId, sampled, TraceState.getDefault())
            val parentSpan = Span.wrap(parentSpanContext)
            val parentContext = Context.root().with(parentSpan)
            tracer.spanBuilder("my span with parent").apply { setParent(parentContext) }
        } else {
            val parentSpanContext = SpanContext.create(Random.nextBytes(ByteArray(16)).toHex(), Random.nextBytes(ByteArray(8)).toHex(), TraceFlags.getSampled(), TraceState.getDefault())
            val parentSpan = Span.wrap(parentSpanContext)
            val parentContext = Context.root().with(parentSpan)
            tracer.spanBuilder("my span with generated parent").apply { setParent(parentContext) }
        }
    }

    private fun setSpanCustomDimensions() {
        Span.current().setAttribute("CustomKey", "my attribute")
    }

    fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
}
