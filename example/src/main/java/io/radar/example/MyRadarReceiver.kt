package io.radar.example

import android.content.Context
import android.location.Location
import androidx.compose.runtime.mutableStateListOf
import io.radar.sdk.Radar
import io.radar.sdk.RadarReceiver
import io.radar.sdk.model.RadarEvent
import io.radar.sdk.model.RadarUser

class MyRadarReceiver : RadarReceiver() {
    var events = mutableStateListOf<RadarEvent>()
    var logs = mutableStateListOf<String>()

    override fun onEventsReceived(context: Context, events: Array<RadarEvent>, user: RadarUser?) {
        this.events.addAll(events)
    }

    override fun onLocationUpdated(context: Context, location: Location, user: RadarUser) {
    }

    override fun onClientLocationUpdated(context: Context, location: Location, stopped: Boolean, source: Radar.RadarLocationSource) {
    }

    override fun onError(context: Context, status: Radar.RadarStatus) {
    }

    override fun onLog(context: Context, message: String) {
        this.logs.add(message)
    }
}
