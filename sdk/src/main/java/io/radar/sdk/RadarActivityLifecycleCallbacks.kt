package io.radar.sdk

import android.app.Activity
import android.app.Application
import android.os.Bundle
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