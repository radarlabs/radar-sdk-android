package io.radar.example.tests.panels

import android.location.Location
import androidx.compose.runtime.Composable
import io.radar.example.Utils
import io.radar.example.components.ActionButton
import io.radar.example.components.ActionButtonStyle
import io.radar.example.components.TogglePanel
import io.radar.example.store.LocalLogStore
import io.radar.sdk.Radar
import io.radar.sdk.RadarTrackingOptions
import io.radar.sdk.RadarTripOptions

@Composable
fun TrackingPanel() {
    val log = LocalLogStore.current

    TogglePanel("Tracking", initiallyExpanded = true) {
        ActionButton("trackOnce") {
            Radar.trackOnce { status, location, events, user ->
                log.writeStatus(
                    status,
                    "trackOnce: ${Utils.stringForRadarStatus(status)}",
                    "location = $location\nevents = ${events?.size ?: 0}\nuserId = ${user?.userId}",
                )
            }
        }
        ActionButton("startTracking (responsive)", style = ActionButtonStyle.PRIMARY) {
            Radar.startTracking(RadarTrackingOptions.RESPONSIVE)
            log.writeResult("startTracking (responsive)")
        }
        ActionButton("startTracking (continuous)", style = ActionButtonStyle.PRIMARY) {
            Radar.startTracking(RadarTrackingOptions.CONTINUOUS)
            log.writeResult("startTracking (continuous)")
        }
        ActionButton("stopTracking", style = ActionButtonStyle.DESTRUCTIVE) {
            Radar.stopTracking()
            log.writeResult("stopTracking")
        }
        ActionButton("getContext") {
            Radar.getContext { status, location, context ->
                log.writeStatus(
                    status,
                    "getContext: ${Utils.stringForRadarStatus(status)}",
                    "geofences = ${context?.geofences?.size ?: 0}\nplace = ${context?.place?.name}\ncountry = ${context?.country?.name}",
                )
            }
        }
        ActionButton("getLocation") {
            Radar.getLocation { status, location, stopped ->
                log.writeStatus(
                    status,
                    "getLocation: ${Utils.stringForRadarStatus(status)}",
                    "location = $location\nstopped = $stopped",
                )
            }
        }
        ActionButton("mockTracking") {
            val origin = Location("example").apply { latitude = 40.78382; longitude = -73.97536 }
            val destination = Location("example").apply { latitude = 40.70390; longitude = -73.98670 }
            val tripOptions = RadarTripOptions(
                "299",
                null,
                "store",
                "123",
                Radar.RadarRouteMode.CAR,
                approachingThreshold = 0,
            )
            Radar.startTrip(tripOptions, RadarTrackingOptions.CONTINUOUS)
            var i = 0
            Radar.mockTracking(origin, destination, Radar.RadarRouteMode.CAR, 3, 3) { status, location, _, _ ->
                log.writeStatus(status, "mockTracking step: ${Utils.stringForRadarStatus(status)}", "location = $location")
                if (i == 2) Radar.completeTrip()
                i++
            }
        }
    }
}
