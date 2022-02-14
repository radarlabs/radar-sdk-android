package io.radar.sdk

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.json.JSONObject

internal class RadarApiHelperMock {

    var mockStatus: Radar.RadarStatus = Radar.RadarStatus.ERROR_UNKNOWN
    var mockResponse: JSONObject? = null
    val helper = mockk<RadarApiHelper>()

    init {
        val slot = slot<RadarApiRequest>()
        every {
            helper.request(capture(slot))
        } answers {
            slot.captured.callback?.onComplete(mockStatus, mockResponse)
        }
    }

}