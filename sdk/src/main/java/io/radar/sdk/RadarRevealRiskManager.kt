package io.radar.sdk

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import io.radar.sdk.Radar.RadarStatus
import io.radar.sdk.model.RadarRevealRiskToken

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
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
