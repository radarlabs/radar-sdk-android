package io.radar.sdk

import org.json.JSONObject

/**
 * An options class used for device verification
 *
 * @see [](https://radar.io/documentation/sdk/android)
 */
data class RadarVerificationSettings(
    /**
     * A server side generated nonce
     */
    var nonce: String,

    /**
     * The GCP Project Number used to generate the integrity token
     */
    var projectNumber: Long,

    /**
     * The integrity token returned by the Integrity API
     */
    var integrityToken: String,

) {

    companion object {

        internal const val KEY_NONCE = "nonce"
        internal const val KEY_PROJECT_NUMBER = "projectNumber"
        internal const val KEY_INTEGRITY_TOKEN = "integrityToken"

        @JvmStatic
        fun fromJson(obj: JSONObject): RadarVerificationSettings {
            return RadarVerificationSettings(
                nonce = obj.optString(KEY_NONCE),
                projectNumber = obj.optLong(KEY_PROJECT_NUMBER),
                integrityToken = obj.optString(KEY_INTEGRITY_TOKEN),
            )
        }

    }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put(KEY_NONCE, nonce)
        obj.put(KEY_PROJECT_NUMBER, projectNumber)
        obj.put(KEY_INTEGRITY_TOKEN, integrityToken)
        return obj
    }

}