package io.radar.sdk.model

import org.json.JSONObject

/**
 * Represents fraud detection signals for location verification.
 *
 * Note that these values should not be trusted unless you called `trackVerified()` instead of `trackOnce()`.
 *
 * @see [](https://radar.com/documentation/fraud)
 */
data class RadarFraud(
    /**
     * A boolean indicating whether the user's IP address is a known proxy. May be `false` if Fraud is not enabled.
     */
    val proxy: Boolean,

    /**
     * A boolean indicating whether the user's location is being mocked, such as in a simulator or using a location spoofing app. May be
     * `false` if Fraud is not enabled.
     */
    val mocked: Boolean,

    /**
     * A boolean indicating whether the user's device has been compromised according to the Play Integrity API. May be `false` if Fraud is not enabled.
     *
     * @see [](https://developer.android.com/google/play/integrity/overview)
     */
    val compromised: Boolean,

    /**
     * A boolean indicating whether the user moved too far too fast. May be `false` if Fraud is not enabled.
     */
    val jumped: Boolean
) {
    companion object {
        private const val PROXY = "proxy"
        private const val MOCKED = "mocked"
        private const val COMPROMISED = "compromised"
        private const val JUMPED = "jumped"

        @JvmStatic
        fun fromJson(json: JSONObject?): RadarFraud {
            return RadarFraud(
                proxy = json?.optBoolean(PROXY, false) ?: false,
                mocked = json?.optBoolean(MOCKED, false) ?: false,
                compromised = json?.optBoolean(COMPROMISED, false) ?: false,
                jumped = json?.optBoolean(JUMPED, false) ?: false
            )
        }
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            putOpt(PROXY, proxy)
            putOpt(MOCKED, mocked)
            putOpt(COMPROMISED, compromised)
            putOpt(JUMPED, jumped)
        }
    }
}
