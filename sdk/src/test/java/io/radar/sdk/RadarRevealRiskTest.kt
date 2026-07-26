package io.radar.sdk

import android.content.Context
import android.location.Location
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.radar.sdk.model.RadarRevealRiskToken
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

internal class MockRadarSDKFraud: RadarSDKFraud() {
    override fun getFraudPayload(context: Context, logger: RadarLogger, location: Location?, googlePlayProjectNumber: Long?, callback: (Radar.RadarStatus, String) -> Unit) {
        callback(Radar.RadarStatus.SUCCESS, "")
    }
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class RadarRevealRiskTest {

    companion object {
        const val LATCH_TIMEOUT = 5L
        const val PUBLISHABLE_KEY = "prj_test_pk_0000000000000000000000000000000000000000"
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val apiHelperMock = RadarApiHelperMock()

    @Before
    fun setUp() {
        Radar.logger = RadarLogger(context)
        Radar.apiClient = RadarApiClient(context, Radar.logger)
        Radar.apiClient.apiHelper = apiHelperMock
        Radar.initialize(context, PUBLISHABLE_KEY)
        apiHelperMock.clearCapturedParams()
    }

    /**
     * Mirrors the iOS `revealRiskCallsFraudSDKThenAPI` test: a fully-populated reveal/risk response
     * is parsed into a fully-populated token, and the request is sent to the verified reveal/risk
     * endpoint. Because the fraud submodule is `compileOnly` (absent from the test classpath), the
     * manager path cannot produce a real fraud payload here, so we exercise the API client directly.
     */
    @Test
    fun test_revealRisk_parsesFullyPopulatedToken() {
        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        apiHelperMock.mockResponse = RadarTestUtils.jsonObjectFromResource("/reveal_risk.json")

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null
        var callbackToken: RadarRevealRiskToken? = null

        Radar.apiClient.revealRisk("mock-fraud-payload") { status, token ->
            callbackStatus = status
            callbackToken = token
            latch.countDown()
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

        // The request hit the verified reveal/risk endpoint.
        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)
        assertEquals("POST", apiHelperMock.lastCapturedMethod)
        assertEquals("v1/reveal/risk", apiHelperMock.lastCapturedPath)
        assertTrue(apiHelperMock.lastCapturedVerified)

        // The response was parsed into a fully-populated token.
        val token = callbackToken!!
        assertEquals("risk-token-123", token.id)
        assertEquals("signed-jwt-token", token.token)
        assertEquals("2026-07-14T12:00:00.000Z", token.expiresAt)
        assertEquals(3600, token.expiresIn)

        assertEquals(RadarRevealRiskToken.RiskLevel.MEDIUM, token.risk.level)
        assertArrayEquals(arrayOf("proxy_detected", "vpn_detected"), token.risk.reasons)

        assertEquals("US", token.network.ipAddress?.countryCode)
        assertEquals("New York", token.network.ipAddress?.city)
        assertEquals(true, token.network.ipAddress?.countryAllowed)
        assertEquals(true, token.network.ipAddress?.stateAllowed)

        assertEquals(true, token.network.privacy?.proxy)
        assertEquals(true, token.network.privacy?.vpn)
        assertEquals(false, token.network.privacy?.hosting)
        assertEquals("SomeVPN", token.network.privacy?.service)

        assertEquals("CLOUDFLARENET", token.network.asn?.name)
        assertEquals("AS13335", token.network.asn?.asn)
        assertEquals("cloudflare.com", token.network.asn?.domain)

        assertEquals("Android", token.device.deviceType)
        assertEquals("install-xyz", token.device.installId)
        assertEquals("com.radar.example", token.device.appId)
    }

    /**
     * Mirrors the iOS `revealRiskSkipsAPIWhenFraudFails` test. The fraud submodule is not on the test
     * classpath, so the manager's fraud lookup fails; the manager must surface an error and must not
     * call the reveal/risk API.
     */
    @Test
    fun test_revealRisk_skipsApiWhenFraudFails() {
        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        apiHelperMock.mockResponse = RadarTestUtils.jsonObjectFromResource("/reveal_risk.json")
        apiHelperMock.clearCapturedParams()

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null
        var callbackToken: RadarRevealRiskToken? = null

        Radar.revealRisk { status, token ->
            callbackStatus = status
            callbackToken = token
            latch.countDown()
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

        // The fraud SDK failed to produce a payload, so no token is produced...
        assertEquals(Radar.RadarStatus.ERROR_PLUGIN, callbackStatus)
        assertNull(callbackToken)

        // ...and the reveal/risk API was never called.
        assertNotEquals("v1/reveal/risk", apiHelperMock.lastCapturedPath)
    }

    @Test
    fun test_revealRisk_returnsServerErrorWhenResponseUnparseable() {
        apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
        // A response missing the required `risk`/`network`/`device` objects cannot be parsed.
        apiHelperMock.mockResponse = JSONObject().apply { put("_id", "risk-token-123") }

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null
        var callbackToken: RadarRevealRiskToken? = null

        Radar.apiClient.revealRisk("mock-fraud-payload") { status, token ->
            callbackStatus = status
            callbackToken = token
            latch.countDown()
        }

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(LATCH_TIMEOUT, TimeUnit.SECONDS)

        assertEquals(Radar.RadarStatus.ERROR_SERVER, callbackStatus)
        assertNull(callbackToken)
    }

    @Test
    fun test_RadarRevealRiskToken_fromJson_null() {
        assertNull(RadarRevealRiskToken.fromJson(null))
    }

    @Test
    fun test_RadarRevealRiskToken_fromJson_riskLevels() {
        assertEquals(RadarRevealRiskToken.RiskLevel.LOW, tokenWithRiskLevel("low").risk.level)
        assertEquals(RadarRevealRiskToken.RiskLevel.MEDIUM, tokenWithRiskLevel("medium").risk.level)
        assertEquals(RadarRevealRiskToken.RiskLevel.HIGH, tokenWithRiskLevel("high").risk.level)
        // An unknown or missing level falls back to NONE.
        assertEquals(RadarRevealRiskToken.RiskLevel.NONE, tokenWithRiskLevel("bogus").risk.level)
    }

    @Test
    fun test_RadarRevealRiskToken_fromJson_handlesMissingOptionalFields() {
        val obj = JSONObject().apply {
            put("_id", "risk-token-123")
            put("risk", JSONObject().apply { put("level", "high") })
            put("network", JSONObject())
            put("device", JSONObject())
        }

        val token = RadarRevealRiskToken.fromJson(obj)!!

        assertEquals("risk-token-123", token.id)
        assertEquals(RadarRevealRiskToken.RiskLevel.HIGH, token.risk.level)
        assertTrue(token.risk.reasons.isEmpty())
        assertNull(token.token)
        assertNull(token.expiresAt)
        assertNull(token.expiresIn)
        assertNull(token.network.ipAddress)
        assertNull(token.network.privacy)
        assertNull(token.network.asn)
    }

    @Test
    fun test_RadarRevealRiskToken_toJson_stripsMeta() {
        val obj = RadarTestUtils.jsonObjectFromResource("/reveal_risk.json")!!
        obj.put("meta", JSONObject().apply { put("code", 200) })

        val token = RadarRevealRiskToken.fromJson(obj)!!

        assertFalse(token.toJson().has("meta"))
        assertEquals("risk-token-123", token.toJson().optString("_id"))
    }

    private fun tokenWithRiskLevel(level: String): RadarRevealRiskToken {
        val obj = JSONObject().apply {
            put("_id", "risk-token-123")
            put(
                "risk",
                JSONObject().apply {
                    put("level", level)
                    put("reasons", JSONArray())
                }
            )
            put("network", JSONObject())
            put("device", JSONObject())
        }
        return RadarRevealRiskToken.fromJson(obj)!!
    }
}
