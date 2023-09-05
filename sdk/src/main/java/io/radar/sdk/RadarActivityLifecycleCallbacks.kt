package io.radar.sdk

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.radar.sdk.model.RadarConfig
import kotlin.math.max


internal class RadarActivityLifecycleCallbacks(
    private val logger: RadarLogger,
    private val fraud: Boolean = false
) : Application.ActivityLifecycleCallbacks {
    private var count = 0

    companion object {
        var foreground: Boolean = false
            private set
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
            logger.e("Error updating permissions status", Radar.RadarLogType.SDK_EXCEPTION, e)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        if (count == 0) {
            try {
                val updated = RadarSettings.updateSessionId(activity.applicationContext)
                if (updated) {
                    val usage = "resume"
                    Radar.apiClient.getConfig(usage, false, object : RadarApiClient.RadarGetConfigApiCallback {
                        override fun onComplete(config: RadarConfig) {
                            Radar.locationManager.updateTrackingFromMeta(config.meta)
                            RadarSettings.setFeatureSettings(activity.applicationContext, config.meta.featureSettings)
                        }
                    })
                }
            } catch (e: Exception) {
                logger.e("Error updating session ID", Radar.RadarLogType.SDK_EXCEPTION, e)
            }
        }
        count++
        foreground = count > 0

        logger.d("Activity resumed | count = $count; foreground = $foreground")

        Radar.logOpenedAppConversion()

        updatePermissionsDenied(activity)

        if (fraud) {
            val touchView = object: View(activity.applicationContext) {
                override fun dispatchTouchEvent(event: MotionEvent): Boolean {
                    try {
                        val inputDevice = InputDevice.getDevice(event.deviceId)
                        logger.d("Intercepted touch | inputDevice = ${inputDevice}; inputDevice.isVirtual = ${inputDevice.isVirtual}; event: $event")
                        if (event.getToolType(0) == MotionEvent.TOOL_TYPE_UNKNOWN || inputDevice.isVirtual) {
                            RadarSettings.setSharing(activity.applicationContext, true)
                        }
                    }  catch (e: Exception) {
                        logger.e("Error intercepting touch", Radar.RadarLogType.SDK_EXCEPTION, e)
                    }
                    return super.dispatchTouchEvent(event)
                }
            }

            activity.addContentView(touchView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }

    }

    override fun onActivityPaused(activity: Activity) {
        count = max(count - 1, 0)
        foreground = count > 0

        logger.d("Activity paused | count = $count; foreground = $foreground")

        updatePermissionsDenied(activity)
    }

    override fun onActivityStarted(activity: Activity) {
        logger.d("Activity started")

        updatePermissionsDenied(activity)
    }

    override fun onActivityStopped(activity: Activity) {
        logger.d("Activity stopped")

        updatePermissionsDenied(activity)
    }

    override fun onActivityDestroyed(activity: Activity) {
        logger.d("Activity destroyed")

        updatePermissionsDenied(activity)
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        logger.d("Activity save instance state")

        updatePermissionsDenied(activity)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        logger.d("Activity created")

        updatePermissionsDenied(activity)
    }
}