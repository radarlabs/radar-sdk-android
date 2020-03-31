package io.radar.sdk

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import androidx.core.content.edit

internal object RadarState {

    private const val KEY_STARTED = "has_started"
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

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences("RadarSDK", Context.MODE_PRIVATE)
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

}