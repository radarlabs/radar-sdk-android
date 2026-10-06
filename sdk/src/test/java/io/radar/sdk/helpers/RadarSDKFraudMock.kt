package io.radar.sdk.helpers

import android.content.Context
import android.location.Location
import io.radar.sdk.Radar
import io.radar.sdk.RadarLogger
import io.radar.sdk.RadarPreparedFraudPayload
import io.radar.sdk.RadarSDKFraud

internal class RadarSDKFraudMock : RadarSDKFraud() {

    internal var mockStatus: Radar.RadarStatus = Radar.RadarStatus.SUCCESS

    internal var mockPayload: String = """{"encv":1}"""

    internal var lastHandle: StubFraudHandle? = null

    class StubFraudHandle(private val payload: String) {
        val sealOptions = mutableListOf<Map<String, Any?>>()

        fun seal(options: Map<String, Any?>): Map<String, Any?> {
            sealOptions.add(options.toMap())
            return mapOf("payload" to payload)
        }
    }

    override fun prepareFraudPayload(
        context: Context,
        logger: RadarLogger,
        location: Location?,
        googlePlayProjectNumber: Long?,
        callback: (Radar.RadarStatus, RadarPreparedFraudPayload?) -> Unit
    ) {
        val handle = StubFraudHandle(mockPayload)
        lastHandle = handle
        callback(mockStatus, RadarPreparedFraudPayload(handle))
    }
}
