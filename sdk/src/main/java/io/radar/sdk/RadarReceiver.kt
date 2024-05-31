package io.radar.sdk

import android.content.Context
import android.location.Location
import io.radar.sdk.Radar.RadarStatus
import io.radar.sdk.model.RadarEvent
import io.radar.sdk.model.RadarLocationPermissionStatus
import io.radar.sdk.model.RadarUser

/**
 * A receiver for client-side delivery of events, location updates, and debug logs. For more information, see [](https://radar.com/documentation/sdk).
 *
 * @see [](https://radar.com/documentation/sdk)
 */
abstract class RadarReceiver {

    /**
     * Tells the receiver that events were received.
     *
     * @param[context] The context.
     * @param[events] The events received.
     * @param[user] The user, if any.
     */
    abstract fun onEventsReceived(context: Context, events: Array<RadarEvent>, user: RadarUser?)

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

    /**
     * Tells the reciever that the location permissions status was updated.
     *
     * @param[context] The context.
     * @param[status] The location permissions status.
     */
    abstract fun onLocationPermissionStatusUpdated(context: Context, status: RadarLocationPermissionStatus)

}