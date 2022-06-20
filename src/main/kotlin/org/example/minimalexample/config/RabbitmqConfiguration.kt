package org.example.minimalexample.config

import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import org.springframework.amqp.core.AmqpAdmin
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.beans.factory.DisposableBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import reactor.core.publisher.Mono
import reactor.rabbitmq.*

@ConfigurationProperties("rabbit-messaging")
@ConstructorBinding
data class MessagingConfiguration(
        val connectionName: String,
        val ssl: Boolean,
        val subscriptionDelay: Int,
        val queue: QueueConfiguration
        ) {

    data class QueueConfiguration(
            val name: String,
            val durable: Boolean,
            val exclusive: Boolean,
            val autoDelete: Boolean,
            val exchange: String,
            val routingKey: String
            )
}

@ConfigurationProperties("rabbit-configuration")
@ConstructorBinding
data class RabbitConfiguration(val connection: RabbitConnection) {
    data class RabbitConnection(val host: String, val port: Int, val virtualHost: String, val username: String, val password: String)
}

@Configuration
@EnableConfigurationProperties(value = [MessagingConfiguration::class, RabbitConfiguration::class])
class TestConfiguration {

    @Bean
    fun customRabbitAmqpAdmin(rabbitConfiguration: RabbitConfiguration, messagingConfiguration: MessagingConfiguration) : AmqpAdmin = RabbitAdmin(
                org.springframework.amqp.rabbit.connection.CachingConnectionFactory()
                    .apply {
                        setUri("${if(messagingConfiguration.ssl){"amqps"}else{"amqp"}}://${rabbitConfiguration.connection.username}:${rabbitConfiguration.connection.password}@${rabbitConfiguration.connection.host}:${rabbitConfiguration.connection.port}/${rabbitConfiguration.connection.virtualHost}")
                    }
    )

    @Bean
    fun customRabbitConnectionFactory(rabbitConfiguration: RabbitConfiguration, messagingConfiguration: MessagingConfiguration): ConnectionFactory =
        ConnectionFactory()
            .apply {
                host = rabbitConfiguration.connection.host
                virtualHost = rabbitConfiguration.connection.virtualHost
                port = rabbitConfiguration.connection.port
                username = rabbitConfiguration.connection.username
                password = rabbitConfiguration.connection.password
                if (messagingConfiguration.ssl){
                    useSslProtocol()
                }
            }

    @Bean
    fun connectionMono(testCustomRabbitConnectionFactory: ConnectionFactory, messagingConfiguration: MessagingConfiguration): Mono<Connection> =
        Mono.fromCallable { testCustomRabbitConnectionFactory.newConnection(messagingConfiguration.connectionName) }.cache()

    @Bean
    @Lazy
    fun receiver(testConnectionMono: Mono<Connection>): Receiver =
        RabbitFlux.createReceiver(ReceiverOptions().connectionMono(testConnectionMono))

    @Bean
    fun destroyer(testConnectionMono: Mono<Connection>): DisposableBean =
        DisposableBean { testConnectionMono.block()?.close() }

}
