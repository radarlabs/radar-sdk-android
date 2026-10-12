package io.radar.sdk

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

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

        internal fun update(context: Context, logger: RadarLogger) {
            val enabled = needsBootRestore(context)
            // Compare against the state we last applied so app opens don't need a PackageManager call
            val savedEnabled = RadarSettings.getBootReceiverEnabled(context)
            val currentlyEnabled = savedEnabled ?: isEnabled(context, logger) ?: return
            if (currentlyEnabled != enabled) {
                setEnabled(context, enabled, logger)
            } else if (savedEnabled == null) {
                RadarSettings.setBootReceiverEnabled(context, enabled)
            }
        }

        private fun isEnabled(context: Context, logger: RadarLogger): Boolean? = try {
            val component = ComponentName(context, RadarBootReceiver::class.java)
            // DEFAULT means the manifest value, which is enabled
            context.packageManager.getComponentEnabledSetting(component) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        } catch (e: Exception) {
            // e.g. the app removed the receiver from its merged manifest
            logger.e("Error reading boot receiver state", Radar.RadarLogType.SDK_EXCEPTION, e)
            null
        }

        private fun setEnabled(context: Context, enabled: Boolean, logger: RadarLogger) {
            try {
                val component = ComponentName(context, RadarBootReceiver::class.java)
                val state = if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                context.packageManager.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP)
                RadarSettings.setBootReceiverEnabled(context, enabled)
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
                    // Don't initialize the SDK just to find there's nothing to restore. Skip the saved
                    // state: this receiver is running, so it's enabled even if the saved state says otherwise.
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
