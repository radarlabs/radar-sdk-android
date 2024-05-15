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
    // tidy up later
    private val permissionsRequestCode = 52456

    // we expose these flags as an advanced feature, we cannot easily listen to permissions changes that
    // happen in other activites so the developer should explicitly say "I want a fresh permissions update when I'm back in this activity"
    private var requestingBackgroundPermissions = false
    // reason through if we need both when we finalize the design
    // private var requestingForegroundPermissions = false

    private lateinit var requestLocationPermissionsLauncher: ActivityResultLauncher<String>

    init {
        if (activity is ComponentActivity) {
            requestLocationPermissionsLauncher = activity.registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    Log.d("devtest", "granted")
                } else {
                    Log.d("devtest", "not granted")
                }
                RadarLocationPermissionsStatus.getFromPreferences(context, activity)
                    ?.let { Radar.sendLocationPermissionsStatus(it) }
            }
        } 
    }

    fun updateLocationPermissionsStatusOnActivityResume(){
        requestingBackgroundPermissions = true
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun requestLocationPermissions(background: Boolean) {
        if (background) {
//            requestingBackgroundPermissions = true
//            ActivityCompat.requestPermissions(
//            activity,
//            arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
//            permissionsRequestCode
//            )
            if (activity is ComponentActivity) {
                Log.d("devtest","i ran")
                requestLocationPermissionsLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        } else {
            RadarLocationPermissionsStatus.saveToPreferences(context, true)
            // requestingForegroundPermissions = true
            if (activity is ComponentActivity) {
                RadarLocationPermissionsStatus.getFromPreferences(context,activity,true)
                    ?.let { Radar.sendLocationPermissionsStatus(it) }
                Log.d("devtest","i ran")
                requestLocationPermissionsLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }



    fun getPermissionsStatus(): RadarLocationPermissionsStatus {
        return RadarLocationPermissionsStatus.getFromPreferences(context, activity) ?: RadarLocationPermissionsStatus()
    }

    override fun onActivityPaused(activity: Activity) {
        Log.d("listerner", "onActivityPaused")
//        if (requestingForegroundPermissions) {
//            RadarLocationPermissionsStatus.getFromPreferences(context,activity,true)
//                ?.let { Radar.sendLocationPermissionsStatus(it) }
//        }
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
        if (requestingBackgroundPermissions ) {
            RadarLocationPermissionsStatus.getFromPreferences(context,activity)
                ?.let { Radar.sendLocationPermissionsStatus(it) }
        }
        // requestingForegroundPermissions = false
        requestingBackgroundPermissions = false
    }

    // we face a few issues here in implementation
    // the callback from lifecycles seems to work the best, although it feels hackish. It can also
    //  be muddled by other stuff triggering and affecting the app lifecycle

    // We can try and incooporate the changes to permissions granted but we run into 2 issues
    // it is an incomplete solution as we don't get info for denied permissions, we also need

    // finally we have registering for activity results, this seems like the most "correct" solution.

    // maybe we should try and use combination of registering activities and also app lifecycles.


}