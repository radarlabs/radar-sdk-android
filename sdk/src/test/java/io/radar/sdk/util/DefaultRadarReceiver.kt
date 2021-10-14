package io.radar.sdk.util

import android.content.Context
import android.location.Location
import io.radar.sdk.Radar
import io.radar.sdk.RadarReceiver
import io.radar.sdk.model.RadarEvent
import io.radar.sdk.model.RadarUser

internal open class DefaultRadarReceiver : RadarReceiver() {

    override fun onEventsReceived(context: Context, events: Array<RadarEvent>, user: RadarUser?) = Unit

    override fun onLocationUpdated(context: Context, location: Location, user: RadarUser) = Unit

    override fun onClientLocationUpdated(
        context: Context,
        location: Location,
        stopped: Boolean,
        source: Radar.RadarLocationSource
    ) = Unit

    override fun onError(context: Context, status: Radar.RadarStatus) = Unit

    override fun onLog(context: Context, message: String) = Unit
}