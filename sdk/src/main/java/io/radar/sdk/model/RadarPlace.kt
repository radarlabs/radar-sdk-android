package io.radar.sdk.model

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Represents a place. For more information about Places, see [](https://radar.io/documentation/places).
 *
 * @see [](https://radar.io/documentation/places)
 */
class RadarPlace(
    /**
     * The Radar ID of the place.
     */
    val _id: String,

    /**
     * The name of the place.
     */
    val name: String,

    /**
     * The categories of the place. For a full list of categories, see [](https://radar.io/documentation/places/categories).
     */
    val categories: Array<String>,

    /**
     * The chain of the place, if known. May be `null` for places without a chain. For a full list of chains, see [](https://radar.io/documentation/places/chains).
     */
    val chain: RadarChain?,

    /**
     * The location of the place.
     */
    val location: RadarCoordinate,

    /**
     * The group for the place, if any. For a full list of groups, see [](https://radar.io/documentation/places/groups).
     */
    val group: String?,

    /**
     * The metadata for the place, if part of a group. For details of metadata fields see [](https://radar.io/documentation/places/groups).
     */
    val metadata: JSONObject?
) {

    internal companion object {
        private const val FIELD_ID = "_id"
        private const val FIELD_NAME = "name"
        private const val FIELD_CATEGORIES = "categories"
        private const val FIELD_CHAIN = "chain"
        private const val FIELD_LOCATION = "location"
        private const val FIELD_COORDINATES = "coordinates"
        private const val FIELD_GROUP = "group"
        private const val FIELD_METADATA = "metadata"

        @JvmStatic
        @Throws(JSONException::class)
        fun fromJson(obj: JSONObject?): RadarPlace? {
            if (obj == null) {
                return null
            }

            val id = obj.optString(FIELD_ID) ?: ""
            val name = obj.optString(FIELD_NAME) ?: ""
            val categories = obj.optJSONArray(FIELD_CATEGORIES)?.let { categoriesArr ->
                Array<String>(categoriesArr.length()) {
                    categoriesArr.optString(it)
                }
            } ?: emptyArray()
            val chain = RadarChain.fromJson(obj.optJSONObject(FIELD_CHAIN))
            val locationObj = obj.optJSONObject(FIELD_LOCATION)
            val locationCoordinatesObj = locationObj?.optJSONArray(FIELD_COORDINATES)
            val location = RadarCoordinate(
                locationCoordinatesObj?.optDouble(1) ?: 0.0,
                locationCoordinatesObj?.optDouble(0) ?: 0.0
            )
            val group: String? = obj.optString(FIELD_GROUP) ?: null
            val metadata: JSONObject? = obj.optJSONObject(FIELD_METADATA) ?: null

            return RadarPlace(
                id,
                name,
                categories,
                chain,
                location,
                group,
                metadata
            )
        }

        @JvmStatic
        @Throws(JSONException::class)
        fun fromJson(arr: JSONArray?): Array<RadarPlace>? {
            if (arr == null) {
                return null
            }

            return Array(arr.length()) { index ->
                fromJson(arr.optJSONObject(index))
            }.filterNotNull().toTypedArray()
        }

        @JvmStatic
        fun toJson(places: Array<RadarPlace>?): JSONArray? {
            if (places == null) {
                return null
            }

            val arr = JSONArray()
            places.forEach { place ->
                arr.put(place.toJson())
            }
            return arr
        }
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.putOpt(FIELD_ID, this._id)
        obj.putOpt(FIELD_NAME, this.name)
        val categoriesArr = JSONArray()
        this.categories.forEach { category -> categoriesArr.put(category) }
        obj.putOpt(FIELD_CATEGORIES, categoriesArr)
        obj.putOpt(FIELD_CHAIN, this.chain?.toJson())
        obj.putOpt(FIELD_GROUP, this.group)
        obj.putOpt(FIELD_METADATA, this.metadata)
        return obj
    }

    /**
     * Returns a boolean indicating whether the place is part of the specified chain.
     *
     * @return A boolean indicating whether the place is part of the specified chain.
     */
    fun isChain(slug: String): Boolean {
        return chain?.slug == slug
    }

    /**
     * Returns a boolean indicating whether the place has the specified category.
     *
     * @return A boolean indicating whether the place has the specified category.
     */
    fun hasCategory(category: String): Boolean {
        return category in categories
    }

}