package io.radar.sdk

import android.content.Context
import org.json.JSONObject

internal class RadarApiHelperMock : RadarApiHelper() {

    internal var mockStatus: Radar.RadarStatus = Radar.RadarStatus.ERROR_UNKNOWN

    /**
     * The JSON data that's expected to be returned by a request.
     */
    internal var mockResponse: JSONObject? = null

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
        verified: Boolean
    ) {
        if (path != "v1/logs") {
            lastCapturedPath = path
            lastCapturedMethod = method
            lastCapturedParams = params
        }

        if (mockResponses.containsKey(path)) {
            callback?.onComplete(mockStatus, mockResponses[path])
        } else {
            callback?.onComplete(mockStatus, mockResponse)
        }
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
    }

}