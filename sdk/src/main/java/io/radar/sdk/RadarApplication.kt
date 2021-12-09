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
import io.radar.sdk.util.RadarSimpleLogBuffer
import org.json.JSONObject
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Contains core radar classes.
 */
@Suppress("LongParameterList")
internal class RadarApplication(
    val context: Context,
    var receiver: RadarReceiver?,
    apiHelper: RadarApiHelper? = null,
    locationManagerClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context),
    permissionsHelper: RadarPermissionsHelper = RadarPermissionsHelper(),
    loggerExecutor: ExecutorService = Executors.newSingleThreadExecutor(),
    val logBuffer: RadarLogBuffer = RadarSimpleLogBuffer()
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

    fun isTestKey(): Boolean {
        val key = settings.getPublishableKey()
        return if (key == null) {
            false
        } else {
            key.startsWith("prj_test") || key.startsWith("org_test")
        }
    }

    /**
     * Sends Radar log events to the server
     */
    fun flushLogs(onComplete: () -> Unit) {
        if (isTestKey()) {
            val flushable = logBuffer.getFlushableLogsStash()
            val logs = flushable.get()
            if (logs.isNotEmpty()) {
                apiClient.log(logs, object : RadarApiClient.RadarLogCallback {
                    override fun onComplete(status: Radar.RadarStatus, res: JSONObject?) {
                        flushable.onFlush(status == Radar.RadarStatus.SUCCESS)
                        onComplete.invoke()
                    }
                })
            }
        }
    }
}