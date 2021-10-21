package io.radar.sdk

import android.content.Context
import android.content.ContextWrapper
import android.location.Location
import android.os.Build
import android.os.Handler
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import io.radar.sdk.model.RadarEvent
import io.radar.sdk.model.RadarUser
import io.radar.sdk.util.RadarLogBuffer
import org.json.JSONObject
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
    val logBuffer = RadarLogBuffer(context, loggerExecutor)//TODO maybe different executor?
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

    fun sendError(status: Radar.RadarStatus) {
        receiver?.onError(Radar.app, status)
        logger.i("📍️ Radar error received", "status" to status)
    }


    fun sendLocation(location: Location, user: RadarUser) {
        receiver?.onLocationUpdated(Radar.app, location, user)
        logger.i(
            "📍 Radar location updated", mapOf(
                "coordinates" to "(${location.latitude}, ${location.longitude})",
                "accuracy" to "${location.accuracy} meters",
                "link" to "link = https://radar.io/dashboard/users/${user._id}"
            )
        )
    }

    fun sendClientLocation(location: Location, stopped: Boolean, source: Radar.RadarLocationSource) {
        receiver?.onClientLocationUpdated(Radar.app, location, stopped, source)
    }


    fun sendEvents(events: Array<RadarEvent>, user: RadarUser? = null) {
        if (events.isEmpty()) {
            return
        }
        receiver?.onEventsReceived(Radar.app, events, user)

        for (event in events) {
            logger.i(
                "📍 Radar event received", mapOf(
                    "type" to RadarEvent.stringForType(event.type),
                    "link" to "https://radar.io/dashboard/events/${event._id}"
                )
            )
        }
    }

    /**
     * Sends Radar log events to the server
     */
    fun flushLogs() {
        val logs = logBuffer.getLogs()
        apiClient.log(logs.get(), object : RadarApiClient.RadarLogCallback {
            override fun onComplete(status: Radar.RadarStatus, res: JSONObject?) {
                logs.onFlush(status == Radar.RadarStatus.SUCCESS)
            }
        })
    }
}