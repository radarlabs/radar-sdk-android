package io.radar.sdk

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import io.radar.sdk.model.RadarLocationPermissionsStatus

class RadarLocationPermissionsManager(private val context: Context, private val activity: Activity): Application.ActivityLifecycleCallbacks {
    // tidy up later
    private val permissionsRequestCode = 52456

    // we expose these flags as an advanced feature, we cannot easily listen to permissions changes that
    // happen in other activites so the developer should explicitly say "I want a fresh permissions update when I'm back in this activity"
    private var requestingBackgroundPermissions = false
    // reason through if we need both when we finalize the design
    private var requestingForegroundPermissions = false

    fun updateLocationPermissionsStatusOnActivityResume(){
        requestingBackgroundPermissions = true
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun requestLocationPermissions(background: Boolean) {
        if (background) {
            requestingBackgroundPermissions = true
            ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
            permissionsRequestCode
        )
        } else {
            RadarLocationPermissionsStatus.saveToPreferences(context, true)
            requestingForegroundPermissions = true
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                permissionsRequestCode
            )
        }
    }

    fun getPermissionsStatus(): RadarLocationPermissionsStatus {
        return RadarLocationPermissionsStatus.getFromPreferences(context, activity) ?: RadarLocationPermissionsStatus()
    }

    override fun onActivityPaused(activity: Activity) {
        Log.d("listerner", "onActivityPaused")
        if (requestingForegroundPermissions) {
            RadarLocationPermissionsStatus.getFromPreferences(context,activity,true)
                ?.let { Radar.sendLocationPermissionsStatus(it) }
        }
    }

    override fun onActivityStopped(p0: Activity) {
        // do nothing
        Log.d("listerner", "onActivityStopped")
    }

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {
        // do nothing
        Log.d("listerner", "onActivitySaveInstanceState")
    }

    override fun onActivityDestroyed(p0: Activity) {
        // do nothing
        Log.d("listerner", "onActivityDestroyed")
    }

    override fun onActivityCreated(p0: Activity, p1: Bundle?) {
        // do nothing
        Log.d("listerner", "onActivityCreated")
    }

    override fun onActivityStarted(p0: Activity) {
        // do nothing
        Log.d("listerner", "onActivityStarted")
    }

    override fun onActivityResumed(activity: Activity) {
        Log.d("listerner", "onActivityResumed")
        if (requestingBackgroundPermissions || requestingForegroundPermissions) {
            RadarLocationPermissionsStatus.getFromPreferences(context,activity)
                ?.let { Radar.sendLocationPermissionsStatus(it) }
        }
        requestingForegroundPermissions = false
        requestingBackgroundPermissions = false
    }

//    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
//
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        Log.d("listerner", "onRequestPermissionsResult")
//        val isFineLocation = permissions.contains(Manifest.permission.ACCESS_FINE_LOCATION)
//        val isBackgroundLocation = permissions.contains(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
//
//        if (isFineLocation || isBackgroundLocation) {
//            val status = RadarLocationPermissionsStatus.getFromPreferences(context, activity)
//            status?.let { Radar.sendLocationPermissionsStatus(it) }
//        }
//    }


}