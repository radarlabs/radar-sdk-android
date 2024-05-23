package io.radar.sdk.model

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import org.json.JSONObject

class RadarLocationPermissionsStatus {

    companion object {
        private const val PREFS_NAME = "RadarLocationPermissionsStatus"
        private const val STATUS_KEY = "status"
        private const val DENIED_KEY = "denied"

        fun getUpdatedStatus(context: Context, activity: Activity, inLocationPopup: Boolean = false): RadarLocationPermissionsStatus? {
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val foregroundPopupRequested = prefs.getBoolean(STATUS_KEY, false)
            val previouslyDeniedForeground = prefs.getBoolean(DENIED_KEY, false)
            return fromForegroundPopupRequested(context, activity, foregroundPopupRequested, previouslyDeniedForeground, inLocationPopup)
        }

        fun saveForegroundPopupRequested(context: Context, foregroundPopupRequested: Boolean) {
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor: SharedPreferences.Editor = prefs.edit()
            editor.putBoolean(STATUS_KEY, foregroundPopupRequested)
            editor.apply()
        }

        private fun savePreviouslyDeniedForeground(context: Context, previouslyDeniedForeground: Boolean) {
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor: SharedPreferences.Editor = prefs.edit()
            editor.putBoolean(DENIED_KEY, previouslyDeniedForeground)
            editor.apply()
        }

        internal const val KEY_STATUS = "status"
        internal const val KEY_FOREGROUND_POPUP_REQUESTED = "foregroundPopupRequested"
        internal const val KEY_FOREGROUND_PERMISSIONS_RESULT = "foregroundPermissionResult"
        internal const val KEY_BACKGROUND_PERMISSIONS_RESULT = "backgroundPermissionResult"
        internal const val KEY_SHOULD_SHOW_REQUEST_PERMISSION_RATIONALE = "shouldShowRequestPermissionRationale"
        internal const val KEY_PREVIOUSLY_DENIED_FOREGROUND = "previouslyDeniedForeground"
        internal const val KEY_APPROXIMATE_PERMISSIONS_REQUEST = "approximatePermissionsRequest"
        internal const val KEY_BACKGROUND_PERMISSIONS_AVAILIBLE = "backgroundPermissionsAvailable"

        private fun fromForegroundPopupRequested(context: Context, activity: Activity, foregroundPopupRequested:Boolean, previouslyDeniedForeground: Boolean, inLocationPopup:Boolean): RadarLocationPermissionsStatus {
            val newStatus = RadarLocationPermissionsStatus()
            newStatus.foregroundPopupRequested = foregroundPopupRequested
            newStatus.foregroundPermissionResult = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            newStatus.approximatePermissionsResult = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                newStatus.backgroundPermissionResult = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
            } else {
                newStatus.backgroundPermissionResult = false
            }
            newStatus.shouldShowRequestPermissionRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                    activity, Manifest.permission.ACCESS_FINE_LOCATION);

            val shouldShowRequestPermissionRationaleBG = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ActivityCompat.shouldShowRequestPermissionRationale(
                    activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                Log.d("devTest","we should not be here")
                false
            };

            newStatus.shouldShowRequestPermissionRationaleBG = shouldShowRequestPermissionRationaleBG
       
            // if this is true, we know foreground permission was denied once and we should mark the flag
            if (newStatus.shouldShowRequestPermissionRationale && !previouslyDeniedForeground) {
                savePreviouslyDeniedForeground(context, true)
                newStatus.previouslyDeniedForeground = true
            } else {
                newStatus.previouslyDeniedForeground = previouslyDeniedForeground
            }
            newStatus.inLocationPopup = inLocationPopup
            // we map the different states to the status to be implemented
            newStatus.status = mapToStatus(newStatus)
            return newStatus
        }

        private fun mapToStatus(status: RadarLocationPermissionsStatus): LocationPermissionStatus {
            
            if (status.backgroundPermissionResult) {
                return LocationPermissionStatus.BACKGROUND_PERMISSIONS_GRANTED
            }
            if (!status.shouldShowRequestPermissionRationaleBG) {
                return LocationPermissionStatus.BACKGROUND_PERMISSIONS_REJECTED
            }
            if (status.foregroundPermissionResult) {
                return LocationPermissionStatus.FOREGROUND_PERMISSIONS_GRANTED
            } else {
                if (status.inLocationPopup) {
                    return LocationPermissionStatus.FOREGROUND_LOCATION_PENDING
                }
                if (status.approximatePermissionsResult){
                    return LocationPermissionStatus.APPROXIMATE_PERMISSIONS_GRANTED
                }
                if (status.shouldShowRequestPermissionRationale) {
                    return LocationPermissionStatus.FOREGROUND_PERMISSIONS_REJECTED_ONCE
                } else {
                    // do we need this? we should reason about it a little more.
                    if (status.foregroundPopupRequested) {
                        if (status.previouslyDeniedForeground) {
                            return LocationPermissionStatus.FOREGROUND_PERMISSIONS_REJECTED
                        }
                    } else {
                        return LocationPermissionStatus.NO_PERMISSIONS_GRANTED
                    }
                }
            }
            return LocationPermissionStatus.UNKNOWN
        }

    }

    fun toJson(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put(KEY_STATUS, status.name)
        jsonObject.put(KEY_FOREGROUND_POPUP_REQUESTED, foregroundPopupRequested)
        jsonObject.put(KEY_FOREGROUND_PERMISSIONS_RESULT, foregroundPermissionResult)
        jsonObject.put(KEY_BACKGROUND_PERMISSIONS_RESULT, backgroundPermissionResult)
        jsonObject.put(KEY_SHOULD_SHOW_REQUEST_PERMISSION_RATIONALE, shouldShowRequestPermissionRationale)
        jsonObject.put(KEY_PREVIOUSLY_DENIED_FOREGROUND,previouslyDeniedForeground)
        jsonObject.put(KEY_APPROXIMATE_PERMISSIONS_REQUEST,approximatePermissionsResult)
        jsonObject.put(KEY_BACKGROUND_PERMISSIONS_AVAILIBLE,shouldShowRequestPermissionRationaleBG)
        return jsonObject
    }

    enum class LocationPermissionStatus {
        NO_PERMISSIONS_GRANTED,
        FOREGROUND_PERMISSIONS_GRANTED,
        APPROXIMATE_PERMISSIONS_GRANTED,
        FOREGROUND_PERMISSIONS_REJECTED_ONCE,
        FOREGROUND_PERMISSIONS_REJECTED,
        FOREGROUND_LOCATION_PENDING,
        BACKGROUND_PERMISSIONS_GRANTED,
        BACKGROUND_PERMISSIONS_REJECTED,
        UNKNOWN
    }

    var status: LocationPermissionStatus = LocationPermissionStatus.UNKNOWN
    var foregroundPopupRequested: Boolean = false
    var foregroundPermissionResult: Boolean = false
    var backgroundPermissionResult: Boolean = false
    var shouldShowRequestPermissionRationale: Boolean = false
    var shouldShowRequestPermissionRationaleBG: Boolean = false
    var previouslyDeniedForeground: Boolean = false
    var inLocationPopup: Boolean = false
    var approximatePermissionsResult: Boolean = false

}