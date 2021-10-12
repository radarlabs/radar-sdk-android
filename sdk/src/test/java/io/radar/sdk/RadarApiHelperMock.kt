package io.radar.sdk

import org.json.JSONObject

internal class RadarApiHelperMock : RadarApiHelper() {

    internal var mockStatus: Radar.RadarStatus = Radar.RadarStatus.ERROR_UNKNOWN
    internal var mockResponse: JSONObject? = null

    override fun request(request: RadarApiRequest) {
        request.callback?.onComplete(mockStatus, mockResponse)
    }

}