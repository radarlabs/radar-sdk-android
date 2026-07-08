package io.radar.example.store

import org.maplibre.android.geometry.LatLng

/**
 * A destination selected on the map (by tapping a geofence) and staged for a trip.
 *
 * Mirrors the iOS `TripDestination` enum. [TripBuilderStore] turns these into
 * `RadarTripOptions` / `RadarTripLeg`s when a trip is started.
 */
sealed class TripDestination {
    /** Stable identifier used for dedupe/selection. */
    abstract val id: String

    /** User-facing label shown in the builder tray. */
    abstract val title: String

    /** Where to drop the pin / center the leg. */
    abstract val coordinate: LatLng

    data class Geofence(
        override val id: String,
        val tag: String?,
        val externalId: String?,
        override val title: String,
        override val coordinate: LatLng,
    ) : TripDestination()

    data class Coordinate(
        override val coordinate: LatLng,
        override val title: String,
    ) : TripDestination() {
        override val id: String = "coord_${coordinate.latitude}_${coordinate.longitude}"
    }
}
