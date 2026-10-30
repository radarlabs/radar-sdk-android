package io.radar.sdk

import android.content.Context
import java.io.IOException

// HTTP instrumentation: attributes of client spans and `traceparent` propagation. Uses OpenTelemetry semantic
// convention names where they exist; those are marked below. `radar.*` names have no semconv equivalent.

/**
 * [headers] plus `traceparent` when [traceSpan]'s session is sampled. Read when the request runs, because a session can be
 * decided while its request waits in a queue.
 */
internal fun tracedHeaders(headers: Map<String, String>?, traceSpan: RadarTraceSpan?): Map<String, String>? {
    val traceparent = traceSpan?.traceparent() ?: return headers
    return (headers ?: emptyMap()) + ("traceparent" to traceparent)
}

/** Set when a request starts running. [queueMs] includes waiting behind earlier requests on its queue. */
internal fun RadarTraceSpan.requestStarted(context: Context, host: String, queueMs: Long) = recording {
    setAttribute("server.address", host) // OTel semconv
    setAttribute("network.connection.type", connectionType(context)) // OTel semconv
    setAttribute("radar.http.queue_ms", queueMs)
}

internal fun RadarTraceSpan.resendCount(count: Int) = apply {
    setAttribute("http.request.resend_count", count.toLong()) // OTel semconv
}

internal fun RadarTraceSpan.networkError(e: IOException) = apply {
    setAttribute("radar.network.error_kind", networkErrorKind(e))
}

/** OpenTelemetry `network.connection.type` values. */
private fun connectionType(context: Context): String = when (RadarUtils.getConnectionType(context)) {
    ConnectionType.WIFI -> "wifi"
    ConnectionType.CELLULAR -> "cell"
    ConnectionType.UNKNOWN -> "unknown"
}

private fun networkErrorKind(e: IOException): String = when (NetworkErrorKind.from(e)) {
    NetworkErrorKind.DNS_FAILURE -> "dns_failure"
    NetworkErrorKind.TIMEOUT -> "timeout"
    NetworkErrorKind.SSL_FAILURE -> "ssl_failure"
    NetworkErrorKind.CONNECT_REFUSED -> "connect_refused"
    NetworkErrorKind.IO_OTHER -> "other"
}
