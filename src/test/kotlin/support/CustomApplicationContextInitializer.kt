package support

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.containers.RabbitMQContainer
import org.testcontainers.utility.DockerImageName

private const val TEST_VHOST = "virtual_host"

class CustomApplicationContextInitializer: ApplicationContextInitializer<ConfigurableApplicationContext> {

    companion object {
        private val testRabbit =
                RabbitMQContainer(DockerImageName.parse("rabbitmq").withTag("3.7.25-management-alpine"))
                        .withVhost(TEST_VHOST)
                        .also {
                            it.start()
                        }

    }

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        TestPropertyValues
                .of(
                        "rabbit-configuration.connection.host=${testRabbit.containerIpAddress}",
                        "rabbit-configuration.connection.port=${testRabbit.amqpPort}",
                        "rabbit-configuration.connection.virtualHost=$TEST_VHOST",
                        "rabbit-configuration.connection.username=${testRabbit.adminUsername}",
                        "rabbit-configuration.connection.password=${testRabbit.adminPassword}"
                )
                .applyTo(applicationContext)
    }
}