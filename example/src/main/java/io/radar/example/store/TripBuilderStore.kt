package io.radar.example.store

import android.location.Location
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.radar.example.map.overlays.MapOverlayRegistry
import io.radar.sdk.Radar
import io.radar.sdk.RadarTripOptions
import io.radar.sdk.model.RadarEvent
import io.radar.sdk.model.RadarTrip
import io.radar.sdk.model.RadarTripLeg
import org.maplibre.android.geometry.LatLng

/**
 * Map-driven trip selection + active-trip mirror. Mirrors the iOS `TripBuilderStore`.
 *
 * Selection state ([selectedDestinations], [pendingHit]) is populated by tapping geofences
 * on the map. Starting a trip turns the selection into a `RadarTripOptions` (single
 * destination) or a set of `RadarTripLeg`s (multi-leg). The active trip and its
 * visualization ([breadcrumbs], [tripEventMarkers]) are mirrored off `Radar.getTrip()`
 * and the [LogStore] event/location funnel.
 */
class TripBuilderStore {

    // Selection state
    val selectedDestinations = mutableStateListOf<TripDestination>()
    var pendingHit by mutableStateOf<TripDestination?>(null)
        private set

    // Active trip mirror
    var activeTrip by mutableStateOf<RadarTrip?>(null)
        private set
    val breadcrumbs = mutableStateListOf<LatLng>()
    val tripEventMarkers = mutableStateListOf<TripEventMarker>()

    val hasActiveTrip: Boolean get() = activeTrip != null

    private var logStore: LogStore? = null
    private var registry: MapOverlayRegistry? = null
    private val main = Handler(Looper.getMainLooper())

    fun bind(logStore: LogStore, registry: MapOverlayRegistry) {
        this.logStore = logStore
        this.registry = registry
        logStore.onEvents = { events, _ ->
            refreshActiveTrip()
            captureTripEvents(events)
        }
        logStore.onLocation = { location ->
            appendBreadcrumb(location)
        }
        refreshActiveTrip()
    }

    // MARK: - Selection

    fun isSelected(id: String): Boolean = selectedDestinations.any { it.id == id }

    fun add(destination: TripDestination): Boolean {
        if (selectedDestinations.any { it.id == destination.id }) return false
        selectedDestinations.add(destination)
        return true
    }

    fun removeAt(index: Int) {
        if (index in selectedDestinations.indices) selectedDestinations.removeAt(index)
    }

    fun move(from: Int, to: Int) {
        if (from in selectedDestinations.indices && to in selectedDestinations.indices) {
            val item = selectedDestinations.removeAt(from)
            selectedDestinations.add(to, item)
        }
    }

    fun clearSelection() = selectedDestinations.clear()

    fun proposeHit(destination: TripDestination) {
        pendingHit = destination
    }

    fun confirmPendingHit() {
        val dest = pendingHit ?: return
        if (isSelected(dest.id)) {
            selectedDestinations.removeAll { it.id == dest.id }
        } else {
            add(dest)
        }
        pendingHit = null
    }

    fun dismissPendingHit() {
        pendingHit = null
    }

    // MARK: - Active trip mirror

    fun refreshActiveTrip() {
        main.post {
            val wasActive = activeTrip != null
            activeTrip = Radar.getTrip()
            val nowActive = activeTrip != null
            registry?.setTripMode(nowActive)
            if (wasActive && !nowActive) {
                clearTripVisualization()
            }
            registry?.requestRender()
        }
    }

    // MARK: - Lifecycle actions

    fun startTrip() {
        if (selectedDestinations.isEmpty()) return

        val externalId = "map_trip_${System.currentTimeMillis() / 1000}"
        val options: RadarTripOptions
        val label: String

        val single = selectedDestinations.singleOrNull()
        if (single is TripDestination.Geofence && !single.tag.isNullOrEmpty() && !single.externalId.isNullOrEmpty()) {
            options = RadarTripOptions(
                externalId = externalId,
                destinationGeofenceTag = single.tag,
                destinationGeofenceExternalId = single.externalId,
                mode = Radar.RadarRouteMode.CAR,
            )
            label = "startTrip (map, single destination)"
        } else {
            val legs = selectedDestinations.map(::buildLeg).toTypedArray()
            options = RadarTripOptions(
                externalId = externalId,
                mode = Radar.RadarRouteMode.CAR,
                legs = legs,
            )
            label = if (legs.size == 1) "startTrip (map, 1 leg)" else "startTrip (map, ${legs.size} legs)"
        }

        logStore?.writeAction(label)
        Radar.startTrip(options) { status, trip, _ ->
            main.post {
                refreshActiveTrip()
                if (status == Radar.RadarStatus.SUCCESS) selectedDestinations.clear()
                logStore?.writeStatus(status, "$label: $status", tripDetail(trip))
            }
        }
    }

    fun advanceCurrentLeg(status: RadarTripLeg.RadarTripLegStatus) {
        val trip = activeTrip ?: return
        val tripId = trip._id
        val legId = trip.currentLegId ?: return
        val label = "updateTripLeg(${RadarTripLeg.stringForStatus(status)}) (map)"
        logStore?.writeAction(label)
        Radar.updateTripLeg(tripId, legId, status) { sdkStatus, trip2, leg, _ ->
            main.post {
                refreshActiveTrip()
                logStore?.writeStatus(sdkStatus, "$label: $sdkStatus", tripDetail(trip2, leg))
            }
        }
    }

