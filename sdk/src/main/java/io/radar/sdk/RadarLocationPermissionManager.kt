package io.radar.sdk

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import io.radar.sdk.model.RadarLocationPermissionStatus

class RadarLocationPermissionManager(private val context: Context, private val activity: Activity?): Application.ActivityLifecycleCallbacks {

    private var danglingBackgroundPermissionRequest = false

    private lateinit var requestForegroundLocationPermissionLauncher: ActivityResultLauncher<String>

    private lateinit var requestBackgroundLocationPermissionLauncher: ActivityResultLauncher<String>

    init {
        if (activity is ComponentActivity) {
            requestForegroundLocationPermissionLauncher = activity.registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (!isGranted) {
                    RadarLocationPermissionStatus.savePreviouslyDeniedForeground(context,true)
                }
                Radar.sendLocationPermissionStatus(RadarLocationPermissionStatus.initWithStatus(context, activity))
                // TODO: sync the user's permissions status here
            }

            requestBackgroundLocationPermissionLauncher = activity.registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (!isGranted) {
                    RadarLocationPermissionStatus.savePreviouslyDeniedBackground(context, true)
                }
                Radar.sendLocationPermissionStatus(RadarLocationPermissionStatus.initWithStatus(context, activity))
                // TODO: sync the user's permissions status here
            }
        } 
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun requestBackgroundLocationPermission() {
        if (activity is ComponentActivity) {
            requestBackgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            // TODO: sync the user's location permissions action with the their permissions status here
        }
    }

    fun requestForegroundLocationPermission() {
        if (activity is ComponentActivity) {
            Radar.sendLocationPermissionStatus(RadarLocationPermissionStatus.initWithStatus(context,activity,true))
            requestForegroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            // TODO: sync the user's location permissions action with the their permissions status here
        }
    }

    fun openAppSettings() {
        danglingBackgroundPermissionRequest = true
        val intent = Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            val uri = Uri.fromParts("package", context.packageName, null)
            data = uri
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun getLocationPermissionStatus(): RadarLocationPermissionStatus {
        if (activity is ComponentActivity) {
            return RadarLocationPermissionStatus.initWithStatus(context, activity)
        }
        return RadarLocationPermissionStatus()
    }

    override fun onActivityPaused(activity: Activity) {
        // do nothing
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
        if (danglingBackgroundPermissionRequest) {
            Radar.sendLocationPermissionStatus(RadarLocationPermissionStatus.initWithStatus(context,activity))
            // TODO: sync the user's location permissions action with the their permissions status here
        }
        danglingBackgroundPermissionRequest = false
    }

}