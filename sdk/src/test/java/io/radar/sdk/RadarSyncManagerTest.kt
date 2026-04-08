package io.radar.sdk

import android.content.Context
import android.location.Location
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.robolectric.shadows.ShadowLooper
import io.radar.sdk.model.RadarBeacon
import io.radar.sdk.model.RadarCircleGeometry
import io.radar.sdk.model.RadarCoordinate
import io.radar.sdk.model.RadarGeofence
import io.radar.sdk.model.RadarPlace
import io.radar.sdk.model.RadarPolygonGeometry
import io.radar.sdk.model.RadarSdkConfiguration
import io.radar.sdk.model.RadarUser
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4:: class)
@Config(sdk = [Build.VERSION_CODES.P])
class RadarSyncManagerTest {

    companion object {
        private const val TEST_LAT = 40.78382
        private const val TEST_LNG = -73.97536
        private const val TEST_LAT_NEARBY = 40.78427
        private const val TEST_LAT_FAR = 40.78562
    }

    private lateinit var context: Context
    private lateinit var syncManager: RadarSyncManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        Radar.initialize(context, "prj_test_pk_0000000000000000000000000000000000000000")
        ShadowLooper.idleMainLooper()
        syncManager = RadarSyncManager(context, Radar.apiClient, Radar.logger)
        syncManager.syncStore.clear()
    }

    @After
    fun tearDown() {
        syncManager.syncStore.clear()
        ShadowLooper.idleMainLooper()
    }

    // region Helpers

    private fun makeLocation(lat: Double, lng: Double, accuracy: Float = 5f): Location {
        val location = Location("test")
        location.latitude = lat
        location.longitude = lng
        location.accuracy = accuracy
        location.time = System.currentTimeMillis()
        return location
    }

    private fun makeCircleGeofence(
        id: String, lat: Double, lng: Double, radius: Double,
        dwellThreshold: Double? = null, stopDetection: Boolean? = null
    ): RadarGeofence {
        return RadarGeofence(
            id, "Test Geofence", "test", id, null, null,
            RadarCircleGeometry(RadarCoordinate(lat, lng), radius),
            dwellThreshold, stopDetection
        )
    }

    private fun makePolygonGeofence(
        id: String, coords: Array<RadarCoordinate>,
        center: RadarCoordinate, radius: Double
    ): RadarGeofence {
        return RadarGeofence(
            id, "Test Polygon", "test", id, null, null,
            RadarPolygonGeometry(coords, center, radius),
            null, null
        )
    }

    private fun makeBeacon(id: String, lat: Double, lng: Double): RadarBeacon {
        return RadarBeacon(
            _id = id, description = "Test Beacon", tag = "test", externalId = id,
            uuid = "test-uuid", major = "1", minor = "1",
            location = RadarCoordinate(lat, lng),
            type = RadarBeacon.RadarBeaconType.IBEACON
        )
    }

    private fun makePlace(id: String, lat: Double, lng: Double, geometryRadius: Double? = null): RadarPlace {
        return RadarPlace(
            _id = id, name = "Test Place", categories = arrayOf("test"),
            chain = null, location = RadarCoordinate(lat, lng),
            group = "test", metadata = null, address = null,
            geometryRadius = geometryRadius
        )
    }

    private fun makeEventsOptions(): RadarTrackingOptions {
        return RadarTrackingOptions.RESPONSIVE.copy(
            sync = RadarTrackingOptions.RadarTrackingOptionsSync.EVENTS
        )
    }

    private fun makeUser(
        placeId: String? = null,
        geofenceIds: List<String> = emptyList(),
        beaconIds: List<String> = emptyList()
    ): RadarUser {
        val location = makeLocation(TEST_LAT, TEST_LNG)
        val place = if (placeId != null) makePlace(placeId, TEST_LAT, TEST_LNG) else null
        val geofences = geofenceIds.map {
            makeCircleGeofence(it, TEST_LAT, TEST_LNG, 100.0)
        }.toTypedArray()
        val beacons = beaconIds.map {
            makeBeacon(it, TEST_LAT, TEST_LNG)
        }.toTypedArray()

        return RadarUser(
            _id = "test_user",
            userId = null,
            deviceId = null,
            description = null,
            metadata = null,
            location = location,
            geofences = geofences,
            place = place,
            beacons = beacons,
            stopped = false,
            foreground = false,
            country = null,
            state = null,
            dma = null,
            postalCode = null,
            nearbyPlaceChains = null,
            segments = null,
            topChains = null,
            source = Radar.RadarLocationSource.BACKGROUND_LOCATION,
            trip = null,
            debug = false,
            fraud = null,
            activityType = null,
            altitude = null
        )
    }

    private fun setState(state: RadarSyncState) {
        syncManager.syncStore.write(state)
    }

    // endregion

    // region shouldTrack

    @Test
    fun test_shouldTrack_noSyncedRegion() {
        val location = makeLocation(TEST_LAT, TEST_LNG)
        val options = makeEventsOptions()

        assertTrue(syncManager.shouldTrack(location, options))
    }

    @Test
    fun test_shouldTrack_outsideSyncedRegion() {
        val state = RadarSyncState(
            syncedRegionCenter = RadarCoordinate(TEST_LAT, TEST_LNG),
            syncedRegionRadius = 100.0
        )
        setState(state)

        val location = makeLocation(TEST_LAT_FAR, TEST_LNG)
        val options = makeEventsOptions()

        assertTrue(syncManager.shouldTrack(location, options))
    }

    @Test
    fun test_shouldTrack_geofenceEntry() {
        val geofence = makeCircleGeofence("geofence1", TEST_LAT, TEST_LNG, 100.0)
        val state = RadarSyncState(
            syncedRegionCenter = RadarCoordinate(TEST_LAT, TEST_LNG),
            syncedRegionRadius = 500.0,
            syncedGeofences =  listOf(geofence),
            lastSyncedGeofenceIds = emptyList()
        )
        setState(state)

        val location = makeLocation(TEST_LAT, TEST_LNG)
        val options = makeEventsOptions()

        assertTrue(syncManager.shouldTrack(location, options))
    }

    @Test
    fun test_shouldTrack_geofenceExit() {
        val geofence = makeCircleGeofence("geofence1", TEST_LAT_FAR, TEST_LNG, 50.0)
        val state = RadarSyncState(
            syncedRegionCenter = RadarCoordinate(TEST_LAT, TEST_LNG),
            syncedRegionRadius = 500.0,
            syncedGeofences = listOf(geofence),
            lastSyncedGeofenceIds = listOf("geofence1")
        )
        setState(state)

        val location = makeLocation(TEST_LAT, TEST_LNG)
        val options = makeEventsOptions()

        assertTrue(syncManager.shouldTrack(location, options))
    }

    @Test
    fun test_shouldNotTrack_noStateChange() {
        val geofence = makeCircleGeofence("geofence1", TEST_LAT, TEST_LNG, 100.0)
        val state = RadarSyncState(
            syncedRegionCenter = RadarCoordinate(TEST_LAT, TEST_LNG),
            syncedRegionRadius =  500.0,
            syncedGeofences = listOf(geofence),
            lastSyncedGeofenceIds = listOf("geofence1"),
            geofenceEntryTimestamps = mutableMapOf("geofence1" to System.currentTimeMillis() / 1000.0)
        )
        setState(state)

        val location = makeLocation(TEST_LAT, TEST_LNG)
        val options = makeEventsOptions()

        assertFalse(syncManager.shouldTrack(location, options))
    }

    // endregion

    // region getGeofences

    @Test
    fun test_getGeofences_insideCircle() {
        val geofence = makeCircleGeofence("geofence1", TEST_LAT, TEST_LNG, 100.0)
        setState(RadarSyncState(syncedGeofences = listOf(geofence)))

        val location = makeLocation(TEST_LAT, TEST_LNG)
        val geofences = syncManager.getGeofences(location)

        assertEquals(1, geofences.size)
        assertEquals("geofence1", geofences.first()._id)
    }

    @Test
    fun test_getGeofences_outsideCircle() {
        val geofence = makeCircleGeofence("geofence1", TEST_LAT_FAR, TEST_LNG, 50.0)
        setState(RadarSyncState(syncedGeofences =  listOf(geofence)))

        val location = makeLocation(TEST_LAT, TEST_LNG)
        val geofences = syncManager.getGeofences(location)

        assertEquals(0, geofences.size)
    }

    @Test
    fun test_getGeofences_noNearbyGeofences() {
        val location = makeLocation(TEST_LAT, TEST_LNG)
        val geofences = syncManager.getGeofences(location)

        assertEquals(0, geofences.size)
    }

    // endregion

    // region geofenceStateChanged

    @Test
    fun test_geofenceStateChanged_entry() {
        val geofence = makeCircleGeofence("geofence1", TEST_LAT, TEST_LNG, 100.0)
        setState(RadarSyncState(
            syncedGeofences = listOf(geofence),
            lastSyncedGeofenceIds = emptyList()
        ))

        val location = makeLocation(TEST_LAT, TEST_LNG)
        assertTrue(syncManager.hasGeofenceStateChanged(location))
    }

    @Test
    fun test_geofenceStateChanged_exit() {
        val geofence = makeCircleGeofence("geofence1", TEST_LAT_FAR, TEST_LNG, 50.0)
        setState(RadarSyncState(
            syncedGeofences = listOf(geofence),
            lastSyncedGeofenceIds = listOf("geofence1"),
            geofenceEntryTimestamps = mutableMapOf("geofence1" to System.currentTimeMillis() / 1000.0)
        ))

        val location = makeLocation(TEST_LAT, TEST_LNG)
        assertTrue(syncManager.hasGeofenceStateChanged(location))
    }

    @Test
    fun test_geofenceStateChanged_noChange() {
        val geofence = makeCircleGeofence("geofence1", TEST_LAT, TEST_LNG, 100.0)
        setState(RadarSyncState(
            syncedGeofences = listOf(geofence),
            lastSyncedGeofenceIds = listOf("geofence1"),
            geofenceEntryTimestamps = mutableMapOf("geofence1" to System.currentTimeMillis() / 1000.0)
        ))

        val location = makeLocation(TEST_LAT, TEST_LNG)
        assertFalse(syncManager.hasGeofenceStateChanged(location))
    }


    // endregion

    // region getBeacons

    @Test
    fun test_getBeacons_withinRange() {
        val beacon = makeBeacon("beacon1", TEST_LAT, TEST_LNG)
        setState(RadarSyncState(syncedBeacons = listOf(beacon)))

        val location = makeLocation(TEST_LAT, TEST_LNG)
        val beacons = syncManager.getBeacons(location)

        assertEquals(1, beacons.size)
        assertEquals("beacon1", beacons.first()._id)
    }

    @Test
    fun test_getBeacons_outsideRange() {
        val beacon = makeBeacon("beacon1", TEST_LAT_FAR, TEST_LNG)
        setState(RadarSyncState(syncedBeacons = listOf(beacon)))

        val location = makeLocation(TEST_LAT, TEST_LNG)
        val beacons = syncManager.getBeacons(location)

        assertEquals(0, beacons.size)
    }

    // endregion

    // region beaconStateChanged

    @Test
    fun test_beaconStateChanged_entry() {
        setState(RadarSyncState(
            lastSyncedBeaconIds = emptyList()
        ))

        val rangedBeaconIds = setOf("beacon1")
        assertTrue(syncManager.hasBeaconStateChanged(rangedBeaconIds))
    }

    @Test
    fun test_beaconStateChanged_exit() {
        setState(RadarSyncState(
            lastSyncedBeaconIds = listOf("beacon1")
        ))

        val rangedBeaconIds = emptySet<String>()
        assertTrue(syncManager.hasBeaconStateChanged(rangedBeaconIds))
    }

    @Test
    fun test_beaconStateChanged_noChange() {
        setState(RadarSyncState(
            lastSyncedBeaconIds = listOf("beacon1")
        ))

        val rangedBeaconIds = setOf("beacon1")
        assertFalse(syncManager.hasBeaconStateChanged(rangedBeaconIds))
    }

    @Test
    fun test_saveBeaconState() {
        setState(RadarSyncState(
            lastSyncedBeaconIds = listOf("beacon1")
        ))

        syncManager.saveBeaconState(listOf("beacon2", "beacon3"))

        val state = syncManager.syncStore.read()!!
        assertEquals(listOf("beacon2", "beacon3"), state.lastSyncedBeaconIds)
    }

    // endregion

    //region getPlaces

    @Test
    fun test_getPlaces_withinRadius() {
        RadarState.setStopped(context, true)
        val place = makePlace("place1", TEST_LAT, TEST_LNG)
        setState(RadarSyncState(syncedPlaces = listOf(place)))

        val location = makeLocation(TEST_LAT, TEST_LNG)
        val places = syncManager.getPlaces(location)

        assertEquals(1, places.size)
        assertEquals("place1", places.first()._id)
    }

    @Test
    fun test_getPlaces_outsideRadius() {
        RadarState.setStopped(context, true)
        val place = makePlace("place1", TEST_LAT_FAR, TEST_LNG)
        setState(RadarSyncState(syncedPlaces = listOf(place)))

        val location = makeLocation(TEST_LAT, TEST_LNG)
        val places = syncManager.getPlaces(location)

        assertEquals(0, places.size)
    }

    // endregion

    // region placeStateChanged

    @Test
    fun test_placeStateChanged_entry() {
        RadarState.setStopped(context, true)
        val place = makePlace("place1", TEST_LAT, TEST_LNG)
        setState(RadarSyncState(
            syncedPlaces = listOf(place),
            lastSyncedPlaceIds = emptyList()
        ))

        val location = makeLocation(TEST_LAT, TEST_LNG)
        assertTrue(syncManager.hasPlaceStateChanged(location))
    }

    @Test
    fun test_placeStateChanged_entrySkippedWhenNotStopped() {
        RadarState.setStopped(context, false)
        val place = makePlace("place1", TEST_LAT, TEST_LNG)
        setState(RadarSyncState(
            syncedPlaces = listOf(place),
            lastSyncedPlaceIds = emptyList()
        ))
        val location = makeLocation(TEST_LAT, TEST_LNG)
        assertFalse(syncManager.hasPlaceStateChanged(location))
    }

    @Test
    fun test_placeStateChanged_exit() {
        RadarState.setStopped(context, false)
        val place = makePlace("place1", TEST_LAT_FAR, TEST_LNG)
        setState(RadarSyncState(
            syncedPlaces = listOf(place),
            lastSyncedPlaceIds = listOf("place1")
        ))

        val location = makeLocation(TEST_LAT, TEST_LNG)
        assertTrue(syncManager.hasPlaceStateChanged(location))
    }

    @Test
    fun test_getPlaces_withinGeometryRadius() {
        RadarState.setStopped(context, true)
        val place = makePlace("place1", TEST_LAT_NEARBY, TEST_LNG, geometryRadius = 100.0)
        setState(RadarSyncState(syncedPlaces =  listOf(place)))

        val location = makeLocation(TEST_LAT, TEST_LNG)
        val places = syncManager.getPlaces(location)

        assertEquals(1, places.size)
    }

    @Test
    fun test_getPlaces_outsideGeometryRadiusPlusDetection() {
        RadarState.setStopped(context, true)
        val place = makePlace("place1", TEST_LAT_FAR, TEST_LNG, geometryRadius = 50.0)
        setState(RadarSyncState(syncedPlaces = listOf(place)))

        val location = makeLocation(TEST_LAT, TEST_LNG)
        val places = syncManager.getPlaces(location)

        assertEquals(0, places.size)
    }

    @Test
    fun test_placeStateChanged_exitWithGeometryRadius() {
        RadarState.setStopped(context, false)
        val place = makePlace("place1", TEST_LAT_NEARBY, TEST_LNG, geometryRadius = 20.0)
        setState(RadarSyncState(
            syncedPlaces = listOf(place),
            lastSyncedPlaceIds = listOf("place1")
        ))

        val location = makeLocation(TEST_LAT, TEST_LNG)
        // TEST_LAT_NEARBY is ~50m from TEST_LAT, exit radius = 20 + 50 = 70m
        // User is ~50m away, within exit radius → no exit
        assertFalse(syncManager.hasPlaceStateChanged(location))
    }

    @Test
    fun test_placeStateChanged_exitBeyondBuffer() {
        RadarState.setStopped(context, false)
        val place = makePlace("place1", TEST_LAT_FAR, TEST_LNG, geometryRadius = 50.0)
        setState(RadarSyncState(
            syncedPlaces = listOf(place),
            lastSyncedPlaceIds = listOf("place1")
        ))

        val location = makeLocation(TEST_LAT, TEST_LNG)
        // TEST_LAT_FAR is ~200m from TEST_LAT, exit radius = 50 + 50 = 100m
        // User is ~200m away, outside exit radius → exit
        assertTrue(syncManager.hasPlaceStateChanged(location))
    }

    @Test
    fun test_placeStateChanged_switchBlockedWithinExitRadius() {
        RadarState.setStopped(context, true)
        val place1 = makePlace("place1", TEST_LAT, TEST_LNG, geometryRadius = 100.0)
        val place2 = makePlace("place2", TEST_LAT_NEARBY, TEST_LNG, geometryRadius = 100.0)
        setState(RadarSyncState(
            syncedPlaces = listOf(place1, place2),
            lastSyncedPlaceIds = listOf("place1")
        ))

        val location = makeLocation(TEST_LAT_NEARBY, TEST_LNG)
        // User is at TEST_LAT_NEARBY (~50m from place1)
        // place1 exit radius = 100 + 50 = 150m, user is ~50m away → still within exit radius
        // Even though user is within place2's entry radius, switch should be blocked
        assertFalse(syncManager.hasPlaceStateChanged(location))
    }

    @Test
    fun test_placeStateChanged_rejectedPlaceNotReentered() {
        RadarState.setStopped(context, true)
        val place = makePlace("place1", TEST_LAT, TEST_LNG)
        setState(RadarSyncState(
            syncedPlaces = listOf(place),
            lastSyncedPlaceIds = emptyList()
        ))

        val location = makeLocation(TEST_LAT, TEST_LNG)
        assertTrue(syncManager.hasPlaceStateChanged(location))

        setState(RadarSyncState(
            syncedPlaces = listOf(place),
            lastSyncedPlaceIds = listOf("place1")
        ))

        // Simulate server rejecting the place
        syncManager.reconcileSyncState(makeUser(placeId = null))

        // Reset lastSyncedPlaceIds to empty (as reconcile would have done)
        // Same location — rejected place should be suppressed
        assertFalse(syncManager.hasPlaceStateChanged(location))
    }

    @Test
    fun test_placeStateChanged_rejectionsClearedOnMovement() {
        RadarState.setStopped(context, true)
        val place = makePlace("place1", TEST_LAT, TEST_LNG)
        setState(RadarSyncState(
            syncedPlaces = listOf(place),
            lastSyncedPlaceIds = emptyList()
        ))

        val location = makeLocation(TEST_LAT, TEST_LNG)
        assertTrue(syncManager.hasPlaceStateChanged(location))

        setState(RadarSyncState(
            syncedPlaces = listOf(place),
            lastSyncedPlaceIds = listOf("place1")
        ))

        // Server rejects
        syncManager.reconcileSyncState(makeUser(placeId = null))

        // Move far away — clears rejections (default accuracy is 5m, TEST_LAT_FAR is ~200m away)
        val farLocation = makeLocation(TEST_LAT_FAR, TEST_LNG)
        syncManager.hasPlaceStateChanged(farLocation)

        // Restore state so place1 can be re-entered
        setState(RadarSyncState(
            syncedPlaces = listOf(place),
            lastSyncedPlaceIds = emptyList()
        ))

        // Back at original location — should detect entry again
        val returnLocation = makeLocation(TEST_LAT, TEST_LNG)
        assertTrue(syncManager.hasPlaceStateChanged(returnLocation))
    }

    @Test
    fun test_getPlaces_returnsSingleClosest() {
        RadarState.setStopped(context, true)
        val place1 = makePlace("place1", TEST_LAT, TEST_LNG, geometryRadius = 100.0)
        val place2 = makePlace("place2", TEST_LAT_NEARBY, TEST_LNG, geometryRadius = 100.0)
        setState(RadarSyncState(syncedPlaces = listOf(place1, place2)))

        val location = makeLocation(TEST_LAT, TEST_LNG)
        val places = syncManager.getPlaces(location)

        assertEquals(1, places.size)
        assertEquals("place1", places.first()._id)
    }

    // endregion

    //region isOutsideSyncedRegion

    @Test
    fun test_isOutsideSyncedRegion_nil() {
        val location = makeLocation(TEST_LAT, TEST_LNG)
        assertTrue(syncManager.isOutsideSyncedRegion(location))
    }

    @Test
    fun test_isOutsideSyncedRegion_inside() {
        setState(RadarSyncState(
            syncedRegionCenter = RadarCoordinate(TEST_LAT, TEST_LNG),
            syncedRegionRadius = 100.0
        ))

        val location = makeLocation(TEST_LAT, TEST_LNG)
        assertFalse(syncManager.isOutsideSyncedRegion(location))
    }

    @Test
    fun test_isOutsideSyncedRegion_outside() {
        setState(RadarSyncState(
            syncedRegionCenter = RadarCoordinate(TEST_LAT, TEST_LNG),
            syncedRegionRadius =  100.0
        ))

        val location = makeLocation(TEST_LAT_FAR, TEST_LNG)
        assertTrue(syncManager.isOutsideSyncedRegion(location))
    }

    // endregion

    // region isNearSyncedRegionBoundary

    @Test
    fun test_isNearSyncedRegionBoundary_near() {
        setState(RadarSyncState(
            syncedRegionCenter = RadarCoordinate(TEST_LAT, TEST_LNG),
            syncedRegionRadius = 1000.0
        ))

        val location = makeLocation(TEST_LAT + 0.0081, TEST_LNG)
        assertTrue(syncManager.isNearSyncedRegionBoundary(location))
    }

    @Test
    fun test_isNearSyncedRegionBoundary_notNear() {
        setState(RadarSyncState(
            syncedRegionCenter = RadarCoordinate(TEST_LAT, TEST_LNG),
            syncedRegionRadius = 1000.0
        ))

        val location = makeLocation(TEST_LAT, TEST_LNG)
        assertFalse(syncManager.isNearSyncedRegionBoundary(location))
    }

    // endregion

    //region Multiple geofences

    @Test
    fun test_multipleGeofences_shouldTrackWhenCrossingNearestBoundary() {
        val geofenceA = makeCircleGeofence("geofenceA", TEST_LAT_NEARBY, TEST_LNG, 100.0)
        val geofenceB = makeCircleGeofence("geofenceB", TEST_LAT_FAR, TEST_LNG, 50.0)
        setState(RadarSyncState(
            syncedRegionCenter = RadarCoordinate(TEST_LAT, TEST_LNG),
            syncedRegionRadius = 500.0,
            syncedGeofences = listOf(geofenceA, geofenceB),
            lastSyncedGeofenceIds = emptyList()
        ))

        val location = makeLocation(TEST_LAT_NEARBY, TEST_LNG)
        val options = makeEventsOptions()

        assertTrue(syncManager.shouldTrack(location, options))
    }

    @Test
    fun test_multipleGeofences_detectsCorrectGeofences() {
        val geofenceA = makeCircleGeofence("geofenceA", TEST_LAT, TEST_LNG, 100.0)
        val geofenceB = makeCircleGeofence("geofenceB", TEST_LAT_NEARBY, TEST_LNG, 100.0)
        val geofenceC = makeCircleGeofence("geofenceC", TEST_LAT_FAR, TEST_LNG, 50.0)
        setState(RadarSyncState(syncedGeofences = listOf(geofenceA, geofenceB, geofenceC)))

        val location = makeLocation(TEST_LAT, TEST_LNG)
        val geofences = syncManager.getGeofences(location)
        assertEquals(2, geofences.size)

        val ids = geofences.map { it._id }.toSet()
        assertTrue(ids.contains("geofenceA"))
        assertTrue(ids.contains("geofenceB"))
        assertFalse(ids.contains("geofenceC"))
    }

    // endregion

    // region Polygon geofences

    @Test
    fun test_getGeofences_insidePolygon() {
        val coords = arrayOf(
            RadarCoordinate(TEST_LAT + 0.001, TEST_LNG - 0.001),
            RadarCoordinate(TEST_LAT + 0.001, TEST_LNG + 0.001),
            RadarCoordinate(TEST_LAT - 0.001, TEST_LNG + 0.001),
            RadarCoordinate(TEST_LAT - 0.001, TEST_LNG - 0.001),
            RadarCoordinate(TEST_LAT + 0.001, TEST_LNG - 0.001)
        )
        val center = RadarCoordinate(TEST_LAT, TEST_LNG)
        val geofence = makePolygonGeofence("poly1", coords, center, 150.0)
        setState(RadarSyncState(syncedGeofences = listOf(geofence)))

        val location = makeLocation(TEST_LAT, TEST_LNG)
        val geofences = syncManager.getGeofences(location)

        assertEquals(1, geofences.size)
        assertEquals("poly1", geofences.first()._id)
    }

    @Test
    fun test_getGeofences_outsidePolygon() {
        val coords = arrayOf(
            RadarCoordinate(TEST_LAT_FAR + 0.001, TEST_LNG - 0.001),
            RadarCoordinate(TEST_LAT_FAR + 0.001, TEST_LNG + 0.001),
            RadarCoordinate(TEST_LAT_FAR - 0.001, TEST_LNG + 0.001),
            RadarCoordinate(TEST_LAT_FAR - 0.001, TEST_LNG - 0.001),
            RadarCoordinate(TEST_LAT_FAR + 0.001, TEST_LNG - 0.001)
        )
        val center = RadarCoordinate(TEST_LAT_FAR, TEST_LNG)
        val geofence = makePolygonGeofence("poly1", coords, center, 150.0)
        setState(RadarSyncState(syncedGeofences = listOf(geofence)))

        val location = makeLocation(TEST_LAT, TEST_LNG)
        val geofences = syncManager.getGeofences(location)

        assertEquals(0, geofences.size)
    }

    @Test
    fun test_getGeofences_mixedCircleAndPolygon() {
        val circleGeofence = makeCircleGeofence("circle1", TEST_LAT, TEST_LNG, 100.0)
        val coords = arrayOf(
            RadarCoordinate(TEST_LAT + 0.001, TEST_LNG - 0.001),
            RadarCoordinate(TEST_LAT + 0.001, TEST_LNG + 0.001),
            RadarCoordinate(TEST_LAT - 0.001, TEST_LNG + 0.001),
            RadarCoordinate(TEST_LAT - 0.001, TEST_LNG - 0.001),
            RadarCoordinate(TEST_LAT + 0.001, TEST_LNG - 0.001)
        )
        val center = RadarCoordinate(TEST_LAT, TEST_LNG)
        val polyGeofence = makePolygonGeofence("poly1", coords, center, 150.0)
        setState(RadarSyncState(syncedGeofences = listOf(circleGeofence, polyGeofence)))

        val location = makeLocation(TEST_LAT, TEST_LNG)
        val geofences = syncManager.getGeofences(location)
        assertEquals(2, geofences.size)

        val ids = geofences.map { it._id}.toSet()
        assertTrue(ids.contains("circle1"))
        assertTrue(ids.contains("poly1"))
    }

    // endregion

    // region Stop detection

    @Test
    fun test_geofenceEntry_stopDetectionBlocks() {
        RadarState.setStopped(context, false)
        val geofence = makeCircleGeofence("geofence1", TEST_LAT, TEST_LNG, 100.0, stopDetection = true)
        setState(RadarSyncState(
            syncedGeofences = listOf(geofence),
            lastSyncedGeofenceIds = emptyList()
        ))

        val location = makeLocation(TEST_LAT, TEST_LNG)
        assertFalse(syncManager.hasGeofenceStateChanged(location))
    }

    @Test
    fun test_geofenceEntry_stopDetectionAllows() {
        RadarState.setStopped(context, true)
        val geofence = makeCircleGeofence("geofence1", TEST_LAT, TEST_LNG, 100.0, stopDetection = true)
        setState(RadarSyncState(
            syncedGeofences = listOf(geofence),
            lastSyncedGeofenceIds = emptyList()
        ))
        val location = makeLocation(TEST_LAT, TEST_LNG)
        assertTrue(syncManager.hasGeofenceStateChanged(location))
    }

    // endregion

    // region Dwell

    @Test
    fun test_geofenceDwell_thresholdReached() {
        val geofence = makeCircleGeofence("geofence1", TEST_LAT, TEST_LNG, 100.0)
        setState(RadarSyncState(
            syncedGeofences = listOf(geofence),
            lastSyncedGeofenceIds = listOf("geofence1"),
            geofenceEntryTimestamps = mutableMapOf("geofence1" to (System.currentTimeMillis() / 1000.0 - 600))
        ))
        RadarSettings.setSdkConfiguration(context, RadarSdkConfiguration.fromJson(
            JSONObject().put("defaultGeofenceDwellThreshold", 5)
        ))
        val location = makeLocation(TEST_LAT, TEST_LNG)
        assertTrue(syncManager.hasGeofenceStateChanged(location))
    }

    @Test
    fun test_geofenceDwell_thresholdNotReached() {
        val geofence = makeCircleGeofence("geofence1", TEST_LAT, TEST_LNG, 100.0)
        setState(RadarSyncState(
            syncedGeofences = listOf(geofence),
            lastSyncedGeofenceIds = listOf("geofence1"),
            geofenceEntryTimestamps = mutableMapOf("geofence1" to (System.currentTimeMillis() / 1000.0 - 60))
        ))

        RadarSettings.setSdkConfiguration(context, RadarSdkConfiguration.fromJson(
            JSONObject().put("defaultGeofenceDwellThreshold", 5)
        ))

        val location = makeLocation(TEST_LAT, TEST_LNG)
        assertFalse(syncManager.hasGeofenceStateChanged(location))
    }

    @Test
    fun test_geofenceDwell_perGeofenceOverride() {
        val geofence = makeCircleGeofence("geofence1", TEST_LAT, TEST_LNG, 100.0, dwellThreshold = 2.0)
        setState(RadarSyncState(
            syncedGeofences = listOf(geofence),
            lastSyncedGeofenceIds = listOf("geofence1"),
            geofenceEntryTimestamps = mutableMapOf("geofence1" to (System.currentTimeMillis() / 1000.0 - 180))
        ))

        val location = makeLocation(TEST_LAT, TEST_LNG)
        assertTrue(syncManager.hasGeofenceStateChanged(location))
    }

    @Test
    fun test_geofenceDwell_alreadyFired() {
        val geofence = makeCircleGeofence("geofence1", TEST_LAT, TEST_LNG, 100.0)
        setState(RadarSyncState(
            syncedGeofences = listOf(geofence),
            lastSyncedGeofenceIds = listOf("geofence1"),
            geofenceEntryTimestamps = mutableMapOf("geofence1" to (System.currentTimeMillis() / 1000.0 -600)),
            dwellEventsFired = mutableListOf("geofence1")
        ))

        RadarSettings.setSdkConfiguration(context, RadarSdkConfiguration.fromJson(
            JSONObject().put("defaultGeofenceDwellThreshold", 5)
        ))

        val location = makeLocation(TEST_LAT, TEST_LNG)
        assertFalse(syncManager.hasGeofenceStateChanged(location))
    }

    // end region

}