package io.radar.sdk.model

import org.json.JSONObject

class RadarExpectedAddress(
    val expectedAddress: String,
    val formattedAddress: String?,
    val latitude: Double?,
    val longitude: Double?,
    val atAddress: Boolean,
    val confidence: Confidence,
    val distance: Double?
) {
    enum class Confidence(val value: String) {
        HIGH("high"),
        MEDIUM("medium"),
        LOW("low"),
        UNKNOWN("unknown");

        companion object {
            fun fromString(value: String): Confidence = entries.find { it.value == value } ?: UNKNOWN
        }
    }

    internal companion object {
        private const val FIELD_EXPECTED_ADDRESS = "expectedAddress"
        private const val FIELD_FORMATTED_ADDRESS = "formattedAddress"
        private const val FIELD_LATITUDE = "latitude"
        private const val FIELD_LONGITUDE = "longitude"
        private const val FIELD_AT_ADDRESS = "atAddress"
        private const val FIELD_CONFIDENCE = "confidence"
        private const val FIELD_DISTANCE = "distance"

        @JvmStatic
        fun fromJson(obj: JSONObject?): RadarExpectedAddress? {
            if (obj == null) {
                return null
            }

            val expectedAddress = obj.optString(FIELD_EXPECTED_ADDRESS).takeIf { it.isNotEmpty() } ?: return null
            val formattedAddress = obj.optString(FIELD_FORMATTED_ADDRESS).takeIf { it.isNotEmpty() }
            val latitude = obj.optDouble(FIELD_LATITUDE).takeIf { !it.isNaN() }
            val longitude = obj.optDouble(FIELD_LONGITUDE).takeIf { !it.isNaN() }
            val atAddress = obj.optBoolean(FIELD_AT_ADDRESS)
            val confidence = Confidence.fromString(obj.optString(FIELD_CONFIDENCE))
            val distance = obj.optDouble(FIELD_DISTANCE).takeIf { !it.isNaN() }

            return RadarExpectedAddress(
                expectedAddress,
                formattedAddress,
                latitude,
                longitude,
                atAddress,
                confidence,
                distance
            )
        }
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.putOpt(FIELD_EXPECTED_ADDRESS, this.expectedAddress)
        obj.putOpt(FIELD_FORMATTED_ADDRESS, this.formattedAddress)
        obj.putOpt(FIELD_LATITUDE, this.latitude)
        obj.putOpt(FIELD_LONGITUDE, this.longitude)
        obj.putOpt(FIELD_AT_ADDRESS, this.atAddress)
        obj.putOpt(FIELD_CONFIDENCE, this.confidence.value)
        obj.putOpt(FIELD_DISTANCE, this.distance)
        return obj
    }
}
