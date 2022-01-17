package org.example.minimalexample.config

import org.zalando.logbook.Correlation
import org.zalando.logbook.HttpLogFormatter
import org.zalando.logbook.HttpRequest
import org.zalando.logbook.HttpResponse
import org.zalando.logbook.Precorrelation

enum class Type {
    CLIENT, SERVER
}

class OneLinerHttpLogFormatter(private val type: Type) : HttpLogFormatter {

    private val requestPathMap = mutableMapOf<String, String>()

    override fun format(precorrelation: Precorrelation, request: HttpRequest): String {
        requestPathMap[precorrelation.id] = path(request)
        return "${type.name.toLowerCase()} request [${precorrelation.id}] ${request.method} ${requestPathMap[precorrelation.id] ?: path(request)} with body '${request.bodyAsString}' headers '${request.headers}'"
    }

    override fun format(correlation: Correlation, response: HttpResponse) =
        "${type.name.toLowerCase()} response [${correlation.id}] ${response.status} for ${requestPathMap.remove(correlation.id)} with body '${response.bodyAsString}' and headers '${response.headers}'. Duration ${correlation.duration.toMillis()} ms"

    private fun path(request: HttpRequest) = when(type) {
        Type.CLIENT -> request.requestUri
        Type.SERVER -> request.path + (request.query.takeIf(String::isNotEmpty)?.let { "?${it}" } ?: "")
    }
}