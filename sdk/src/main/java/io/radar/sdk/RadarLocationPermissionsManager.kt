package io.radar.sdk

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import io.radar.sdk.model.RadarLocationPermissionsStatus

class RadarLocationPermissionsManager(private val context: Context): Application.ActivityLifecycleCallbacks {
    // tidy up later
    private val permissionsRequestCode = 52456
    private var requestingBackgroundPermissions = false
    private var requestingForegroundPermissions = false

    @RequiresApi(Build.VERSION_CODES.Q)
    fun requestLocationPermissions(background: Boolean) {
        if (background) {
            requestingBackgroundPermissions = true
            ActivityCompat.requestPermissions(
            context as Activity,
            arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
            permissionsRequestCode
        )
        } else {
            RadarLocationPermissionsStatus.saveToPreferences(context, true)
            requestingForegroundPermissions = true
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                permissionsRequestCode
            )
        }
    }

    fun getPermissionsStatus(): RadarLocationPermissionsStatus {
        return RadarLocationPermissionsStatus.getFromPreferences(context) ?: RadarLocationPermissionsStatus()
    }

    override fun onActivityPaused(activity: Activity) {
        if (requestingForegroundPermissions) {
            RadarLocationPermissionsStatus.getFromPreferences(context,true)
                ?.let { Radar.sendLocationPermissionsStatus(it) }
        }
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
        if (requestingBackgroundPermissions || requestingForegroundPermissions) {
            RadarLocationPermissionsStatus.getFromPreferences(context)
                ?.let { Radar.sendLocationPermissionsStatus(it) }
        }
        requestingForegroundPermissions = false
        requestingBackgroundPermissions = false
    }
}