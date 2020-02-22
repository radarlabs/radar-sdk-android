package io.radar.sdk

import android.content.Context
import org.json.JSONObject
import java.net.URL

internal class RadarApiHelperMock : RadarApiHelper() {

    internal var mockStatus: Radar.RadarStatus = Radar.RadarStatus.ERROR_UNKNOWN
    internal var mockResponse: JSONObject? = null

    override fun request(context: Context,
                         method: String,
                         url: URL,
                         headers: Map<String, String>?,
                         params: JSONObject?,
                         callback: RadarApiCallback?) {
        callback?.onComplete(mockStatus, mockResponse)
    }

}