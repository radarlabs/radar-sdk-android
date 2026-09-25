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
            location = Location("test"),
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
        assertEquals("""{"encv":1}""", apiHelperMock.lastCapturedParams?.getString("fraudPayload"))

        val options = handle.sealOptions.single()
        assertEquals("POST", options["method"])
        assertEquals("/v1/track", options["canonicalRoute"])
        assertEquals(apiHelperMock.lastCapturedParams?.getString("installId"), options["installId"])
        assertEquals(context.packageName, options["origin"])
        assertEquals(publishableKey, options["authorization"])
        assertTrue((options["encryptionAttemptId"] as String).matches(Regex("[A-Za-z0-9_-]{22}")))
    }

    @Test
    fun trackVerifiedDoesNotSendWhenSealingFails() {
        val handle = RecordingFraudHandle(
            mapOf("error" to "Failed to encrypt fraud payload")
        )
        var callbackStatus: Radar.RadarStatus? = null

        Radar.apiClient.track(
            location = Location("test"),
            stopped = false,
            foreground = true,
            source = Radar.RadarLocationSource.FOREGROUND_LOCATION,
            replayed = false,
            beacons = null,
            verified = true,
            preparedFraudPayload = RadarPreparedFraudPayload(handle),
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

        assertEquals(1, handle.sealOptions.size)
        assertEquals(Radar.RadarStatus.ERROR_PLUGIN, callbackStatus)
        assertNull(apiHelperMock.lastCapturedPath)
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
                apiHelperMock.lastCapturedParams?.getString("fraudPayload")
            )

            apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
            apiHelperMock.mockResponse = JSONObject()
            Radar.flushReplays()

            assertEquals("v1/track/replay", apiHelperMock.lastCapturedPath)
            val replays = apiHelperMock.lastCapturedParams!!.getJSONArray("replays")
            val replay = replays.getJSONObject(replays.length() - 1)
            assertFalse(replay.has("fraudPayload"))
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
