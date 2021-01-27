package io.radar.sdk

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.math.max

internal class RadarActivityLifecycleCallbacks : Application.ActivityLifecycleCallbacks {
    private var count = 0

    companion object {
        var foreground: Boolean = false
            private set
    }

    override fun onActivityResumed(activity: Activity) {
        if (count == 0) {
            val updated = RadarSettings.updateSessionId(activity.applicationContext)
            if (updated) {
                Radar.apiClient.getConfig()
            }
        }
        count++
        foreground = count > 0

        if (ContextCompat.checkSelfPermission(activity.applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
            RadarSettings.setPermissionsDenied(activity.applicationContext, true)
        }
    }

    override fun onActivityPaused(activity: Activity) {
        count = max(count - 1, 0)
        foreground = count > 0
    }

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivityDestroyed(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle?) {}

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

}