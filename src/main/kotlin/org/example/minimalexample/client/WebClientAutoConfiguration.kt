package org.example.minimalexample.client

import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.DefaultUriBuilderFactory
import org.springframework.web.util.UriBuilderFactory

@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(WebClientAutoConfiguration::class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
internal class DefaultWebClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    fun webClient(builder: WebClient.Builder, clientHttpConnector: ClientHttpConnector? = null, uriBuilderFactory: UriBuilderFactory? = DefaultUriBuilderFactory()): WebClient =
        builder.clientConnector(clientHttpConnector).uriBuilderFactory(uriBuilderFactory).build()

}