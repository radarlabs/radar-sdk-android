package io.radar.sdk.model

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import org.json.JSONObject

class RadarLocationPermissionStatus {

    companion object {
        private const val PREFS_NAME = "RadarLocationPermissionStatus"

        internal const val KEY_STATUS = "status"
        internal const val KEY_FOREGROUND_PERMISSION_RESULT = "foregroundPermissionResult"
        internal const val KEY_BACKGROUND_PERMISSION_RESULT = "backgroundPermissionResult"
        internal const val KEY_SHOULD_SHOW_REQUEST_PERMISSION_RATIONALE_FG = "shouldShowRequestPermissionRationale"
        internal const val KEY_PREVIOUSLY_DENIED_FOREGROUND = "previouslyDeniedForeground"
        internal const val KEY_PREVIOUSLY_DENIED_BACKGROUND = "previouslyDeniedBackground"
        internal const val KEY_APPROXIMATE_PERMISSION_REQUEST = "approximatePermissionRequest"
        internal const val KEY_SHOULD_SHOW_REQUEST_PERMISSION_RATIONALE_BG = "backgroundPermissionAvailable"
        internal const val KEY_IN_LOCATION_PROMPT = "inLocationPrompt"

        fun initWithStatus(context: Context, activity: Activity, inLocationPrompt: Boolean = false): RadarLocationPermissionStatus {

            val newStatus = RadarLocationPermissionStatus()
            newStatus.foregroundPermissionResult = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            newStatus.approximatePermissionResult = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
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
            newStatus.previouslyDeniedForeground = prefs.getBoolean(KEY_FOREGROUND_PERMISSION_RESULT, false)
            if (newStatus.backgroundPermissionResult) {
                savePreviouslyDeniedBackground(context,false)
            }
            newStatus.previouslyDeniedBackground = prefs.getBoolean(KEY_PREVIOUSLY_DENIED_BACKGROUND, false)

            newStatus.inLocationPrompt = inLocationPrompt
            newStatus.status = locationPermissionStateForLocationManagerStatus(newStatus)

            return newStatus
        }

        fun savePreviouslyDeniedForeground(context: Context, previouslyDeniedForeground: Boolean) {
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor: SharedPreferences.Editor = prefs.edit()
            editor.putBoolean(KEY_FOREGROUND_PERMISSION_RESULT, previouslyDeniedForeground)
            editor.apply()
        }

        fun savePreviouslyDeniedBackground(context: Context, previouslyDeniedBackground: Boolean) {
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor: SharedPreferences.Editor = prefs.edit()
            editor.putBoolean(KEY_PREVIOUSLY_DENIED_BACKGROUND, previouslyDeniedBackground)
            editor.apply()
        }

        private fun locationPermissionStateForLocationManagerStatus(status: RadarLocationPermissionStatus): LocationPermissionState {
            
            if (status.backgroundPermissionResult) {
                return LocationPermissionState.BACKGROUND_PERMISSION_GRANTED
            }

            if (status.previouslyDeniedBackground && status.foregroundPermissionResult) {
                return if (status.shouldShowRequestPermissionRationaleBG) {
                    LocationPermissionState.BACKGROUND_PERMISSION_REJECTED_ONCE
                } else {
                    LocationPermissionState.BACKGROUND_PERMISSION_REJECTED
                }
            }

            if (status.foregroundPermissionResult) {
                return LocationPermissionState.FOREGROUND_PERMISSION_GRANTED
            } else {
                if (status.inLocationPrompt) {
                    return LocationPermissionState.FOREGROUND_PERMISSION_PENDING
                }
                if (status.approximatePermissionResult){
                    return LocationPermissionState.APPROXIMATE_PERMISSION_GRANTED
                }
                return if (status.shouldShowRequestPermissionRationaleFG) {
                    LocationPermissionState.FOREGROUND_PERMISSION_REJECTED_ONCE
                } else {
                    if (status.previouslyDeniedForeground) {
                        LocationPermissionState.FOREGROUND_PERMISSION_REJECTED
                    } else {
                        LocationPermissionState.NO_PERMISSION_GRANTED
                    }
                }
            }
        }

        fun stringForLocationPermissionState(state: LocationPermissionState): String {
            return when (state) {
                LocationPermissionState.NO_PERMISSION_GRANTED -> "NO_PERMISSION_GRANTED"
                LocationPermissionState.FOREGROUND_PERMISSION_GRANTED -> "FOREGROUND_PERMISSION_GRANTED"
                LocationPermissionState.APPROXIMATE_PERMISSION_GRANTED -> "APPROXIMATE_PERMISSION_GRANTED"
                LocationPermissionState.FOREGROUND_PERMISSION_REJECTED_ONCE -> "FOREGROUND_PERMISSION_REJECTED_ONCE"
                LocationPermissionState.FOREGROUND_PERMISSION_REJECTED -> "FOREGROUND_PERMISSION_REJECTED"
                LocationPermissionState.FOREGROUND_PERMISSION_PENDING -> "FOREGROUND_PERMISSION_PENDING"
                LocationPermissionState.BACKGROUND_PERMISSION_GRANTED -> "BACKGROUND_PERMISSION_GRANTED"
                LocationPermissionState.BACKGROUND_PERMISSION_REJECTED -> "BACKGROUND_PERMISSION_REJECTED"
                LocationPermissionState.BACKGROUND_PERMISSION_REJECTED_ONCE -> "BACKGROUND_PERMISSION_REJECTED_ONCE"
                LocationPermissionState.UNKNOWN -> "UNKNOWN"
            }
        }
    }

