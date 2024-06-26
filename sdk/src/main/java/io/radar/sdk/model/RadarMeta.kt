package io.radar.sdk.model

import io.radar.sdk.RadarTrackingOptions
import org.json.JSONObject

internal data class RadarMeta(
    val remoteTrackingOptions: RadarTrackingOptions?,
    val featureSettings: RadarFeatureSettings,
    val sdkConfiguration: RadarSDKConfiguration,
) {
    companion object {
        private const val TRACKING_OPTIONS = "trackingOptions"
        private const val FEATURE_SETTINGS = "featureSettings"
        private const val SDK_CONFIGURATION = "sdkConfiguration"

        fun fromJson(meta: JSONObject?): RadarMeta {
            val rawOptions = meta?.optJSONObject(TRACKING_OPTIONS)
            val rawFeatureSettings = meta?.optJSONObject(FEATURE_SETTINGS)
            val rawSdkConiguration = meta?.optJSONObject(SDK_CONFIGURATION)
            
            var trackingOptions: RadarTrackingOptions? = null
            if (rawOptions != null) {
                trackingOptions = RadarTrackingOptions.fromJson(rawOptions)
            }
            return RadarMeta(
                trackingOptions,  
                RadarFeatureSettings.fromJson(rawFeatureSettings),
                RadarSDKConfiguration.fromJson(rawSdkConiguration),
            )
        }
    }
}
