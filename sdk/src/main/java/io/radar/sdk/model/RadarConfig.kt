package io.radar.sdk.model

import org.json.JSONObject

/**
 * Remote configuration object
 */
internal data class RadarConfig(
    val meta: RadarMeta,
    val settings: RadarFeatureSettings
) {

    companion object {
        private const val META = "meta"
        private const val SETTINGS = "settings"

        fun fromJson(res: JSONObject?) = RadarConfig(
            RadarMeta.fromJson(res?.optJSONObject(META)),
            RadarFeatureSettings.fromJson(res?.optJSONObject(SETTINGS))
        )
    }
}