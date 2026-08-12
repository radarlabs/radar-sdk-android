package io.radar.sdk

import android.content.Context
import io.radar.sdk.Radar.RadarStatus
import io.radar.sdk.model.RadarRevealRiskToken

internal class RadarRevealRiskManager(
    private val context: Context,
    private val logger: RadarLogger
) {
    private var revealRiskId: String? = null

    fun setRevealRiskId(revealRiskId: String?) {
        this.revealRiskId = revealRiskId
    }

    fun getRevealRiskId(): String? = revealRiskId

    fun clearRevealRiskId() {
        this.revealRiskId = null
    }

    fun revealRisk(
        callback: (
            status: RadarStatus,
            result: RadarRevealRiskToken?
        ) -> Unit
    ) {
        RadarSDKFraud.getFraudPayload(context, logger) { status, fraudPayload ->
            if (status != RadarStatus.SUCCESS) {
                callback(status, null)
                return@getFraudPayload
            }

            Radar.apiClient.revealRisk(
                fraudPayload,
                callback = { status, token ->
                    run {
                        setRevealRiskId(token?.id)
                        callback(status, token)
                    }
                }
            )
        }
    }
}
