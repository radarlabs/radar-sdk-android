package io.radar.sdk

import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import java.util.Random

/**
 * A deliberately small, dependency-free span. The manager owns sampling and export.
 *
 * Follows the OpenTelemetry trace data model and OTLP/JSON encoding for the fields it supports (IDs, parent,
 * kind, timestamps, status, typed attributes) and propagates context as a W3C `traceparent`. It is not an
 * implementation of the OTel API, and does not support span events, links, `tracestate`, or array
 * attributes. OTel spans and traces: https://opentelemetry.io/docs/specs/otel/trace/api/#span
 *
 * Safe for concurrent use, like OTel spans: attribute updates and [end] are serialized on one lock, and the
 * span records a snapshot of its attributes.
 *
 * `manager` is null only for the shared no-op span returned when there is no trace manager, no publishable
 * key, or the session is not sampled. That span records nothing and propagates no context.
 *
 * A span started before the server has decided whether its session is sampled still records, so its trace
 * can be uploaded if the decision is yes.
 *
 * A trace's `sessionId` is the session the trace started in. All child spans inherit that session Id.
 */
internal class RadarTraceSpan private constructor(
    val name: String,
    val traceId: String,
    val spanId: String,
    val parentSpanId: String?,
    private val attributes: MutableMap<String, Any>,
    private val manager: RadarTraceManager?,
    private val project: String?,
    private val sessionId: String,
    private val kind: RadarSpanKind
) {
    private val startedAtElapsedMs = SystemClock.elapsedRealtime()
    private val startedAtUnixMs = wallClockMs()
    private val lock = Any()

    // Guarded by lock, along with attributes.
    private var ended = false

    fun setAttribute(key: String, value: String) = putAttribute(key, value)

    fun setAttribute(key: String, value: Long) = putAttribute(key, value)

    fun setAttribute(key: String, value: Double) = putAttribute(key, value)

    /** Ignored on no-op spans and after [end]. */
    private fun putAttribute(key: String, value: Any) {
        if (manager == null) {
            return
        }
        synchronized(lock) {
            if (!ended) {
                attributes[key] = value
            }
        }
    }

    /** Starts a child span in this trace. Children of a no-op span are the same no-op span. */
    fun child(name: String, kind: RadarSpanKind = RadarSpanKind.INTERNAL): RadarTraceSpan = manager?.let {
        RadarTraceSpan(name, traceId, newSpanId(), spanId, mutableMapOf(), manager, project, sessionId, kind)
    } ?: this

    /**
     * The W3C `traceparent` header for requests made in this span: `version-traceId-parentId-flags`, where flags
     * 01 means sampled. Null unless the session is sampled: while the decision is pending there is no header.
     * https://www.w3.org/TR/trace-context/#traceparent-header
     */
    fun traceparent(): String? = if (manager?.samplingState(sessionId) == RadarTraceManager.SamplingState.SAMPLED) "00-$traceId-$spanId-01" else null

    fun end(status: Radar.RadarStatus) {
        if (manager == null) {
            return
        }
        val snapshot = synchronized(lock) {
            if (ended) {
                return
            }
            ended = true
            attributes.toMap()
        }
        val elapsedMs = SystemClock.elapsedRealtime() - startedAtElapsedMs
        val completed = RadarCompletedSpan(
            name,
            traceId,
            spanId,
            parentSpanId,
            kind,
            startedAtUnixMs * 1_000_000,
            (startedAtUnixMs + elapsedMs) * 1_000_000,
            status,
            snapshot
        )
        manager.record(completed, project, sessionId)
    }

    companion object {
        /**
         * Wall clock for span start times, replaceable so tests can assert exact timestamps. Declared before
         * [NOOP], which reads it during companion initialization.
         */
        @VisibleForTesting
        internal var wallClockMs: () -> Long = { System.currentTimeMillis() }

        /** Shared span returned when tracing is off. It records nothing, so one instance serves every caller. */
        private val NOOP = RadarTraceSpan("", "", "", null, mutableMapOf(), null, null, "", RadarSpanKind.INTERNAL)

        fun start(name: String, attributes: Map<String, Any> = emptyMap()): RadarTraceSpan {
            val manager = Radar.traceManager
            val project = manager?.currentProjectFingerprint()
            val sessionId = manager?.currentSessionId()
            if (manager == null || project == null || sessionId == null ||
                manager.samplingState(sessionId) == RadarTraceManager.SamplingState.NOT_SAMPLED
            ) {
                return NOOP
            }
            return RadarTraceSpan(name, newTraceId(), newSpanId(), null, attributes.toMutableMap(), manager, project, sessionId, RadarSpanKind.INTERNAL)
        }

        private val random = Random()

        /**
         * A random, non-zero 8-byte span ID as 16 lowercase hex characters. Inspired by the OpenTelemetry Java
         * SDK's RandomIdGenerator, which opentelemetry-android uses:
         * https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk/trace/src/main/java/io/opentelemetry/sdk/trace/RandomIdGenerator.java
         * The W3C format, where the span ID is called parent-id: https://www.w3.org/TR/trace-context/#parent-id
         */
        private fun newSpanId(): String = hex(nonZeroLong())

        /**
         * A random, non-zero 16-byte trace ID as 32 lowercase hex characters. See [newSpanId].
         * https://www.w3.org/TR/trace-context/#trace-id
         */
        private fun newTraceId(): String = hex(random.nextLong()) + hex(nonZeroLong())

        /** All-zero span and trace IDs are invalid in W3C Trace Context. */
        private fun nonZeroLong(): Long {
            var id: Long
            do {
                id = random.nextLong()
            } while (id == 0L)
            return id
        }

        /** 16 lowercase hex characters, zero-padded. */
        private fun hex(value: Long): String = java.lang.Long.toHexString(value).padStart(16, '0')
    }
}
