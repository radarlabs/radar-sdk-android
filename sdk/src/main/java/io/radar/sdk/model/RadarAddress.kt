package io.radar.sdk.model

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
     * The street of the address.
     */
    val street: String?,

    /**
     * The street number of the address.
     */
    val number: String?,

    /**
     * The name of the address.
     */
    val addressLabel: String?,

    /**
     * The place name of the address.
     */
    val placeLabel: String?,

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
        private const val FIELD_STREET = "street"
        private const val FIELD_NUMBER = "number"
        private const val FIELD_ADDRESS_LABEL = "addressLabel"
        private const val FIELD_PLACE_LABEL = "placeLabel"
        private const val FIELD_CONFIDENCE = "confidence"

        fun fromJson(obj: JSONObject?): RadarAddress? {
            if (obj == null) {
                return null
            }

            val coordinate = RadarCoordinate(obj.optDouble(FIELD_LATITUDE), obj.optDouble(FIELD_LONGITUDE))
            val formattedAddress = obj.optString(FIELD_FORMATTED_ADDRESS)
            val country = obj.optString(FIELD_COUNTRY)
            val countryCode = obj.optString(FIELD_COUNTRY_CODE)
            val countryFlag = obj.optString(FIELD_COUNTRY_FLAG)
            val state = obj.optString(FIELD_STATE)
            val stateCode = obj.optString(FIELD_STATE_CODE)
            val postalCode = obj.optString(FIELD_POSTAL_CODE)
            val city = obj.optString(FIELD_CITY)
            val borough = obj.optString(FIELD_BOROUGH)
            val county = obj.optString(FIELD_COUNTY)
            val neighborhood = obj.optString(FIELD_NEIGHBORHOOD)
            val street = obj.optString(FIELD_STREET)
            val number = obj.optString(FIELD_NUMBER)
            val addressLabel = obj.optString(FIELD_ADDRESS_LABEL)
            val placeLabel = obj.optString(FIELD_PLACE_LABEL)
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
                street,
                number,
                addressLabel,
                placeLabel,
                confidence
            )
        }

        fun fromJson(arr: JSONArray?): Array<RadarAddress>? {
            if (arr == null) {
                return null
            }

            return Array(arr.length()) { index ->
                fromJson(arr.optJSONObject(index))
            }.filterNotNull().toTypedArray()
        }

        fun toJson(addresses: Array<RadarAddress>?): JSONArray? {
            if (addresses == null) {
                return null
            }

            val arr = JSONArray()
            addresses.forEach { address ->
                arr.put(address.toJson())
            }
            return arr
        }

        fun stringForConfidence(confidence: RadarAddressConfidence): String {
            return when(confidence) {
                RadarAddressConfidence.EXACT -> "exact"
                RadarAddressConfidence.INTERPOLATED -> "interpolated"
                RadarAddressConfidence.FALLBACK -> "fallback"
                else -> "none"
            }
        }
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.putOpt(FIELD_LATITUDE, this.coordinate.latitude)
        obj.putOpt(FIELD_LONGITUDE, this.coordinate.latitude)
        obj.putOpt(FIELD_FORMATTED_ADDRESS, this.formattedAddress)
        obj.putOpt(FIELD_COUNTRY, this.country)
        obj.putOpt(FIELD_COUNTRY_CODE, this.countryCode)
        obj.putOpt(FIELD_COUNTRY_FLAG, this.countryFlag)
        obj.putOpt(FIELD_STATE, this.state)
        obj.putOpt(FIELD_STATE_CODE, this.stateCode)
        obj.putOpt(FIELD_POSTAL_CODE, this.postalCode)
        obj.putOpt(FIELD_CITY, this.city)
        obj.putOpt(FIELD_BOROUGH, this.borough)
        obj.putOpt(FIELD_COUNTY, this.county)
        obj.putOpt(FIELD_NEIGHBORHOOD, this.neighborhood)
        obj.putOpt(FIELD_STREET, this.street)
        obj.putOpt(FIELD_NUMBER, this.number)
        obj.putOpt(FIELD_ADDRESS_LABEL, this.addressLabel)
        obj.putOpt(FIELD_PLACE_LABEL, this.placeLabel)
        obj.putOpt(FIELD_CONFIDENCE, stringForConfidence(this.confidence))
        return obj
    }

}
