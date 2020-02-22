package io.radar.sdk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import io.radar.sdk.Radar.RadarStatus
import io.radar.sdk.model.RadarEvent
import io.radar.sdk.model.RadarUser
import org.json.JSONException
import org.json.JSONObject
import java.text.ParseException

/**
 * A receiver for client-side delivery of events, location updates, and debug logs. For more information, see [](https://radar.io/documentation/sdk).
 *
 * @see [](https://radar.io/documentation/sdk)
 */
abstract class RadarReceiver : BroadcastReceiver() {

    companion object {

        const val ACTION_RECEIVED = "io.radar.sdk.RECEIVED"

        internal const val EXTRA_RESPONSE = "response"
        internal const val EXTRA_LOCATION = "location"
        internal const val EXTRA_STOPPED = "stopped"
        internal const val EXTRA_SOURCE = "source"
        internal const val EXTRA_STATUS = "status"
        internal const val EXTRA_MESSAGE = "message"

        internal fun createSuccessIntent(res: JSONObject, location: Location) =
            Intent(ACTION_RECEIVED).apply {
                putExtra(EXTRA_RESPONSE, res.toString())
                putExtra(EXTRA_LOCATION, location)
            }

        internal fun createLocationIntent(location: Location, stopped: Boolean, source: Radar.RadarLocationSource) =
            Intent(ACTION_RECEIVED).apply {
                putExtra(EXTRA_LOCATION, location)
                putExtra(EXTRA_STOPPED, stopped)
                putExtra(EXTRA_SOURCE, source.ordinal)
            }

        internal fun createErrorIntent(status: RadarStatus) =
            Intent(ACTION_RECEIVED).apply {
                putExtra(EXTRA_STATUS, status.ordinal)
            }

        internal fun createLogIntent(message: String) =
            Intent(ACTION_RECEIVED).apply {
                putExtra(EXTRA_MESSAGE, message)
            }


    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_RECEIVED) {
            if (intent.hasExtra(EXTRA_LOCATION) && intent.hasExtra(EXTRA_RESPONSE)) {
                handleSuccess(context, intent)
            } else if (intent.hasExtra(EXTRA_LOCATION) && intent.hasExtra(EXTRA_STOPPED) && intent.hasExtra(
                    EXTRA_SOURCE)) {
                handleLocation(context, intent)
            } else if (intent.hasExtra(EXTRA_STATUS)) {
                handleError(context, intent)
            } else if (intent.hasExtra(EXTRA_MESSAGE)) {
                handleLog(context, intent)
            }
        }
    }

    private fun handleSuccess(context: Context, intent: Intent) {
        val res = intent.getStringExtra(EXTRA_RESPONSE)
        val location: Location? = intent.getParcelableExtra(EXTRA_LOCATION)

        if (res != null && location != null) {
            try {
                val response = JSONObject(res)
                val eventsArr = response.getJSONArray("events")
                val events = RadarEvent.deserializeArray(eventsArr)
                val user = RadarUser.deserialize(response.getJSONObject("user"))

                if (user != null) {
                    if (events != null && events.isNotEmpty()) {
                        onEventsReceived(context, events, user)
                    }
                    onLocationUpdated(context, location, user)
                }
            } catch (e: JSONException) {
                onError(context, RadarStatus.ERROR_UNKNOWN)
            } catch (e: ParseException) {
                onError(context, RadarStatus.ERROR_UNKNOWN)
            }
        }
    }

    private fun handleLocation(context: Context, intent: Intent) {
        val location : Location? = intent.getParcelableExtra(EXTRA_LOCATION)
        val stopped = intent.getBooleanExtra(EXTRA_STOPPED, false)
        val sourceIndex = intent.getIntExtra(EXTRA_SOURCE, -1)

        val sourceValues = Radar.RadarLocationSource.values()
        val source = when (sourceIndex) {
            in sourceValues.indices -> sourceValues[sourceIndex]
            else -> Radar.RadarLocationSource.UNKNOWN
        }

        if (location != null) {
            onClientLocationUpdated(context, location, stopped, source)
        }
    }

    private fun handleError(context: Context, intent: Intent) {
        val statusIndex = intent.getIntExtra(EXTRA_STATUS, -1)

        val statusValues = RadarStatus.values()
        val status = when (statusIndex) {
            in statusValues.indices -> statusValues[statusIndex]
            else -> RadarStatus.ERROR_UNKNOWN
        }

        if (status != RadarStatus.SUCCESS && status != RadarStatus.ERROR_UNKNOWN) {
            onError(context, status)
        }
    }

    private fun handleLog(context: Context, intent: Intent) {
        val message : String? = intent.getStringExtra(EXTRA_MESSAGE)

        if (message != null) {
            onLog(context, message)
        }
    }

    /**
     * Tells the receiver that events were received for the current user.
     *
     * @param[context] The context.
     * @param[events] The events received.
     * @param[user] The current user.
     */
    abstract fun onEventsReceived(context: Context, events: Array<RadarEvent>, user: RadarUser)

    /**
     * Tells the receiver that the current user's location was updated and synced to the server.
     *
     * @param[context] The context.
     * @param[location] The location.
     * @param[user] The current user.
     */
    abstract fun onLocationUpdated(context: Context, location: Location, user: RadarUser)

    /**
     * Tells the receiver that client's location was updated but not necessarily synced to the server. To receive server-synced location updates and user state, use [onLocationUpdated] instead.
     *
     * @param[context] The context.
     * @param[location] The location.
     * @param[stopped] A boolean indicating whether the client is stopped.
     * @param[source] The source of the location.
     */
    abstract fun onClientLocationUpdated(context: Context, location: Location, stopped: Boolean, source: Radar.RadarLocationSource)

    /**
     * Tells the receiver that a request failed.
     *
     * @param[context] The context.
     * @param[status] The status.
     */
    abstract fun onError(context: Context, status: RadarStatus)

    /**
     * Tells the receiver that a debug log message was received.
     *
     * @param[message] The message.
     */
    abstract fun onLog(context: Context, message: String)

}