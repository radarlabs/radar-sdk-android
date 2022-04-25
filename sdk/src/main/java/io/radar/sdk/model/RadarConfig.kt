package io.radar.sdk.model

import org.json.JSONObject

/**
 * Remote configuration object
 */
internal data class RadarConfig(
    val meta: RadarMeta,
    val featureSettings: RadarFeatureSettings
) {

    companion object {
        private const val META = "meta"
        private const val FEATURE_SETTINGS = "settings"

        fun fromJson(res: JSONObject?) = RadarConfig(
            RadarMeta.fromJson(res?.optJSONObject(META)),
            RadarFeatureSettings.fromJson(res?.optJSONObject(FEATURE_SETTINGS))
        )
    }
}