package io.radar.sdk

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.radar.sdk.model.RadarConfig
import kotlin.math.max


internal class RadarActivityLifecycleCallbacks(
    private val fraud: Boolean = false
) : Application.ActivityLifecycleCallbacks {
    private var count = 0
    private var isFirstOnResume = true

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
        if (count == 0 && !isFirstOnResume) {
            try {
                val updated = RadarSettings.updateSessionId(activity.applicationContext)
                if (updated) {
                    val usage = "resume"
                    Radar.apiClient.getConfig(usage, false, object : RadarApiClient.RadarGetConfigApiCallback {
                        override fun onComplete(status: Radar.RadarStatus, config: RadarConfig?) {
                            if (config == null) {
                                return
                            }

                            if (status == Radar.RadarStatus.SUCCESS) {
                                Radar.locationManager.updateTrackingFromMeta(config.meta)
                                RadarSettings.setSdkConfiguration(activity.applicationContext, config.meta.sdkConfiguration)
                            }

                            val sdkConfiguration = RadarSettings.getSdkConfiguration(activity.applicationContext)
                            if (sdkConfiguration.trackOnceOnAppOpen || sdkConfiguration.startTrackingOnInitialize) {
                                Radar.trackOnce()
                                if (sdkConfiguration.startTrackingOnInitialize && !RadarSettings.getTracking(activity.applicationContext)) {
                                    Radar.startTracking(Radar.getTrackingOptions())
                                }
                            }
                        }
                    })
                } else {
                    val sdkConfiguration = RadarSettings.getSdkConfiguration(activity.applicationContext)
                    if (sdkConfiguration.trackOnceOnAppOpen || sdkConfiguration.startTrackingOnInitialize) {
                        Radar.trackOnce()
                        if (sdkConfiguration.startTrackingOnInitialize && !RadarSettings.getTracking(activity.applicationContext)) {
                            Radar.startTracking(Radar.getTrackingOptions())
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, e.message, e)
            }
        }
        count++
        isFirstOnResume = false
        foreground = count > 0

        Radar.logOpenedAppConversion()

        updatePermissionsDenied(activity)

        if (fraud) {
            val touchView = object: View(activity.applicationContext) {
                override fun dispatchTouchEvent(event: MotionEvent): Boolean {
                    try {
                        val inputDevice = InputDevice.getDevice(event.deviceId)
                        if (event.getToolType(0) == MotionEvent.TOOL_TYPE_UNKNOWN || inputDevice?.isVirtual == true) {
                            RadarSettings.setSharing(activity.applicationContext, true)
                        }
                    }  catch (e: Exception) {
                        Log.e(TAG, e.message, e)
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

        updatePermissionsDenied(activity)
        Radar.logResigningActive()
    }

    override fun onActivityStarted(activity: Activity) {
        updatePermissionsDenied(activity)
    }

    override fun onActivityStopped(activity: Activity) {
        updatePermissionsDenied(activity)
        Radar.logBackgrounding()
    }

    override fun onActivityDestroyed(activity: Activity) {
        updatePermissionsDenied(activity)
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        updatePermissionsDenied(activity)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        Log.w(TAG, "ON CREATE ${count}")
        updatePermissionsDenied(activity)
    }
}