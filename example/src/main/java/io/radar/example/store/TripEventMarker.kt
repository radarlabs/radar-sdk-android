package io.radar.example.store

import io.radar.sdk.model.RadarEvent
import org.maplibre.android.geometry.LatLng

/**
 * A trip-related event captured during an active trip, rendered as a pin on the map
 * by `TripEventsSource`. Mirrors the iOS `TripEventMarker`.
 */
data class TripEventMarker(
    val id: String,
    val coordinate: LatLng,
    val type: RadarEvent.RadarEventType,
    val timestamp: Long,
)
