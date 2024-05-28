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
import io.radar.sdk.model.RadarLocationPermissionsStatus

class RadarLocationPermissionsManager(private val context: Context, private val activity: Activity?): Application.ActivityLifecycleCallbacks {

    private var danglingBackgroundPermissionsRequest = false

    private lateinit var requestForegroundLocationPermissionsLauncher: ActivityResultLauncher<String>

    private lateinit var requestBackgroundLocationPermissionsLauncher: ActivityResultLauncher<String>

    init {
        if (activity is ComponentActivity) {
            requestForegroundLocationPermissionsLauncher = activity.registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) {
                isGranted: Boolean ->
                if (!isGranted) {
                    RadarLocationPermissionsStatus.savePreviouslyDeniedForeground(context,true)
                }
                RadarLocationPermissionsStatus.getUpdatedStatus(context, activity)
                    ?.let { Radar.sendLocationPermissionsStatus(it) }
            }

            requestBackgroundLocationPermissionsLauncher = activity.registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) {
                    isGranted: Boolean ->
                if (!isGranted) {
                    RadarLocationPermissionsStatus.savePreviouslyDeniedBackground(context, true)
                }
                RadarLocationPermissionsStatus.getUpdatedStatus(context, activity)
                    ?.let { Radar.sendLocationPermissionsStatus(it) }
            }
        } 
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    fun requestBackgroundLocationPermissions() {
        if (activity is ComponentActivity) {
            requestBackgroundLocationPermissionsLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            // TODO: sync the user's location permissions action with the their permissions status here
        }
    }

    fun requestForegroundLocationPermissions() {
        if (activity is ComponentActivity) {
            RadarLocationPermissionsStatus.getUpdatedStatus(context,activity,true)
                ?.let { Radar.sendLocationPermissionsStatus(it) }
            requestForegroundLocationPermissionsLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            // TODO: sync the user's location permissions action with the their permissions status here
        }
    }

    fun openAppSettings() {
        danglingBackgroundPermissionsRequest = true
        val intent = Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            val uri = Uri.fromParts("package", context.packageName, null)
            data = uri
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun getPermissionsStatus(): RadarLocationPermissionsStatus {
        if (activity is ComponentActivity) {
            return RadarLocationPermissionsStatus.getUpdatedStatus(context, activity) ?: RadarLocationPermissionsStatus()
        }
        return RadarLocationPermissionsStatus()
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
        if (danglingBackgroundPermissionsRequest ) {
            RadarLocationPermissionsStatus.getUpdatedStatus(context,activity)
                ?.let { Radar.sendLocationPermissionsStatus(it) }
        }
        danglingBackgroundPermissionsRequest = false
    }

}