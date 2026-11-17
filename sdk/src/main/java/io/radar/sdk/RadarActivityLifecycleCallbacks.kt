package io.radar.sdk

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.InputDevice
import android.view.MotionEvent
import android.view.Window
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.radar.sdk.model.RadarConfig
import kotlin.math.max

internal class RadarActivityLifecycleCallbacks(
    private val fraud: Boolean = false
) : Application.ActivityLifecycleCallbacks {
    private var count = 0
    private var isFirstOnResume = true
    private val wrappedActivities = mutableSetOf<Activity>()

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

    internal fun wrapActivity(activity: Activity) {
        val window = activity.window
        val originalCallback = window.callback

        // Each Radar.initialize() makes a new callbacks object, so an earlier one can have wrapped this
        // window already. Do not add a second layer.
        if (originalCallback !is RadarTouchWindowCallback) {
            window.callback = RadarTouchWindowCallback(originalCallback, activity.applicationContext)
        }

        wrappedActivities.add(activity)
    }

    /**
     * Sets the sharing flag when a touch comes from a virtual or unknown input device, then passes the
     * event to [original].
     */
    internal class RadarTouchWindowCallback(
        internal val original: Window.Callback,
        private val context: Context
    ) : Window.Callback by original {
        override fun dispatchTouchEvent(event: MotionEvent): Boolean {
            try {
                val inputDevice = InputDevice.getDevice(event.deviceId)

                if (
                    event.getToolType(0) == MotionEvent.TOOL_TYPE_UNKNOWN ||
                    inputDevice?.isVirtual == true
                ) {
                    RadarSettings.setSharing(context, true)
                }
            } catch (e: Exception) {
                Log.e(TAG, e.message, e)
            }

            return original.dispatchTouchEvent(event)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        val wasBackgrounded = count == 0
        if (count == 0 && !isFirstOnResume) {
            try {
                val updated = RadarSettings.updateSessionId(activity.applicationContext)
                if (updated) {
                    val usage = "resume"
                    Radar.apiClient.getConfig(
                        usage = usage,
                        callback = object : RadarApiClient.RadarGetConfigApiCallback {
                            override fun onComplete(status: Radar.RadarStatus, config: RadarConfig?) {
                                if (config == null) {
                                    return
                                }

                                if (status == Radar.RadarStatus.SUCCESS) {
                                    Radar.locationManager.updateTrackingFromMeta(config.meta)
                                    RadarSettings.setSdkConfiguration(activity.applicationContext, config.meta.sdkConfiguration)

                                    val updatedConfig = RadarSettings.getSdkConfiguration(activity.applicationContext)
                                    if (updatedConfig.useSyncRegion) {
                                        Radar.syncManager.start(86400 * 1000L)
                                    } else {
                                        Radar.syncManager.stop()
                                    }
                                }

                                val sdkConfiguration = RadarSettings.getSdkConfiguration(activity.applicationContext)
                                if (sdkConfiguration.trackOnceOnAppOpen || sdkConfiguration.startTrackingOnInitialize) {
                                    Radar.trackOnce()
                                    if (sdkConfiguration.startTrackingOnInitialize && !RadarSettings.getTracking(activity.applicationContext)) {
                                        Radar.startTracking(Radar.getTrackingOptions())
                                    }
                                }
                            }
                        }
                    )
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
        if (wasBackgrounded) {
            Radar.handleForegroundProcessStart()
            Radar.handleBeaconRangingForeground()
        }
        activity.intent?.let { Radar.logOpenedAppConversion(it) } ?: Radar.logOpenedAppConversion()

        updatePermissionsDenied(activity)

        if (fraud && !wrappedActivities.contains(activity)) {
            wrapActivity(activity)
        }
    }

    override fun onActivityPaused(activity: Activity) {
        count = max(count - 1, 0)
        foreground = count > 0
        if (!foreground) {
            Radar.handleBeaconRangingBackground()
        }

        updatePermissionsDenied(activity)
        Radar.logResigningActive()
        Radar.dismissInAppMessage()
    }

    override fun onActivityStarted(activity: Activity) {
        updatePermissionsDenied(activity)
    }

    override fun onActivityStopped(activity: Activity) {
        updatePermissionsDenied(activity)
        Radar.logBackgrounding()
    }

    override fun onActivityDestroyed(activity: Activity) {
        wrappedActivities.remove(activity)
        updatePermissionsDenied(activity)
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        updatePermissionsDenied(activity)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        updatePermissionsDenied(activity)
    }
}
