package io.radar.sdk.model

import org.json.JSONObject

/**
 * Remote configuration object
 */
internal data class RadarConfig(
    val meta: RadarMeta,
    val featureSettings: RadarFeatureSettings,
    val serverPublicKey: String?,
    val googlePlayProjectNumber: Long?,
    val nonce: String?
) {

    companion object {
        private const val META = "meta"
        private const val FEATURE_SETTINGS = "settings"
        private const val SERVER_PUBLIC_KEY = "serverPublicKey"
        private const val GOOGLE_CLOUD_PROJECT_NUMBER = "googleCloudProjectNumber"
        private const val NONCE = "nonce"

        fun fromJson(res: JSONObject?) = RadarConfig(
            RadarMeta.fromJson(res?.optJSONObject(META)),
            RadarFeatureSettings.fromJson(res?.optJSONObject(FEATURE_SETTINGS)),
            res?.optString(SERVER_PUBLIC_KEY),
            res?.optLong(GOOGLE_CLOUD_PROJECT_NUMBER),
            res?.optString(NONCE)
        )
    }
}