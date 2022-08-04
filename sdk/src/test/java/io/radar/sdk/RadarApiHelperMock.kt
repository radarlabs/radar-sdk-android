package io.radar.sdk

import android.content.Context
import org.json.JSONObject
import java.net.URL

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
    internal var mockResponses: MutableMap<URL, JSONObject> = mutableMapOf()

    override fun request(context: Context,
                         method: String,
                         url: URL,
                         headers: Map<String, String>?,
                         params: JSONObject?,
                         sleep: Boolean,
                         callback: RadarApiCallback?,
                         stream: Boolean,
                         logPayload: Boolean) {
        // Use the entry in the mockResponses map, if any.
        if (mockResponses.containsKey(url)) {
            callback?.onComplete(mockStatus, mockResponses[url])
        } else {
            callback?.onComplete(mockStatus, mockResponse)
        }
    }

    /**
     * Set the expected response for a requested URL. This is useful when a `RadarApiHelperMock`
     * instance makes multiple requests in sequence, like when testing `Radar.sendEvent()`, which
     * first calls `/v1/track`, and then `/v1/event`.
     */
    fun addMockResponse(url: URL, response: JSONObject) {
        mockResponses[url] = response
    }

}