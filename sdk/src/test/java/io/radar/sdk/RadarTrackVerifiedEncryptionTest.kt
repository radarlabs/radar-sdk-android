package io.radar.sdk

import android.content.Context
import android.location.Location
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.radar.sdk.helpers.RadarApiHelperMock
import io.radar.sdk.model.RadarConfig
import io.radar.sdk.model.RadarEvent
import io.radar.sdk.model.RadarGeofence
import io.radar.sdk.model.RadarUser
import io.radar.sdk.model.RadarVerifiedLocationToken
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class RadarTrackVerifiedEncryptionTest {
    class RecordingFraudHandle(
        private val result: Map<String, Any?> = mapOf("payload" to """{"encv":1}""")
    ) {
        val sealOptions = mutableListOf<Map<String, Any?>>()

        fun seal(options: Map<String, Any?>): Map<String, Any?> {
            sealOptions.add(options.toMap())
            return result
        }
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val apiHelperMock = RadarApiHelperMock()
    private val publishableKey = "prj_test_pk_0000000000000000000000000000000000000000"

    @Before
    fun setUp() {
        Radar.logger = RadarLogger(context)
        Radar.apiClient = RadarApiClient(context, Radar.logger)
        Radar.apiClient.apiHelper = apiHelperMock
        Radar.initialize(context, publishableKey)
        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        apiHelperMock.mockResponse = JSONObject().put("meta", JSONObject())
        apiHelperMock.clearCapturedParams()
    }

    @Test
    fun trackVerifiedSealsForItsRequest() {
        val handle = RecordingFraudHandle()

        Radar.apiClient.track(
            location = Location("test").apply {
                latitude = 40.78382
                longitude = -73.97536
            },
            stopped = false,
            foreground = true,
            source = Radar.RadarLocationSource.FOREGROUND_LOCATION,
            replayed = false,
            beacons = null,
            verified = true,
            preparedFraudPayload = RadarPreparedFraudPayload(handle)
        )

        assertEquals("POST", apiHelperMock.lastCapturedMethod)
        assertEquals("v1/track", apiHelperMock.lastCapturedPath)
        assertTrue(apiHelperMock.lastCapturedVerified)
        assertEquals("""{"encv":1}""", apiHelperMock.lastCapturedParams?.toString())

        val options = handle.sealOptions.single()
        assertEquals("POST", options["method"])
        assertEquals("/v1/track", options["canonicalRoute"])
        val coreBody = JSONObject(options["body"] as String)
        assertEquals(RadarSettings.getInstallId(context), coreBody.getString("installId"))
        assertEquals(40.78382, coreBody.getDouble("latitude"), 0.0)
        assertEquals(-73.97536, coreBody.getDouble("longitude"), 0.0)
        assertEquals(RadarUtils.sdkVersion, coreBody.getString("sdkVersion"))
        assertFalse(coreBody.has("fraudPayload"))
        assertFalse(options.containsKey("installId"))
        assertEquals(context.packageName, options["origin"])
        assertEquals(publishableKey, options["authorization"])
        assertTrue((options["encryptionAttemptId"] as String).matches(Regex("[A-Za-z0-9_-]{22}")))
    }

    @Test
    fun trackVerifiedDoesNotSendWhenPreparationFails() {
        val results = listOf(
            null,
            emptyMap(),
            mapOf("error" to "Failed to encrypt request body"),
            mapOf("payload" to "not JSON"),
            mapOf("payload" to "[]"),
            mapOf("payload" to "null"),
            mapOf("payload" to 42)
        )
        results.forEach { result ->
            apiHelperMock.clearCapturedParams()
            val handle = result?.let { RecordingFraudHandle(it) }
            var callbackStatus: Radar.RadarStatus? = null

            Radar.apiClient.track(
                location = Location("test"),
                stopped = false,
                foreground = true,
                source = Radar.RadarLocationSource.FOREGROUND_LOCATION,
                replayed = false,
                beacons = null,
                verified = true,
                preparedFraudPayload = handle?.let { RadarPreparedFraudPayload(it) },
                callback = object : RadarApiClient.RadarTrackApiCallback {
                    override fun onComplete(
                        status: Radar.RadarStatus,
                        res: JSONObject?,
                        events: Array<RadarEvent>?,
                        user: RadarUser?,
                        nearbyGeofences: Array<RadarGeofence>?,
                        config: RadarConfig?,
                        token: RadarVerifiedLocationToken?
                    ) {
                        callbackStatus = status
                    }
                }
            )

            assertEquals(if (result == null) 0 else 1, handle?.sealOptions?.size ?: 0)
            assertEquals(Radar.RadarStatus.ERROR_PLUGIN, callbackStatus)
            assertNull(apiHelperMock.lastCapturedPath)
        }
    }

    @Test
    fun ordinaryTrackSendsCoreBodyWithoutSealing() {
        val handle = RecordingFraudHandle(mapOf("error" to "Must not be called"))
        Radar.apiClient.track(
            location = Location("test"),
            stopped = false,
            foreground = true,
            source = Radar.RadarLocationSource.FOREGROUND_LOCATION,
            replayed = false,
            beacons = null,
            verified = false,
            preparedFraudPayload = RadarPreparedFraudPayload(handle)
        )

        assertTrue(handle.sealOptions.isEmpty())
        assertFalse(apiHelperMock.lastCapturedVerified)
        val body = apiHelperMock.lastCapturedParams!!
        assertEquals(RadarSettings.getInstallId(context), body.getString("installId"))
        assertTrue(body.has("latitude"))
        assertFalse(body.has("fraudPayload"))
        assertFalse(body.has("encv"))
    }

    @Test
    fun failedTrackVerifiedReplayOmitsFraudPayload() {
        val originalRemoteOptions = RadarSettings.getRemoteTrackingOptions(context)

        try {
            RadarSettings.setRemoteTrackingOptions(
                context,
                RadarTrackingOptions.EFFICIENT.copy(
                    replay = RadarTrackingOptions.RadarTrackingOptionsReplay.ALL
                )
            )
            apiHelperMock.mockStatus = Radar.RadarStatus.ERROR_NETWORK
            apiHelperMock.mockResponse = null

            Radar.apiClient.track(
                location = Location("test"),
                stopped = false,
                foreground = true,
                source = Radar.RadarLocationSource.FOREGROUND_LOCATION,
                replayed = false,
                beacons = null,
                verified = true,
                preparedFraudPayload = RadarPreparedFraudPayload(RecordingFraudHandle())
            )

            assertEquals("v1/track", apiHelperMock.lastCapturedPath)
            assertEquals(
                """{"encv":1}""",
                apiHelperMock.lastCapturedParams?.toString()
            )

            apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
            apiHelperMock.mockResponse = JSONObject()
            Radar.flushReplays()

            assertEquals("v1/track/replay", apiHelperMock.lastCapturedPath)
            val replays = apiHelperMock.lastCapturedParams!!.getJSONArray("replays")
            val replay = replays.getJSONObject(replays.length() - 1)
            assertFalse(replay.has("fraudPayload"))
            assertFalse(replay.has("encv"))
            assertEquals(RadarSettings.getInstallId(context), replay.getString("installId"))
            assertTrue(replay.has("latitude"))
            assertTrue(replay.getBoolean("replayed"))
        } finally {
            if (originalRemoteOptions == null) {
                RadarSettings.removeRemoteTrackingOptions(context)
            } else {
                RadarSettings.setRemoteTrackingOptions(context, originalRemoteOptions)
            }
        }
    }
}
