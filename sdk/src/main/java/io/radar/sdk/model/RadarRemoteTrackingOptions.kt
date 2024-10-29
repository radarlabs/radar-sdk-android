package io.radar.sdk.model

import io.radar.sdk.RadarTrackingOptions
import org.json.JSONArray
import org.json.JSONObject

class RadarRemoteTrackingOptions(
    val type: String,
    val trackingOptions: RadarTrackingOptions,
    val geofenceTags: Array<String>?
) {
    internal companion object {
        private const val TYPE_ID = "type"
        private const val TRACKING_OPTIONS = "trackingOptions"
        private const val GEOFENCE_TAGS = "geofenceTags"

        @JvmStatic
        fun fromJson(obj: JSONObject?): RadarRemoteTrackingOptions? {
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
            return RadarRemoteTrackingOptions(type, trackingOptions, geofenceTags)
        }
        
        @JvmStatic
        fun fromJson(arr: JSONArray?): Array<RadarRemoteTrackingOptions>? {
            if (arr == null) {
                return null
            }
            return Array(arr.length()) { index ->
                fromJson(arr.optJSONObject(index))
            }.filterNotNull().toTypedArray()
        }

        @JvmStatic
        fun toJson(remoteTrackingOptions: Array<RadarRemoteTrackingOptions>?): JSONArray? {
            if (remoteTrackingOptions == null) {
                return null
            }
            val arr = JSONArray()
            remoteTrackingOptions.forEach { remoteTrackingOption ->
                arr.put(remoteTrackingOption.toJson())
            }
            return arr
        }

        @JvmStatic
        fun getRemoteTrackingOptionsWithKey(remoteTrackingOptions: Array<RadarRemoteTrackingOptions>?, key: String): RadarTrackingOptions? {
            if (remoteTrackingOptions == null) {
                return null
            }
            for (remoteTrackingOption in remoteTrackingOptions) {
                if (remoteTrackingOption.type == key) {
                    return remoteTrackingOption.trackingOptions
                }
            }
            return null
        }

        @JvmStatic
        fun getGeofenceTagsWithKey(remoteTrackingOptions: Array<RadarRemoteTrackingOptions>?, key: String): Array<String>? {
            if (remoteTrackingOptions == null) {
                return null
            }
            var geofenceTags: Array<String>? = null
            for (remoteTrackingOption in remoteTrackingOptions) {
                if (remoteTrackingOption.type == key) {
                    geofenceTags = remoteTrackingOption.geofenceTags
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