package io.radar.sdk

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.os.Build
import androidx.annotation.RequiresApi
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
       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
           val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

            return EncryptedSharedPreferences.create(
                "PreferencesFilename",
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } else {
            return context.getSharedPreferences("RadarSDK", Context.MODE_PRIVATE)
        }
    }

    internal fun getLastLocation(context: Context): Location? {
        val lastLocationLatitude = getSharedPreferences(context).getFloat(KEY_LAST_LOCATION_LATITUDE, 0f)
        val lastLocationLongitude = getSharedPreferences(context).getFloat(KEY_LAST_LOCATION_LONGITUDE, 0f)
        val lastLocationAccuracy = getSharedPreferences(context).getFloat(KEY_LAST_LOCATION_ACCURACY, 0f)
        val lastLocationProvider = getSharedPreferences(context).getString(KEY_LAST_LOCATION_PROVIDER, "RadarSDK")
        val lastLocationTime = getSharedPreferences(context).getLong(KEY_LAST_LOCATION_TIME, 0L)
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
            getSharedPreferences(context).edit {
                remove(KEY_LAST_LOCATION_LATITUDE)
                remove(KEY_LAST_LOCATION_LONGITUDE)
                remove(KEY_LAST_LOCATION_ACCURACY)
                remove(KEY_LAST_LOCATION_PROVIDER)
                remove(KEY_LAST_LOCATION_TIME)
            }

            return
        }

        getSharedPreferences(context).edit {
            putFloat(KEY_LAST_LOCATION_LATITUDE, location.latitude.toFloat())
            putFloat(KEY_LAST_LOCATION_LONGITUDE, location.longitude.toFloat())
            putFloat(KEY_LAST_LOCATION_ACCURACY, location.accuracy)
            putString(KEY_LAST_LOCATION_PROVIDER, location.provider)
            putLong(KEY_LAST_LOCATION_TIME, location.time)
        }
    }

    internal fun getLastMovedLocation(context: Context): Location? {
        val lastMovedLocationLatitude = getSharedPreferences(context).getFloat(KEY_LAST_MOVED_LOCATION_LATITUDE, 0f)
        val lastMovedLocationLongitude = getSharedPreferences(context).getFloat(KEY_LAST_MOVED_LOCATION_LONGITUDE, 0f)
        val lastMovedLocationAccuracy = getSharedPreferences(context).getFloat(KEY_LAST_MOVED_LOCATION_ACCURACY, 0f)
        val lastMovedLocationProvider = getSharedPreferences(context).getString(KEY_LAST_MOVED_LOCATION_PROVIDER, "RadarSDK")
        val lastMovedLocationTime = getSharedPreferences(context).getLong(KEY_LAST_MOVED_LOCATION_TIME, 0L)
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

        getSharedPreferences(context).edit {
            putFloat(KEY_LAST_MOVED_LOCATION_LATITUDE, location.latitude.toFloat())
            putFloat(KEY_LAST_MOVED_LOCATION_LONGITUDE, location.longitude.toFloat())
            putFloat(KEY_LAST_MOVED_LOCATION_ACCURACY, location.accuracy)
            putString(KEY_LAST_MOVED_LOCATION_PROVIDER, location.provider)
            putLong(KEY_LAST_MOVED_LOCATION_TIME, location.time)
        }
    }

    internal fun getLastMovedAt(context: Context): Long {
        return getSharedPreferences(context).getLong(KEY_LAST_MOVED_AT, 0L)
    }

    internal fun setLastMovedAt(context: Context, lastMovedAt: Long) {
        getSharedPreferences(context).edit { putLong(KEY_LAST_MOVED_AT, lastMovedAt) }
    }

    internal fun getStopped(context: Context): Boolean {
        return getSharedPreferences(context).getBoolean(KEY_STOPPED, false)
    }

    internal fun setStopped(context: Context, stopped: Boolean) {
        getSharedPreferences(context).edit { putBoolean(KEY_STOPPED, stopped) }
    }

    internal fun updateLastSentAt(context: Context) {
        getSharedPreferences(context).edit { putLong(KEY_LAST_SENT_AT,  System.currentTimeMillis()) }
    }

    internal fun getLastSentAt(context: Context): Long {
        return getSharedPreferences(context).getLong(KEY_LAST_SENT_AT, 0L)
    }

    internal fun getCanExit(context: Context): Boolean {
        return getSharedPreferences(context).getBoolean(KEY_CAN_EXIT, true)
    }

    internal fun setCanExit(context: Context, canExit: Boolean) {
        getSharedPreferences(context).edit { putBoolean(KEY_CAN_EXIT, canExit) }
    }

    internal fun getLastFailedStoppedLocation(context: Context): Location? {
        val lastFailedStoppedLocationLatitude = getSharedPreferences(context).getFloat(KEY_LAST_FAILED_STOPPED_LOCATION_LATITUDE, 0f)
        val lastFailedStoppedLocationLongitude = getSharedPreferences(context).getFloat(KEY_LAST_FAILED_STOPPED_LOCATION_LONGITUDE, 0f)
        val lastFailedStoppedLocationAccuracy = getSharedPreferences(context).getFloat(KEY_LAST_FAILED_STOPPED_LOCATION_ACCURACY, 0f)
        val lastFailedStoppedLocationProvider = getSharedPreferences(context).getString(KEY_LAST_FAILED_STOPPED_LOCATION_PROVIDER, "RadarSDK")
        val lastFailedStoppedLocationTime = getSharedPreferences(context).getLong(KEY_LAST_FAILED_STOPPED_LOCATION_TIME, 0L)
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
            getSharedPreferences(context).edit {
                remove(KEY_LAST_FAILED_STOPPED_LOCATION_LATITUDE)
                remove(KEY_LAST_FAILED_STOPPED_LOCATION_LONGITUDE)
                remove(KEY_LAST_FAILED_STOPPED_LOCATION_ACCURACY)
                remove(KEY_LAST_FAILED_STOPPED_LOCATION_PROVIDER)
                remove(KEY_LAST_FAILED_STOPPED_LOCATION_TIME)
            }

            return
        }

        getSharedPreferences(context).edit {
            putFloat(KEY_LAST_FAILED_STOPPED_LOCATION_LATITUDE, location.latitude.toFloat())
            putFloat(KEY_LAST_FAILED_STOPPED_LOCATION_LONGITUDE, location.longitude.toFloat())
            putFloat(KEY_LAST_FAILED_STOPPED_LOCATION_ACCURACY, location.accuracy)
            putString(KEY_LAST_FAILED_STOPPED_LOCATION_PROVIDER, location.provider)
            putLong(KEY_LAST_FAILED_STOPPED_LOCATION_TIME, location.time)
        }
    }

    internal fun getGeofenceIds(context: Context): MutableSet<String>? {
        return getSharedPreferences(context).getStringSet(KEY_GEOFENCE_IDS, null)
    }

    internal fun setGeofenceIds(context: Context, geofenceIds: Set<String>?) {
        getSharedPreferences(context).edit { putStringSet(KEY_GEOFENCE_IDS, geofenceIds) }
    }

    internal fun getPlaceId(context: Context): String? {
        return getSharedPreferences(context).getString(KEY_PLACE_ID, null)
    }

    internal fun setPlaceId(context: Context, placeId: String?) {
        getSharedPreferences(context).edit { putString(KEY_PLACE_ID, placeId) }
    }

    internal fun getRegionIds(context: Context): MutableSet<String>? {
        return getSharedPreferences(context).getStringSet(KEY_REGION_IDS, null)
    }

    internal fun setRegionIds(context: Context, regionIds: Set<String>?) {
        getSharedPreferences(context).edit { putStringSet(KEY_REGION_IDS, regionIds) }
    }

    internal fun getBeaconIds(context: Context): MutableSet<String>? {
        return getSharedPreferences(context).getStringSet(KEY_BEACON_IDS, null)
    }

    internal fun setBeaconIds(context: Context, beaconIds: Set<String>?) {
        getSharedPreferences(context).edit { putStringSet(KEY_BEACON_IDS, beaconIds) }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    internal fun getLastBeacons(context: Context): Array<RadarBeacon>? {
        val set = getSharedPreferences(context).getStringSet(KEY_LAST_BEACONS, null)
        return RadarBeaconUtils.beaconsForStringSet(set)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    internal fun setLastBeacons(context: Context, beacons: Array<RadarBeacon>?) {
        val set = RadarBeaconUtils.stringSetForBeacons(beacons)
        getSharedPreferences(context).edit { putStringSet(KEY_LAST_BEACONS, set) }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    internal fun getLastBeaconUUIDs(context: Context): Array<String>? {
        return getSharedPreferences(context).getStringSet(KEY_LAST_BEACON_UUIDS, null)?.toTypedArray()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    internal fun setLastBeaconUUIDs(context: Context, uuids: Array<String>?) {
        getSharedPreferences(context).edit { putStringSet(KEY_LAST_BEACON_UUIDS, uuids?.toSet()) }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    internal fun getLastBeaconUIDs(context: Context): Array<String>? {
        return getSharedPreferences(context).getStringSet(KEY_LAST_BEACON_UIDS, null)?.toTypedArray()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    internal fun setLastBeaconUIDs(context: Context, uids: Array<String>?) {
        getSharedPreferences(context).edit { putStringSet(KEY_LAST_BEACON_UIDS, uids?.toSet()) }
    }

}