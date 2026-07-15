package io.radar.sdk

import android.content.Context
import io.radar.sdk.Radar.RadarStatus
import io.radar.sdk.model.RadarRevealRiskToken

internal class RadarRevealRiskManager(
    private val context: Context,
    private val logger: RadarLogger
) {
    fun revealRisk(
        callback: (
            status: RadarStatus,
            result: RadarRevealRiskToken?
        ) -> Unit
    ) {
        RadarSDKFraud.getFraudPayload(context, logger) { status, fraudPayload ->
            if (status != RadarStatus.SUCCESS) {
                callback(RadarStatus.ERROR_PLUGIN, null)
                return@getFraudPayload
            }
            Radar.apiClient.revealRisk(
                fraudPayload,
                callback = callback
            )
        }
    }
}