    fun completeTrip() {
        val label = "completeTrip (map)"
        logStore?.writeAction(label)
        Radar.completeTrip { status, trip, _ ->
            main.post {
                refreshActiveTrip()
                logStore?.writeStatus(status, "$label: $status", tripDetail(trip))
            }
        }
    }

    fun cancelTrip() {
        val label = "cancelTrip (map)"
        logStore?.writeAction(label)
        Radar.cancelTrip { status, trip, _ ->
            main.post {
                refreshActiveTrip()
                logStore?.writeStatus(status, "$label: $status", tripDetail(trip))
            }
        }
    }

    enum class LegMoveDirection { UP, DOWN }

    fun moveLeg(legId: String, direction: LegMoveDirection) {
        val trip = activeTrip ?: return
        val legs = trip.legs ?: return
        val currentIndex = legs.indexOfFirst { it._id == legId }
        if (currentIndex < 0) return
        val targetIndex = if (direction == LegMoveDirection.UP) currentIndex - 1 else currentIndex + 1
        if (targetIndex !in legs.indices) return
        if (legs[targetIndex].status != RadarTripLeg.RadarTripLegStatus.PENDING) return

        val legIds = legs.mapNotNull { it._id }.toMutableList()
        if (legIds.size != legs.size) return
        val tmp = legIds[currentIndex]
        legIds[currentIndex] = legIds[targetIndex]
        legIds[targetIndex] = tmp

        val arrow = if (direction == LegMoveDirection.UP) "↑" else "↓"
        val label = "reorderTripLegs (map, $arrow)"
        logStore?.writeAction(label)
        Radar.reorderTripLegs(trip._id, legIds.toTypedArray()) { status, trip2, events ->
            main.post {
                refreshActiveTrip()
                if (!events.isNullOrEmpty()) captureTripEvents(events, forced = true)
                logStore?.writeStatus(status, "$label: $status", tripDetail(trip2))
            }
        }
    }

    // MARK: - Visualization

    private fun appendBreadcrumb(location: Location) {
        main.post {
            if (activeTrip == null) return@post
            val last = breadcrumbs.lastOrNull()
            if (last != null) {
                val results = FloatArray(1)
                Location.distanceBetween(last.latitude, last.longitude, location.latitude, location.longitude, results)
                if (results[0] < BREADCRUMB_DEDUPE_METERS) return@post
            }
            breadcrumbs.add(LatLng(location.latitude, location.longitude))
            registry?.requestRender()
        }
    }

    private fun captureTripEvents(events: Array<RadarEvent>, forced: Boolean = false) {
        main.post {
            var appended = false
            for (event in events) {
                if (!forced && event.type !in TRIP_EVENT_TYPES) continue
                if (tripEventMarkers.any { it.id == event._id }) continue
                tripEventMarkers.add(
                    TripEventMarker(
                        id = event._id,
                        coordinate = LatLng(event.location.latitude, event.location.longitude),
                        type = event.type,
                        timestamp = event.createdAt.time,
                    ),
                )
                appended = true
            }
            if (appended) registry?.requestRender()
        }
    }

    private fun clearTripVisualization() {
        breadcrumbs.clear()
        tripEventMarkers.clear()
        registry?.requestRender()
    }

    // MARK: - Helpers

    private fun buildLeg(destination: TripDestination): RadarTripLeg = when (destination) {
        is TripDestination.Geofence ->
            if (!destination.tag.isNullOrEmpty() && !destination.externalId.isNullOrEmpty()) {
                RadarTripLeg.forGeofence(destination.tag, destination.externalId)
            } else {
                RadarTripLeg.forGeofenceId(destination.id)
            }

        is TripDestination.Coordinate ->
            RadarTripLeg.forCoordinates(destination.coordinate.latitude, destination.coordinate.longitude)
                .apply { arrivalRadius = 100 }
    }

    private fun tripDetail(trip: RadarTrip?, leg: RadarTripLeg? = null): String {
        if (trip == null) return "no trip"
        val lines = mutableListOf(
            "externalId: ${trip.externalId}",
            "trip.status: ${trip.status.name.lowercase()}",
            "currentLegId: ${trip.currentLegId ?: "—"}",
        )
        trip.legs?.let { lines.add("legs: ${it.size}") }
        leg?.let { lines.add("leg.status: ${RadarTripLeg.stringForStatus(it.status)}") }
        return lines.joinToString("\n")
    }

    companion object {
        private const val BREADCRUMB_DEDUPE_METERS = 10f

        private val TRIP_EVENT_TYPES = setOf(
            RadarEvent.RadarEventType.USER_STARTED_TRIP,
            RadarEvent.RadarEventType.USER_APPROACHING_TRIP_DESTINATION,
            RadarEvent.RadarEventType.USER_ARRIVED_AT_TRIP_DESTINATION,
            RadarEvent.RadarEventType.USER_STOPPED_TRIP,
        )
    }
}
