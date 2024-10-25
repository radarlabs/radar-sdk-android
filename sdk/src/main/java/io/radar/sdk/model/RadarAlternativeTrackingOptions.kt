package io.radar.sdk.model

import io.radar.sdk.RadarTrackingOptions
import org.json.JSONArray
import org.json.JSONObject

class RadarAlternativeTrackingOptions(
    val type: String,
    val trackingOptions: RadarTrackingOptions,
    val geofenceTags: Array<String>?
) {
    internal companion object {
        private const val TYPE_ID = "type"
        private const val TRACKING_OPTIONS = "trackingOptions"
        private const val GEOFENCE_TAGS = "geofenceTags"

        @JvmStatic
        fun fromJson(obj: JSONObject?): RadarAlternativeTrackingOptions? {
            if (obj == null) {
                return null
            }
            val type = obj.optString(TYPE_ID)
            val trackingOptions = RadarTrackingOptions.fromJson(obj.optJSONObject(TRACKING_OPTIONS))
            val geofenceTags = obj.optJSONArray(GEOFENCE_TAGS)?.let { tags ->
                (0 until tags.length()).map { index ->
                    tags.getString(index)
                }.toTypedArray()
            }
            return RadarAlternativeTrackingOptions(type, trackingOptions, geofenceTags)
        }
        
        @JvmStatic
        fun fromJson(arr: JSONArray?): Array<RadarAlternativeTrackingOptions>? {
            if (arr == null) {
                return null
            }
            return Array(arr.length()) { index ->
                fromJson(arr.optJSONObject(index))
            }.filterNotNull().toTypedArray()
        }

        @JvmStatic
        fun toJson(alternativeTrackingOptions: Array<RadarAlternativeTrackingOptions>?): JSONArray? {
            if (alternativeTrackingOptions == null) {
                return null
            }
            val arr = JSONArray()
            alternativeTrackingOptions.forEach { alternativeTrackingOption ->
                arr.put(alternativeTrackingOption.toJson())
            }
            return arr
        }

        @JvmStatic
        fun getRemoteTrackingOptionsWithKey(alternativeTrackingOptions: Array<RadarAlternativeTrackingOptions>?, key: String): RadarTrackingOptions? {
            if (alternativeTrackingOptions == null) {
                return null
            }
            for (alternativeTrackingOption in alternativeTrackingOptions) {
                if (alternativeTrackingOption.type == key) {
                    return alternativeTrackingOption.trackingOptions
                }
            }
            return null
        }

        @JvmStatic
        fun getGeofenceTagsWithKey(alternativeTrackingOptions: Array<RadarAlternativeTrackingOptions>?, key: String): Array<String>? {
            if (alternativeTrackingOptions == null) {
                return null
            }
            var geofenceTags: Array<String>? = null
            for (alternativeTrackingOption in alternativeTrackingOptions) {
                if (alternativeTrackingOption.type == key) {
                    geofenceTags = alternativeTrackingOption.geofenceTags
                }
            }
            return geofenceTags
        }
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.putOpt(TYPE_ID, type)
        obj.putOpt(TRACKING_OPTIONS, trackingOptions.toJson())
        val geofenceTagsArr = JSONArray()
        if (geofenceTags != null) {
            geofenceTags.forEach { geofenceTag -> geofenceTagsArr.put(geofenceTag) }
            obj.putOpt(GEOFENCE_TAGS, geofenceTagsArr)
        }
        return obj
    }

}