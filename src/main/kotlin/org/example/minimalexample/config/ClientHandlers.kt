package org.example.minimalexample.config

import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.netty.handler.codec.http.HttpMessage
import org.slf4j.MDC
import org.zalando.logbook.Logbook
import org.zalando.logbook.netty.LogbookClientHandler

class ClientRequestHandler(logbook: Logbook): ChannelDuplexHandler() {

    private val delegate = LogbookClientHandler(logbook)
    private var spanContext: List<String> = emptyList()

    override fun write(context: ChannelHandlerContext, message: Any, promise: ChannelPromise) {
        if (message is HttpMessage) {
            spanContext = message.headers()["traceparent"]?.split('-')?.takeIf { it.size == 4 }?.drop(1) ?: emptyList()
        }

        handleWithMdc { delegate.write(context, message, promise) }
    }

    override fun channelRead(context: ChannelHandlerContext, message: Any) {
        handleWithMdc { delegate.channelRead(context, message) }
    }

    private fun handleWithMdc(block: () -> Unit) {
        //otel agent not starting span when we log request/response, as a result we're missing mdc bits in the log
        //so, here we're reconstructing it manually.
        if (spanContext.isNotEmpty() && MDC.get("traceId") == null) {
            MDC.putCloseable("traceId", spanContext[0]).use {
                MDC.putCloseable("spanId", spanContext[1]).use {
                    MDC.putCloseable("sampled", ("01" == spanContext[2]).toString()).use {
                        block()
                    }
                }
            }
        } else {
            block()
        }
    }
}