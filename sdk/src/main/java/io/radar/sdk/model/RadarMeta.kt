package io.radar.sdk.model

import io.radar.sdk.RadarTrackingOptions
import org.json.JSONObject

internal data class RadarMeta(
    val remoteTrackingOptions: RadarTrackingOptions?,
    val sdkConfiguration: RadarSdkConfiguration?,
) {
    companion object {
        private const val TRACKING_OPTIONS = "trackingOptions"
        private const val SDK_CONFIGURATION = "sdkConfiguration"

        fun fromJson(meta: JSONObject?): RadarMeta {
            val rawOptions = meta?.optJSONObject(TRACKING_OPTIONS)
            val rawSdkConfiguration = meta?.optJSONObject(SDK_CONFIGURATION)
            
            var trackingOptions: RadarTrackingOptions? = null
            if (rawOptions != null) {
                trackingOptions = RadarTrackingOptions.fromJson(rawOptions)
            }
            return RadarMeta(
                trackingOptions,
                RadarSdkConfiguration.fromJson(rawSdkConfiguration),
            )
        }
    }
}
