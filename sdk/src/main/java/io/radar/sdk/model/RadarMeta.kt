package io.radar.sdk.model

import io.radar.sdk.RadarTrackingOptions
import org.json.JSONObject

/**
 * Defines Metadata that is used by the Radar SDK
 */
internal data class RadarMeta(
    val remoteTrackingOptions: RadarTrackingOptions?
) {
    companion object {
        private const val TRACKING_OPTIONS = "trackingOptions"

        fun fromJson(meta: JSONObject?): RadarMeta {
            val rawOptions = meta?.optJSONObject(TRACKING_OPTIONS)

            return if (rawOptions == null) {
                RadarMeta(null)
            } else {
                RadarMeta(RadarTrackingOptions.fromJson(rawOptions))
            }
        }
    }
}
