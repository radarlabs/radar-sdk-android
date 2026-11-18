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
import io.radar.sdk.model.RadarResponseFormat
import io.radar.sdk.model.RadarUser
import io.radar.sdk.model.RadarVerifiedLocationToken
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class RadarTrackResponseFormatTest {

    class StubFraudHandle {
        fun seal(options: Map<String, Any?>): Map<String, Any?> = mapOf("payload" to """{"encv":1}""")
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val apiHelper = RadarApiHelperMock()
    private val receiver = RecordingReceiver()
    private val verifiedReceiver = RecordingVerifiedReceiver()

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
    }

    @After
    fun tearDown() {
        Radar.setReceiver(null)
        Radar.setVerifiedReceiver(null)
    }

    @Test
    fun reducedVerifiedResponsesDeliverOnlyTokenAndConfig() {
        val cases = listOf(
            response("jwe"),
            response("jwe").put("passed", false),
            response("id")
        )

        cases.forEach { obj ->
            val before = stateSnapshot()
            val result = track(obj)
            val expectedFormat = RadarResponseFormat.fromJson(obj.opt("format"))

            assertEquals(RadarStatus.SUCCESS, result.status)
            assertEquals(expectedFormat, result.token?.format)
            assertNull(result.events)
            assertNull(result.user)
            assertNull(result.nearbyGeofences)
            assertNotNull(result.config)
            assertEquals(
                17,
                result.config!!.meta.remoteTrackingOptions!!.desiredMovingUpdateInterval
            )

            val token = requireNotNull(result.token)
            assertSame(token.toJson(), result.response)
            assertSame(token, verifiedReceiver.tokens.single())
            assertNull(token.user)
            assertNull(token.events)
            assertNull(token.failureReasons)

            if (expectedFormat == RadarResponseFormat.JWE) {
                assertEquals(obj.getBoolean("passed"), token.passed)
                assertEquals("opaque-token", token.token)
            } else {
                assertNull(token.token)
            }

            assertEquals(before, stateSnapshot())
            assertEquals(0, receiver.locations)
            assertEquals(0, receiver.eventBatches)
            assertTrue(receiver.errors.isEmpty())
        }
    }

    @Test
    fun invalidVerifiedResponsesFailBeforeUpdatingUserState() {
        val invalid = listOf(
            response("unknown"),
            response().put("format", JSONObject.NULL),
            response("jwe").apply { remove("token") },
            response("jwe").put("passed", "true"),
            response("id").apply { remove("_id") }
        )

        invalid.forEach { obj ->
            val before = stateSnapshot()
            val result = track(obj)

            assertEquals(RadarStatus.ERROR_SERVER, result.status)
            assertNull(result.response)
            assertNull(result.token)
            assertNull(result.events)
            assertNull(result.user)
            assertNull(result.config)
            assertEquals(listOf(RadarStatus.ERROR_SERVER), receiver.errors)
            assertTrue(verifiedReceiver.tokens.isEmpty())
            assertEquals(0, receiver.locations)
            assertEquals(0, receiver.eventBatches)
            assertEquals(before, stateSnapshot())
        }
    }

    @Test
    fun legacyAndExplicitJwsKeepTheExistingResponsePath() {
        listOf(null, "jws").forEach { format ->
            val obj = response(format)
            val result = track(obj)

            assertEquals(RadarStatus.SUCCESS, result.status)
            assertSame(obj, result.response)
            assertEquals(RadarResponseFormat.JWS, result.token?.format)
            assertEquals("response-user", result.user?._id)
            assertEquals(1, result.events?.size)
            assertEquals("response-user", RadarSettings.getId(context))
            assertEquals(1, receiver.locations)
            assertEquals(1, receiver.eventBatches)
            assertSame(result.token, verifiedReceiver.tokens.single())
            assertTrue(receiver.errors.isEmpty())
        }
    }

    @Test
    fun ordinaryPlaintextTrackingDoesNotRequireATokenOrKnownFormat() {
        listOf(null, "unknown").forEach { format ->
            val obj = response(format).apply {
                remove("token")
                remove("expiresAt")
            }
            val result = track(obj, verified = false)

            assertEquals(RadarStatus.SUCCESS, result.status)
            assertSame(obj, result.response)
            assertEquals("response-user", result.user?._id)
            assertEquals(1, result.events?.size)
            assertNull(result.token)
            assertEquals(1, receiver.locations)
            assertEquals(1, receiver.eventBatches)
            assertTrue(verifiedReceiver.tokens.isEmpty())
            assertTrue(receiver.errors.isEmpty())
        }
    }

    @Test
    fun ordinaryTrackingDoesNotAcceptReducedResponses() {
        val reduced = listOf(
            response("jwe").apply {
                remove("user")
                remove("events")
                remove("failureReasons")
                remove("nearbyGeofences")
            },
            JSONObject().put("format", "id").put("_id", "evaluation-id")
        )

        reduced.forEach { obj ->
            val before = stateSnapshot()
            val result = track(obj, verified = false)

            assertEquals(RadarStatus.ERROR_SERVER, result.status)
            assertNull(result.token)
            assertTrue(verifiedReceiver.tokens.isEmpty())
            assertEquals(listOf(RadarStatus.ERROR_SERVER), receiver.errors)
            assertEquals(before, stateSnapshot())
        }
    }

    @Test
    fun malformedLegacyResponseReportsServerErrorToBothCallers() {
        val result = track(JSONObject().put("meta", JSONObject()))

        assertEquals(RadarStatus.ERROR_SERVER, result.status)
        assertEquals(listOf(RadarStatus.ERROR_SERVER), receiver.errors)
        assertTrue(verifiedReceiver.tokens.isEmpty())
    }

    private fun response(format: String? = null): JSONObject = JSONObject().apply {
        put(
            "meta",
            JSONObject().put(
                "trackingOptions",
                JSONObject().put("desiredMovingUpdateInterval", 17)
            )
        )
        put("_id", "evaluation-id")
        put("token", "opaque-token")
        put("expiresAt", "2030-01-01T00:00:00.000Z")
        put("expiresIn", 90)
        put("passed", true)
        put("user", JSONObject().put("_id", "response-user"))
        put(
            "events",
            JSONArray().put(
                JSONObject()
                    .put("_id", "event-id")
                    .put("type", "user.entered_geofence")
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
        RadarSettings.getTripOptions(context)?.toJson()?.toString(),
        RadarState.getLastUser(context)?.toJson()?.toString(),
        RadarState.getCanExit(context),
        RadarState.getGeofenceIds(context)?.toSet(),
        RadarState.getPlaceId(context),
        RadarState.getRegionIds(context)?.toSet(),
        RadarState.getBeaconIds(context)?.toSet(),
        RadarState.getAltitudeAdjustments(context)?.toString()
    )

    private fun track(obj: JSONObject, verified: Boolean = true): Result {
        receiver.locations = 0
        receiver.eventBatches = 0
        receiver.errors.clear()
        verifiedReceiver.tokens.clear()

        apiHelper.mockStatus = RadarStatus.SUCCESS
        apiHelper.mockResponse = obj

        var result: Result? = null
        var callbackCount = 0

        Radar.apiClient.track(
            location = Location("test").apply {
                latitude = 40.0
                longitude = -73.0
            },
            stopped = false,
            foreground = true,
            source = Radar.RadarLocationSource.FOREGROUND_LOCATION,
            replayed = false,
            beacons = null,
            verified = verified,
            preparedFraudPayload = if (verified) {
                RadarPreparedFraudPayload(StubFraudHandle())
            } else {
                null
            },
            callback = object : RadarApiClient.RadarTrackApiCallback {
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
        )

        assertEquals("Callback must run exactly once", 1, callbackCount)
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

        override fun onEventsReceived(
            context: Context,
            events: Array<RadarEvent>,
            user: RadarUser?
        ) {
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
