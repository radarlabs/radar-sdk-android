package io.radar.sdk

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.json.JSONArray
import android.location.Location
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
@Config(sdk = [Build.VERSION_CODES.P])
class RadarVerifiedHostOverrideTest {

    private val publishableKey = "prj_test_pk_0000000000000000000000000000000000000000"
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val apiHelperMock = RadarApiHelperMock()

    private fun verifiedTrackResponse(includeTokenFields: Boolean = true): JSONObject {
        val response = RadarTestUtils.jsonObjectFromResource("/track.json")!!
            .put("meta", JSONObject())

        if (includeTokenFields) {
            response.put("token", "verified.jwt.token")
            response.put("expiresAt", "2026-12-31T23:59:59.000Z")
            response.put("expiresIn", 3600)
            response.put("passed", true)
            response.put("failureReasons", JSONArray())
        }

        return response
    }

    private fun incompleteTrackResponse(): JSONObject {
        val response = RadarTestUtils.jsonObjectFromResource("/track.json")!!
            .put("meta", JSONObject())
        response.remove("events")
        return response
    }

    @Before
    fun setUp() {
        Radar.logger = RadarLogger(context)
        Radar.apiClient = RadarApiClient(context, Radar.logger)
        Radar.apiClient.apiHelper = apiHelperMock
        Radar.initialize(context, publishableKey)
        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        apiHelperMock.mockResponse = JSONObject().put("meta", JSONObject())
        apiHelperMock.clearCapturedParams()
        apiHelperMock.lastCapturedVerified = false
        apiHelperMock.lastCapturedVerifiedHostOverride = null
        apiHelperMock.lastUrl = null
    }

    @Test
    fun `trackVerifiedAutoFailover defaults to false`() {
        val options = RadarInitializeOptions()
        assertFalse(options.trackVerifiedAutoFailover)
    }

    @Test
    fun `trackVerifiedAutoFailover persists through initialize`() {
        Radar.initialize(
            context,
            publishableKey,
            RadarInitializeOptions(trackVerifiedAutoFailover = true)
        )
        assertTrue(RadarSettings.getTrackVerifiedAutoFailover(context))

        Radar.initialize(
            context,
            publishableKey,
            RadarInitializeOptions(trackVerifiedAutoFailover = false)
        )
        assertFalse(RadarSettings.getTrackVerifiedAutoFailover(context))
    }

    @Test
    fun `default verified host secondary is expected`() {
        assertEquals("https://api-verified.radar.com", RadarSettings.getDefaultVerifiedHostSecondary())
    }

    @Test
    fun `getConfig verified with no override passes null through`() {
        Radar.apiClient.getConfig("verify", true, null, object : RadarApiClient.RadarGetConfigApiCallback {
            override fun onComplete(status: Radar.RadarStatus, config: io.radar.sdk.model.RadarConfig?) {}
        })

        assertTrue(apiHelperMock.lastCapturedVerified)
        assertNull(apiHelperMock.lastCapturedVerifiedHostOverride)
    }

    @Test
    fun `getConfig verified with override propagates to helper`() {
        val secondary = RadarSettings.getDefaultVerifiedHostSecondary()
        Radar.apiClient.getConfig("verify", true, secondary, object : RadarApiClient.RadarGetConfigApiCallback {
            override fun onComplete(status: Radar.RadarStatus, config: io.radar.sdk.model.RadarConfig?) {}
        })

        assertTrue(apiHelperMock.lastCapturedVerified)
        assertEquals(secondary, apiHelperMock.lastCapturedVerifiedHostOverride)
    }

    @Test
    fun `getConfig with non-Radar response yields null config`() {
        // Response missing top-level "meta" — i.e. not a Radar response.
        apiHelperMock.mockResponse = JSONObject().put("error", "not a radar response")

        var observedConfig: io.radar.sdk.model.RadarConfig? = JSONObject().let { _ -> null }
        var called = false
        Radar.apiClient.getConfig("verify", true, null, object : RadarApiClient.RadarGetConfigApiCallback {
            override fun onComplete(status: Radar.RadarStatus, config: io.radar.sdk.model.RadarConfig?) {
                observedConfig = config
                called = true
            }
        })

        assertTrue(called)
        assertNull(observedConfig)
    }

    @Test
    fun `getConfig with Radar response yields non-null config`() {
        apiHelperMock.mockResponse = JSONObject().put("meta", JSONObject())

        var observedConfig: io.radar.sdk.model.RadarConfig? = null
        Radar.apiClient.getConfig("verify", true, null, object : RadarApiClient.RadarGetConfigApiCallback {
            override fun onComplete(status: Radar.RadarStatus, config: io.radar.sdk.model.RadarConfig?) {
                observedConfig = config
            }
        })

        assertTrue(observedConfig != null)
    }

