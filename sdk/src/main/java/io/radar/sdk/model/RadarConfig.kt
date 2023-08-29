package io.radar.sdk.model

import org.json.JSONObject

internal data class RadarConfig(
    val meta: RadarMeta,
    val googlePlayProjectNumber: Long?,
    val nonce: String?
) {

    companion object {
        private const val META = "meta"
        private const val GOOGLE_CLOUD_PROJECT_NUMBER = "googleCloudProjectNumber"
        private const val NONCE = "nonce"

        fun fromJson(res: JSONObject?) = RadarConfig(
            RadarMeta.fromJson(res?.optJSONObject(META)),
            res?.optLong(GOOGLE_CLOUD_PROJECT_NUMBER),
            res?.optString(NONCE)
        )
    }
}