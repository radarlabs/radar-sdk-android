package io.radar.sdk

import io.radar.sdk.model.RadarBeacon
import io.radar.sdk.model.RadarCoordinate
import io.radar.sdk.model.RadarGeofence
import io.radar.sdk.model.RadarPlace
import org.json.JSONArray
import org.json.JSONObject

internal data class RadarSyncState(
    var syncedRegionCenter: RadarCoordinate? = null,
    var syncedRegionRadius: Double? = null,
    var syncedGeofences: List<RadarGeofence>? = null,
    var syncedPlaces: List<RadarPlace>? = null,
    var syncedBeacons: List<RadarBeacon>? = null,
    var lastSyncedGeofenceIds: List<String> = emptyList(),
    var lastSyncedPlaceIds: List<String> = emptyList(),
    var lastSyncedBeaconIds: List<String> = emptyList(),
    var geofenceEntryTimestamps: MutableMap<String, Double> = mutableMapOf(),
    var dwellEventsFired: MutableList<String> = mutableListOf()
) {
    companion object {
        private const val KEY_REGION_CENTER = "syncedRegionCenter"
        private const val KEY_REGION_RADIUS = "syncedRegionRadius"
        private const val KEY_GEOFENCES = "syncedGeofences"
        private const val KEY_PLACES = "syncedPlaces"
        private const val KEY_BEACONS = "syncedBeacons"
        private const val KEY_LAST_GEOFENCE_IDS = "lastSyncedGeofenceIds"
        private const val KEY_LAST_PLACE_IDS = "lastSyncedPlaceIds"
        private const val KEY_LAST_BEACON_IDS = "lastSyncedBeaconIds"
        private const val KEY_ENTRY_TIMESTAMPS = "geofenceEntryTimestamps"
        private const val KEY_DWELL_FIRED = "dwellEventsFired"

        fun fromJson(obj: JSONObject): RadarSyncState {
            val state = RadarSyncState()

            state.syncedRegionCenter = RadarCoordinate.fromJson(obj.optJSONObject(KEY_REGION_CENTER))

            if (obj.has(KEY_REGION_RADIUS)) {
                state.syncedRegionRadius = obj.optDouble(KEY_REGION_RADIUS)
            }

            state.syncedGeofences = RadarGeofence.fromJson(obj.optJSONArray(KEY_GEOFENCES))?.toList()
            state.syncedPlaces = RadarPlace.fromJson(obj.optJSONArray(KEY_PLACES))?.toList()
            state.syncedBeacons = RadarBeacon.fromJson(obj.optJSONArray(KEY_BEACONS))?.toList()

            obj.optJSONArray(KEY_LAST_GEOFENCE_IDS)?.let { arr ->
                state.lastSyncedGeofenceIds = (0 until arr.length()).map { arr.getString(it) }
            }
            obj.optJSONArray(KEY_LAST_PLACE_IDS)?.let { arr ->
                state.lastSyncedPlaceIds = (0 until arr.length()).map { arr.getString(it) }
            }
            obj.optJSONArray(KEY_LAST_BEACON_IDS)?.let { arr ->
                state.lastSyncedBeaconIds = (0 until arr.length()).map { arr.getString(it) }
            }

            obj.optJSONObject(KEY_ENTRY_TIMESTAMPS)?.let { tsObj ->
                tsObj.keys().forEach { key ->
                    state.geofenceEntryTimestamps[key] = tsObj.getDouble(key)
                }
            }

            obj.optJSONArray(KEY_DWELL_FIRED)?.let { arr ->
                state.dwellEventsFired = (0 until arr.length()).map { arr.getString(it) }.toMutableList()
            }

            return state
        }
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()

        syncedRegionCenter?.let { obj.put(KEY_REGION_CENTER, it.toJson()) }
        syncedRegionRadius?.let { obj.put(KEY_REGION_RADIUS, it) }

        syncedGeofences?.let { obj.put(KEY_GEOFENCES, RadarGeofence.toJson(it.toTypedArray())) }
        syncedPlaces?.let { obj.put(KEY_PLACES, RadarPlace.toJson(it.toTypedArray())) }
        syncedBeacons?.let { obj.put(KEY_BEACONS, RadarBeacon.toJson(it.toTypedArray())) }

        obj.put(KEY_LAST_GEOFENCE_IDS, JSONArray(lastSyncedGeofenceIds))
        obj.put(KEY_LAST_PLACE_IDS, JSONArray(lastSyncedPlaceIds))
        obj.put(KEY_LAST_BEACON_IDS, JSONArray(lastSyncedBeaconIds))

        obj.put(KEY_ENTRY_TIMESTAMPS, JSONObject().apply {
            geofenceEntryTimestamps.forEach { (k, v) -> put(k, v) }
        })
        obj.put(KEY_DWELL_FIRED, JSONArray(dwellEventsFired))

        return obj
    }
}