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
    val formattedAddress: String? = null,

    /**
    * The name of the country of the address.
    */
    val country: String? = null,

    /**
     * The unique code of the country of the address.
     */
    val countryCode: String? = null,

    /**
     * The flag of the country of the address.
     */
    val countryFlag: String? = null,

    /**
     * The name of the DMA of the address.
     */
    val dma: String? = null,

    /**
     * The unique code of the DMA of the address.
     */
    val dmaCode: String? = null,

    /**
     * The name of the state of the address.
     */
    val state: String? = null,

    /**
     * The unique code of the state of the address.
     */
    val stateCode: String? = null,

    /**
     * The postal code of the address.
     */
    val postalCode: String? = null,

    /**
     * The city of the address.
     */
    val city: String? = null,

    /**
     * The borough of the address.
     */
    val borough: String? = null,

    /**
     * The county of the address.
     */
    val county: String? = null,

    /**
     * The neighborhood of the address.
     */
    val neighborhood: String? = null,

    /**
     * The street of the address.
     */
    val street: String? = null,

    /**
     * The street number of the address.
     */
    val number: String? = null,

    /**
     * The name of the address.
     */
    val addressLabel: String? = null,

    /**
     * The place name of the address.
     */
    val placeLabel: String? = null,

    /**
     * The unit of the address
     */
    val unit: String? = null,

    /**
     * The plus4 of the zip of the address
     */
    val plus4: String? = null,

    /**
     * The distance to the search anchor in meters
     */
    val distance: Int? = null,

    /**
     * The layer of the address
     */
    val layer: String? = null,

    /**
     * The metadata of the address.
     */
    val metadata: JSONObject? = null,

    /**
     * The confidence level of the geocoding result.
     */
    val confidence: RadarAddressConfidence = RadarAddressConfidence.NONE,

    /**
     * The time zone information of the location.
     */
    val timeZone: RadarTimeZone? = null,

    /**
     * The categories of the address.
     */
    val categories: Array<String>? = null,
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
        private const val FIELD_TIME_ZONE = "timeZone"
        private const val FIELD_CATEGORIES = "categories"
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
            val timeZone = RadarTimeZone.fromJson(obj.optJSONObject(FIELD_TIME_ZONE))
            val categories = obj.optJSONArray(FIELD_CATEGORIES)?.let { categoriesArr ->
                val list = mutableListOf<String>()
                for (i in 0 until categoriesArr.length()) {
                    val value = categoriesArr.optString(i)
                    if (!value.isNullOrEmpty()) {
                        list.add(value)
                    }
                }
                list.toTypedArray()
            }

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
                timeZone,
                categories,
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
        obj.putOpt(FIELD_TIME_ZONE, this.timeZone?.toJson())
        obj.putOpt(FIELD_CATEGORIES, this.categories?.let { categories ->
            JSONArray().apply {
                categories.forEach { put(it) }
            }
        })
        return obj
    }

}
