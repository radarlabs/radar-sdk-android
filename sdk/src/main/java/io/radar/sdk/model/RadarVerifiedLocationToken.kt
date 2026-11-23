package io.radar.sdk.model

import io.radar.sdk.RadarUtils
import java.util.Date
import org.json.JSONObject

/**
 * Represents a user's verified location.
 *
 * @see [](https://radar.com/documentation/fraud)
 */
class RadarVerifiedLocationToken @JvmOverloads constructor(
    /** The user. Available for JWS responses. */
    val user: RadarUser?,

    /** The events. Available for JWS responses. */
    val events: Array<RadarEvent>?,

    /**
     * The response token. Relay it to the backend for verification.
     * JWE tokens must first be decrypted by the backend.
     */
    val token: String?,

    /** The token expiration datetime. Unavailable for ID responses. */
    val expiresAt: Date?,

    /** The token lifetime in seconds. Zero for ID responses. */
    val expiresIn: Int,

    /**
     * Whether the evaluation passed. Unavailable for ID responses, which return false.
     * Authorization decisions require backend verification.
     */
    val passed: Boolean,

    /** Failure reasons. Available for JWS responses. */
    val failureReasons: Array<String>?,

    /** The Radar ID of the location check. */
    val _id: String,

    /** The response JSON. Reduced responses contain only their clear fields. */
    val fullJson: JSONObject,

    /** The response format selected by the server. */
    val format: RadarResponseFormat = RadarResponseFormat.JWS,

    /** The level of the check that ran. Null if the response does not include it. */
    val verificationLevel: RadarVerificationLevel? = null,

    /**
     * The level that the app must run next. [RadarVerificationLevel.PRECISE] means that the app
     * must run a precise check. Null means that no further check is necessary.
     * Use this field, not [passed], to decide on a step-up. The backend must verify the result.
     */
    val requiredVerificationLevel: RadarVerificationLevel? = null
) {
    internal companion object {
        private const val FIELD_FORMAT = "format"
        private const val FIELD_USER = "user"
        private const val FIELD_EVENTS = "events"
        private const val FIELD_TOKEN = "token"
        private const val FIELD_EXPIRES_AT = "expiresAt"
        private const val FIELD_EXPIRES_IN = "expiresIn"
        private const val FIELD_PASSED = "passed"
        private const val FIELD_FAILURE_REASONS = "failureReasons"
        private const val FIELD_ID = "_id"
        private const val FIELD_VERIFICATION_LEVEL = "verificationLevel"
        private const val FIELD_REQUIRED_VERIFICATION_LEVEL = "requiredVerificationLevel"

        fun fromJson(obj: JSONObject?): RadarVerifiedLocationToken? {
            if (obj == null) {
                return null
            }

            val format = RadarResponseFormat.fromJson(obj.opt(FIELD_FORMAT))
                ?: return null

            return when (format) {
                RadarResponseFormat.JWS -> fromJwsJson(obj)
                RadarResponseFormat.JWE -> fromJweJson(obj)
                RadarResponseFormat.ID -> fromIdJson(obj)
            }
        }

        private fun fromJwsJson(obj: JSONObject): RadarVerifiedLocationToken? {
            // Preserve the existing JWS parser and its defaults.
            val user = RadarUser.fromJson(obj.optJSONObject(FIELD_USER))
            val events = RadarEvent.fromJson(obj.optJSONArray(FIELD_EVENTS))
            val token: String? = obj.optString(FIELD_TOKEN)
            val expiresAt = RadarUtils.isoStringToDate(obj.optString(FIELD_EXPIRES_AT))
            val expiresIn = obj.optInt(FIELD_EXPIRES_IN)
            val passed = obj.optBoolean(FIELD_PASSED)
            val failureReasons = obj.optJSONArray(FIELD_FAILURE_REASONS)?.let { reasons ->
                Array(reasons.length()) { reasons.optString(it) }
            } ?: emptyArray()
            val id = obj.optString(FIELD_ID)

            if (user == null || events == null || token == null || expiresAt == null) {
                return null
            }

            return RadarVerifiedLocationToken(
                user, events, token, expiresAt, expiresIn,
                passed, failureReasons, id, obj,
                verificationLevel = RadarVerificationLevel.fromJson(obj.opt(FIELD_VERIFICATION_LEVEL)),
                requiredVerificationLevel = RadarVerificationLevel.fromJson(obj.opt(FIELD_REQUIRED_VERIFICATION_LEVEL))
            )
        }

        private fun fromJweJson(obj: JSONObject): RadarVerifiedLocationToken? {
            val id = (obj.opt(FIELD_ID) as? String)
                ?.takeIf { it.isNotEmpty() } ?: return null
            val token = (obj.opt(FIELD_TOKEN) as? String)
                ?.takeIf { it.isNotEmpty() } ?: return null
            val expiration = obj.opt(FIELD_EXPIRES_AT) as? String ?: return null
            val expiresAt = RadarUtils.isoStringToDate(expiration) ?: return null
            val lifetime = obj.opt(FIELD_EXPIRES_IN) as? Number ?: return null
            val expiresIn = lifetime.toInt()
            val passed = obj.opt(FIELD_PASSED) as? Boolean ?: return null

            // Reject fractional or out-of-range lifetimes instead of truncating them.
            if (lifetime.toDouble() != expiresIn.toDouble()) {
                return null
            }

            val clearJson = JSONObject(
                obj,
                arrayOf(
                    "meta",
                    FIELD_FORMAT,
                    FIELD_ID,
                    FIELD_TOKEN,
                    FIELD_EXPIRES_AT,
                    FIELD_EXPIRES_IN,
                    FIELD_PASSED,
                    FIELD_VERIFICATION_LEVEL,
                    FIELD_REQUIRED_VERIFICATION_LEVEL
                )
            )

            return RadarVerifiedLocationToken(
                user = null,
                events = null,
                token = token,
                expiresAt = expiresAt,
                expiresIn = expiresIn,
                passed = passed,
                failureReasons = null,
                _id = id,
                fullJson = clearJson,
                format = RadarResponseFormat.JWE,
                verificationLevel = RadarVerificationLevel.fromJson(obj.opt(FIELD_VERIFICATION_LEVEL)),
                requiredVerificationLevel = RadarVerificationLevel.fromJson(obj.opt(FIELD_REQUIRED_VERIFICATION_LEVEL))
            )
        }

        private fun fromIdJson(obj: JSONObject): RadarVerifiedLocationToken? {
            val id = (obj.opt(FIELD_ID) as? String)
                ?.takeIf { it.isNotEmpty() } ?: return null

            return RadarVerifiedLocationToken(
                user = null,
                events = null,
                token = null,
                expiresAt = null,
                expiresIn = 0,
                passed = false,
                failureReasons = null,
                _id = id,
                fullJson = JSONObject(
                    obj,
                    arrayOf(
                        "meta",
                        FIELD_FORMAT,
                        FIELD_ID,
                        FIELD_VERIFICATION_LEVEL,
                        FIELD_REQUIRED_VERIFICATION_LEVEL
                    )
                ),
                format = RadarResponseFormat.ID,
                verificationLevel = RadarVerificationLevel.fromJson(obj.opt(FIELD_VERIFICATION_LEVEL)),
                requiredVerificationLevel = RadarVerificationLevel.fromJson(obj.opt(FIELD_REQUIRED_VERIFICATION_LEVEL))
            )
        }
    }

    fun toJson(): JSONObject = fullJson
}
