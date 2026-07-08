package io.radar.example.tests.panels

import androidx.compose.runtime.Composable
import io.radar.example.Utils
import io.radar.example.components.ActionButton
import io.radar.example.components.ActionButtonStyle
import io.radar.example.components.TogglePanel
import io.radar.example.store.LocalLogStore
import io.radar.sdk.Radar
import io.radar.sdk.RadarTrackingOptions
import io.radar.sdk.RadarTripOptions
import java.util.Date

@Composable
fun TripsPanel() {
    val log = LocalLogStore.current

    TogglePanel("Trips") {
        ActionButton("startTrip", style = ActionButtonStyle.PRIMARY) {
            val options = RadarTripOptions("400", null, "store", "123", Radar.RadarRouteMode.CAR, approachingThreshold = 9)
            Radar.startTrip(options) { status, trip, _ ->
                log.writeStatus(status, "startTrip: ${Utils.stringForRadarStatus(status)}", "trip = ${trip?.externalId}, status = ${trip?.status}")
            }
        }
        ActionButton("startTrip (startTracking = false)") {
            val options = RadarTripOptions("501", null, "store", "123", Radar.RadarRouteMode.CAR, approachingThreshold = 9, startTracking = false)
            Radar.startTrip(options) { status, trip, _ ->
                log.writeStatus(status, "startTrip (no tracking): ${Utils.stringForRadarStatus(status)}", "trip = ${trip?.externalId}")
            }
        }
        ActionButton("startTrip (with tracking options)") {
            val options = RadarTripOptions("502", null, "store", "123", Radar.RadarRouteMode.CAR, approachingThreshold = 9)
            Radar.startTrip(options, RadarTrackingOptions.CONTINUOUS) { status, trip, _ ->
                log.writeStatus(status, "startTrip (tracking options): ${Utils.stringForRadarStatus(status)}", "trip = ${trip?.externalId}")
            }
        }
        ActionButton("startTrip (startTrackingAfter +3m)") {
            val options = RadarTripOptions("507", null, "store", "123", Radar.RadarRouteMode.CAR, approachingThreshold = 9, startTracking = false)
            val trackingOptions = RadarTrackingOptions.CONTINUOUS.copy().apply {
                startTrackingAfter = Date(System.currentTimeMillis() + 3 * 60 * 1000)
            }
            Radar.startTrip(options, trackingOptions) { status, trip, _ ->
                log.writeStatus(status, "startTrip (startTrackingAfter): ${Utils.stringForRadarStatus(status)}", "trip = ${trip?.externalId}")
            }
        }
        ActionButton("completeTrip") {
            Radar.completeTrip { status, trip, _ ->
                log.writeStatus(status, "completeTrip: ${Utils.stringForRadarStatus(status)}", "trip = ${trip?.externalId}, status = ${trip?.status}")
            }
        }
        ActionButton("cancelTrip", style = ActionButtonStyle.DESTRUCTIVE) {
            Radar.cancelTrip { status, trip, _ ->
                log.writeStatus(status, "cancelTrip: ${Utils.stringForRadarStatus(status)}", "trip = ${trip?.externalId}, status = ${trip?.status}")
            }
        }
    }
}
