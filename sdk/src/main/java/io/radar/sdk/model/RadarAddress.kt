package io.radar.sdk.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Represents an address.
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
     * The name of the DMA of the address.
     */
    val dma: String?,

    /**
     * The unique code of the DMA of the address.
     */
    val dmaCode: String?,

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
     * The unit of the address
     */
    val unit: String?,

    /**
     * The plus4 of the zip of the address
     */
    val plus4: String?,

    /**
     * The distance to the search anchor in meters
     */
    val distance: Int?,

    /**
     * The layer of the address
     */
    val layer: String?,

    /**
     * The metadata of the address.
     */
    val metadata: JSONObject?,

    /**
     * The confidence level of the geocoding result.
     */
    val confidence: RadarAddressConfidence,

    /**
     * The timezone information of the address.
     */
    val timezone: RadarTimezone?,
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
        private const val FIELD_DMA = "dma"
        private const val FIELD_DMA_CODE = "dmaCode"
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
        private const val FIELD_UNIT = "unit"
        private const val FIELD_PLUS4 = "plus4"
        private const val FIELD_DISTANCE = "distance"
        private const val FIELD_LAYER = "layer"
        private const val FIELD_METADATA = "metadata"
        private const val FIELD_CONFIDENCE = "confidence"
        private const val FIELD_TIMEZONE = "timezone"

        @JvmStatic
        fun fromJson(obj: JSONObject?): RadarAddress? {
            if (obj == null) {
                return null
            }

            val coordinate = RadarCoordinate(obj.optDouble(FIELD_LATITUDE), obj.optDouble(FIELD_LONGITUDE))
            val formattedAddress = obj.optString(FIELD_FORMATTED_ADDRESS) ?: null
            val country = obj.optString(FIELD_COUNTRY) ?: null
            val countryCode = obj.optString(FIELD_COUNTRY_CODE) ?: null
            val countryFlag = obj.optString(FIELD_COUNTRY_FLAG) ?: null
            val dma = obj.optString(FIELD_DMA) ?: null
            val dmaCode = obj.optString(FIELD_DMA_CODE) ?: null
            val state = obj.optString(FIELD_STATE) ?: null
            val stateCode = obj.optString(FIELD_STATE_CODE) ?: null
            val postalCode = obj.optString(FIELD_POSTAL_CODE) ?: null
            val city = obj.optString(FIELD_CITY) ?: null
            val borough = obj.optString(FIELD_BOROUGH) ?: null
            val county = obj.optString(FIELD_COUNTY) ?: null
            val neighborhood = obj.optString(FIELD_NEIGHBORHOOD) ?: null
            val street = obj.optString(FIELD_STREET) ?: null
            val number = obj.optString(FIELD_NUMBER) ?: null
            val addressLabel = obj.optString(FIELD_ADDRESS_LABEL) ?: null
            val placeLabel = obj.optString(FIELD_PLACE_LABEL) ?: null
            val unit = obj.optString(FIELD_UNIT) ?: null
            val plus4 = obj.optString(FIELD_PLUS4) ?: null
            val distance = obj.optInt(FIELD_DISTANCE)
            val layer = obj.optString(FIELD_LAYER) ?: null
            val metadata: JSONObject? = obj.optJSONObject(FIELD_METADATA) ?: null
            val confidence = when(obj.optString(FIELD_CONFIDENCE)) {
                "exact" -> RadarAddressConfidence.EXACT
                "interpolated" -> RadarAddressConfidence.INTERPOLATED
                "fallback" -> RadarAddressConfidence.FALLBACK
                else -> RadarAddressConfidence.NONE
            }
            val timezone = RadarTimezone.fromJson(obj.optJSONObject(FIELD_TIMEZONE))

            return RadarAddress(
                coordinate,
                formattedAddress,
                country,
                countryCode,
                countryFlag,
                dma,
                dmaCode,
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
                unit,
                plus4,
                distance,
                layer,
                metadata,
                confidence,
                timezone,
            )
        }

        @JvmStatic
        fun fromJson(arr: JSONArray?): Array<RadarAddress>? {
            if (arr == null) {
                return null
            }

            return Array(arr.length()) { index ->
                fromJson(arr.optJSONObject(index))
            }.filterNotNull().toTypedArray()
        }

        @JvmStatic
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

        @JvmStatic
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

        var latitude = this.coordinate.latitude
        var longitude = this.coordinate.longitude
        //check that lat and long are valid double numbers
        if (latitude.isNaN() || longitude.isNaN()) {
            latitude = 0.0
            longitude = 0.0
        }

        val obj = JSONObject()
        obj.putOpt(FIELD_LATITUDE, latitude)
        obj.putOpt(FIELD_LONGITUDE, longitude)
        obj.putOpt(FIELD_FORMATTED_ADDRESS, this.formattedAddress)
        obj.putOpt(FIELD_COUNTRY, this.country)
        obj.putOpt(FIELD_COUNTRY_CODE, this.countryCode)
        obj.putOpt(FIELD_COUNTRY_FLAG, this.countryFlag)
        obj.putOpt(FIELD_DMA, this.dma)
        obj.putOpt(FIELD_DMA_CODE, this.dmaCode)
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
        obj.putOpt(FIELD_UNIT, this.unit)
        obj.putOpt(FIELD_PLUS4, this.plus4)
        obj.putOpt(FIELD_DISTANCE, this.distance)
        obj.putOpt(FIELD_LAYER, this.layer)
        obj.putOpt(FIELD_METADATA, this.metadata)
        obj.putOpt(FIELD_CONFIDENCE, stringForConfidence(this.confidence))
        obj.putOpt(FIELD_TIMEZONE, this.timezone?.toJson())
        return obj
    }

}
