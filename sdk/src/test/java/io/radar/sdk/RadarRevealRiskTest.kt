package io.radar.sdk

import android.content.Context
import android.os.Build
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.radar.sdk.helpers.RadarApiHelperMock
import io.radar.sdk.helpers.RadarTestUtils
import io.radar.sdk.model.RadarResponseFormat
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

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class RadarRevealRiskTest {

    companion object {
        const val LATCH_TIMEOUT = 5L
        const val PUBLISHABLE_KEY = "prj_test_pk_0000000000000000000000000000000000000000"
        const val SEALED_FRAUD_PAYLOAD = """{"encv":1}"""
    }

    class RecordingFraudHandle(
        private val result: Map<String, Any?> = mapOf("payload" to SEALED_FRAUD_PAYLOAD)
    ) {
        val sealOptions = mutableListOf<Map<String, Any?>>()

        fun seal(options: Map<String, Any?>): Map<String, Any?> {
            sealOptions.add(options.toMap())
            return result
        }
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
        val fraudHandle = RecordingFraudHandle()

        val latch = CountDownLatch(1)
        var callbackStatus: Radar.RadarStatus? = null
        var callbackToken: RadarRevealRiskToken? = null

        Radar.apiClient.revealRisk(RadarPreparedFraudPayload(fraudHandle)) { status, token ->
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
        assertEquals(SEALED_FRAUD_PAYLOAD, apiHelperMock.lastCapturedParams?.toString())

        val sealOptions = fraudHandle.sealOptions.single()
        assertEquals("POST", sealOptions["method"])
        assertEquals("/v1/reveal/risk", sealOptions["canonicalRoute"])
        val coreBody = JSONObject(sealOptions["body"] as String)
        assertEquals(RadarSettings.getInstallId(context), coreBody.getString("installId"))
        assertEquals("Android", coreBody.getString("deviceType"))
        assertEquals(RadarUtils.SDK_VERSION, coreBody.getString("sdkVersion"))
        assertFalse(coreBody.has("fraudPayload"))
        assertFalse(sealOptions.containsKey("installId"))
        assertEquals(context.packageName, sealOptions["origin"])
        assertEquals(RadarUtils.SDK_VERSION, sealOptions["sdkVersion"])
        assertEquals(PUBLISHABLE_KEY, sealOptions["authorization"])
        val attemptId = sealOptions["encryptionAttemptId"] as String
        assertTrue(attemptId.matches(Regex("[A-Za-z0-9_-]{22}")))
        assertEquals(16, Base64.decode(attemptId, Base64.URL_SAFE or Base64.NO_WRAP).size)
        val nowSeconds = System.currentTimeMillis() / 1000L
        assertTrue((sealOptions["issuedAt"] as Long) in (nowSeconds - 5)..(nowSeconds + 5))

        // The response was parsed into a fully-populated token.
        val token = callbackToken!!
        val risk = requireNotNull(token.risk)
        val network = requireNotNull(token.network)
        val device = requireNotNull(token.device)
        assertEquals("risk-token-123", token.id)
        assertEquals("signed-jwt-token", token.token)
        assertEquals("2026-07-14T12:00:00.000Z", token.expiresAt)
        assertEquals(3600, token.expiresIn)

        assertEquals(RadarRevealRiskToken.RiskLevel.MEDIUM, risk.level)
        assertArrayEquals(arrayOf("proxy_detected", "vpn_detected"), risk.reasons)

        assertEquals("US", network.ipAddress?.countryCode)
        assertEquals("New York", network.ipAddress?.city)
        assertEquals(true, network.ipAddress?.countryAllowed)
        assertEquals(true, network.ipAddress?.stateAllowed)

        assertEquals(true, network.privacy?.proxy)
        assertEquals(true, network.privacy?.vpn)
        assertEquals(false, network.privacy?.hosting)
        assertEquals("SomeVPN", network.privacy?.service)

        assertEquals("CLOUDFLARENET", network.asn?.name)
        assertEquals("AS13335", network.asn?.asn)
        assertEquals("cloudflare.com", network.asn?.domain)

        assertEquals("Android", device.deviceType)
        assertEquals("install-xyz", device.installId)
        assertEquals("com.radar.example", device.appId)
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

        Radar.apiClient.revealRisk(RadarPreparedFraudPayload(RecordingFraudHandle())) { status, token ->
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
    fun test_revealRisk_doesNotSendWhenSealingFails() {
        val results = listOf(
            emptyMap(),
            mapOf("error" to "Failed to encrypt request body"),
            mapOf("payload" to "not JSON"),
            mapOf("payload" to "[]"),
            mapOf("payload" to "null"),
            mapOf("payload" to 42)
        )
        results.forEach { result ->
            apiHelperMock.clearCapturedParams()
            val fraudHandle = RecordingFraudHandle(result)
            var callbackStatus: Radar.RadarStatus? = null
            var callbackToken: RadarRevealRiskToken? = null

            Radar.apiClient.revealRisk(RadarPreparedFraudPayload(fraudHandle)) { status, token ->
                callbackStatus = status
                callbackToken = token
            }

            assertEquals(1, fraudHandle.sealOptions.size)
            assertEquals(Radar.RadarStatus.ERROR_BAD_REQUEST, callbackStatus)
            assertNull(callbackToken)
            assertNotEquals("v1/reveal/risk", apiHelperMock.lastCapturedPath)
        }
    }

    @Test
    fun test_RadarRevealRiskToken_fromJson_null() {
        assertNull(RadarRevealRiskToken.fromJson(null))
    }

    @Test
    fun test_RadarRevealRiskToken_fromJson_riskLevels() {
        assertEquals(RadarRevealRiskToken.RiskLevel.LOW, tokenWithRiskLevel("low").risk!!.level)
        assertEquals(RadarRevealRiskToken.RiskLevel.MEDIUM, tokenWithRiskLevel("medium").risk!!.level)
        assertEquals(RadarRevealRiskToken.RiskLevel.HIGH, tokenWithRiskLevel("high").risk!!.level)
        // An unknown or missing level falls back to NONE.
        assertEquals(RadarRevealRiskToken.RiskLevel.NONE, tokenWithRiskLevel("bogus").risk!!.level)
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
        val risk = requireNotNull(token.risk)
        val network = requireNotNull(token.network)

        assertEquals("risk-token-123", token.id)
        assertEquals(RadarRevealRiskToken.RiskLevel.HIGH, risk.level)
        assertTrue(risk.reasons.isEmpty())
        assertNull(token.token)
        assertNull(token.expiresAt)
        assertNull(token.expiresIn)
        assertNull(network.ipAddress)
        assertNull(network.privacy)
        assertNull(network.asn)
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

    @Test
    fun test_revealRisk_acceptsJweAndIdResponses() {
        listOf("jwe", "id").forEach { format ->
            apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
            apiHelperMock.mockResponse = reducedResponse(format)
            apiHelperMock.clearCapturedParams()

            var callbackCount = 0
            var callbackStatus: Radar.RadarStatus? = null
            var callbackToken: RadarRevealRiskToken? = null

            Radar.apiClient.revealRisk(
                RadarPreparedFraudPayload(RecordingFraudHandle())
            ) { status, token ->
                callbackCount++
                callbackStatus = status
                callbackToken = token
            }

            assertEquals(1, callbackCount)
            assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)
            assertEquals("v1/reveal/risk", apiHelperMock.lastCapturedPath)
            assertTrue(apiHelperMock.lastCapturedVerified)

            val token = requireNotNull(callbackToken)
            assertEquals(RadarResponseFormat.fromJson(format), token.format)
            assertEquals("risk-id", token.id)
            assertNull(token.risk)
            assertNull(token.network)
            assertNull(token.device)
            assertEquals(if (format == "jwe") "opaque-token" else null, token.token)
        }
    }

    @Test
    fun test_revealRisk_rejectsMalformedReducedResponses() {
        val responses = listOf(
            reducedResponse("unknown"),
            reducedResponse("jwe").apply { remove("token") },
            reducedResponse("jwe").put("expiresIn", "90"),
            reducedResponse("id").apply { remove("_id") }
        )

        responses.forEach { response ->
            apiHelperMock.mockStatus = Radar.RadarStatus.SUCCESS
            apiHelperMock.mockResponse = response

            var callbackCount = 0
            var callbackStatus: Radar.RadarStatus? = null
            var callbackToken: RadarRevealRiskToken? = null

            Radar.apiClient.revealRisk(
                RadarPreparedFraudPayload(RecordingFraudHandle())
            ) { status, token ->
                callbackCount++
                callbackStatus = status
                callbackToken = token
            }

            assertEquals(1, callbackCount)
            assertEquals(Radar.RadarStatus.ERROR_SERVER, callbackStatus)
            assertNull(callbackToken)
        }
    }

    private fun reducedResponse(format: String): JSONObject = JSONObject().apply {
        put("format", format)
        put("_id", "risk-id")
        if (format == "jwe") {
            put("token", "opaque-token")
            put("expiresAt", "2030-01-01T00:00:00.000Z")
            put("expiresIn", 90)
        }
    }
}
