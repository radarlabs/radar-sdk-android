package io.radar.sdk.model

import io.radar.sdk.model.RadarCoordinate

/**
 * Represents the full address output by the geocoding endpoints.
 */
class RadarAddress(
    /**
     * The location coordinate of the address.
     */
    val coordinate: RadarCoordinate,

    /**
     * The fully formatted representation of the address.
     */
    val formattedAddress: String?,

    /**
    * The country of the address.
    */
    val country: String?,

    /**
     * The country code of the address.
     */
    val countryCode: String?,

    /**
     * The country flag of the address.
     */
    val countryFlag: String?,

    /**
     * The state of the address.
     */
    val state: String?,

    /**
     * The state code of the address.
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
     * The number / house number of the address.
     */
    val number: String?,

    /**
     * Confidence in the address received from the API.
     */
    val confidence: RadarAddressConfidence
) {

    /**
     * The confidence levels for addresses
     */
    enum class RadarAddressConfidence {
        RadarAddressConfidenceExact,
        RadarAddressConfidenceInterpolated,
        RadarAddressConfidenceFallback
    }

    internal companion object {
        private const val FIELD_COORDINATE = "coordinate"
        private const val FIELD_FORMATTED_ADDRESS = "formattedAddress"
        private const val FIELD_COUNTRY = "country"
        private const val FIElD_COUNTRY_CODE = "countryCode"
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

            val coords = obj.optJSONArray(FIELD_COORDINATE)
            val coordinate = RadarCoordinate(coords?.optDouble(1) ?: 0.0, coords?.optDouble(0) ?: 0.0)

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

            val confidence = when(obj.optString(FIEND_CONFIDENCE)) {
                "exact" -> RadarAddressConfidenceExact
                "interpolated" -> RadarAddressConfidenceInterpolated
                else -> RadarAddressConfidenceFallback
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
    }
}