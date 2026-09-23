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
        RadarSDKFraud.prepareFraudPayload(context, logger) { status, preparedPayload ->
            if (status != RadarStatus.SUCCESS || preparedPayload == null) {
                callback(
                    if (status == RadarStatus.SUCCESS) RadarStatus.ERROR_PLUGIN else status,
                    null
                )
                return@prepareFraudPayload
            }

            Radar.apiClient.revealRisk(
                preparedFraudPayload = preparedPayload,
                callback = { apiStatus, token ->
                    setRevealRiskId(token?.id)
                    callback(apiStatus, token)
                }
            )
        }
    }
}
