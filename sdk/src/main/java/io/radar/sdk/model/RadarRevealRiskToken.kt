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
     * A String indicating the current fraud level, none, low, medium, or high
     */
    val level: String,

    /**
     * A double indicating the fraud score, this is a number from 0 to 100
     */
    val score: Double,

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
        private const val FIELD_FRAUD_LEVEL = "level"
        private const val FIELD_FRAUD_SCORE = "score"
        private const val FIELD_FAILURE_REASONS = "failureReasons"
        private const val FIELD_ID = "_id"

        fun fromJson(obj: JSONObject?): RadarRevealRiskToken? {
            if (obj == null) {
                return null
            }

            val token: String? = obj.optString(FIELD_TOKEN)
            val level: String = obj.optString(FIELD_FRAUD_LEVEL)
            val score: Double = obj.optDouble(FIELD_FRAUD_SCORE)
            val failureReasons = obj.optJSONArray(FIELD_FAILURE_REASONS)?.let { failureReasons ->
                Array<String>(failureReasons.length()) {
                    failureReasons.optString(it)
                }
            } ?: emptyArray()
            val id = obj.optString(FIELD_ID) ?: ""

            if (token == null) {
                return null
            }

            return RadarRevealRiskToken(token, level, score, failureReasons, id, obj)
        }
    }

    fun toJson(): JSONObject = fullJson
}
