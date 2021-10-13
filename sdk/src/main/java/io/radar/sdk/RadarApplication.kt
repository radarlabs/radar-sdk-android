package io.radar.sdk

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.Handler
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Contains core radar classes.
 */
internal class RadarApplication(
    val context: Context,
    val receiver: RadarReceiver?,
    apiHelper: RadarApiHelper? = null,
    locationManagerClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context),
    permissionsHelper: RadarPermissionsHelper = RadarPermissionsHelper(),
    loggerExecutor: ExecutorService = Executors.newSingleThreadExecutor()
) : ContextWrapper(context) {
    val handler = Handler(mainLooper)
    val settings = RadarSettings(this)
    val state = RadarState(this)
    val logger = RadarLogger(this, loggerExecutor)
    val apiClient = RadarApiClient(this, apiHelper ?: RadarApiHelper(logger))
    val locationManager = RadarLocationManager(this, locationManagerClient, permissionsHelper)
    val beaconManager: RadarBeaconManager?

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.beaconManager = RadarBeaconManager(this)
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