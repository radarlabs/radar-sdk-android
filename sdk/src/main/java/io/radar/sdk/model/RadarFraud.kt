package io.radar.sdk.model

import io.radar.sdk.RadarUtils
import org.json.JSONObject
import java.util.Date

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
    val inaccurate: Boolean,

    /**
     * A boolean indicating whether the user has been manually blocked. May be `false` if Fraud is not enabled.
     */
    val blocked: Boolean,

    /**
     A timestamp indicating the last time that the user failed the mocked fraud check.
    */
    val lastMockedAt: Date?,

    /**
     A timestamp indicating the last time that the user failed the jumped fraud check.
    */
    val lastJumpedAt: Date?,

    /**
     A timestamp indicating the last time that the user failed the compromised fraud check.
    */
    val lastCompromisedAt: Date?,

    /**
     A timestamp indicating the last time that the user failed the inaccurate fraud check.
    */
    val lastInaccurateAt: Date?,

    /**
     A timestamp indicating the last time that the user failed the proxy fraud check.
    */
    val lastProxyAt: Date?,

    /**
     A timestamp indicating the last time that the user failed the sharing fraud check.
    */
    val lastSharingAt: Date?,
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
        private const val BLOCKED = "blocked"
        private const val LAST_MOCKED_AT = "lastMockedAt"
        private const val LAST_JUMPED_AT = "lastJumpedAt"
        private const val LAST_COMPROMISED_AT = "lastCompromisedAt"
        private const val LAST_INACCURATE_AT = "lastInaccurateAt"
        private const val LAST_PROXY_AT = "lastProxyAt"
        private const val LAST_SHARING_AT = "lastSharingAt"

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
                inaccurate = json?.optBoolean(INACCURATE, false) ?: false,
                blocked = json?.optBoolean(BLOCKED, false) ?: false,
                lastMockedAt = RadarUtils.isoStringToDate(json?.optString(LAST_MOCKED_AT)),
                lastJumpedAt = RadarUtils.isoStringToDate(json?.optString(LAST_JUMPED_AT)),
                lastCompromisedAt = RadarUtils.isoStringToDate(json?.optString(LAST_COMPROMISED_AT)),
                lastInaccurateAt = RadarUtils.isoStringToDate(json?.optString(LAST_INACCURATE_AT)),
                lastProxyAt = RadarUtils.isoStringToDate(json?.optString(LAST_PROXY_AT)),
                lastSharingAt = RadarUtils.isoStringToDate(json?.optString(LAST_SHARING_AT)),
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
            putOpt(BLOCKED, blocked)
            putOpt(LAST_MOCKED_AT, RadarUtils.dateToISOString(lastMockedAt))
            putOpt(LAST_JUMPED_AT, RadarUtils.dateToISOString(lastJumpedAt))
            putOpt(LAST_COMPROMISED_AT, RadarUtils.dateToISOString(lastCompromisedAt))
            putOpt(LAST_INACCURATE_AT, RadarUtils.dateToISOString(lastInaccurateAt))
            putOpt(LAST_PROXY_AT, RadarUtils.dateToISOString(lastProxyAt))
            putOpt(LAST_SHARING_AT, RadarUtils.dateToISOString(lastSharingAt))
        }
    }
}
