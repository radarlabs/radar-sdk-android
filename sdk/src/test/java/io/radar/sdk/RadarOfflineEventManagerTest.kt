package io.radar.sdk

import android.content.Context
import android.location.Location
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.radar.sdk.model.RadarCircleGeometry
import io.radar.sdk.model.RadarCoordinate
import io.radar.sdk.model.RadarGeofence
import io.radar.sdk.model.RadarSdkConfiguration
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class RadarOfflineEventManagerTest {

    companion object {
        private const val TEST_LAT = 40.78382
        private const val TEST_LNG = -73.97536
        private const val TEST_LAT_FAR = 40.78562
    }

    private lateinit var context: Context
    private lateinit var syncManager: RadarSyncManager
    private lateinit var offlineEventManager: RadarOfflineEventManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        Radar.initialize(context, "prj_test_pk_0000000000000000000000000000000000000000")
        ShadowLooper.idleMainLooper()
        syncManager = RadarSyncManager(context, Radar.apiClient, Radar.logger)
        syncManager.syncStore.clear()
        offlineEventManager = RadarOfflineEventManager(context, syncManager, Radar.logger)
        RadarSettings.setSdkConfiguration(context, null)
        RadarState.setLastUser(context, null)
    }

    @After
    fun tearDown() {
        syncManager.syncStore.clear()
        RadarSettings.setSdkConfiguration(context, null)
        RadarState.setLastUser(context, null)
        offlineEventManager.reset()
        ShadowLooper.idleMainLooper()
    }

    // region Helpers

    private fun makeLocation(lat: Double, lng: Double, accuracy: Float = 5f): Location {
        val loc = Location("test")
        loc.latitude = lat
        loc.longitude = lng
        loc.accuracy = accuracy
        loc.time = System.currentTimeMillis()
        return loc
    }

    private fun makeCircleGeofence(
        id: String, lat: Double, lng: Double, radius: Double, tag: String = "test"
    ): RadarGeofence {
        return RadarGeofence(
            id, "Test Geofence", tag, id, null, null,
            RadarCircleGeometry(RadarCoordinate(lat, lng), radius),
            null, null
        )
    }

    private fun setState(state: RadarSyncState) {
        syncManager.syncStore.write(state)
    }

    private fun makeRemoteTrackingOptionsJson(
        type: String,
        preset: String,
        geofenceTags: List<String>? = null
    ): JSONObject {
        val trackingOptions = when (preset) {
            "continuous" -> RadarTrackingOptions.CONTINUOUS
            "responsive" -> RadarTrackingOptions.RESPONSIVE
            else -> RadarTrackingOptions.EFFICIENT
        }
        return JSONObject().apply {
            put("type", type)
            put("trackingOptions", trackingOptions.toJson())
            geofenceTags?.let { put("geofenceTags", JSONArray(it)) }
        }
    }

    private fun makeSdkConfig(
        offlineEventGenerationEnabled: Boolean = false,
        useOfflineRTOUpdates: Boolean = false,
        remoteTrackingOptions: List<JSONObject>? = null
    ): RadarSdkConfiguration {
        val json = JSONObject().apply {
            put("offlineEventGenerationEnabled", offlineEventGenerationEnabled)
            put("useOfflineRTOUpdates", useOfflineRTOUpdates)
            if (remoteTrackingOptions != null) {
                put("remoteTrackingOptions", JSONArray().apply {
                    remoteTrackingOptions.forEach { put(it) }
                })
            }
        }
        return RadarSdkConfiguration.fromJson(json)
    }

    // endregion

    // region handleTrackFailure gating

    @Test
    fun test_handleTrackFailure_doesNothing_whenDisabled() {
        RadarSettings.setSdkConfiguration(context, makeSdkConfig(offlineEventGenerationEnabled = false))

        val geofence = makeCircleGeofence("geo1", TEST_LAT, TEST_LNG, 100.0)
        setState(RadarSyncState(
            syncedGeofences = listOf(geofence),
            lastSyncedGeofenceIds = emptyList()
        ))

        val location = makeLocation(TEST_LAT, TEST_LNG)
        offlineEventManager.handleTrackFailure(location)

        // offlineGeofenceIds should still be empty — verify by calling generateEvents.
        // If handleTrackFailure had run, offlineGeofenceIds would contain "geo1" and
        // the next generateEvents call would detect no entry. Since it didn't run,
        // baseline IDs (empty) are used, so "geo1" is detected as a fresh entry.
        var eventCount = 0
        offlineEventManager.generateEvents(location) { events, _, _ ->
            eventCount = events.size
        }
        assertEquals(1, eventCount)
    }

    @Test
    fun test_handleTrackFailure_runs_whenEnabled() {
        RadarSettings.setSdkConfiguration(context, makeSdkConfig(offlineEventGenerationEnabled = true))

        val geofence = makeCircleGeofence("geo1", TEST_LAT, TEST_LNG, 100.0)
        setState(RadarSyncState(
            syncedGeofences = listOf(geofence),
            lastSyncedGeofenceIds = emptyList()
        ))

        val location = makeLocation(TEST_LAT, TEST_LNG)
        offlineEventManager.handleTrackFailure(location)

        // offlineGeofenceIds now contains "geo1". Next generateEvents call detects no change.
        var eventCount = -1
        offlineEventManager.generateEvents(location) { events, _, _ ->
            eventCount = events.size
        }
        assertEquals(0, eventCount)
    }

    // endregion

    // region generateEvents

    @Test
    fun test_generateEvents_detectsEntry() {
        val geofence = makeCircleGeofence("geo1", TEST_LAT, TEST_LNG, 100.0)
        setState(RadarSyncState(
            syncedGeofences = listOf(geofence),
            lastSyncedGeofenceIds = emptyList()
        ))

        val location = makeLocation(TEST_LAT, TEST_LNG)
        var events = emptyList<io.radar.sdk.model.RadarEvent>()
        offlineEventManager.generateEvents(location) { e, _, _ ->
            events = e
        }

        assertEquals(1, events.size)
        assertEquals(io.radar.sdk.model.RadarEvent.RadarEventType.USER_ENTERED_GEOFENCE, events[0].type)
        assertEquals("geo1", events[0].geofence?._id)
    }

    @Test
    fun test_generateEvents_detectsExit() {
        val geofence = makeCircleGeofence("geo1", TEST_LAT_FAR, TEST_LNG, 50.0)
        setState(RadarSyncState(
            syncedGeofences = listOf(geofence),
            lastSyncedGeofenceIds =  listOf("geo1")
        ))

        val location = makeLocation(TEST_LAT, TEST_LNG)
        var events = emptyList<io.radar.sdk.model.RadarEvent>()
        offlineEventManager.generateEvents(location) {e, _, _ ->
            events = e
        }
        assertEquals(1, events.size)
        assertEquals(io.radar.sdk.model.RadarEvent.RadarEventType.USER_EXITED_GEOFENCE, events[0].type)
    }

    @Test
    fun test_generateEvents_noChange() {
        val geofence = makeCircleGeofence("geo1", TEST_LAT, TEST_LNG, 100.0)
        setState(RadarSyncState(
            syncedGeofences = listOf(geofence),
            lastSyncedGeofenceIds = listOf("geo1")
        ))

        val location = makeLocation(TEST_LAT, TEST_LNG)
        var events = listOf<io.radar.sdk.model.RadarEvent>(/* sentinel */)
        offlineEventManager.generateEvents(location) { e, _, _ ->
            events = e
        }
        assertTrue(events.isEmpty())
    }

    @Test
    fun test_generateEvents_eventMetadata() {
        val geofence = makeCircleGeofence("geo1", TEST_LAT, TEST_LNG, 100.0)
        setState(RadarSyncState(
            syncedGeofences = listOf(geofence),
            lastSyncedGeofenceIds = emptyList()
        ))

        val location = makeLocation(TEST_LAT, TEST_LNG)
        var events = emptyList<io.radar.sdk.model.RadarEvent>()
        offlineEventManager.generateEvents(location) { e, _, _ ->
            events = e
        }
        assertEquals(1, events.size)
        val event = events[0]
        assertFalse(event.replayed)
        assertEquals(true, event.metadata?.optBoolean("offline"))
    }

    // endregion

    // region reset

    @Test
    fun test_reset_clearsOfflineGeofenceIds() {
        val geofence = makeCircleGeofence("geo1", TEST_LAT, TEST_LNG, 100.0)
        setState(RadarSyncState(
            syncedGeofences = listOf(geofence),
            lastSyncedGeofenceIds = emptyList()
        ))

        val location = makeLocation(TEST_LAT, TEST_LNG)

        // 1st call: entry detected + offlineGeofenceIds populated with "geo1"
        offlineEventManager.generateEvents(location) { _, _, _ -> }

        // 2nd call: no change expected
        var noChange = listOf<io.radar.sdk.model.RadarEvent>(/* sentinel */)
        offlineEventManager.generateEvents(location) { e, _, _ -> noChange = e }
        assertTrue(noChange.isEmpty())

        // Reset — offlineGeofenceIds cleared; baseline IDs (still empty) used next call.
        offlineEventManager.reset()

        var postReset = emptyList<io.radar.sdk.model.RadarEvent>()
        offlineEventManager.generateEvents(location) { e, _, _ -> postReset = e }
        assertEquals(1, postReset.size)
        assertEquals(io.radar.sdk.model.RadarEvent.RadarEventType.USER_ENTERED_GEOFENCE, postReset[0].type)
    }

    // endregion

    // region updateTrackingOptions (by tags)

    @Test
    fun test_updateTrackingOptions_returnsInGeofence_whenTagsMatch() {
        RadarSettings.setSdkConfiguration(context, makeSdkConfig(
            useOfflineRTOUpdates = true,
            remoteTrackingOptions = listOf(
                makeRemoteTrackingOptionsJson("default", "responsive"),
                makeRemoteTrackingOptionsJson("inGeofence", "continuous", listOf("neighborhood"))
            )
        ))

        val result = offlineEventManager.updateTrackingOptions(listOf("neighborhood"))
        assertNotNull(result)
        assertEquals(RadarTrackingOptions.CONTINUOUS, result)
    }

    @Test
    fun test_updateTrackingOptions_returnsDefault_whenTagsDoNotMatch() {
        RadarSettings.setSdkConfiguration(context, makeSdkConfig(
            useOfflineRTOUpdates = true,
            remoteTrackingOptions = listOf(
                makeRemoteTrackingOptionsJson("default", "responsive"),
                makeRemoteTrackingOptionsJson("inGeofence", "continuous", listOf("neighborhood"))
            )
        ))

        val result = offlineEventManager.updateTrackingOptions(listOf("some-other-tag"))
        assertEquals(RadarTrackingOptions.RESPONSIVE, result)
    }

    @Test
    fun test_updateTrackingOptions_returnsNull_whenNoRemoteOptions() {
        RadarSettings.setSdkConfiguration(context, makeSdkConfig(useOfflineRTOUpdates = false))
        val result = offlineEventManager.updateTrackingOptions(listOf("neighborhood"))
        assertNull(result)
    }

    @Test
    fun test_updateTrackingOptions_returnsDefault_whenTagsEmpty() {
        RadarSettings.setSdkConfiguration(context, makeSdkConfig(
            useOfflineRTOUpdates = true,
            remoteTrackingOptions = listOf(
                makeRemoteTrackingOptionsJson("default", "responsive"),
                makeRemoteTrackingOptionsJson("inGeofence", "continuous", listOf("neighborhood"))
            )
        ))

        val result = offlineEventManager.updateTrackingOptions(emptyList<String>())
        assertEquals(RadarTrackingOptions.RESPONSIVE, result)
    }

    // endregion

    // region updateTrackingOptions (by location)

    @Test
    fun test_updateTrackingOptions_byLocation_insideTaggedGeofence() {
        val geofence = makeCircleGeofence("geo1", TEST_LAT, TEST_LNG, 100.0, tag = "neighborhood")
        setState(RadarSyncState(syncedGeofences = listOf(geofence)))

        RadarSettings.setSdkConfiguration(context, makeSdkConfig(
            useOfflineRTOUpdates = true,
            remoteTrackingOptions = listOf(
                makeRemoteTrackingOptionsJson("default", "responsive"),
                makeRemoteTrackingOptionsJson("inGeofence", "continuous", listOf("neighborhood"))
            )
        ))

        val location = makeLocation(TEST_LAT, TEST_LNG)
        val result = offlineEventManager.updateTrackingOptions(location)
        assertEquals(RadarTrackingOptions.CONTINUOUS, result)
    }

    @Test
    fun test_updateTrackingOptions_byLocation_outsideGeofence_returnsDefault() {
        val geofence = makeCircleGeofence("geo1", TEST_LAT, TEST_LNG, 100.0, tag = "neighborhood")
        setState(RadarSyncState(syncedGeofences = listOf(geofence)))

        RadarSettings.setSdkConfiguration(context, makeSdkConfig(
            useOfflineRTOUpdates = true,
            remoteTrackingOptions = listOf(
                makeRemoteTrackingOptionsJson("default", "responsive"),
                makeRemoteTrackingOptionsJson("inGeofence", "continuous", listOf("neighborhood"))
            )
        ))

        val location = makeLocation(TEST_LAT_FAR, TEST_LNG)
        val result = offlineEventManager.updateTrackingOptions(location)
        assertEquals(RadarTrackingOptions.RESPONSIVE, result)
    }

    // endregion
}