package io.radar.sdk

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import kotlin.math.max

internal class RadarActivityLifecycleCallbacks : Application.ActivityLifecycleCallbacks {
    private var count = 0

    companion object {
        var foreground: Boolean = false
            private set

        private const val TAG = "RadarActivityLifecycle"
    }

    private fun updatePermissionsDenied(activity: Activity) {
        try {
            if (ContextCompat.checkSelfPermission(activity.applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED &&
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                RadarSettings.setPermissionsDenied(activity.applicationContext, true)
            }
            if (ContextCompat.checkSelfPermission(activity.applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED &&
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                RadarSettings.setPermissionsDenied(activity.applicationContext, true)
            }
        } catch (e: Exception) {
            Log.e(TAG, e.message, e)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        if (count == 0) {
            try {
                val updated = RadarSettings.updateSessionId(activity.applicationContext)
                if (updated) {
                    Radar.apiClient.getConfig(object : RadarApiClient.RadarGetConfigApiCallback {
                        override fun onComplete(
                            status: Radar.RadarStatus,
                            res: JSONObject?,
                            meta: RadarApiClient.RadarMeta?
                        ) {
                            Radar.locationManager?.updateTrackingFromMeta(
                                activity.applicationContext,
                                meta
                            )
                        }
                    })
                }
            } catch (e: Exception) {
                Log.e(TAG, e.message, e)
            }
        }
        count++
        foreground = count > 0

        updatePermissionsDenied(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        count = max(count - 1, 0)
        foreground = count > 0

        updatePermissionsDenied(activity)
    }

    override fun onActivityStarted(activity: Activity) {
        updatePermissionsDenied(activity)
    }

    override fun onActivityStopped(activity: Activity) {
        updatePermissionsDenied(activity)
    }

    override fun onActivityDestroyed(activity: Activity) {
        updatePermissionsDenied(activity)
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        updatePermissionsDenied(activity)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        updatePermissionsDenied(activity)
    }

}