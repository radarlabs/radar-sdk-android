package io.radar.sdk.model

import io.radar.sdk.util.toStringArray
import org.json.JSONArray
import org.json.JSONObject

/**
 * Represents what address enrichment knows about a location.
 */
class RadarAddressLocationDetails(
    /**
     * How the location is used, or `unknown`.
     */
    val usage: String,

    /**
     * The dwelling type, `n/a` for usages that have none, or `unknown`.
     */
    val dwelling: String,

    /**
     * The grounds of the location.
     */
    val grounds: Array<String>,

    /**
     * The places on the grounds of the location.
     */
    val places: Array<RadarAddressPlace>
) {

    internal companion object {
        private const val FIELD_USAGE = "usage"
        private const val FIELD_DWELLING = "dwelling"
        private const val FIELD_GROUNDS = "grounds"
        private const val FIELD_PLACES = "places"

        @JvmStatic
        fun fromJson(obj: JSONObject?): RadarAddressLocationDetails? {
            if (obj == null) {
                return null
            }

            return RadarAddressLocationDetails(
                obj.optString(FIELD_USAGE),
                obj.optString(FIELD_DWELLING),
                obj.optJSONArray(FIELD_GROUNDS).toStringArray(),
                RadarAddressPlace.fromJson(obj.optJSONArray(FIELD_PLACES))
            )
        }
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.putOpt(FIELD_USAGE, this.usage)
        obj.putOpt(FIELD_DWELLING, this.dwelling)
        obj.putOpt(
            FIELD_GROUNDS,
            JSONArray().apply {
                grounds.forEach { put(it) }
            }
        )
        obj.putOpt(FIELD_PLACES, RadarAddressPlace.toJson(this.places))
        return obj
    }
}
