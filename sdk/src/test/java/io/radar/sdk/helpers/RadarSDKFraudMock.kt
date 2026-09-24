package io.radar.sdk.helpers

import android.content.Context
import android.location.Location
import io.radar.sdk.Radar
import io.radar.sdk.RadarLogger
import io.radar.sdk.RadarSDKFraud

internal class RadarSDKFraudMock : RadarSDKFraud() {

    internal var mockStatus: Radar.RadarStatus = Radar.RadarStatus.SUCCESS

    internal var mockPayload: String = "mock-fraud-payload"

    override fun getFraudPayload(
        context: Context,
        logger: RadarLogger,
        location: Location?,
        googlePlayProjectNumber: Long?,
        callback: (Radar.RadarStatus, String) -> Unit
    ) {
        callback(mockStatus, mockPayload)
    }
}
