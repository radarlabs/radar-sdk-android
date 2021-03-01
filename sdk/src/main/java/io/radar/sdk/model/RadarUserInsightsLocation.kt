package io.radar.sdk.model

import android.annotation.SuppressLint
import org.json.JSONException
import org.json.JSONObject
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

/**
 * Represents a learned home or work location.
 *
 * @see [](https://radar.io/documentation/insights)
 */
class RadarUserInsightsLocation(
    /**
     * The type of the learned location.
     * */
    val type: RadarUserInsightsLocationType,

    /**
     * The learned location.
     */
    val location: RadarCoordinate?,

    /**
     * The confidence level of the learned location.
     */
    val confidence: RadarUserInsightsLocationConfidence,

    /**
     * The datetime when the learned location was updated.
     */
    val updatedAt: Date,

    /**
     * The country of the learned location. May be `null`.
     */
    val country: RadarRegion?,

    /**
     * The state of the learned location. May be `null`.
     */
    val state: RadarRegion?,

    /**
     * The DMA of the learned location. May be `null`.
     */
    val dma: RadarRegion?,

    /**
     * The postal code of the learned location. May be `null`.
     */
    val postalCode: RadarRegion?
) {

    /**
     * The types for learned locations.
     */
    enum class RadarUserInsightsLocationType {
        /** Unknown */
        UNKNOWN,
        /** Home */
        HOME,
        /** Work */
        OFFICE
    }

    /**
     * The confidence levels for learned locations.
     */
    enum class RadarUserInsightsLocationConfidence {
        /** Unknown confidence */
        NONE,
        /** Low confidence */
        LOW,
        /** Medium confidence */
        MEDIUM,
        /** High confidence */
        HIGH
    }

    internal companion object {
        private const val FIELD_TYPE = "type"
        private const val FIELD_LOCATION = "location"
        private const val FIELD_COORDINATES = "coordinates"
        private const val FIELD_CONFIDENCE = "confidence"
        private const val FIELD_UPDATED_AT = "updatedAt"
        private const val FIELD_COUNTRY = "country"
        private const val FIELD_STATE = "state"
        private const val FIELD_DMA = "dma"
        private const val FIELD_POSTAL_CODE = "postalCode"

        @JvmStatic
        @SuppressLint("SimpleDateFormat")
        @Throws(JSONException::class, ParseException::class)
        fun fromJson(obj: JSONObject?): RadarUserInsightsLocation? {
            if (obj == null) {
                return null
            }

            val type = when (obj.optString(FIELD_TYPE)) {
                "home" -> RadarUserInsightsLocationType.HOME
                "office" -> RadarUserInsightsLocationType.OFFICE
                else -> RadarUserInsightsLocationType.UNKNOWN
            }
            val locationObj = obj.optJSONObject(FIELD_LOCATION)
            val locationCoordinatesObj = locationObj?.optJSONArray(FIELD_COORDINATES)
            val location = RadarCoordinate(
                locationCoordinatesObj?.optDouble(1) ?: 0.0,
                locationCoordinatesObj?.optDouble(0) ?: 0.0
            )
            val confidence = when (obj.optInt(FIELD_CONFIDENCE)) {
                3 -> RadarUserInsightsLocationConfidence.HIGH
                2 -> RadarUserInsightsLocationConfidence.MEDIUM
                1 -> RadarUserInsightsLocationConfidence.LOW
                else -> RadarUserInsightsLocationConfidence.NONE
            }
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
            val updatedAt = obj.optString(FIELD_UPDATED_AT).let { updatedAtStr ->
                dateFormat.parse(updatedAtStr)
            } ?: Date()
            val country = RadarRegion.fromJson(obj.optJSONObject(FIELD_COUNTRY))
            val state = RadarRegion.fromJson(obj.optJSONObject(FIELD_STATE))
            val dma = RadarRegion.fromJson(obj.optJSONObject(FIELD_DMA))
            val postalCode = RadarRegion.fromJson(obj.optJSONObject(FIELD_POSTAL_CODE))

            return RadarUserInsightsLocation(
                type,
                location,
                confidence,
                updatedAt,
                country,
                state,
                dma,
                postalCode
            )
        }

        @JvmStatic
        fun stringForType(type: RadarUserInsightsLocationType): String? {
            return when (type) {
                RadarUserInsightsLocationType.HOME -> "home"
                RadarUserInsightsLocationType.OFFICE -> "office"
                else -> "unknown"
            }
        }
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.putOpt(FIELD_TYPE, stringForType(this.type))
        obj.putOpt(FIELD_LOCATION, this.location?.toJson())
        obj.putOpt(FIELD_STATE, this.state?.toJson())
        return obj
    }

}
