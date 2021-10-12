package io.radar.sdk

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.Handler

/**
 * Contains core radar classes.
 */
internal class RadarApplication(val context: Context) : ContextWrapper(context) {
    val handler = Handler(mainLooper)
    val settings = RadarSettings(this)
    val state = RadarState(this)
    val logger = RadarLogger(settings)
    val apiClient = RadarApiClient(this)
    val locationManager = RadarLocationManager(this)
    val beaconManager: RadarBeaconManager?

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.beaconManager = RadarBeaconManager(this, logger)
        } else {
            this.beaconManager = null
        }
    }

    fun <T> post(t: T?, block: T.() -> Unit?) {
        if (t != null) {
            handler.post {
                block.invoke(t)
            }
        }
    }
}