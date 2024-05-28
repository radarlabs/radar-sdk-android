package io.radar.sdk.model

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import org.json.JSONObject

class RadarLocationPermissionsStatus {

    companion object {
        private const val PREFS_NAME = "RadarLocationPermissionsStatus"

        internal const val KEY_STATUS = "status"
        internal const val KEY_FOREGROUND_PERMISSIONS_RESULT = "foregroundPermissionResult"
        internal const val KEY_BACKGROUND_PERMISSIONS_RESULT = "backgroundPermissionResult"
        internal const val KEY_SHOULD_SHOW_REQUEST_PERMISSION_RATIONALE_FG = "shouldShowRequestPermissionRationale"
        internal const val KEY_PREVIOUSLY_DENIED_FOREGROUND = "previouslyDeniedForeground"
        internal const val KEY_PREVIOUSLY_DENIED_BACKGROUND = "previouslyDeniedBackground"
        internal const val KEY_APPROXIMATE_PERMISSIONS_REQUEST = "approximatePermissionsRequest"
        internal const val KEY_SHOULD_SHOW_REQUEST_PERMISSION_RATIONALE_BG = "backgroundPermissionsAvailable"

        fun initWithStatus(context: Context, activity: Activity, inLocationPopup: Boolean = false): RadarLocationPermissionsStatus {

            val newStatus = RadarLocationPermissionsStatus()
            newStatus.foregroundPermissionResult = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            newStatus.approximatePermissionsResult = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                newStatus.backgroundPermissionResult = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
            } else {
                newStatus.backgroundPermissionResult = false
            }
            newStatus.shouldShowRequestPermissionRationaleFG = ActivityCompat.shouldShowRequestPermissionRationale(
                activity, Manifest.permission.ACCESS_FINE_LOCATION)

            newStatus.shouldShowRequestPermissionRationaleBG= if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ActivityCompat.shouldShowRequestPermissionRationale(
                    activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                false
            }

            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (newStatus.foregroundPermissionResult){
                savePreviouslyDeniedForeground(context,false)
            }
            newStatus.previouslyDeniedForeground = prefs.getBoolean(KEY_FOREGROUND_PERMISSIONS_RESULT, false)
            if (newStatus.backgroundPermissionResult) {
                savePreviouslyDeniedBackground(context,false)
            }
            newStatus.previouslyDeniedBackground = prefs.getBoolean(KEY_PREVIOUSLY_DENIED_BACKGROUND, false)

            newStatus.inLocationPopup = inLocationPopup
            newStatus.status = locationPermissionStateForLocationManagerStatus(newStatus)

            return newStatus
        }

        fun savePreviouslyDeniedForeground(context: Context, previouslyDeniedForeground: Boolean) {
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor: SharedPreferences.Editor = prefs.edit()
            editor.putBoolean(KEY_FOREGROUND_PERMISSIONS_RESULT, previouslyDeniedForeground)
            editor.apply()
        }

        fun savePreviouslyDeniedBackground(context: Context, previouslyDeniedBackground: Boolean) {
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor: SharedPreferences.Editor = prefs.edit()
            editor.putBoolean(KEY_PREVIOUSLY_DENIED_BACKGROUND, previouslyDeniedBackground)
            editor.apply()
        }

        private fun locationPermissionStateForLocationManagerStatus(status: RadarLocationPermissionsStatus): LocationPermissionState {
            
            if (status.backgroundPermissionResult) {
                return LocationPermissionState.BACKGROUND_PERMISSIONS_GRANTED
            }

            if (status.previouslyDeniedBackground) {
                return if (status.shouldShowRequestPermissionRationaleBG) {
                    LocationPermissionState.BACKGROUND_PERMISSIONS_REJECTED_ONCE
                } else {
                    LocationPermissionState.BACKGROUND_PERMISSIONS_REJECTED
                }
            }

            if (status.foregroundPermissionResult) {
                return LocationPermissionState.FOREGROUND_PERMISSIONS_GRANTED
            } else {
                if (status.inLocationPopup) {
                    return LocationPermissionState.FOREGROUND_LOCATION_PENDING
                }
                if (status.approximatePermissionsResult){
                    return LocationPermissionState.APPROXIMATE_PERMISSIONS_GRANTED
                }
                return if (status.shouldShowRequestPermissionRationaleFG) {
                    LocationPermissionState.FOREGROUND_PERMISSIONS_REJECTED_ONCE
                } else {
                    if (status.previouslyDeniedForeground) {
                        LocationPermissionState.FOREGROUND_PERMISSIONS_REJECTED
                    } else {
                        LocationPermissionState.NO_PERMISSIONS_GRANTED
                    }
                }
            }
        }
    }

    fun toJson(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put(KEY_STATUS, status.name)
        jsonObject.put(KEY_FOREGROUND_PERMISSIONS_RESULT, foregroundPermissionResult)
        jsonObject.put(KEY_BACKGROUND_PERMISSIONS_RESULT, backgroundPermissionResult)
        jsonObject.put(KEY_SHOULD_SHOW_REQUEST_PERMISSION_RATIONALE_FG, shouldShowRequestPermissionRationaleFG)
        jsonObject.put(KEY_PREVIOUSLY_DENIED_FOREGROUND,previouslyDeniedForeground)
        jsonObject.put(KEY_APPROXIMATE_PERMISSIONS_REQUEST,approximatePermissionsResult)
        jsonObject.put(KEY_SHOULD_SHOW_REQUEST_PERMISSION_RATIONALE_BG,shouldShowRequestPermissionRationaleBG)
        jsonObject.put(KEY_PREVIOUSLY_DENIED_BACKGROUND,previouslyDeniedBackground)
        return jsonObject
    }

    enum class LocationPermissionState {
        NO_PERMISSIONS_GRANTED,
        FOREGROUND_PERMISSIONS_GRANTED,
        APPROXIMATE_PERMISSIONS_GRANTED,
        FOREGROUND_PERMISSIONS_REJECTED_ONCE,
        FOREGROUND_PERMISSIONS_REJECTED,
        FOREGROUND_LOCATION_PENDING,
        BACKGROUND_PERMISSIONS_GRANTED,
        BACKGROUND_PERMISSIONS_REJECTED,
        BACKGROUND_PERMISSIONS_REJECTED_ONCE,
        UNKNOWN
    }

    var status: LocationPermissionState = LocationPermissionState.UNKNOWN
    var foregroundPermissionResult: Boolean = false
    var backgroundPermissionResult: Boolean = false
    var shouldShowRequestPermissionRationaleFG: Boolean = false
    var shouldShowRequestPermissionRationaleBG: Boolean = false
    var previouslyDeniedForeground: Boolean = false
    var inLocationPopup: Boolean = false
    var approximatePermissionsResult: Boolean = false
    var previouslyDeniedBackground: Boolean = false
}