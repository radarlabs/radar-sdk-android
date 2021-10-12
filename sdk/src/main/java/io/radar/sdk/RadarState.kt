package io.radar.sdk

import android.content.SharedPreferences
import android.location.Location
import androidx.core.content.edit

@Suppress("TooManyFunctions")
internal class RadarState(val context: RadarApplication) {

    companion object {
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
        private const val RADAR = "RadarSDK"
    }

    private fun getSharedPreferences(): SharedPreferences {
        return RadarUtils.getSharedPreferences(context)
    }

    internal fun getLastMovedLocation(): Location? {
        val lastMovedLocationLatitude = getSharedPreferences().getFloat(KEY_LAST_MOVED_LOCATION_LATITUDE, 0f)
        val lastMovedLocationLongitude = getSharedPreferences().getFloat(KEY_LAST_MOVED_LOCATION_LONGITUDE, 0f)
        val lastMovedLocationAccuracy = getSharedPreferences().getFloat(KEY_LAST_MOVED_LOCATION_ACCURACY, 0f)
        val lastMovedLocationProvider = getSharedPreferences().getString(KEY_LAST_MOVED_LOCATION_PROVIDER, RADAR)
        val lastMovedLocationTime = getSharedPreferences().getLong(KEY_LAST_MOVED_LOCATION_TIME, 0L)
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

    internal fun setLastMovedLocation(location: Location?) {
        if (location == null || !RadarUtils.valid(location)) {
            return
        }

        getSharedPreferences().edit {
            putFloat(KEY_LAST_MOVED_LOCATION_LATITUDE, location.latitude.toFloat())
            putFloat(KEY_LAST_MOVED_LOCATION_LONGITUDE, location.longitude.toFloat())
            putFloat(KEY_LAST_MOVED_LOCATION_ACCURACY, location.accuracy)
            putString(KEY_LAST_MOVED_LOCATION_PROVIDER, location.provider)
            putLong(KEY_LAST_MOVED_LOCATION_TIME, location.time)
        }
    }

    internal fun getLastMovedAt(): Long {
        return getSharedPreferences().getLong(KEY_LAST_MOVED_AT, 0L)
    }

    internal fun setLastMovedAt(lastMovedAt: Long) {
        getSharedPreferences().edit { putLong(KEY_LAST_MOVED_AT, lastMovedAt) }
    }

    internal fun getStopped(): Boolean {
        return getSharedPreferences().getBoolean(KEY_STOPPED, false)
    }

    internal fun setStopped(stopped: Boolean) {
        getSharedPreferences().edit { putBoolean(KEY_STOPPED, stopped) }
    }

    internal fun updateLastSentAt() {
        getSharedPreferences().edit { putLong(KEY_LAST_SENT_AT,  System.currentTimeMillis()) }
    }

    internal fun getLastSentAt(): Long {
        return getSharedPreferences().getLong(KEY_LAST_SENT_AT, 0L)
    }

    internal fun getCanExit(): Boolean {
        return getSharedPreferences().getBoolean(KEY_CAN_EXIT, true)
    }

    internal fun setCanExit(canExit: Boolean) {
        getSharedPreferences().edit { putBoolean(KEY_CAN_EXIT, canExit) }
    }

    internal fun getLastFailedStoppedLocation(): Location? {
        val prefs = getSharedPreferences()
        val lastFailedStoppedLocationLatitude = prefs.getFloat(KEY_LAST_FAILED_STOPPED_LOCATION_LATITUDE, 0f)
        val lastFailedStoppedLocationLongitude = prefs.getFloat(KEY_LAST_FAILED_STOPPED_LOCATION_LONGITUDE, 0f)
        val lastFailedStoppedLocationAccuracy = prefs.getFloat(KEY_LAST_FAILED_STOPPED_LOCATION_ACCURACY, 0f)
        val lastFailedStoppedLocationProvider = prefs.getString(KEY_LAST_FAILED_STOPPED_LOCATION_PROVIDER, RADAR)
        val lastFailedStoppedLocationTime = prefs.getLong(KEY_LAST_FAILED_STOPPED_LOCATION_TIME, 0L)
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

    internal fun setLastFailedStoppedLocation(location: Location?) {
        if (location == null || !RadarUtils.valid(location)) {
            getSharedPreferences().edit {
                remove(KEY_LAST_FAILED_STOPPED_LOCATION_LATITUDE)
                remove(KEY_LAST_FAILED_STOPPED_LOCATION_LONGITUDE)
                remove(KEY_LAST_FAILED_STOPPED_LOCATION_ACCURACY)
                remove(KEY_LAST_FAILED_STOPPED_LOCATION_PROVIDER)
                remove(KEY_LAST_FAILED_STOPPED_LOCATION_TIME)
            }

            return
        }

        getSharedPreferences().edit {
            putFloat(KEY_LAST_FAILED_STOPPED_LOCATION_LATITUDE, location.latitude.toFloat())
            putFloat(KEY_LAST_FAILED_STOPPED_LOCATION_LONGITUDE, location.longitude.toFloat())
            putFloat(KEY_LAST_FAILED_STOPPED_LOCATION_ACCURACY, location.accuracy)
            putString(KEY_LAST_FAILED_STOPPED_LOCATION_PROVIDER, location.provider)
            putLong(KEY_LAST_FAILED_STOPPED_LOCATION_TIME, location.time)
        }
    }

}