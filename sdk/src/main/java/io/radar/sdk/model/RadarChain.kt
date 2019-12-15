package io.radar.sdk.model

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
        fun fromJson(obj: JSONObject): RadarChain {
            val slug = obj.optString(FIELD_SLUG)
            val name = obj.optString(FIELD_NAME)
            val externalId: String? = obj.optString(FIELD_EXTERNAL_ID, null)
            val metadata: JSONObject? = obj.optJSONObject(FIELD_METADATA)

            return RadarChain(slug, name, externalId, metadata)
        }

    }

}