package io.radar.sdk.model

import io.radar.sdk.RadarTrackingOptions
import org.json.JSONObject

internal data class RadarMeta(
    val remoteTrackingOptions: RadarTrackingOptions?,
    val featureSettings: RadarFeatureSettings,
) {
    companion object {
        private const val TRACKING_OPTIONS = "trackingOptions"
        private const val FEATURE_SETTINGS = "featureSettings"

        fun fromJson(meta: JSONObject?): RadarMeta {
            val rawOptions = meta?.optJSONObject(TRACKING_OPTIONS)
            val rawFeatureSettings = meta?.optJSONObject(FEATURE_SETTINGS)

            return if (rawOptions == null) {
                RadarMeta(null, RadarFeatureSettings.fromJson(rawFeatureSettings))
            } else {
                RadarMeta(RadarTrackingOptions.fromJson(rawOptions),  RadarFeatureSettings.fromJson(rawFeatureSettings))
            }
        }
    }
}
