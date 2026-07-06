package io.radar.sdk.model

import io.radar.sdk.RadarUtils
import java.util.Date
import org.json.JSONObject

/**
 * Represents a user's reveal risk information.
 *
 * @see [](https://radar.com/documentation/fraud)
 */
class RadarRevealRiskToken(
    /**
     * A signed JSON Web Token (JWT) containing the user and array of events. Verify the token server-side using your secret key.
     */
    val token: String,

    /**
     * The datetime when the token expires.
     */
    val expiresAt: Date,

    /**
     * The number of seconds until the token expires.
     */
    val expiresIn: Int,

    /**
     * A boolean indicating whether the user passed all jurisdiction and fraud detection checks.
     */
    val passed: Boolean,

    /**
     * An array of failure reasons for jurisdiction and fraud detection checks.
     */
    val failureReasons: Array<String>,

    /**
     * The Radar ID of the reveal risk check.
     */
    val _id: String,

    /**
     * The full JSON value of the token.
     */
    val fullJson: JSONObject
) {
    internal companion object {
        private const val FIELD_TOKEN = "token"
        private const val FIELD_EXPIRES_AT = "expiresAt"
        private const val FIELD_EXPIRES_IN = "expiresIn"
        private const val FIELD_PASSED = "passed"
        private const val FIELD_FAILURE_REASONS = "failureReasons"
        private const val FIELD_ID = "_id"

        fun fromJson(obj: JSONObject?): RadarRevealRiskToken? {
            if (obj == null) {
                return null
            }

            val token: String? = obj.optString(FIELD_TOKEN)
            val expiresAt: Date? = RadarUtils.isoStringToDate(obj.optString(FIELD_EXPIRES_AT))
            val expiresIn: Int = obj.optInt(FIELD_EXPIRES_IN)
            val passed: Boolean = obj.optBoolean(FIELD_PASSED)
            val failureReasons = obj.optJSONArray(FIELD_FAILURE_REASONS)?.let { failureReasons ->
                Array<String>(failureReasons.length()) {
                    failureReasons.optString(it)
                }
            } ?: emptyArray()
            val id = obj.optString(FIELD_ID) ?: ""

            if (token == null || expiresAt == null) {
                return null
            }

            return RadarRevealRiskToken(token, expiresAt, expiresIn, passed, failureReasons, id, obj)
        }
    }

    fun toJson(): JSONObject = fullJson
}
