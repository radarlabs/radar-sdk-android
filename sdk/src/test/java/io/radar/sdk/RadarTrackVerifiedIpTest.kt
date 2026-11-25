package io.radar.sdk

import android.content.Context
import android.location.Location
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.radar.sdk.Radar.RadarStatus
import io.radar.sdk.helpers.RadarApiHelperMock
import io.radar.sdk.model.RadarConfig
import io.radar.sdk.model.RadarEvent
import io.radar.sdk.model.RadarGeofence
import io.radar.sdk.model.RadarUser
import io.radar.sdk.model.RadarVerificationLevel
import io.radar.sdk.model.RadarVerifiedLocationToken
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

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class RadarTrackVerifiedIpTest {

    /** Records the request body that the SDK seals, so tests can read the plaintext params. */
    class RecordingFraudHandle {
        val bodies = mutableListOf<String>()

        fun seal(options: Map<String, Any?>): Map<String, Any?> {
            bodies += options["body"] as String
            return mapOf("payload" to """{"encv":1}""")
        }
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val apiHelper = RadarApiHelperMock()
    private val receiver = RecordingReceiver()
    private val verifiedReceiver = RecordingVerifiedReceiver()
    private lateinit var originalOptions: RadarTrackingOptions

    @Before
    fun setUp() {
        Radar.logger = RadarLogger(context)
        Radar.apiClient = RadarApiClient(context, Radar.logger)
        Radar.apiClient.apiHelper = apiHelper

        Radar.initialize(
            context,
            "prj_test_pk_0000000000000000000000000000000000000000"
        )

        Radar.setReceiver(receiver)
        Radar.setVerifiedReceiver(verifiedReceiver)
        originalOptions = Radar.getTrackingOptions()

        RadarSettings.setId(context, "existing-user")
        RadarSettings.setUserDebug(context, true)
        RadarState.setLastUser(
            context,
            RadarUser.fromJson(JSONObject().put("_id", "existing-user"))
        )
        RadarState.setCanExit(context, true)
        RadarState.setGeofenceIds(context, setOf("existing-geofence"))
        RadarState.setPlaceId(context, "existing-place")
        RadarState.setRegionIds(context, setOf("existing-region"))
        RadarState.setBeaconIds(context, setOf("existing-beacon"))
        RadarState.setAltitudeAdjustments(
            context,
            JSONArray().put(JSONObject().put("offset", 123))
        )
        RadarState.setLastFailedStoppedLocation(
            context,
            Location("test").apply {
                latitude = 1.0
                longitude = 2.0
            }
        )
        RadarSettings.updateLastTrackedTime(context)
        // An IP check that wrote the last tracked time would now write a later value.
        Thread.sleep(5)
    }

    @After
    fun tearDown() {
        Radar.setReceiver(null)
        Radar.setVerifiedReceiver(null)
        RadarSettings.setTrackingOptions(context, originalOptions)
    }

    @Test
    fun ipRequestSendsNoLocationFields() {
        val handle = RecordingFraudHandle()
        track(response(), location = null, handle = handle)

        val body = JSONObject(handle.bodies.single())
        listOf(
            "latitude", "longitude", "accuracy", "speed", "course", "altitude",
            "verticalAccuracy", "speedAccuracy", "courseAccuracy", "updatedAtMsDiff",
            "locationMs", "nearbyGeofences"
        ).forEach { field ->
            assertFalse("IP request must not send $field", body.has(field))
        }
        assertEquals("ip", body.getString("verificationLevel"))
        assertTrue(body.getBoolean("verified"))
        assertEquals("ip reason", body.getString("reason"))
    }

    @Test
    fun preciseRequestKeepsLocationFieldsAndNoLevel() {
        val handle = RecordingFraudHandle()
        track(response(), location = testLocation(), handle = handle)

        val body = JSONObject(handle.bodies.single())
        assertEquals(40.0, body.getDouble("latitude"), 0.0)
        assertEquals(-73.0, body.getDouble("longitude"), 0.0)
        assertTrue(body.has("accuracy"))
        assertFalse(body.has("verificationLevel"))
    }

    @Test
    fun ipJwsResponseDeliversTokenAndEventsWithoutChangingState() {
        val before = stateSnapshot()
        val result = track(response(), location = null)

        assertEquals(RadarStatus.SUCCESS, result.status)
        val token = requireNotNull(result.token)
        assertEquals(RadarVerificationLevel.IP, token.verificationLevel)
        assertEquals(token, verifiedReceiver.tokens.single())
        assertNotNull(result.user)
        assertEquals(1, receiver.eventBatches)
        assertEquals(0, receiver.locations)
        assertNull(result.nearbyGeofences)
        assertTrue(receiver.errors.isEmpty())
        assertEquals(before, stateSnapshot())
    }

    @Test
    fun ipReducedResponsesUseTheIpLevelWhenTheServerOmitsIt() {
        listOf(
            JSONObject().put("format", "id").put("_id", "evaluation-id"),
            response("jwe")
        ).forEach { obj ->
            val result = track(obj, location = null)

            assertEquals(RadarStatus.SUCCESS, result.status)
            assertEquals(RadarVerificationLevel.IP, result.token?.verificationLevel)
        }
    }

    @Test
    fun serverLevelWinsOverTheRequestedLevel() {
        val result = track(response().put("verificationLevel", "precise"), location = null)

        assertEquals(RadarVerificationLevel.PRECISE, result.token?.verificationLevel)
    }

    @Test
    fun ipFailureDoesNotReplayOrStoreLocation() {
        val options = RadarTrackingOptions.CONTINUOUS.copy(
            replay = RadarTrackingOptions.RadarTrackingOptionsReplay.ALL
        )
        RadarSettings.setTrackingOptions(context, options)
        RadarState.setLastFailedStoppedLocation(context, null)
        val replaysBefore = replayCount()

        val result = track(response(), location = null, status = RadarStatus.ERROR_NETWORK)

        assertEquals(RadarStatus.ERROR_NETWORK, result.status)
        assertEquals(replaysBefore, replayCount())
        assertNull(RadarState.getLastFailedStoppedLocation(context))
        assertEquals(listOf(RadarStatus.ERROR_NETWORK), receiver.errors)
    }

    @Test
    fun ipJwsResponseWithoutUserLocationStillDeliversTokenAndEvents() {
        val before = stateSnapshot()
        val obj = response().apply { getJSONObject("user").remove("location") }

        val result = track(obj, location = null)

        assertEquals(RadarStatus.SUCCESS, result.status)
        assertNull(result.user)
        assertEquals(1, receiver.eventBatches)
        assertEquals(RadarVerificationLevel.IP, verifiedReceiver.tokens.single().verificationLevel)
        assertEquals(before, stateSnapshot())
    }

    @Test
    fun ipJwsResponseWithNullUserLocationLeavesTheUserOut() {
        val obj = response().apply { getJSONObject("user").put("location", JSONObject.NULL) }

        val result = track(obj, location = null)

        assertEquals(RadarStatus.SUCCESS, result.status)
        assertNull(result.user)
        assertNull(verifiedReceiver.tokens.single().user)
    }

    @Test
    fun ipRequestIsNotBatched() {
        RadarSettings.setTrackingOptions(context, Radar.getTrackingOptions().copy(batchSize = 10))
        val handle = RecordingFraudHandle()

        val result = track(response(), location = null, handle = handle)

        assertEquals(RadarStatus.SUCCESS, result.status)
        assertEquals("FOREGROUND_LOCATION", JSONObject(handle.bodies.single()).getString("source"))
        assertNotNull(result.token)
    }

    @Test
    fun ipJwsResponseWithoutEventsFails() {
        val before = stateSnapshot()
        val result = track(response().apply { remove("events") }, location = null)

        assertEquals(RadarStatus.ERROR_SERVER, result.status)
        assertTrue(verifiedReceiver.tokens.isEmpty())
        assertEquals(before, stateSnapshot())
    }

    private fun replayCount(): Int {
        val field = Radar::class.java.getDeclaredField("replayBuffer").apply { isAccessible = true }
        return (field.get(Radar) as io.radar.sdk.util.RadarReplayBuffer).getSize()
    }

    private fun testLocation() = Location("test").apply {
        latitude = 40.0
        longitude = -73.0
        accuracy = 10f
    }

    private fun response(format: String? = null): JSONObject = JSONObject().apply {
        put("meta", JSONObject())
        put("_id", "evaluation-id")
        put("token", "opaque-token")
        put("expiresAt", "2030-01-01T00:00:00.000Z")
        put("expiresIn", 90)
        put("passed", true)
        put(
            "user",
            JSONObject()
                .put("_id", "response-user")
                .put(
                    "location",
                    JSONObject().put("type", "Point").put("coordinates", JSONArray().put(-73.0).put(40.0))
                )
                .put("geofences", JSONArray().put(JSONObject().put("_id", "response-geofence")))
                .put("altitudeAdjustments", JSONArray().put(JSONObject().put("offset", 999)))
        )
        put(
            "events",
            JSONArray().put(
                JSONObject()
                    .put("_id", "event-id")
                    .put("type", "user.failed_fraud")
                    .put("createdAt", "2030-01-01T00:00:00.000Z")
            )
        )
        put("failureReasons", JSONArray())
        put("nearbyGeofences", JSONArray())
        if (format != null) {
            put("format", format)
        }
    }

    private fun stateSnapshot(): List<Any?> = listOf(
        RadarSettings.getId(context),
        RadarSettings.getUserDebug(context),
        RadarSettings.getTrip(context)?.toJson()?.toString(),
        RadarState.getLastUser(context)?.toJson()?.toString(),
        RadarState.getCanExit(context),
        RadarState.getGeofenceIds(context)?.toSet(),
        RadarState.getPlaceId(context),
        RadarState.getRegionIds(context)?.toSet(),
        RadarState.getBeaconIds(context)?.toSet(),
        RadarState.getAltitudeAdjustments(context)?.toString(),
        RadarState.getLastFailedStoppedLocation(context)?.let { "${it.latitude},${it.longitude}" },
        RadarSettings.getLastTrackedTime(context)
    )

    private fun track(
        obj: JSONObject,
        location: Location?,
        status: RadarStatus = RadarStatus.SUCCESS,
        handle: RecordingFraudHandle = RecordingFraudHandle()
    ): Result {
        receiver.locations = 0
        receiver.eventBatches = 0
        receiver.errors.clear()
        verifiedReceiver.tokens.clear()
        apiHelper.clearCapturedParams()

        apiHelper.mockStatus = status
        apiHelper.mockResponse = obj

        var result: Result? = null
        var callbackCount = 0

        val callback = object : RadarApiClient.RadarTrackApiCallback {
            override fun onComplete(
                status: RadarStatus,
                res: JSONObject?,
                events: Array<RadarEvent>?,
                user: RadarUser?,
                nearbyGeofences: Array<RadarGeofence>?,
                config: RadarConfig?,
                token: RadarVerifiedLocationToken?
            ) {
                callbackCount++
                result = Result(status, res, events, user, nearbyGeofences, config, token)
            }
        }

        val payload = RadarPreparedFraudPayload(handle)
        if (location == null) {
            Radar.apiClient.trackIp(
                stopped = false,
                foreground = true,
                reason = "ip reason",
                preparedFraudPayload = payload,
                callback = callback
            )
        } else {
            Radar.apiClient.track(
                location = location,
                stopped = false,
                foreground = true,
                source = Radar.RadarLocationSource.FOREGROUND_LOCATION,
                replayed = false,
                beacons = null,
                verified = true,
                reason = "ip reason",
                preparedFraudPayload = payload,
                callback = callback
            )
        }

        assertEquals("Callback must run exactly once", 1, callbackCount)
        assertNotNull(result)
        return requireNotNull(result)
    }

    private data class Result(
        val status: RadarStatus,
        val response: JSONObject?,
        val events: Array<RadarEvent>?,
        val user: RadarUser?,
        val nearbyGeofences: Array<RadarGeofence>?,
        val config: RadarConfig?,
        val token: RadarVerifiedLocationToken?
    )

    private class RecordingVerifiedReceiver : RadarVerifiedReceiver() {
        val tokens = mutableListOf<RadarVerifiedLocationToken>()

        override fun onTokenUpdated(context: Context, token: RadarVerifiedLocationToken) {
            tokens += token
        }
    }

    private class RecordingReceiver : RadarReceiver() {
        var locations = 0
        var eventBatches = 0
        val errors = mutableListOf<RadarStatus>()

        override fun onEventsReceived(context: Context, events: Array<RadarEvent>, user: RadarUser?) {
            eventBatches++
        }

        override fun onLocationUpdated(context: Context, location: Location, user: RadarUser) {
            locations++
        }

        override fun onClientLocationUpdated(
            context: Context,
            location: Location,
            stopped: Boolean,
            source: Radar.RadarLocationSource
        ) {}

        override fun onError(context: Context, status: RadarStatus) {
            errors += status
        }

        override fun onLog(context: Context, message: String) {}
    }
}
