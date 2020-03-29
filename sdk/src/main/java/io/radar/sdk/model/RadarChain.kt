package io.radar.sdk.model

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Represents the chain of a place. For more information about Places, see [](https://radar.io/documentation/places).
 *
 * @see [](https://radar.io/documentation/places)
 */
class RadarChain(
    /**
     * The unique ID of the chain. For a full list of chains, see [](https://radar.io/documentation/places/chains).
     */
    val slug: String,

    /**
     * The name of the chain. For a full list of chains, see [](https://radar.io/documentation/places/chains).
     */
    val name: String,

    /**
     * The external ID of the chain.
     */
    val externalId: String?,

    /**
     * The optional set of custom key-value pairs for the chain.
     */
    val metadata: JSONObject?
) {

    internal companion object {
        private const val FIELD_SLUG = "slug"
        private const val FIELD_NAME = "name"
        private const val FIELD_EXTERNAL_ID = "externalId"
        private const val FIELD_METADATA = "metadata"

        @Throws(JSONException::class)
        fun fromJson(obj: JSONObject?): RadarChain? {
            if (obj == null) {
                return null
            }

            val slug = obj.optString(FIELD_SLUG)
            val name = obj.optString(FIELD_NAME)
            val externalId: String? = obj.optString(FIELD_EXTERNAL_ID)
            val metadata: JSONObject? = obj.optJSONObject(FIELD_METADATA)

            return RadarChain(
                slug,
                name,
                externalId,
                metadata
            )
        }

        fun fromJson(array: JSONArray?): Array<RadarChain>? {
            if (array == null) {
                return null
            }

            return Array(array.length()) { index ->
                fromJson(array.optJSONObject(index))
            }.filterNotNull().toTypedArray()
        }

        fun toJson(chains: Array<RadarChain>?): JSONArray? {
            if (chains == null) {
                return null
            }

            val arr = JSONArray()
            chains.forEach { chain ->
                arr.put(chain.toJson())
            }
            return arr
        }
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.putOpt(FIELD_SLUG, this.slug)
        obj.putOpt(FIELD_NAME, this.name)
        obj.putOpt(FIELD_EXTERNAL_ID, this.externalId)
        obj.putOpt(FIELD_METADATA, this.metadata)
        return obj
    }

}