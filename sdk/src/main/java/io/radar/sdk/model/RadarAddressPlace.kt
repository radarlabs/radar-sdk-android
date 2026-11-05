package io.radar.sdk.model

import io.radar.sdk.util.toStringArray
import org.json.JSONArray
import org.json.JSONObject

/**
 * Represents a place on the grounds of an enriched address.
 */
class RadarAddressPlace(
    /**
     * The name of the place.
     */
    val name: String,

    /**
     * The categories of the place.
     */
    val categories: Array<String>
) {

    internal companion object {
        private const val FIELD_NAME = "name"
        private const val FIELD_CATEGORIES = "categories"

        @JvmStatic
        fun fromJson(obj: JSONObject?): RadarAddressPlace? {
            if (obj == null) {
                return null
            }

            return RadarAddressPlace(
                obj.optString(FIELD_NAME),
                obj.optJSONArray(FIELD_CATEGORIES).toStringArray()
            )
        }

        @JvmStatic
        fun fromJson(arr: JSONArray?): Array<RadarAddressPlace> {
            if (arr == null) {
                return emptyArray()
            }

            return (0 until arr.length()).mapNotNull { fromJson(arr.optJSONObject(it)) }.toTypedArray()
        }

        @JvmStatic
        fun toJson(places: Array<RadarAddressPlace>): JSONArray {
            val arr = JSONArray()
            places.forEach { place ->
                arr.put(place.toJson())
            }
            return arr
        }
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.putOpt(FIELD_NAME, this.name)
        obj.putOpt(
            FIELD_CATEGORIES,
            JSONArray().apply {
                categories.forEach { put(it) }
            }
        )
        return obj
    }
}
