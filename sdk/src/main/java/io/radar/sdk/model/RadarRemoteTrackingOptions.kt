package io.radar.sdk.model

import io.radar.sdk.RadarTrackingOptions
import org.json.JSONArray
import org.json.JSONObject

internal data class RadarRemoteTrackingOptions(
    val type: String,
    val trackingOptions: RadarTrackingOptions,
    val geofenceTags: List<String>?
) {
    companion object {
        fun fromJson(json: JSONObject): RadarRemoteTrackingOptions? {
            val type = json.optString("type")
            if (type.isBlank()) return null
            val trackingOptionsJson = json.optJSONObject("trackingOptions") ?: return  null
            val trackingOptions = RadarTrackingOptions.fromJson(trackingOptionsJson)

            val geofenceTags = json.optJSONArray("geofenceTags")?.let { arr ->
                (0 until arr.length())
                    .map { arr.optString(it) }
                    .filter { it.isNotBlank() }
            }

            return RadarRemoteTrackingOptions(type, trackingOptions, geofenceTags)
        }

        fun fromJsonArray(array: JSONArray?): List<RadarRemoteTrackingOptions>? {
            if (array == null || array.length() == 0) return null
            return (0 until array.length()).mapNotNull { i ->
                array.optJSONObject(i)?.let { fromJson(it) }
            }.ifEmpty { null }
        }

        fun toJsonArray(options: List<RadarRemoteTrackingOptions>?): JSONArray? {
            if (options == null) return null
            return JSONArray().apply {
                options.forEach { put(it.toJson()) }
            }
        }

        fun trackingOptions(forKey: String, options: List<RadarRemoteTrackingOptions>?): RadarTrackingOptions? {
            return options?.firstOrNull { it.type == forKey }?.trackingOptions
        }

        fun geofenceTags(forKey: String, options: List<RadarRemoteTrackingOptions>?): List<String>? {
            return options?.firstOrNull { it.type == forKey }?.geofenceTags
        }
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("type", type)
            put("trackingOptions", trackingOptions.toJson())
            geofenceTags?.let  { put("geofenceTags", JSONArray(it)) }
        }
    }
}