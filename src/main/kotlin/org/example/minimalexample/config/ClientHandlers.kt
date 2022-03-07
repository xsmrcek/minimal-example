package org.example.minimalexample.config

import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import mu.KotlinLogging
import org.zalando.logbook.Logbook
import org.zalando.logbook.netty.LogbookClientHandler
import io.opentelemetry.api.trace.Span

class ClientRequestHandler(logbook: Logbook): ChannelDuplexHandler() {

    private val delegate = LogbookClientHandler(logbook)
    val logger = KotlinLogging.logger(javaClass.name)

    override fun write(context: ChannelHandlerContext, message: Any, promise: ChannelPromise) {
        handleWithMdc { delegate.write(context, message, promise) }
    }

    override fun channelRead(context: ChannelHandlerContext, message: Any) {
        handleWithMdc { delegate.channelRead(context, message) }
    }

    private fun handleWithMdc(block: () -> Unit) {
        logger.info { Span.current().spanContext.traceId }
        block()
    }
}