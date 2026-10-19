package io.radar.sdk.helpers

import android.content.Context
import io.radar.sdk.Radar
import io.radar.sdk.RadarApiHelper
import io.radar.sdk.RadarRequestQueue
import io.radar.sdk.RadarSettings
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONObject

internal class RadarApiHelperMock : RadarApiHelper() {

    internal var mockStatus: Radar.RadarStatus = Radar.RadarStatus.ERROR_UNKNOWN

    /**
     * The JSON data that's expected to be returned by a request.
     */
    internal var mockResponse: JSONObject? = null

    internal var mockThrowable: Throwable? = null

    /**
     * The JSON responses that are expected to be returned by various URL requests. Call
     * `setMockResponse()` to associate a response with a URL.
     */
    internal var mockResponses: MutableMap<String, JSONObject> = mutableMapOf()

    /**
     * Captured parameters from the last request for testing purposes.
     */
    internal var lastCapturedParams: JSONObject? = null
    internal var lastCapturedPath: String? = null
    internal var lastCapturedMethod: String? = null
    internal var lastCapturedVerified: Boolean = false
    internal var lastCapturedVerifiedHostOverride: String? = null
    internal var lastUrl: String? = null

    /**
     * Paths of every request since the last `clearCapturedParams()`, in order.
     */
    internal val capturedPaths = mutableListOf<String>()

    /**
     * Sequential responses to return for repeated requests against the same path. When set,
     * each call to `request()` for that path consumes the head of the queue. Useful for
     * exercising failover flows where the primary and retry hit the same path.
     *
     * Concurrent because trace uploads read the queues from the trace manager's disk thread while
     * tests modify them.
     */
    internal var mockResponseQueues: MutableMap<String, ArrayDeque<JSONObject?>> = ConcurrentHashMap()

    /**
     * Sequential statuses to return for repeated requests against the same path. When a path's queue
     * is empty, `mockStatus` is returned. Concurrent for the same reason as `mockResponseQueues`.
     */
    internal var mockStatusQueues: MutableMap<String, ArrayDeque<Radar.RadarStatus>> = ConcurrentHashMap()

    /**
     * Payloads of every `v1/traces` request. Trace uploads are started from the trace manager's disk
     * thread at times tests don't control, so they are kept out of the `lastCaptured*` fields to avoid
     * overwriting values tests assert on. Synchronized because that thread writes while tests read.
     */
    internal val capturedTracePayloads: MutableList<JSONObject> = Collections.synchronizedList(mutableListOf())
    internal var lastCapturedTraceHeaders: Map<String, String>? = null
    internal var lastCapturedTraceQueue: RadarRequestQueue? = null

    override fun request(
        context: Context,
        method: String,
        path: String,
        headers: Map<String, String>?,
        params: JSONObject?,
        sleep: Boolean,
        callback: RadarApiCallback?,
        extendedTimeout: Boolean,
        stream: Boolean,
        logPayload: Boolean,
        verified: Boolean,
        imageCallback: RadarImageApiCallback?,
        verifiedHostOverride: String?,
        prepareRequest: (() -> JSONObject)?,
        preparationFailureHandler: ((Exception) -> Unit)?,
        queue: RadarRequestQueue
    ) {
        val requestParams = try {
            prepareRequest?.invoke() ?: params
        } catch (e: Exception) {
            if (preparationFailureHandler != null) {
                preparationFailureHandler(e)
            } else {
                callback?.onComplete(Radar.RadarStatus.ERROR_BAD_REQUEST, throwable = e)
            }
            return
        }

        if (path == "v1/traces") {
            requestParams?.let { capturedTracePayloads.add(it) }
            lastCapturedTraceHeaders = headers
            lastCapturedTraceQueue = queue
        }
        if (path != "v1/logs" && path != "v1/traces") {
            val host = if (verified) {
                verifiedHostOverride ?: RadarSettings.getVerifiedHost(context)
            } else {
                RadarSettings.getHost(context)
            }
            lastUrl = "$host/$path"
            capturedPaths.add(path)
            lastCapturedPath = path
            lastCapturedMethod = method
            lastCapturedParams = requestParams
            lastCapturedVerified = verified
            lastCapturedVerifiedHostOverride = verifiedHostOverride
        }

        val status = mockStatusQueues.entries.firstOrNull { path.startsWith(it.key) }?.value?.removeFirstOrNull() ?: mockStatus

        val queueKey = mockResponseQueues.keys.firstOrNull { path.startsWith(it) }
        if (queueKey != null) {
            val responses = mockResponseQueues[queueKey]!!
            val head = if (responses.isNotEmpty()) responses.removeFirst() else null
            callback?.onComplete(status, head, mockThrowable)
            return
        }

        if (mockResponses.containsKey(path)) {
            callback?.onComplete(status, mockResponses[path], mockThrowable)
        } else {
            callback?.onComplete(status, mockResponse, mockThrowable)
        }
    }

    /**
     * All spans uploaded to `v1/traces`, flattened across requests.
     */
    fun capturedTraceSpans(): List<JSONObject> = synchronized(capturedTracePayloads) { capturedTracePayloads.toList() }.flatMap { payload ->
        val resourceSpans = payload.getJSONArray("resourceSpans")
        (0 until resourceSpans.length()).flatMap { r ->
            val scopeSpans = resourceSpans.getJSONObject(r).getJSONArray("scopeSpans")
            (0 until scopeSpans.length()).flatMap { s ->
                val spans = scopeSpans.getJSONObject(s).getJSONArray("spans")
                (0 until spans.length()).map { spans.getJSONObject(it) }
            }
        }
    }

    fun queueMockStatuses(path: String, statuses: List<Radar.RadarStatus>) {
        mockStatusQueues[path] = ArrayDeque(statuses)
    }

    fun queueMockResponses(path: String, responses: List<JSONObject?>) {
        mockResponseQueues[path] = ArrayDeque(responses)
    }

    /**
     * Set the expected response for a requested URL. This is useful when a `RadarApiHelperMock`
     * instance makes multiple requests in sequence, like when testing `Radar.sendEvent()`, which
     * first calls `/v1/track`, and then `/v1/event`.
     */
    fun addMockResponse(path: String, response: JSONObject) {
        mockResponses[path] = response
    }

    /**
     * Clear captured parameters from previous requests.
     */
    fun clearCapturedParams() {
        lastCapturedParams = null
        lastCapturedPath = null
        lastCapturedMethod = null
        capturedPaths.clear()
        capturedTracePayloads.clear()
        lastCapturedTraceHeaders = null
        lastCapturedTraceQueue = null
    }
}