    @Test
    fun `getConfig with Radar error response yields non-null config`() {
        // A non-200 response that still contains "meta" is a Radar response and must not trigger failover.
        apiHelperMock.mockStatus = Radar.RadarStatus.ERROR_SERVER
        apiHelperMock.mockResponse = JSONObject().put("meta", JSONObject().put("code", 503))

        var observedConfig: io.radar.sdk.model.RadarConfig? = null
        Radar.apiClient.getConfig("verify", true, null, object : RadarApiClient.RadarGetConfigApiCallback {
            override fun onComplete(status: Radar.RadarStatus, config: io.radar.sdk.model.RadarConfig?) {
                observedConfig = config
            }
        })

        assertTrue(observedConfig != null)
    }

    @Test
    fun `getConfig non-verified ignores override`() {
        val secondary = RadarSettings.getDefaultVerifiedHostSecondary()
        Radar.apiClient.getConfig("verify", false, secondary, object : RadarApiClient.RadarGetConfigApiCallback {
            override fun onComplete(status: Radar.RadarStatus, config: io.radar.sdk.model.RadarConfig?) {}
        })

        assertFalse(apiHelperMock.lastCapturedVerified)
        assertFalse(apiHelperMock.lastUrl?.startsWith(secondary) ?: false)
        assertTrue(apiHelperMock.lastUrl?.startsWith("https://api.radar.io") ?: false)
    }

    @Test
    fun `track verified with override propagates to helper`() {
        val secondary = RadarSettings.getDefaultVerifiedHostSecondary()
        Radar.apiClient.track(
            location = Location("test"),
            stopped = false,
            foreground = true,
            source = Radar.RadarLocationSource.FOREGROUND_LOCATION,
            replayed = false,
            beacons = null,
            verified = true,
            verifiedHostOverride = secondary,
        )

        assertTrue(apiHelperMock.lastCapturedVerified)
        assertEquals(secondary, apiHelperMock.lastCapturedVerifiedHostOverride)
        assertTrue(apiHelperMock.lastUrl?.startsWith(secondary) ?: false)
    }

    @Test
    fun `track verified with no override passes null through`() {
        Radar.apiClient.track(
            location = Location("test"),
            stopped = false,
            foreground = true,
            source = Radar.RadarLocationSource.FOREGROUND_LOCATION,
            replayed = false,
            beacons = null,
            verified = true,
        )

        assertTrue(apiHelperMock.lastCapturedVerified)
        assertNull(apiHelperMock.lastCapturedVerifiedHostOverride)
        assertTrue(apiHelperMock.lastUrl?.startsWith("https://api-verified.radar.io") ?: false)
    }

    @Test
    fun `track non-verified ignores override`() {
        val secondary = RadarSettings.getDefaultVerifiedHostSecondary()
        Radar.apiClient.track(
            location = Location("test"),
            stopped = false,
            foreground = true,
            source = Radar.RadarLocationSource.FOREGROUND_LOCATION,
            replayed = false,
            beacons = null,
            verified = false,
            verifiedHostOverride = secondary,
        )

        assertFalse(apiHelperMock.lastCapturedVerified)
        assertFalse(apiHelperMock.lastUrl?.startsWith(secondary) ?: false)
        assertTrue(apiHelperMock.lastUrl?.startsWith("https://api.radar.io") ?: false)
    }

    @Test
    fun `verified track with token succeeds on primary host`() {
        apiHelperMock.mockResponse = verifiedTrackResponse()

        var observedStatus: Radar.RadarStatus? = null
        var observedToken: io.radar.sdk.model.RadarVerifiedLocationToken? = null
        Radar.apiClient.track(
            location = Location("test"),
            stopped = false,
            foreground = true,
            source = Radar.RadarLocationSource.FOREGROUND_LOCATION,
            replayed = false,
            beacons = null,
            verified = true,
            callback = object : RadarApiClient.RadarTrackApiCallback {
                override fun onComplete(
                    status: Radar.RadarStatus,
                    res: JSONObject?,
                    events: Array<io.radar.sdk.model.RadarEvent>?,
                    user: io.radar.sdk.model.RadarUser?,
                    nearbyGeofences: Array<io.radar.sdk.model.RadarGeofence>?,
                    config: io.radar.sdk.model.RadarConfig?,
                    token: io.radar.sdk.model.RadarVerifiedLocationToken?
                ) {
                    observedStatus = status
                    observedToken = token
                }
            }
        )

        assertEquals(Radar.RadarStatus.SUCCESS, observedStatus)
        assertNotNull(observedToken)
        assertEquals("verified.jwt.token", observedToken?.token)
    }

