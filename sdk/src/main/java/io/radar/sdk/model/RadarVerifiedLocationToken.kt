package io.radar.sdk.model

import io.radar.sdk.RadarUtils
import org.json.JSONObject
import java.util.Date

/**
 * Represents a user's verified location.
 *
 * @see [](https://radar.com/documentation/fraud)
 */
class RadarVerifiedLocationToken(
    /**
     * The user.
     */
    val user: RadarUser,

    /**
     * An array of events.
     */
    val events: Array<RadarEvent>,

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
    val passed: Boolean
) {
    internal companion object {
        private const val FIELD_USER = "user"
        private const val FIELD_EVENTS = "events"
        private const val FIELD_TOKEN = "token"
        private const val FIELD_EXPIRES_AT = "expiresAt"
        private const val FIELD_EXPIRES_IN = "expiresIn"
        private const val FIELD_PASSED = "passed"

        fun fromJson(obj: JSONObject?): RadarVerifiedLocationToken? {
            if (obj == null) {
                return null
            }

            val user: RadarUser? = RadarUser.fromJson(obj.optJSONObject(FIELD_USER))
            val events: Array<RadarEvent>? = RadarEvent.fromJson(obj.optJSONArray(FIELD_EVENTS))
            val token: String? = obj.optString(FIELD_TOKEN)
            val expiresAt: Date? = RadarUtils.isoStringToDate(obj.optString(FIELD_EXPIRES_AT))
            val expiresIn: Int = obj.optInt(FIELD_EXPIRES_IN)
            val passed: Boolean = user?.fraud?.passed == true && user.country?.passed == true && user.state?.passed == true

            if (user == null || events == null || token == null || expiresAt == null) {
                return null
            }

            return RadarVerifiedLocationToken(user, events, token, expiresAt, expiresIn, passed)
        }
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.putOpt(FIELD_USER, this.user.toJson())
        obj.putOpt(FIELD_EVENTS, RadarEvent.toJson(this.events))
        obj.putOpt(FIELD_TOKEN, this.token)
        obj.putOpt(FIELD_EXPIRES_AT, RadarUtils.dateToISOString(this.expiresAt))
        obj.putOpt(FIELD_EXPIRES_IN, this.expiresIn)
        obj.putOpt(FIELD_PASSED, this.passed)
        return obj
    }

}