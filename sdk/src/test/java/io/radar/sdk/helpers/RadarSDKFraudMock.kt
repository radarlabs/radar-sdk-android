package io.radar.sdk.helpers

import android.content.Context
import android.location.Location
import io.radar.sdk.Radar
import io.radar.sdk.RadarLogger
import io.radar.sdk.RadarPreparedFraudPayload
import io.radar.sdk.RadarSDKFraud

internal class RadarSDKFraudMock : RadarSDKFraud() {

    internal var mockStatus: Radar.RadarStatus = Radar.RadarStatus.SUCCESS

    internal var mockPayload: String = "mock-fraud-payload"

    class StubFraudHandle(private val payload: String) {
        fun seal(options: Map<String, Any?>): Map<String, Any?> = mapOf("payload" to payload)
    }

    override fun prepareFraudPayload(
        context: Context,
        logger: RadarLogger,
        location: Location?,
        googlePlayProjectNumber: Long?,
        callback: (Radar.RadarStatus, RadarPreparedFraudPayload?) -> Unit
    ) {
        callback(mockStatus, RadarPreparedFraudPayload(StubFraudHandle(mockPayload)))
    }
}
