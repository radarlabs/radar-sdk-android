package io.radar.sdk.model

import org.json.JSONObject

/**
 * Represents the enriched form of a selected address.
 *
 * Carries only these two fields. It is not a [RadarAddress], and none of that type's other
 * fields are returned.
 *
 * @see [](https://radar.com/documentation/api#autocomplete)
 */
class RadarAutocompleteEnrichedAddress(
    /**
     * The formatted string representation of the address.
     */
    val formattedAddress: String?,

    /**
     * What address enrichment knows about the location.
     */
    val locationDetails: RadarAddressLocationDetails
) {

    internal companion object {
        private const val FIELD_FORMATTED_ADDRESS = "formattedAddress"
        private const val FIELD_LOCATION_DETAILS = "locationDetails"

        @JvmStatic
        fun fromJson(obj: JSONObject?): RadarAutocompleteEnrichedAddress? {
            if (obj == null) {
                return null
            }

            // The server always sends locationDetails alongside the address; without it there is
            // no enrichment to deliver.
            val locationDetails = RadarAddressLocationDetails.fromJson(obj.optJSONObject(FIELD_LOCATION_DETAILS))
                ?: return null

            return RadarAutocompleteEnrichedAddress(
                obj.optString(FIELD_FORMATTED_ADDRESS).ifEmpty { null },
                locationDetails
            )
        }
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.putOpt(FIELD_FORMATTED_ADDRESS, this.formattedAddress)
        obj.putOpt(FIELD_LOCATION_DETAILS, this.locationDetails.toJson())
        return obj
    }
}
