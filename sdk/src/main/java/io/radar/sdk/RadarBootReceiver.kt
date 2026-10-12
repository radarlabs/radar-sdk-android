package io.radar.sdk

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Restores tracking after a reboot or app update.
 *
 * Ships enabled so installs that are already tracking keep restoring across the SDK upgrade, then
 * disables itself whenever there's nothing to restore so apps that don't track aren't cold-started
 * on every reboot and update. [update] re-enables it when tracking starts.
 */
internal class RadarBootReceiver : BroadcastReceiver() {

    internal companion object {

        internal fun needsBootRestore(context: Context): Boolean = RadarSettings.getTracking(context) || RadarSettings.getTrackingOptions(context).startTrackingAfter != null

        // PackageManager calls are binder IPCs, and update() runs from initialize() on the main thread
        // during app startup. A single thread keeps updates in order, and each one reads the latest
        // tracking state when it runs.
        internal var executor: Executor = Executors.newSingleThreadExecutor()

        internal fun update(context: Context, logger: RadarLogger) {
            executor.execute {
                setEnabled(context, needsBootRestore(context), logger)
            }
        }

        private fun setEnabled(context: Context, enabled: Boolean, logger: RadarLogger) {
            try {
                val component = ComponentName(context, RadarBootReceiver::class.java)
                val packageManager = context.packageManager
                // DEFAULT means the manifest value, which is enabled
                val currentlyEnabled = packageManager.getComponentEnabledSetting(component) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                if (currentlyEnabled == enabled) {
                    return
                }

                val state = if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                packageManager.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP)
                logger.d("Updated boot receiver | enabled = $enabled")
            } catch (e: Exception) {
                // e.g. the app removed the receiver from its merged manifest
                logger.e("Error updating boot receiver", Radar.RadarLogType.SDK_EXCEPTION, e)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                if (!needsBootRestore(context)) {
                    // Don't initialize the SDK just to find there's nothing to restore. This runs
                    // synchronously so it finishes before the process can be killed.
                    setEnabled(context, false, RadarLogger(context.applicationContext))
                    return
                }

                if (!Radar.initialized) {
                    val fraudEnabled = RadarSettings.getFraudEnabled(context)
                    val initializeOptions = RadarInitializeOptions(fraud = fraudEnabled)
                    Radar.initialize(context, null, initializeOptions)
                }

                Radar.logger.d("Received broadcast | action = ${intent.action}")

                Radar.handleBootCompleted(context)
            }
        }
    }
}
