package io.radar.sdk

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import io.radar.sdk.model.RadarBeacon

internal object RadarState {

    private const val KEY_STARTED = "has_started"
    private const val KEY_LAST_LOCATION_LATITUDE = "last_location_latitude"
    private const val KEY_LAST_LOCATION_LONGITUDE = "last_location_longitude"
    private const val KEY_LAST_LOCATION_ACCURACY = "last_location_accuracy"
    private const val KEY_LAST_LOCATION_PROVIDER = "last_location_provider"
    private const val KEY_LAST_LOCATION_TIME = "last_location_time"
    private const val KEY_LAST_MOVED_LOCATION_LATITUDE = "last_moved_location_latitude"
    private const val KEY_LAST_MOVED_LOCATION_LONGITUDE = "last_moved_location_longitude"
    private const val KEY_LAST_MOVED_LOCATION_ACCURACY = "last_moved_location_accuracy"
    private const val KEY_LAST_MOVED_LOCATION_PROVIDER = "last_moved_location_provider"
    private const val KEY_LAST_MOVED_LOCATION_TIME = "last_moved_location_time"
    private const val KEY_LAST_MOVED_AT = "last_moved_at"
    private const val KEY_STOPPED = "stopped"
    private const val KEY_LAST_SENT_AT = "last_sent_at"
    private const val KEY_CAN_EXIT = "can_exit"
    private const val KEY_LAST_FAILED_STOPPED_LOCATION_LATITUDE = "last_failed_stopped_location_latitude"
    private const val KEY_LAST_FAILED_STOPPED_LOCATION_LONGITUDE = "last_failed_stopped_location_longitude"
    private const val KEY_LAST_FAILED_STOPPED_LOCATION_ACCURACY = "last_failed_stopped_location_accuracy"
    private const val KEY_LAST_FAILED_STOPPED_LOCATION_PROVIDER = "last_failed_stopped_location_provider"
    private const val KEY_LAST_FAILED_STOPPED_LOCATION_TIME = "last_failed_stopped_location_time"
    private const val KEY_GEOFENCE_IDS = "geofence_ids"
    private const val KEY_PLACE_ID = "place_id"
    private const val KEY_REGION_IDS = "region_ids"
    private const val KEY_BEACON_IDS = "beacon_ids"
    private const val KEY_LAST_BEACONS = "last_beacons"
    private const val KEY_LAST_BEACON_UUIDS = "last_beacon_uuids"
    private const val KEY_LAST_BEACON_UIDS = "last_beacon_uids"

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences("RadarSDK", Context.MODE_PRIVATE)
    }

    private fun getEncryptedSharedPreferences(context: Context): SharedPreferences {

        return EncryptedSharedPreferences.create(
            "RadarState",
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

    }

    private fun getFloat(context: Context, key: String, default: Float): Float {
        return getEncryptedSharedPreferences(context).getFloat(key, getSharedPreferences(context).getFloat(key, default))
    }

    private fun getLong(context: Context, key: String, default: Long): Long {
        return getEncryptedSharedPreferences(context).getLong(key, getSharedPreferences(context).getLong(key, default))
    }

    private fun getString(context: Context, key: String, default: String?): String? {
        return getEncryptedSharedPreferences(context).getString(key, getSharedPreferences(context).getString(key, default))

    }

    private fun getStringSet(context: Context, key: String, default: Set<String>?): Set<String>? {
        return getEncryptedSharedPreferences(context).getStringSet(key, getSharedPreferences(context).getStringSet(key, default))
    }

    private fun getBoolean(context: Context, key: String, default: Boolean): Boolean {
        return getEncryptedSharedPreferences(context).getBoolean(key, getSharedPreferences(context).getBoolean(key, default))
    }

    internal fun getLastLocation(context: Context): Location? {
        val lastLocationLatitude = getFloat(context, KEY_LAST_LOCATION_LATITUDE, 0f)
        val lastLocationLongitude = getFloat(context, KEY_LAST_LOCATION_LONGITUDE, 0f)
        val lastLocationAccuracy = getFloat(context, KEY_LAST_LOCATION_ACCURACY, 0f)
        val lastLocationProvider = getString(context, KEY_LAST_LOCATION_PROVIDER, "RadarSDK")
        val lastLocationTime = getLong(context, KEY_LAST_LOCATION_TIME, 0L)
        val location = Location(lastLocationProvider)
        location.latitude = lastLocationLatitude.toDouble()
        location.longitude = lastLocationLongitude.toDouble()
        location.accuracy = lastLocationAccuracy
        location.time = lastLocationTime

        if (!RadarUtils.valid(location)) {
            return null
        }

        return location
    }

    internal fun setLastLocation(context: Context, location: Location?) {
        if (location == null || !RadarUtils.valid(location)) {
            getEncryptedSharedPreferences(context).edit {
                remove(KEY_LAST_LOCATION_LATITUDE)
                remove(KEY_LAST_LOCATION_LONGITUDE)
                remove(KEY_LAST_LOCATION_ACCURACY)
                remove(KEY_LAST_LOCATION_PROVIDER)
                remove(KEY_LAST_LOCATION_TIME)
            }

            return
        }

        getEncryptedSharedPreferences(context).edit {
            putFloat(KEY_LAST_LOCATION_LATITUDE, location.latitude.toFloat())
            putFloat(KEY_LAST_LOCATION_LONGITUDE, location.longitude.toFloat())
            putFloat(KEY_LAST_LOCATION_ACCURACY, location.accuracy)
            putString(KEY_LAST_LOCATION_PROVIDER, location.provider)
            putLong(KEY_LAST_LOCATION_TIME, location.time)
        }
    }

    internal fun getLastMovedLocation(context: Context): Location? {
        val lastMovedLocationLatitude = getFloat(context, KEY_LAST_MOVED_LOCATION_LATITUDE, 0f)
        val lastMovedLocationLongitude = getFloat(context, KEY_LAST_MOVED_LOCATION_LONGITUDE, 0f)
        val lastMovedLocationAccuracy = getFloat(context, KEY_LAST_MOVED_LOCATION_ACCURACY, 0f)
        val lastMovedLocationProvider = getString(context, KEY_LAST_MOVED_LOCATION_PROVIDER, "RadarSDK")
        val lastMovedLocationTime = getLong(context, KEY_LAST_MOVED_LOCATION_TIME, 0L)
        val location = Location(lastMovedLocationProvider)
        location.latitude = lastMovedLocationLatitude.toDouble()
        location.longitude = lastMovedLocationLongitude.toDouble()
        location.accuracy = lastMovedLocationAccuracy
        location.time = lastMovedLocationTime

        if (!RadarUtils.valid(location)) {
            return null
        }

        return location
    }

    internal fun setLastMovedLocation(context: Context, location: Location?) {
        if (location == null || !RadarUtils.valid(location)) {
            return
        }

        getEncryptedSharedPreferences(context).edit {
            putFloat(KEY_LAST_MOVED_LOCATION_LATITUDE, location.latitude.toFloat())
            putFloat(KEY_LAST_MOVED_LOCATION_LONGITUDE, location.longitude.toFloat())
            putFloat(KEY_LAST_MOVED_LOCATION_ACCURACY, location.accuracy)
            putString(KEY_LAST_MOVED_LOCATION_PROVIDER, location.provider)
            putLong(KEY_LAST_MOVED_LOCATION_TIME, location.time)
        }
    }

    internal fun getLastMovedAt(context: Context): Long {
        return getLong(context, KEY_LAST_MOVED_AT, 0L)
    }

    internal fun setLastMovedAt(context: Context, lastMovedAt: Long) {
        getEncryptedSharedPreferences(context).edit { putLong(KEY_LAST_MOVED_AT, lastMovedAt) }
    }

    internal fun getStopped(context: Context): Boolean {
        return getBoolean(context, KEY_STOPPED, false)
    }

    internal fun setStopped(context: Context, stopped: Boolean) {
        getEncryptedSharedPreferences(context).edit { putBoolean(KEY_STOPPED, stopped) }
    }

    internal fun updateLastSentAt(context: Context) {
        getEncryptedSharedPreferences(context).edit { putLong(KEY_LAST_SENT_AT,  System.currentTimeMillis()) }
    }

    internal fun getLastSentAt(context: Context): Long {
        return getLong(context, KEY_LAST_SENT_AT, 0L)
    }

    internal fun getCanExit(context: Context): Boolean {
        return getBoolean(context, KEY_CAN_EXIT, true)
    }

    internal fun setCanExit(context: Context, canExit: Boolean) {
        getEncryptedSharedPreferences(context).edit { putBoolean(KEY_CAN_EXIT, canExit) }
    }

    internal fun getLastFailedStoppedLocation(context: Context): Location? {
        val lastFailedStoppedLocationLatitude = getFloat(context, KEY_LAST_FAILED_STOPPED_LOCATION_LATITUDE, 0f)
        val lastFailedStoppedLocationLongitude = getFloat(context, KEY_LAST_FAILED_STOPPED_LOCATION_LONGITUDE, 0f)
        val lastFailedStoppedLocationAccuracy = getFloat(context, KEY_LAST_FAILED_STOPPED_LOCATION_ACCURACY, 0f)
        val lastFailedStoppedLocationProvider = getString(context, KEY_LAST_FAILED_STOPPED_LOCATION_PROVIDER, "RadarSDK")
        val lastFailedStoppedLocationTime = getLong(context, KEY_LAST_FAILED_STOPPED_LOCATION_TIME, 0L)
        val location = Location(lastFailedStoppedLocationProvider)
        location.latitude = lastFailedStoppedLocationLatitude.toDouble()
        location.longitude = lastFailedStoppedLocationLongitude.toDouble()
        location.accuracy = lastFailedStoppedLocationAccuracy
        location.time = lastFailedStoppedLocationTime

        if (!RadarUtils.valid(location)) {
            return null
        }

        return location
    }

    internal fun setLastFailedStoppedLocation(context: Context, location: Location?) {
        if (location == null || !RadarUtils.valid(location)) {
            getEncryptedSharedPreferences(context).edit {
                remove(KEY_LAST_FAILED_STOPPED_LOCATION_LATITUDE)
                remove(KEY_LAST_FAILED_STOPPED_LOCATION_LONGITUDE)
                remove(KEY_LAST_FAILED_STOPPED_LOCATION_ACCURACY)
                remove(KEY_LAST_FAILED_STOPPED_LOCATION_PROVIDER)
                remove(KEY_LAST_FAILED_STOPPED_LOCATION_TIME)
            }

            return
        }

        getEncryptedSharedPreferences(context).edit {
            putFloat(KEY_LAST_FAILED_STOPPED_LOCATION_LATITUDE, location.latitude.toFloat())
            putFloat(KEY_LAST_FAILED_STOPPED_LOCATION_LONGITUDE, location.longitude.toFloat())
            putFloat(KEY_LAST_FAILED_STOPPED_LOCATION_ACCURACY, location.accuracy)
            putString(KEY_LAST_FAILED_STOPPED_LOCATION_PROVIDER, location.provider)
            putLong(KEY_LAST_FAILED_STOPPED_LOCATION_TIME, location.time)
        }
    }

    internal fun getGeofenceIds(context: Context): MutableSet<String>? {
        return getStringSet(context, KEY_GEOFENCE_IDS, null)?.toMutableSet()
    }

    internal fun setGeofenceIds(context: Context, geofenceIds: Set<String>?) {
        getEncryptedSharedPreferences(context).edit { putStringSet(KEY_GEOFENCE_IDS, geofenceIds) }
    }

    internal fun getPlaceId(context: Context): String? {
        return getString(context, KEY_PLACE_ID, null)
    }

    internal fun setPlaceId(context: Context, placeId: String?) {
        getEncryptedSharedPreferences(context).edit { putString(KEY_PLACE_ID, placeId) }
    }

    internal fun getRegionIds(context: Context): MutableSet<String>? {
        return getStringSet(context, KEY_REGION_IDS, null)?.toMutableSet()
    }

    internal fun setRegionIds(context: Context, regionIds: Set<String>?) {
        getEncryptedSharedPreferences(context).edit { putStringSet(KEY_REGION_IDS, regionIds) }
    }

    internal fun getBeaconIds(context: Context): MutableSet<String>? {
        return getStringSet(context, KEY_BEACON_IDS, null)?.toMutableSet()
    }

    internal fun setBeaconIds(context: Context, beaconIds: Set<String>?) {
        getEncryptedSharedPreferences(context).edit { putStringSet(KEY_BEACON_IDS, beaconIds) }
    }

    internal fun getLastBeacons(context: Context): Array<RadarBeacon>? {
        val set = getStringSet(context, KEY_LAST_BEACONS, null)
        return RadarBeaconUtils.beaconsForStringSet(set)
    }

    internal fun setLastBeacons(context: Context, beacons: Array<RadarBeacon>?) {
        val set = RadarBeaconUtils.stringSetForBeacons(beacons)
        getEncryptedSharedPreferences(context).edit { putStringSet(KEY_LAST_BEACONS, set) }
    }

    internal fun getLastBeaconUUIDs(context: Context): Array<String>? {
        return getStringSet(context, KEY_LAST_BEACON_UUIDS, null)?.toTypedArray()
    }

    internal fun setLastBeaconUUIDs(context: Context, uuids: Array<String>?) {
        getEncryptedSharedPreferences(context).edit { putStringSet(KEY_LAST_BEACON_UUIDS, uuids?.toSet()) }
    }

    internal fun getLastBeaconUIDs(context: Context): Array<String>? {
        return getStringSet(context, KEY_LAST_BEACON_UIDS, null)?.toTypedArray()
    }

    internal fun setLastBeaconUIDs(context: Context, uids: Array<String>?) {
        getEncryptedSharedPreferences(context).edit { putStringSet(KEY_LAST_BEACON_UIDS, uids?.toSet()) }
    }

}