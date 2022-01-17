package org.example.minimalexample.client

import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelOption
import io.netty.handler.logging.LogLevel
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.web.reactive.function.client.ReactorNettyHttpClientMapper
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.netty.http.client.HttpClient
import reactor.netty.transport.logging.AdvancedByteBufFormat
import java.util.concurrent.TimeUnit

internal const val ROOT_PREFIX = "webclient"
private const val CONNECTOR_PREFIX = "${ROOT_PREFIX}.default-http-connector"

@ConstructorBinding
@ConfigurationProperties(CONNECTOR_PREFIX)
data class DefaultHttpConnectorProperties(val timeouts: Timeouts?) {
    data class Timeouts(val connect: Timeout?, val read: Timeout?, val write: Timeout?)
    data class Timeout(val value: Long, val unit: TimeUnit = TimeUnit.SECONDS) {
        fun milliseconds() = unit.toMillis(value)
    }
}

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DefaultHttpConnectorProperties::class)
internal class ReactorClientHttpConnectorAutoConfiguration {

    @Bean
    fun connectTimeoutCustomizer(properties: DefaultHttpConnectorProperties) =
        ReactorNettyHttpClientMapper { httpClient: HttpClient ->
            properties.timeouts?.connect?.let { connectTimeout ->
                httpClient.tcpConfiguration { it.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout.milliseconds().toInt()) }
            }
        }

    @Bean
    fun readTimeoutCustomizer(properties: DefaultHttpConnectorProperties) =
        timeoutHandler(properties.timeouts?.read) { timeout, unit ->  ReadTimeoutHandler(timeout, unit) }

    @Bean
    fun writeTimeoutCustomizer(properties: DefaultHttpConnectorProperties) =
        timeoutHandler(properties.timeouts?.write) { timeout, unit ->  WriteTimeoutHandler(timeout, unit) }

    private fun timeoutHandler(timeout: DefaultHttpConnectorProperties.Timeout?, channelHandler: (timeout: Long, unit: TimeUnit) -> ChannelHandler) =
        ReactorNettyHttpClientMapper { httpClient: HttpClient ->
            timeout?.let {
                httpClient.tcpConfiguration { config ->
                    config.doOnConnected { connection -> connection.addHandlerLast(channelHandler(it.value, it.unit)) }
                }
            }
    }

}