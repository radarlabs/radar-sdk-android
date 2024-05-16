package io.radar.sdk

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import io.radar.sdk.model.RadarLocationPermissionsStatus

class RadarLocationPermissionsManager(private val context: Context, private val activity: Activity): Application.ActivityLifecycleCallbacks {

    private var requestingBackgroundPermissions = false

    private var requestingForegroundPermissions = false

    private lateinit var requestLocationPermissionsLauncher: ActivityResultLauncher<String>

    init {
        if (activity is ComponentActivity) {
            requestLocationPermissionsLauncher = activity.registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) {
                isGranted: Boolean ->
                if (!isGranted) {
                    Log.d("PermissionsManager", "Permission not granted")
                }
                RadarLocationPermissionsStatus.getUpdatedStatus(context, activity)
                    ?.let { Radar.sendLocationPermissionsStatus(it) }
            }
        } 
    }

    // we expose these flags as an advanced feature, we cannot easily listen to permissions changes that
    // happen in other activities so the developer should explicitly say "I want a fresh permissions update when I'm back in this activity"
    fun updateLocationPermissionsStatusOnActivityResume(){
        requestingBackgroundPermissions = true
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun requestBackgroundLocationPermissions() {
        if (activity is ComponentActivity) {
            requestLocationPermissionsLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    fun requestForegroundLocationPermissions() {
        RadarLocationPermissionsStatus.saveForegroundPopupRequested(context, true)
        if (activity is ComponentActivity) {
            // is there any garuntees this will launch?
            RadarLocationPermissionsStatus.getUpdatedStatus(context,activity,true)
                ?.let { Radar.sendLocationPermissionsStatus(it) }
            requestLocationPermissionsLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun getPermissionsStatus(): RadarLocationPermissionsStatus {
        return RadarLocationPermissionsStatus.getUpdatedStatus(context, activity) ?: RadarLocationPermissionsStatus()
    }

    override fun onActivityPaused(activity: Activity) {
        // do nothing
        Log.d("PermissionsManager", "onActivityPaused")
    }

    override fun onActivityStopped(p0: Activity) {
        // do nothing
    }

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {
        // do nothing
    }

    override fun onActivityDestroyed(p0: Activity) {
        // do nothing
    }

    override fun onActivityCreated(p0: Activity, p1: Bundle?) {
        // do nothing
    }

    override fun onActivityStarted(p0: Activity) {
        // do nothing
    }

    override fun onActivityResumed(activity: Activity) {
        Log.d("PermissionsManager", "onActivityResumed")
        if (requestingBackgroundPermissions ) {
            RadarLocationPermissionsStatus.getUpdatedStatus(context,activity)
                ?.let { Radar.sendLocationPermissionsStatus(it) }
        }
        requestingBackgroundPermissions = false
    }

}