package io.radar.sdk.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Represents what is known about the location at an address.
 */
class RadarAddressLocationDetails(
    /**
     * How the location is used.
     */
    val usage: String,

    /**
     * The type of dwelling at the location.
     */
    val dwelling: String,

    /**
     * The grounds at the location.
     */
    val grounds: Array<String>,

    /**
     * The places at the location.
     */
    val places: Array<RadarAddressPlace>
) {

    internal companion object {
        private const val FIELD_USAGE = "usage"
        private const val FIELD_DWELLING = "dwelling"
        private const val FIELD_GROUNDS = "grounds"
        private const val FIELD_PLACES = "places"

        private const val UNKNOWN = "unknown"

        @JvmStatic
        fun fromJson(obj: JSONObject?): RadarAddressLocationDetails? {
            if (obj == null) {
                return null
            }

            return RadarAddressLocationDetails(
                obj.optString(FIELD_USAGE).ifEmpty { UNKNOWN },
                obj.optString(FIELD_DWELLING).ifEmpty { UNKNOWN },
                RadarAddressPlace.stringArrayFromJson(obj.optJSONArray(FIELD_GROUNDS)),
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
