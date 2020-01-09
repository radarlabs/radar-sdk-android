package io.radar.sdk.model

import io.radar.sdk.model.RadarCoordinate

import org.json.JSONArray
import org.json.JSONObject

/**
 * Represents the full address output by the geocoding endpoints.
 */
class RadarAddress(
    /**
     * The location coordinate of the address.
     */
    val coordinate: RadarCoordinate,

    /**
     * The formatted representation of the address.
     */
    val formattedAddress: String?,

    /**
    * The name of the country of the address.
    */
    val country: String?,

    /**
     * The unique code of the country of the address.
     */
    val countryCode: String?,

    /**
     * The flag of the country of the address.
     */
    val countryFlag: String?,

    /**
     * The name of the state of the address.
     */
    val state: String?,

    /**
     * The unique code of the state of the address.
     */
    val stateCode: String?,

    /**
     * The postal code of the address.
     */
    val postalCode: String?,

    /**
     * The city of the address.
     */
    val city: String?,

    /**
     * The borough of the address.
     */
    val borough: String?,

    /**
     * The county of the address.
     */
    val county: String?,

    /**
     * The neighborhood of the address.
     */
    val neighborhood: String?,

    /**
     * The street number of the address.
     */
    val number: String?,

    /**
     * The confidence level of the geocoding result.
     */
    val confidence: RadarAddressConfidence
) {

    /**
     * The confidence levels for geocoding results.
     */
    enum class RadarAddressConfidence {
        EXACT,
        INTERPOLATED,
        FALLBACK,
        NONE
    }

    internal companion object {
        private const val FIELD_LATITUDE = "latitude"
        private const val FIELD_LONGITUDE = "longitude"
        private const val FIELD_FORMATTED_ADDRESS = "formattedAddress"
        private const val FIELD_COUNTRY = "country"
        private const val FIELD_COUNTRY_CODE = "countryCode"
        private const val FIELD_COUNTRY_FLAG = "countryFlag"
        private const val FIELD_STATE = "state"
        private const val FIELD_STATE_CODE = "stateCode"
        private const val FIELD_POSTAL_CODE = "postalCode"
        private const val FIELD_CITY = "city"
        private const val FIELD_BOROUGH = "borough"
        private const val FIELD_COUNTY = "county"
        private const val FIELD_NEIGHBORHOOD = "neighborhood"
        private const val FIELD_NUMBER = "number"
        private const val FIELD_CONFIDENCE = "confidence"

        fun fromJson(obj: JSONObject): RadarAddress {

            val coordinate = RadarCoordinate(obj.optDouble(FIELD_LATITUDE), obj.optDouble(FIELD_LONGITUDE))
            val formattedAddress = obj.optString(FIELD_FORMATTED_ADDRESS, null)
            val country = obj.optString(FIELD_COUNTRY, null)
            val countryCode = obj.optString(FIELD_COUNTRY_CODE, null)
            val countryFlag = obj.optString(FIELD_COUNTRY_FLAG, null)
            val state = obj.optString(FIELD_STATE, null)
            val stateCode = obj.optString(FIELD_STATE_CODE, null)
            val postalCode = obj.optString(FIELD_POSTAL_CODE, null)
            val city = obj.optString(FIELD_CITY, null)
            val borough = obj.optString(FIELD_BOROUGH, null)
            val county = obj.optString(FIELD_COUNTY, null)
            val neighborhood = obj.optString(FIELD_NEIGHBORHOOD, null)
            val number = obj.optString(FIELD_NUMBER, null)

            val confidence = when(obj.optString(FIELD_CONFIDENCE)) {
                "exact" -> RadarAddressConfidence.EXACT
                "interpolated" -> RadarAddressConfidence.INTERPOLATED
                "fallback" -> RadarAddressConfidence.FALLBACK
                else -> RadarAddressConfidence.NONE
            }

            return RadarAddress(
                coordinate,
                formattedAddress,
                country,
                countryCode,
                countryFlag,
                state,
                stateCode,
                postalCode,
                city,
                borough,
                county,
                neighborhood,
                number,
                confidence
            )
        }

        fun fromJSONArray(array: JSONArray): Array<RadarAddress> {
            return Array(array.length()) { index ->
                fromJson(array.optJSONObject(index))
            }.filterNotNull().toTypedArray()
        }
    }
}
