package io.radar.sdk.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Represents a place at an address.
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
                stringArrayFromJson(obj.optJSONArray(FIELD_CATEGORIES))
            )
        }

        @JvmStatic
        fun fromJson(arr: JSONArray?): Array<RadarAddressPlace> {
            if (arr == null) {
                return emptyArray()
            }

            return Array(arr.length()) { index ->
                fromJson(arr.optJSONObject(index))
            }.filterNotNull().toTypedArray()
        }

        @JvmStatic
        fun toJson(places: Array<RadarAddressPlace>?): JSONArray? {
            if (places == null) {
                return null
            }

            val arr = JSONArray()
            places.forEach { place ->
                arr.put(place.toJson())
            }
            return arr
        }

        /**
         * Reads a JSON array of strings, skipping any entry that is not one.
         */
        internal fun stringArrayFromJson(arr: JSONArray?): Array<String> {
            if (arr == null) {
                return emptyArray()
            }

            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val value = arr.optString(i)
                if (value.isNotEmpty()) {
                    list.add(value)
                }
            }
            return list.toTypedArray()
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
