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
     * A boolean indicating whether the user passed fraud detection checks. May be `false` if Fraud is not enabled.
     */
    val passed: Boolean,

    /**
     * A boolean indicating whether fraud detection checks were bypassed for the user for testing. May be `false` if Fraud is not enabled.
     */
    val bypassed: Boolean,

    /**
     * A boolean indicating whether the request was made with SSL pinning configured successfully. May be `false` if Fraud is not enabled.
     */
    val verified: Boolean,

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
    val jumped: Boolean,

    /**
     * A boolean indicating whether the user is screen sharing. May be `false` if Fraud is not enabled.
     */
    val sharing: Boolean,

    /**
     * A boolean indicating whether the user's location is not accurate enough. May be `false` if Fraud is not enabled.
     */
    val inaccurate: Boolean
) {
    companion object {
        private const val PASSED = "passed"
        private const val BYPASSED = "bypassed"
        private const val VERIFIED = "verified"
        private const val PROXY = "proxy"
        private const val MOCKED = "mocked"
        private const val COMPROMISED = "compromised"
        private const val JUMPED = "jumped"
        private const val SHARING = "sharing"
        private const val INACCURATE = "inaccurate"

        @JvmStatic
        fun fromJson(json: JSONObject?): RadarFraud {
            return RadarFraud(
                passed = json?.optBoolean(PASSED, false) ?: false,
                bypassed = json?.optBoolean(BYPASSED, false) ?: false,
                verified = json?.optBoolean(VERIFIED, false) ?: false,
                proxy = json?.optBoolean(PROXY, false) ?: false,
                mocked = json?.optBoolean(MOCKED, false) ?: false,
                compromised = json?.optBoolean(COMPROMISED, false) ?: false,
                jumped = json?.optBoolean(JUMPED, false) ?: false,
                sharing = json?.optBoolean(SHARING, false) ?: false,
                inaccurate = json?.optBoolean(INACCURATE, false) ?: false
            )
        }
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            putOpt(PASSED, passed)
            putOpt(BYPASSED, bypassed)
            putOpt(VERIFIED, verified)
            putOpt(PROXY, proxy)
            putOpt(MOCKED, mocked)
            putOpt(COMPROMISED, compromised)
            putOpt(JUMPED, jumped)
            putOpt(SHARING, sharing)
            putOpt(INACCURATE, inaccurate)
        }
    }
}
