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

        // maybe we cna dump this into radarsettings? we are really simply saving a bool. the rest of the object is derived at call time
        fun getFromPreferences(context: Context, activity: Activity ,inLocationPopup: Boolean = false): RadarLocationPermissionsStatus? {
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val foregroundPopupRequested = prefs.getBoolean(STATUS_KEY, false)
            val previouslyDeniedForeground = prefs.getBoolean(DENIED_KEY, false)
            return fromForegroundPopupRequested(context, activity, foregroundPopupRequested, previouslyDeniedForeground, inLocationPopup)
        }

        fun saveToPreferences(context: Context, foregroundPopupRequested: Boolean) {
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor: SharedPreferences.Editor = prefs.edit()
            editor.putBoolean(STATUS_KEY, foregroundPopupRequested)
            editor.apply()
        }

        fun saveDeniedOnce(context: Context, previouslyDeniedForeground: Boolean) {
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
       
            // if this is true, we know it is denied once
            if (newStatus.shouldShowRequestPermissionRationale && !previouslyDeniedForeground) {
                saveDeniedOnce(context, true)
                newStatus.previouslyDeniedForeground = true
            } else {
                newStatus.previouslyDeniedForeground = previouslyDeniedForeground
            }
            newStatus.inLocationPopup = inLocationPopup
            // we map the different states to the status to be implemented
            newStatus.status = mapToStatus(newStatus)
            // print all the states
            Log.d("LocationPermissions", "Foreground Popup Requested: ${newStatus.foregroundPopupRequested}")
            Log.d("LocationPermissions", "Foreground Permission Result: ${newStatus.foregroundPermissionResult}")
            Log.d("LocationPermissions", "Background Permission Result: ${newStatus.backgroundPermissionResult}")
            Log.d("LocationPermissions", "Should Show Request Permission Rationale: ${newStatus.shouldShowRequestPermissionRationale}")
            Log.d("LocationPermissions", "Status: ${newStatus.status}")
            Log.d("LocationPermissions", "Previously Denied Foreground: ${newStatus.previouslyDeniedForeground}")
            return newStatus
        }


        // some notes and states we need to map to
        // start -> f,f,f,f
        // after requested foreground (pending) -> t,f,f,f have an in-memory flag to denote in pop-up
        // after granted foreground -> t,t,f,f , triggered by app state
        // after denied foreground once -> t,f,f,t trigger with app state, handled not much differently than having not granted
        // after denied foreground -> t,f,f,f (edge, may not need to support)
        // after requested background -> t,t,t,* (trigger from app state)

        // this is messsy, we need to clean this up
        private fun mapToStatus(status: RadarLocationPermissionsStatus): PermissionStatus {
            
            if (status.backgroundPermissionResult) {
                return PermissionStatus.BACKGROUND_PERMISSIONS_GRANTED
            }

            if (status.foregroundPermissionResult) {
                return PermissionStatus.FOREGROUND_PERMISSIONS_GRANTED
            } else {
                if (status.inLocationPopup) {
                    return PermissionStatus.FOREGROUND_LOCATION_PENDING
                }
                if (status.approximatePermissionsResult){
                    return PermissionStatus.APPROXIMATE_PERMISSIONS_GRANTED
                }
                if (status.shouldShowRequestPermissionRationale) {
                    return PermissionStatus.FOREGROUND_PERMISSIONS_REJECTED_ONCE
                } else {
                    if (status.foregroundPopupRequested) {
                        if (status.previouslyDeniedForeground) {
                            return PermissionStatus.FOREGROUND_PERMISSIONS_REJECTED
                        }
                        
                    } else {
                        return PermissionStatus.NO_PERMISSIONS_GRANTED
                    }
                }
            }
            return PermissionStatus.UNKNOWN
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
        return jsonObject
    }

    enum class PermissionStatus {
        NO_PERMISSIONS_GRANTED,
        FOREGROUND_PERMISSIONS_GRANTED,
        APPROXIMATE_PERMISSIONS_GRANTED,
        FOREGROUND_PERMISSIONS_REJECTED_ONCE,
        FOREGROUND_PERMISSIONS_REJECTED,
        FOREGROUND_LOCATION_PENDING,
        BACKGROUND_PERMISSIONS_GRANTED,
        UNKNOWN
    }

    var status: PermissionStatus = PermissionStatus.UNKNOWN
    var foregroundPopupRequested: Boolean = false
    var foregroundPermissionResult: Boolean = false
    var backgroundPermissionResult: Boolean = false
    var shouldShowRequestPermissionRationale: Boolean = false
    var previouslyDeniedForeground: Boolean = false
    var inLocationPopup: Boolean = false
    var approximatePermissionsResult: Boolean = false

}