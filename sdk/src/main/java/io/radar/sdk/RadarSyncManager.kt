package io.radar.sdk

import android.content.Context
import android.location.Location
import android.os.Handler
import android.os.Looper
import io.radar.sdk.Radar.RadarStatus
import io.radar.sdk.model.RadarBeacon
import io.radar.sdk.model.RadarCircleGeometry
import io.radar.sdk.model.RadarCoordinate
import io.radar.sdk.model.RadarGeofence
import io.radar.sdk.model.RadarPlace
import io.radar.sdk.model.RadarPolygonGeometry
import io.radar.sdk.model.RadarUser
import io.radar.sdk.util.RadarTypedFileStorage
import kotlin.math.*

internal class RadarSyncManager(
    private val context: Context,
    private val apiClient: RadarApiClient,
    private val logger: RadarLogger
) {
    internal val syncStore = RadarTypedFileStorage<RadarSyncState>(
        context,
        "radar_sync_state.json",
        serializer = { it.toJson() },
        deserializer = { RadarSyncState.fromJson(it) }
    )

    private val handler = Handler(Looper.getMainLooper())
    private var syncRunnable: Runnable? = null
    private var previousSyncedGeofenceIds: List<String>? = null
    private var previousSyncedPlaceIds: List<String>? = null
    private var previousSyncedBeaconIds: List<String>? = null
    private var isFetchingSyncRegion = false
    private var rejectedPlaceIds: Set<String> = emptySet()
    private var rejectedAtLocation: Location? = null
    private var lastPlaceCheckLocation: Location? = null
    companion object {
        private const val PLACE_DETECTION_RADIUS = 75.0
        private const val PLACE_EXIT_BUFFER = 50.0
        private const val BEACON_RANGE = 100.0
        private const val BOUNDARY_THRESHOLD_FRACTION = 0.2
    }

    // region Lifecycle

    fun start(intervalMs: Long) {
        stop()
        fetchSyncRegion()

        val runnable = object : Runnable {
            override fun run() {
                fetchSyncRegion()
                handler.postDelayed(this, intervalMs)
            }
        }
        syncRunnable = runnable
        handler.postDelayed(runnable, intervalMs)
    }

    fun stop() {
        syncRunnable?.let { handler.removeCallbacks(it) }
        syncRunnable = null
        logger.d("SyncManager: Stopped sync region polling")
    }

    // endregion

    //region API

    fun fetchSyncRegion() {
        if (isFetchingSyncRegion) return
        isFetchingSyncRegion = true

        val lastLocation = RadarState.getLastLocation(context)
        if (lastLocation == null) {
            isFetchingSyncRegion = false
            logger.d("SyncManager: No last location, skipping sync region fetch")
            return
        }

        apiClient.fetchSyncRegion(
            lastLocation.latitude,
            lastLocation.longitude,
            object : RadarApiClient.RadarSyncRegionApiCallback {
                override fun onComplete(
                    status: RadarStatus,
                    geofences: Array<RadarGeofence>?,
                    places: Array<RadarPlace>?,
                    beacons: Array<RadarBeacon>?,
                    regionCenter: RadarCoordinate?,
                    regionRadius: Double?
                ) {
                    isFetchingSyncRegion = false
                    if (status != RadarStatus.SUCCESS) {
                        logger.w("SyncManager: Sync region request failed")
                        return
                    }

                    val currentState = syncStore.read()

                    if (regionCenter != null && regionRadius != null && regionRadius > 0) {
                        if (currentState?.syncedRegionCenter == null) {
                            logger.i("SyncManager: Initial sync region set | lat = ${regionCenter.latitude}; lng = ${regionCenter.longitude}; radius = $regionRadius")
                        } else if (currentState.syncedRegionCenter?.latitude != regionCenter.latitude ||
                            currentState.syncedRegionCenter?.longitude != regionCenter.longitude ||
                            currentState.syncedRegionRadius != regionRadius
                        ) {
                            logger.i("SyncManager: Sync region changed | lat = ${regionCenter.latitude}; lng = ${regionCenter.longitude}; radius = $regionRadius")
                        }
                    } else {
                        if (currentState?.syncedRegionCenter != null) {
                            logger.i("SyncManager: Sync region cleared")
                        }
                    }

                    syncStore.modify { state ->
                        val s = state ?: RadarSyncState()
                        s.syncedGeofences = geofences?.toList()
                        s.syncedPlaces = places?.toList()
                        s.syncedBeacons = beacons?.toList()
                        s.syncedRegionCenter = regionCenter
                        s.syncedRegionRadius = regionRadius
                        s
                    }
                }
            }
        )
    }

    // endregion

    // region Track Decision

    fun shouldTrack(location: Location, options: RadarTrackingOptions): Boolean {
        if (options.sync != RadarTrackingOptions.RadarTrackingOptionsSync.EVENTS) {
            logger.d("SyncManager: shouldTrack = YES | reason: sync != events")
            return true
        }

        if (!hasSyncedRegion()) {
            logger.i("SyncManager: No synced region, should track")
            return true
        }

        if (isOutsideSyncedRegion(location)) {
            logger.i("SyncManager: Outside synced region, should track")
            return true
        }

        if (isNearSyncedRegionBoundary(location)) {
            logger.i("SyncManager: Near synced region boundary, refreshing")
            fetchSyncRegion()
        }

        if (Radar.getTripOptions() != null && options.type != RadarTrackingOptions.RadarTrackingOptionsType.ON_TRIP) {
            logger.i("SyncManager: On active trip, should track")
            return true
        }

        val geofenceChanged = hasGeofenceStateChanged(location)
        val placeChanged = hasPlaceStateChanged(location)

        if (geofenceChanged || placeChanged) {
            logger.i("SyncManager: shouldTrack = YES | reason: state changed (geofence=$geofenceChanged, place=$placeChanged)")
            saveAndUpdateSyncState(location)
            return true
        }

        logger.i("SyncManager: No state change detected, skipping track")
        return false
    }

    // endregion

    // region Region Checks

    fun hasSyncedRegion(): Boolean {
        val state = syncStore.read()
        return state?.syncedRegionCenter != null && (state.syncedRegionRadius ?: 0.0) > 0
    }

    fun isNearSyncedRegionBoundary(location: Location): Boolean {
        val state = syncStore.read() ?: return false
        val center = state.syncedRegionCenter ?: return false
        val radius = state.syncedRegionRadius ?: return false

        val distanceFromCenter = distanceBetween(
            location.latitude, location.longitude,
            center.latitude, center.longitude
        )
        if (distanceFromCenter > radius) return false

        val distanceFromEdge = radius - distanceFromCenter
        return distanceFromEdge <= (radius * BOUNDARY_THRESHOLD_FRACTION)
    }

    fun isOutsideSyncedRegion(location: Location): Boolean {
        val state = syncStore.read() ?: return true
        val center = state.syncedRegionCenter ?: return true
        val radius = state.syncedRegionRadius ?: return true
        if (radius <= 0) return true

        return distanceBetween(
            location.latitude, location.longitude,
            center.latitude, center.longitude
        ) > radius
    }

    // endregion

    //region Geometry Helpers

    private fun distanceBetween(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, results)
        return results[0].toDouble()
    }

    private fun isPointInsideCircle(location: Location, center: RadarCoordinate, radius: Double): Boolean {
        return distanceBetween(location.latitude, location.longitude, center.latitude, center.longitude) <= radius
    }

    private fun isPointInsidePolygon(point: Location, polygon: Array<RadarCoordinate>): Boolean {
        if (polygon.size < 3) return false

        var inside = false
        var previousIndex = polygon.size - 1

        for (currentIndex in polygon.indices) {
            val currentVertex = polygon[currentIndex]
            val previousVertex = polygon[previousIndex]

            val currentAbove = currentVertex.latitude > point.latitude
            val previousAbove = previousVertex.latitude > point.latitude

            if (currentAbove != previousAbove) {
                val edgeLongitudeAtPointLatitude = previousVertex.longitude +
                        (point.latitude - previousVertex.latitude) /
                        (currentVertex.latitude - previousVertex.latitude) *
                        (currentVertex.longitude - previousVertex.longitude)

                if (point.longitude < edgeLongitudeAtPointLatitude) {
                    inside = !inside
                }
            }

            previousIndex = currentIndex
        }

        return inside
    }

    private fun distanceToPolygonEdge(point: Location, polygon: Array<RadarCoordinate>): Double {
        if (polygon.size < 3) return Double.MAX_VALUE

        var minimumDistance  = Double.MAX_VALUE

        for (currentIndex in polygon.indices) {
            val nextIndex = (currentIndex + 1) % polygon.size
            val distance = distanceFromPointToSegment(point, polygon[currentIndex], polygon[nextIndex])
            minimumDistance = min(minimumDistance, distance)
        }

        return minimumDistance
    }

    private fun distanceFromPointToSegment(
        point: Location,
        segmentStart: RadarCoordinate,
        segmentEnd: RadarCoordinate
    ): Double {
        val distanceToStart = distanceBetween(point.latitude, point.longitude, segmentStart.latitude, segmentStart.longitude)
        val distanceToEnd = distanceBetween(point.latitude, point.longitude, segmentEnd.latitude, segmentEnd.longitude)
        val segmentLength = distanceBetween(segmentStart.latitude, segmentStart.longitude, segmentEnd.latitude, segmentEnd.longitude)

        if (segmentLength == 0.0) return distanceToStart

        val fractionAlongSegment = max(0.0, min(1.0,
            (distanceToStart * distanceToStart - distanceToEnd * distanceToEnd + segmentLength * segmentLength) /
                    (2 * segmentLength * segmentLength)
        ))

        val closestPoint = greatCircleInterpolate(segmentStart, segmentEnd, fractionAlongSegment)
        return distanceBetween(point.latitude, point.longitude, closestPoint.latitude, closestPoint.longitude)
    }

    private fun greatCircleInterpolate(a: RadarCoordinate, b: RadarCoordinate, t: Double): RadarCoordinate {
        val lat1 = Math.toRadians(a.latitude)
        val lon1 = Math.toRadians(a.longitude)
        val lat2 = Math.toRadians(b.latitude)
        val lon2 = Math.toRadians(b.longitude)

        val deltaLat = lat2 - lat1
        val deltaLon = lon2 - lon1
        val sinHalfDLat = sin(deltaLat / 2)
        val sinHalfDLon = sin(deltaLon / 2)
        val h = sinHalfDLat * sinHalfDLat + cos(lat1) * cos(lat2) * sinHalfDLon * sinHalfDLon
        val angularDistance = 2.0 * atan2(sqrt(h.coerceIn(0.0, 1.0)), sqrt((1 - h).coerceAtLeast(0.0)))

        if (angularDistance < 1e-12) return a

        val sinD = sin(angularDistance)
        val aCoeff = sin((1 - t ) * angularDistance) / sinD
        val bCoeff = sin(t * angularDistance) / sinD

        val x = aCoeff * cos(lat1) * cos(lon1) + bCoeff * cos(lat2) * cos(lon2)
        val y = aCoeff * cos(lat1) * sin(lon1) + bCoeff * cos(lat2) * sin(lon2)
        val z = aCoeff * sin(lat1) + bCoeff * sin(lat2)

        val lat = Math.toDegrees(atan2(z, sqrt( x * x + y * y)))
        val lon = Math.toDegrees(atan2(y, x))

        return RadarCoordinate(lat, lon)
    }

    // endregion

    // region Containment Queries

    private fun isLocationInside(
        geofence: RadarGeofence,
        location: Location,
        accuracy: Double,
        checkingForExit: Boolean
    ): Boolean {
        val sdkConfig = RadarSettings.getSdkConfiguration(context)
        val shouldBuffer = if (checkingForExit) sdkConfig.bufferGeofenceExits else sdkConfig.bufferGeofenceEntries

        return when (val geometry = geofence.geometry) {
            is RadarCircleGeometry -> {
                var effectiveRadius = geometry.radius
                if (shouldBuffer) effectiveRadius += accuracy
                isPointInsideCircle(location, geometry.center, effectiveRadius)
            }
            is RadarPolygonGeometry -> {
                val coordinates = geometry.coordinates
                if (coordinates != null && isPointInsidePolygon(location, coordinates)) {
                    return true
                }
                if (shouldBuffer) {
                    val bufferRadius = geometry.radius + accuracy
                    if (!isPointInsideCircle(location, geometry.center, bufferRadius)) {
                        return false
                    }
                    if (coordinates != null) {
                        return distanceToPolygonEdge(location, coordinates) <= accuracy
                    }
                }
                false
            }
            else -> false
        }
    }

    fun getGeofences(location: Location, checkingForExit: Boolean = false): List<RadarGeofence> {
        val geofences = syncStore.read()?.syncedGeofences ?: return  emptyList()
        val accuracy = max(location.accuracy.toDouble(), 0.0)
        return geofences.filter { isLocationInside(it, location, accuracy, checkingForExit) }
    }

    fun getBeacons(location: Location): List<RadarBeacon> {
        val beacons = syncStore.read()?.syncedBeacons ?: return  emptyList()
        return beacons.filter { beacon ->
            val beaconLocation = beacon.location ?: return@filter false
            distanceBetween(
                location.latitude, location.longitude,
                beaconLocation.latitude, beaconLocation.longitude
            ) <= BEACON_RANGE
        }
    }

    fun getPlaces(location: Location): List<RadarPlace> {
        val places = syncStore.read()?.syncedPlaces ?: return emptyList()
        val isStopped = RadarState.getStopped(context)
        if (!isStopped) return emptyList()
        val closest = places
            .mapNotNull { place ->
                val radius = (place.geometryRadius ?: 0.0) + PLACE_DETECTION_RADIUS
                val distance = distanceBetween(
                    location.latitude, location.longitude,
                    place.location.latitude, place.location.longitude
                )
                if (distance <= radius) place to distance else null
            }
            .minByOrNull { it.second }
            ?.first
        if (closest != null) {
            logger.d("SyncManager: getPlaces matched ${closest._id} (geoR=${closest.geometryRadius})")
            return listOf(closest)
        }
        return emptyList()
    }

    // endregion

    // region State Detection

    fun hasGeofenceStateChanged(location: Location): Boolean {
        val state = syncStore.read() ?: RadarSyncState()
        val lastKnownGeofenceIds = state.lastSyncedGeofenceIds.toSet()
        val currentGeofences = getGeofences(location)
        val currentGeofenceIds = currentGeofences.map { it._id }.toSet()

        if (checkForGeofenceEntries(currentGeofences, currentGeofenceIds, lastKnownGeofenceIds)) return  true
        if (checkForGeofenceExits(location, lastKnownGeofenceIds)) return true
        if (checkForGeofenceDwell(currentGeofences, currentGeofenceIds, lastKnownGeofenceIds)) return true

        return false
    }

    fun hasBeaconStateChanged(rangedBeaconIds: Set<String>): Boolean {
        val state = syncStore.read() ?: RadarSyncState()
        val lastKnownBeaconIds = state.lastSyncedBeaconIds.toSet()

        val enteredBeaconIds = rangedBeaconIds - lastKnownBeaconIds
        val exitedBeaconIds = lastKnownBeaconIds - rangedBeaconIds

        if (enteredBeaconIds.isNotEmpty()) {
            logger.i("SyncManager: Detected beacon entry (BLE confirmed): $enteredBeaconIds")
        }

        if (exitedBeaconIds.isNotEmpty()) {
            logger.i("SyncManager: Detected beacon exit (BLE confirmed): $exitedBeaconIds")
        }

        return rangedBeaconIds != lastKnownBeaconIds
    }

    fun hasPlaceStateChanged(location: Location): Boolean {
        lastPlaceCheckLocation = location

        val rejectedLocation = rejectedAtLocation
        if (rejectedLocation != null && rejectedPlaceIds.isNotEmpty()) {
            val moved = location.distanceTo(rejectedLocation) > rejectedLocation.accuracy
            val accuracyImproved = location.accuracy < rejectedLocation.accuracy * 0.5f
            if (moved || accuracyImproved) {
                logger.d("SyncManager: Clearing place rejections | moved=${location.distanceTo(rejectedLocation)} accuracy=${location.accuracy} wasAccuracy=${rejectedLocation.accuracy}")
                rejectedPlaceIds = emptySet()
                rejectedAtLocation = null
            }
        }

        val state = syncStore.read() ?: RadarSyncState()
        val lastKnownPlaceIds = state.lastSyncedPlaceIds.toSet()
        val allPlaces = state.syncedPlaces ?: emptyList()

        // Check for exits — user moved beyond geometryRadius + 50m
        if (lastKnownPlaceIds.isNotEmpty()) {
            val lastPlace = allPlaces.firstOrNull { it._id in lastKnownPlaceIds }
            if (lastPlace != null) {
                val exitRadius = (lastPlace.geometryRadius ?: 0.0) + PLACE_EXIT_BUFFER
                if (!isPointInsideCircle(location, lastPlace.location, exitRadius)) {
                    logger.i("SyncManager: Detected place exit: ${lastPlace._id}")
                    return true
                }
            }
        }

        // Check for entries — stopped + within geometryRadius + 75m, excluding rejected places
        val currentPlaces = getPlaces(location)
        val currentIds = currentPlaces.map { it._id }.toSet()
        val enteredPlaceIds = currentIds - lastKnownPlaceIds - rejectedPlaceIds

        if (enteredPlaceIds.isNotEmpty()) {
            if (lastKnownPlaceIds.isNotEmpty()) {
                val lastPlace = allPlaces.firstOrNull { it._id in lastKnownPlaceIds }
                if (lastPlace != null) {
                    val exitRadius = (lastPlace.geometryRadius ?: 0.0) + PLACE_EXIT_BUFFER
                    if (isPointInsideCircle(location, lastPlace.location, exitRadius)) {
                        logger.d("SyncManager: Skipping place switch (still within exit radius of last place)")
                        return false
                    }
                }
            }
            logger.i("SyncManager: Detected place entry: $enteredPlaceIds")
            return true
        }

        return false
    }

    private fun checkForGeofenceEntries(
        currentGeofences: List<RadarGeofence>,
        currentGeofenceIds: Set<String>,
        lastKnownGeofenceIds: Set<String>
    ): Boolean {
        val enteredGeofenceIds = currentGeofenceIds - lastKnownGeofenceIds
        if (enteredGeofenceIds.isEmpty()) return false

        val sdkConfig = RadarSettings.getSdkConfiguration(context)
        val projectStopDetection = sdkConfig.stopDetection
        val isStopped = RadarState.getStopped(context)

        val timestamps = syncStore.read()?.geofenceEntryTimestamps?.toMutableMap() ?: mutableMapOf()
        var hasEntry = false

        for (id in enteredGeofenceIds) {
            val geofence = currentGeofences.firstOrNull { it._id == id }
            val requireStop = geofence?.stopDetection ?: projectStopDetection
            if (requireStop && !isStopped) {
                logger.d("SyncManager: Skipping geofence entry (stop detection, not stopped): $id")
                continue
            }
            logger.d("SyncManager: Detected geofence entry: $id")
            timestamps[id] = System.currentTimeMillis()  / 1000.0
            hasEntry = true
        }

        if (hasEntry) {
            syncStore.modify { state ->
                val s = state ?: RadarSyncState()
                s.geofenceEntryTimestamps = timestamps
                s
            }
        }
        return hasEntry
    }

    private fun checkForGeofenceExits(
        location: Location,
        lastKnownGeofenceIds: Set<String>
    ): Boolean {
        val exitCheckGeofences = getGeofences(location, checkingForExit = true)
        val exitCheckGeofenceIds = exitCheckGeofences.map { it._id}.toSet()
        val exitedGeofenceIds = lastKnownGeofenceIds - exitCheckGeofenceIds

        if (exitedGeofenceIds.isEmpty()) return false

        for (id in exitedGeofenceIds) {
            logger.d("SyncManager: Detected geofence exit: $id")
        }

        syncStore.modify {  state ->
            val s = state ?: return@modify state
            for (id in exitedGeofenceIds) {
                s.geofenceEntryTimestamps.remove(id)
                s.dwellEventsFired.removeAll { it == id }
            }
            s
        }
        return true
    }

    private fun checkForGeofenceDwell(
        currentGeofences: List<RadarGeofence>,
        currentGeofenceIds: Set<String>,
        lastKnownGeofenceIds: Set<String>
    ): Boolean {
        val sdkConfig = RadarSettings.getSdkConfiguration(context)
        val projectDwellThreshold = sdkConfig.defaultGeofenceDwellThreshold
        val anyGeofenceHasDwell = currentGeofences.any { it.dwellThreshold != null }

        if (projectDwellThreshold <= 0 && !anyGeofenceHasDwell) return false

        val state = syncStore.read() ?: RadarSyncState()
        val timestamps = state.geofenceEntryTimestamps
        val dwellFired = state.dwellEventsFired.toSet()

        for (id in currentGeofenceIds.intersect(lastKnownGeofenceIds)) {
            if (id in dwellFired) continue
            val entryTimestamp = timestamps[id] ?: continue

            val geofence = currentGeofences.firstOrNull { it._id == id }
            val thresholdMinutes: Double = when {
                geofence?.dwellThreshold != null -> geofence.dwellThreshold
                projectDwellThreshold > 0 -> projectDwellThreshold.toDouble()
                else -> continue
            }

            val elapsedMinutes = (System.currentTimeMillis() / 1000.0 - entryTimestamp) / 60.0

            if (elapsedMinutes >= thresholdMinutes) {
                logger.d("SyncManager: Dwell threshold reached for geofence: $id")
                syncStore.modify { state ->
                    state?.dwellEventsFired?.add(id)
                    state
                }
                return true
            }
        }

        return false
    }

    // endregion

    // region State Updates

    private fun updateLastKnownSyncState(location: Location) {
        val timestamps = syncStore.read()?.geofenceEntryTimestamps ?: emptyMap()
        val acceptedGeofenceIds = timestamps.keys.toList()
        val currentPlaceIds = getPlaces(location).map { it._id }

        logger.i("SyncManager: Optimistic update | geofences=$acceptedGeofenceIds places=$currentPlaceIds")

        syncStore.modify { state ->
            val s = state ?: RadarSyncState()
            s.lastSyncedGeofenceIds = acceptedGeofenceIds
            s.lastSyncedPlaceIds = currentPlaceIds
            s
        }
    }

    private fun saveAndUpdateSyncState(location: Location) {
        val state = syncStore.read() ?: RadarSyncState()
        previousSyncedGeofenceIds = state.lastSyncedGeofenceIds
        previousSyncedPlaceIds = state.lastSyncedPlaceIds

        logger.i(
            "SyncManager: Saving previous state before optimistic update | " +
                    "geofences=${previousSyncedGeofenceIds?.size ?: 0} " +
                    "places=${previousSyncedPlaceIds?.size ?: 0} "
        )

        updateLastKnownSyncState(location)
    }

    // endregion

    // region Server Reconciliation

    fun reconcileSyncState(user: RadarUser) {
        val serverGeofenceIds = user.geofences?.map { it._id } ?: emptyList()
        val serverPlaceIds = if (user.place?._id != null) listOf(user.place._id) else emptyList()
        val serverBeaconIds = user.beacons?.mapNotNull { it._id } ?: emptyList()

        val state = syncStore.read() ?: RadarSyncState()
        val clientGeofenceIds = state.lastSyncedGeofenceIds
        val clientPlaceIds = state.lastSyncedPlaceIds
        val clientBeaconIds = state.lastSyncedBeaconIds

        val geofenceMismatch = serverGeofenceIds.toSet() != clientGeofenceIds.toSet()
        val placeMismatch = serverPlaceIds.toSet() != clientPlaceIds.toSet()
        val beaconMismatch = serverBeaconIds.toSet() != clientBeaconIds.toSet()

        if (geofenceMismatch || placeMismatch || beaconMismatch) {
            if (geofenceMismatch) {
                val serverOnly = serverGeofenceIds.toSet() - clientGeofenceIds.toSet()
                val clientOnly = clientGeofenceIds.toSet() - serverGeofenceIds.toSet()
                if (serverOnly.isNotEmpty()) logger.i("SyncManager: Server added geofences: $serverOnly")
                if (clientOnly.isNotEmpty()) logger.i("SyncManager: Server removed geofences: $clientOnly")
            }
            if (beaconMismatch) {
                val serverOnly = serverBeaconIds.toSet() - clientBeaconIds.toSet()
                val clientOnly = clientBeaconIds.toSet() - serverBeaconIds.toSet()
                if (serverOnly.isNotEmpty()) logger.i("SyncManager: Server added beacons: $serverOnly")
                if (clientOnly.isNotEmpty()) logger.i("SyncManager: Server removed beacons: $clientOnly")
            }
            if (placeMismatch) {
                val serverOnly = serverPlaceIds.toSet() - clientPlaceIds.toSet()
                val clientOnly = clientPlaceIds.toSet() - serverPlaceIds.toSet()
                if (serverOnly.isNotEmpty()) logger.i("SyncManager: Server added places: $serverOnly")
                if (clientOnly.isNotEmpty()) {
                    logger.i("SyncManager: Server removed places: $clientOnly")
                    rejectedPlaceIds = rejectedPlaceIds + clientOnly
                    rejectedAtLocation = lastPlaceCheckLocation
                }
            }

            syncStore.modify { state ->
                val s = state ?: RadarSyncState()
                s.lastSyncedGeofenceIds = serverGeofenceIds
                s.lastSyncedPlaceIds = serverPlaceIds
                s.lastSyncedBeaconIds = serverBeaconIds

                val serverSet = serverGeofenceIds.toSet()
                s.geofenceEntryTimestamps = s.geofenceEntryTimestamps.filter { it.key in serverSet }.toMutableMap()
                s.dwellEventsFired = s.dwellEventsFired.filter { it in serverSet }.toMutableList()
                s
            }
        } else {
            logger.i("SyncManager: Client state matches server")
        }
        clearPreviousState()
    }

    fun saveBeaconState(beaconIds: List<String>) {
        previousSyncedBeaconIds = syncStore.read()?.lastSyncedBeaconIds

        logger.i("SyncManager: Saving beacon state | previous=${previousSyncedBeaconIds?.size ?: 0} new=${beaconIds.size}")

        syncStore.modify { state ->
            val s = state ?: RadarSyncState()
            s.lastSyncedBeaconIds = beaconIds
            s
        }
    }

    fun rollbackSyncState() {
        if (previousSyncedGeofenceIds == null && previousSyncedPlaceIds == null && previousSyncedBeaconIds == null) return

        logger.i("SyncManager: Track failed, rolling back to previous sync state")

        syncStore.modify { state ->
            val s = state ?: RadarSyncState()
            previousSyncedGeofenceIds?.let { s.lastSyncedGeofenceIds = it }
            previousSyncedPlaceIds?.let { s.lastSyncedPlaceIds = it }
            previousSyncedBeaconIds?.let { s.lastSyncedBeaconIds = it }
            s
        }
        clearPreviousState()
    }

    private fun clearPreviousState() {
        previousSyncedGeofenceIds = null
        previousSyncedPlaceIds = null
        previousSyncedBeaconIds = null
    }

    // endregion
}