    fun toJson(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put(KEY_STATUS, status.name)
        jsonObject.put(KEY_FOREGROUND_PERMISSION_RESULT, foregroundPermissionResult)
        jsonObject.put(KEY_BACKGROUND_PERMISSION_RESULT, backgroundPermissionResult)
        jsonObject.put(KEY_SHOULD_SHOW_REQUEST_PERMISSION_RATIONALE_FG, shouldShowRequestPermissionRationaleFG)
        jsonObject.put(KEY_PREVIOUSLY_DENIED_FOREGROUND,previouslyDeniedForeground)
        jsonObject.put(KEY_APPROXIMATE_PERMISSION_REQUEST,approximatePermissionResult)
        jsonObject.put(KEY_SHOULD_SHOW_REQUEST_PERMISSION_RATIONALE_BG,shouldShowRequestPermissionRationaleBG)
        jsonObject.put(KEY_PREVIOUSLY_DENIED_BACKGROUND,previouslyDeniedBackground)
        jsonObject.put(KEY_IN_LOCATION_PROMPT,inLocationPrompt)
        return jsonObject
    }

    enum class LocationPermissionState {
        NO_PERMISSION_GRANTED,
        FOREGROUND_PERMISSION_GRANTED,
        APPROXIMATE_PERMISSION_GRANTED,
        FOREGROUND_PERMISSION_REJECTED_ONCE,
        FOREGROUND_PERMISSION_REJECTED,
        FOREGROUND_PERMISSION_PENDING,
        BACKGROUND_PERMISSION_GRANTED,
        BACKGROUND_PERMISSION_REJECTED,
        BACKGROUND_PERMISSION_REJECTED_ONCE,
        UNKNOWN
    }

    var status: LocationPermissionState = LocationPermissionState.UNKNOWN
    var foregroundPermissionResult: Boolean = false
    var backgroundPermissionResult: Boolean = false
    var shouldShowRequestPermissionRationaleFG: Boolean = false
    var shouldShowRequestPermissionRationaleBG: Boolean = false
    var previouslyDeniedForeground: Boolean = false
    var inLocationPrompt: Boolean = false
    var approximatePermissionResult: Boolean = false
    var previouslyDeniedBackground: Boolean = false
}