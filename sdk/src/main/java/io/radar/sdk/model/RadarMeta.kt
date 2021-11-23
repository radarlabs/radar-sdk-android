package io.radar.sdk.model

import io.radar.sdk.RadarTrackingOptions
import org.json.JSONObject

data class RadarMeta(
    val config: JSONObject?,
    val remoteTrackingOptions: RadarTrackingOptions?
) {
    companion object {
        fun parse(res: JSONObject?): RadarMeta {
            val meta: JSONObject? = res?.optJSONObject("meta")

            val config: JSONObject? = meta?.optJSONObject("config")
            val rawOptions = meta?.optJSONObject("trackingOptions")

            var remoteTrackingOptions: RadarTrackingOptions? = null
            if (rawOptions != null) {
                remoteTrackingOptions = RadarTrackingOptions.fromJson(rawOptions)
            }

            return RadarMeta(config, remoteTrackingOptions)
        }
    }
}

