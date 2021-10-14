package io.radar.sdk

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.math.max

internal class RadarActivityLifecycleCallbacks(
    private val settings: RadarSettings,
    private val logger: RadarLogger,
    private val apiClient: RadarApiClient
) : Application.ActivityLifecycleCallbacks {
    private var count = 0

    companion object {
        var foreground: Boolean = false
            private set

        private const val TAG = "RadarActivityLifecycle"
    }

    private fun isPermissionDenied(activity: Activity, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(activity.applicationContext, permission) == PERMISSION_DENIED &&
                ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    private fun updatePermissionsDenied(activity: Activity) {
        try {
            if (isPermissionDenied(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                || isPermissionDenied(activity, Manifest.permission.ACCESS_COARSE_LOCATION))
                settings.setPermissionsDenied(true)
        } catch (e: Exception) {
            Log.e(TAG, e.message, e)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        if (count == 0) {
            try {
                val updated = settings.updateSessionId()
                logger.d("New session", "sessionId" to settings.getSessionId())
                if (updated) {
                    apiClient.getConfig()
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