    @Test
    fun `verified track without parseable token returns error`() {
        apiHelperMock.mockResponse = verifiedTrackResponse(includeTokenFields = false)

        var observedStatus: Radar.RadarStatus? = null
        var observedToken: io.radar.sdk.model.RadarVerifiedLocationToken? = null
        Radar.apiClient.track(
            location = Location("test"),
            stopped = false,
            foreground = true,
            source = Radar.RadarLocationSource.FOREGROUND_LOCATION,
            replayed = false,
            beacons = null,
            verified = true,
            callback = object : RadarApiClient.RadarTrackApiCallback {
                override fun onComplete(
                    status: Radar.RadarStatus,
                    res: JSONObject?,
                    events: Array<io.radar.sdk.model.RadarEvent>?,
                    user: io.radar.sdk.model.RadarUser?,
                    nearbyGeofences: Array<io.radar.sdk.model.RadarGeofence>?,
                    config: io.radar.sdk.model.RadarConfig?,
                    token: io.radar.sdk.model.RadarVerifiedLocationToken?
                ) {
                    observedStatus = status
                    observedToken = token
                }
            }
        )

        assertEquals(Radar.RadarStatus.ERROR_SERVER, observedStatus)
        assertNull(observedToken)
    }

    @Test
    fun `non verified track with incomplete payload returns error`() {
        apiHelperMock.mockResponse = incompleteTrackResponse()

        var observedStatus: Radar.RadarStatus? = null
        Radar.apiClient.track(
            location = Location("test"),
            stopped = false,
            foreground = true,
            source = Radar.RadarLocationSource.FOREGROUND_LOCATION,
            replayed = false,
            beacons = null,
            verified = false,
            callback = object : RadarApiClient.RadarTrackApiCallback {
                override fun onComplete(
                    status: Radar.RadarStatus,
                    res: JSONObject?,
                    events: Array<io.radar.sdk.model.RadarEvent>?,
                    user: io.radar.sdk.model.RadarUser?,
                    nearbyGeofences: Array<io.radar.sdk.model.RadarGeofence>?,
                    config: io.radar.sdk.model.RadarConfig?,
                    token: io.radar.sdk.model.RadarVerifiedLocationToken?
                ) {
                    observedStatus = status
                }
            }
        )

        assertEquals(Radar.RadarStatus.ERROR_SERVER, observedStatus)
    }

    @Test
    fun `verified track can succeed on secondary host after non radar config response`() {
        val secondary = RadarSettings.getDefaultVerifiedHostSecondary()
        apiHelperMock.mockResponse = JSONObject().put("error", "not a radar response")

        var observedConfig: io.radar.sdk.model.RadarConfig? = null
        Radar.apiClient.getConfig("trackVerified", true, null, object : RadarApiClient.RadarGetConfigApiCallback {
            override fun onComplete(status: Radar.RadarStatus, config: io.radar.sdk.model.RadarConfig?) {
                observedConfig = config
            }
        })

        assertNull(observedConfig)

        apiHelperMock.mockResponse = verifiedTrackResponse()

        var observedStatus: Radar.RadarStatus? = null
        var observedToken: io.radar.sdk.model.RadarVerifiedLocationToken? = null
        Radar.apiClient.track(
            location = Location("test"),
            stopped = false,
            foreground = true,
            source = Radar.RadarLocationSource.FOREGROUND_LOCATION,
            replayed = false,
            beacons = null,
            verified = true,
            verifiedHostOverride = secondary,
            callback = object : RadarApiClient.RadarTrackApiCallback {
                override fun onComplete(
                    status: Radar.RadarStatus,
                    res: JSONObject?,
                    events: Array<io.radar.sdk.model.RadarEvent>?,
                    user: io.radar.sdk.model.RadarUser?,
                    nearbyGeofences: Array<io.radar.sdk.model.RadarGeofence>?,
                    config: io.radar.sdk.model.RadarConfig?,
                    token: io.radar.sdk.model.RadarVerifiedLocationToken?
                ) {
                    observedStatus = status
                    observedToken = token
                }
            }
        )

        assertTrue(apiHelperMock.lastUrl?.startsWith("$secondary/v1/track") ?: false)
        assertEquals(Radar.RadarStatus.SUCCESS, observedStatus)
        assertNotNull(observedToken)
    }
}
