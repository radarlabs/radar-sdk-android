package io.radar.sdk.model

import io.radar.sdk.RadarTrackingOptions
import io.radar.sdk.RadarVerificationSettings
import org.json.JSONObject

/**
 * Defines Metadata that is used by the Radar SDK
 */
internal data class RadarMeta(
    val remoteTrackingOptions: RadarTrackingOptions?,
    val remoteVerificationOptions: RadarVerificationSettings?
) {
    companion object {
        private const val TRACKING_OPTIONS = "trackingOptions"
        private const val VERIFICATION_SETTINGS = "verificationSettings"

        fun fromJson(meta: JSONObject?): RadarMeta {
            val tOptions = meta?.optJSONObject(TRACKING_OPTIONS)
            val vOptions = meta?.optJSONObject(VERIFICATION_SETTINGS)
            return RadarMeta(tOptions?.let { RadarTrackingOptions.fromJson(it) },
                vOptions?.let { RadarVerificationSettings.fromJson(it) })
        }
    }
}
