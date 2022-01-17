package org.example.minimalexample.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.autoconfigure.web.reactive.function.client.ReactorNettyHttpClientMapper
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.embedded.netty.NettyServerCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.zalando.logbook.*
import org.zalando.logbook.json.JsonBodyFilters
import org.zalando.logbook.netty.LogbookClientHandler
import org.zalando.logbook.netty.LogbookServerHandler
import reactor.netty.http.client.HttpClient
import reactor.netty.http.server.HttpServer

@Configuration(proxyBeanMethods = false)
class LogbookAutoConfiguration {

    @Bean
    fun serverLogbook(): Logbook =
            logbook(OneLinerHttpLogFormatter(Type.SERVER))

    @Bean
    fun clientLogbook(): Logbook =
            logbook(OneLinerHttpLogFormatter(Type.CLIENT))

    private fun logbook(httpLogFormatter: HttpLogFormatter) =
            Logbook.builder()
                    .condition(null)
                    .correlationId(DefaultCorrelationId())
                    .requestFilters(listOf(RequestFilters.defaultValue()))
                    .responseFilters(listOf(ResponseFilters.defaultValue()))
                    .strategy(DefaultStrategy())
                    .sink(DefaultSink(httpLogFormatter, DefaultHttpLogWriter()))
                    .build()

}

@Configuration(proxyBeanMethods = false)
class NettyServerConfiguration {

    @Bean
    fun logbookNettyServerCustomizer(serverLogbook: Logbook): NettyServerCustomizer =
            NettyServerCustomizer { httpServer: HttpServer ->
                httpServer.tcpConfiguration { config -> config.doOnConnection { it.addHandlerLast(LogbookServerHandler(serverLogbook)) } }
            }

}

@Configuration(proxyBeanMethods = false)
class NettyClientConfiguration {

    @Bean
    fun logbookNettyClientCustomizer(clientLogbook: Logbook) =
            ReactorNettyHttpClientMapper { httpClient: HttpClient ->
                httpClient.tcpConfiguration { tcpConfig -> tcpConfig.doOnConnected { it.addHandlerLast(ClientRequestHandler(clientLogbook)) } }
            }

}
