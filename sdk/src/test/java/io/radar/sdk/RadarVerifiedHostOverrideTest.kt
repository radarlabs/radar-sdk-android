package io.radar.sdk

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import android.location.Location
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
class RadarVerifiedHostOverrideTest {

    private val publishableKey = "prj_test_pk_0000000000000000000000000000000000000000"
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val apiHelperMock = RadarApiHelperMock()

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
}
