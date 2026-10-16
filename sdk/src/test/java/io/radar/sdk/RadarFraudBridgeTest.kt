package io.radar.sdk

import android.content.Context
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.radar.sdk.helpers.RadarApiHelperMock
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class RadarFraudBridgeTest {
    class PreparedHandle(var result: Any? = mapOf("payload" to "encrypted-envelope")) {
        val sealOptions = mutableListOf<Map<String, Any?>>()
        var failure: Exception? = null

        fun seal(options: Map<String, Any?>): Any? {
            sealOptions.add(options)
            failure?.let { throw it }
            return result
        }
    }

    class FraudInstance {
        companion object {
            var version: Any? = 1
            var compatibilityFailure: Exception? = null
            var result: Map<String, Any?>? = null
            var failure: Exception? = null
            var instances = 0
            val options = mutableListOf<Map<String, Any?>>()

            @JvmStatic
            fun coreCompatibilityVersion(): Any? {
                compatibilityFailure?.let { throw it }
                return version
            }

            @JvmStatic
            fun sharedInstance(): FraudInstance {
                instances++
                return FraudInstance()
            }
        }

        fun prepareFraudPayload(options: Map<String, Any?>, callback: (Map<String, Any?>?) -> Unit) {
            Companion.options.add(options)
            failure?.let { throw it }
            callback(result)
        }
    }

    class LegacyFraudInstance {
        companion object {
            @JvmStatic
            fun sharedInstance(): LegacyFraudInstance = LegacyFraudInstance()
        }
    }

    class FraudWithoutCompatibilityMarker {
        companion object {
            @JvmStatic
            fun sharedInstance(): FraudWithoutCompatibilityMarker {
                FraudInstance.instances++
                return FraudWithoutCompatibilityMarker()
            }
        }

        fun prepareFraudPayload(options: Map<String, Any?>, callback: (Map<String, Any?>?) -> Unit) {
            FraudInstance().prepareFraudPayload(options, callback)
        }
    }

    class FraudWithoutSharedInstance {
        fun prepareFraudPayload(options: Map<String, Any?>, callback: (Map<String, Any?>?) -> Unit) {
            FraudInstance().prepareFraudPayload(options, callback)
        }
    }

    class FraudWithoutPreparation {
        companion object {
            @JvmStatic
            fun coreCompatibilityVersion(): Int = 1

            @JvmStatic
            fun sharedInstance(): FraudWithoutPreparation = FraudWithoutPreparation()
        }
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val logger = RadarLogger(context)

    @Before
    fun setUp() {
        FraudInstance.version = 1
        FraudInstance.compatibilityFailure = null
        FraudInstance.result = null
        FraudInstance.failure = null
        FraudInstance.instances = 0
        FraudInstance.options.clear()
        RadarSettings.setLogLevel(context, Radar.RadarLogLevel.DEBUG)
        ShadowLog.clear()
    }

    @Test
    fun compatibleSdkPreparesOnceAndForwardsSealing() {
        val handle = PreparedHandle()
        FraudInstance.result = mapOf("preparedPayload" to handle)
        var prepared: RadarPreparedFraudPayload? = null

        bridge().prepareFraudPayload(context, logger, googlePlayProjectNumber = 123L) { status, payload ->
            assertEquals(Radar.RadarStatus.SUCCESS, status)
            prepared = payload
        }

        assertNotNull(prepared)
        assertEquals(1, FraudInstance.instances)
        assertEquals(1, FraudInstance.options.size)
        assertSame(context, FraudInstance.options.single()["context"])
        assertEquals(123L, FraudInstance.options.single()["googlePlayProjectNumber"])
        assertTrue(handle.sealOptions.isEmpty())
        assertEquals("encrypted-envelope", prepared!!.seal(mapOf("encryptionAttemptId" to "first")))
        assertEquals("encrypted-envelope", prepared!!.seal(mapOf("encryptionAttemptId" to "second")))
        assertEquals(listOf("first", "second"), handle.sealOptions.map { it["encryptionAttemptId"] })
    }

    @Test
    fun sdkWithoutCompatibilityMarkerWarnsAndForwardsSealing() {
        val handle = PreparedHandle()
        FraudInstance.result = mapOf("preparedPayload" to handle)
        var prepared: RadarPreparedFraudPayload? = null
        var callbacks = 0

        bridge(hasCompatibilityMarker = false).prepareFraudPayload(context, logger) { status, payload ->
            callbacks++
            assertEquals(Radar.RadarStatus.SUCCESS, status)
            prepared = payload
        }

        assertEquals(1, callbacks)
        assertEquals(1, FraudInstance.instances)
        assertEquals(1, FraudInstance.options.size)
        assertNotNull(prepared)
        assertEquals("encrypted-envelope", prepared!!.seal(mapOf("encryptionAttemptId" to "first")))
        assertEquals("encrypted-envelope", prepared!!.seal(mapOf("encryptionAttemptId" to "second")))
        assertEquals(listOf("first", "second"), handle.sealOptions.map { it["encryptionAttemptId"] })
        assertTrue(
            ShadowLog.getLogsForTag("RadarLogger").any {
                it.type == Log.WARN &&
                    it.msg.contains("Installed Radar Fraud SDK does not report compatibility with this Radar SDK.")
            }
        )
    }

    @Test
    fun missingSdkReturnsPluginErrorWithActionableMessage() {
        assertFailure(RadarSDKFraud { throw ClassNotFoundException("Fraud SDK absent") }, Radar.RadarStatus.ERROR_PLUGIN)

        assertLogContains("A compatible Radar Fraud SDK is required for verified requests.")
    }

    @Test
    fun missingMethodsAndDependencyLinkageFailuresReturnPluginError() {
        for (type in listOf(LegacyFraudInstance::class.java, FraudWithoutPreparation::class.java, FraudWithoutSharedInstance::class.java)) {
            assertFailure(RadarSDKFraud { type }, Radar.RadarStatus.ERROR_PLUGIN)
        }
        assertFailure(RadarSDKFraud { throw NoClassDefFoundError("Missing SDK dependency") }, Radar.RadarStatus.ERROR_PLUGIN)

        assertLogContains("Installed Radar Fraud SDK is incompatible with this Radar SDK.")
        assertTrue(FraudInstance.options.isEmpty())
    }

    @Test
    fun unsupportedAndMalformedVersionsStopBeforeSdkInitialization() {
        for (version in listOf(null, -1, 0, 2, 1.5, "1", 1L)) {
            FraudInstance.version = version
            assertFailure(bridge(), Radar.RadarStatus.ERROR_PLUGIN)
        }

        assertEquals(0, FraudInstance.instances)
        assertTrue(FraudInstance.options.isEmpty())
        assertLogContains("Update to compatible Radar SDK and Radar Fraud SDK versions.")
    }

    @Test
    fun throwingCompatibilityMarkerIsRejectedRatherThanTreatedAsMissing() {
        FraudInstance.compatibilityFailure = NoSuchMethodException("Marker invocation failed")

        assertFailure(bridge(), Radar.RadarStatus.ERROR_PLUGIN)

        assertEquals(0, FraudInstance.instances)
        assertTrue(FraudInstance.options.isEmpty())
    }

    @Test
    fun sdkWithoutMarkerSendsEncryptedRevealRiskAndStopsOnSealingFailure() {
        val originalFraud = RadarSDKFraud.shared
        val originalKey = RadarSettings.getPublishableKey(context)
        val helper = RadarApiHelperMock().apply {
            mockStatus = Radar.RadarStatus.SUCCESS
            mockResponse = JSONObject()
                .put("_id", "risk-token")
                .put("risk", JSONObject().put("level", "low"))
                .put("network", JSONObject())
                .put("device", JSONObject())
        }
        try {
            Radar.logger = logger
            Radar.apiClient = RadarApiClient(context, logger).apply { apiHelper = helper }
            Radar.initialize(context, "prj_test_pk_radar_sdk_android")
            RadarSDKFraud.shared = bridge(hasCompatibilityMarker = false)
            for (sealingFails in listOf(false, true)) {
                helper.clearCapturedParams()
                val envelope = """{"encv":1,"ct":"encrypted-body"}"""
                val handle = PreparedHandle(
                    if (sealingFails) mapOf("error" to "Seal failed") else mapOf("payload" to envelope)
                )
                FraudInstance.result = mapOf("preparedPayload" to handle)
                var callbacks = 0
                RadarRevealRiskManager(context, logger).revealRisk { status, token ->
                    callbacks++
                    assertEquals(if (sealingFails) Radar.RadarStatus.ERROR_BAD_REQUEST else Radar.RadarStatus.SUCCESS, status)
                    assertEquals(if (sealingFails) null else "risk-token", token?.id)
                }

                assertEquals(1, callbacks)
                assertEquals(1, handle.sealOptions.size)
                assertEquals(if (sealingFails) null else "v1/reveal/risk", helper.lastCapturedPath)
                assertEquals(if (sealingFails) null else envelope, helper.lastCapturedParams?.toString())
            }
        } finally {
            RadarSDKFraud.shared = originalFraud
            RadarSettings.setPublishableKey(context, originalKey)
        }
    }

    @Test
    fun failedOrMalformedPreparationReturnsBadRequest() {
        val handle = PreparedHandle()
        val results = listOf(
            null,
            emptyMap(),
            mapOf("error" to "Collection failed"),
            mapOf("preparedPayload" to Any()),
            mapOf("preparedPayload" to "not a prepared payload"),
            mapOf("error" to "Collection failed", "preparedPayload" to handle)
        )
        for (result in results) {
            FraudInstance.result = result
            assertFailure(bridge(), Radar.RadarStatus.ERROR_BAD_REQUEST)
            assertFailure(bridge(hasCompatibilityMarker = false), Radar.RadarStatus.ERROR_BAD_REQUEST)
        }

        assertEquals(results.size * 2, FraudInstance.options.size)
        assertTrue(handle.sealOptions.isEmpty())
        assertLogContains("Failed to prepare fraud payload")
    }

    @Test
    fun sdkPreparationExceptionReturnsBadRequestAndLogsOriginalCause() {
        val failure = IllegalStateException("Collection failed")
        FraudInstance.failure = failure
        assertFailure(bridge(), Radar.RadarStatus.ERROR_BAD_REQUEST)
        assertFailure(bridge(hasCompatibilityMarker = false), Radar.RadarStatus.ERROR_BAD_REQUEST)

        assertSame(failure, ShadowLog.getLogsForTag("RadarLogger").last().throwable)
    }

    @Test
    fun revealRiskPreservesPluginAndPreparationFailuresAndRejectsMissingPayload() {
        val originalFraud = RadarSDKFraud.shared
        try {
            for (preparationStatus in listOf(Radar.RadarStatus.ERROR_PLUGIN, Radar.RadarStatus.ERROR_BAD_REQUEST, Radar.RadarStatus.SUCCESS)) {
                RadarSDKFraud.shared = object : RadarSDKFraud() {
                    override fun prepareFraudPayload(
                        context: Context,
                        logger: RadarLogger,
                        location: Location?,
                        googlePlayProjectNumber: Long?,
                        callback: (Radar.RadarStatus, RadarPreparedFraudPayload?) -> Unit
                    ) {
                        callback(preparationStatus, null)
                    }
                }
                var callbacks = 0
                RadarRevealRiskManager(context, logger).revealRisk { status, token ->
                    callbacks++
                    val expected = if (preparationStatus == Radar.RadarStatus.SUCCESS) {
                        Radar.RadarStatus.ERROR_BAD_REQUEST
                    } else {
                        preparationStatus
                    }
                    assertEquals(expected, status)
                    assertNull(token)
                }
                assertEquals(1, callbacks)
            }
        } finally {
            RadarSDKFraud.shared = originalFraud
        }
    }

    @Test
    fun failedOrMalformedSealingReturnsDescriptiveError() {
        val results = listOf(
            null,
            "not a result map",
            emptyMap<String, Any?>(),
            mapOf("error" to "Seal failed"),
            mapOf("payload" to ""),
            mapOf("payload" to 123),
            mapOf("error" to "Seal failed", "payload" to "stale-envelope")
        )
        for (result in results) {
            val handle = PreparedHandle(result)
            val error = assertThrows(IllegalStateException::class.java) {
                RadarPreparedFraudPayload(handle).seal(emptyMap())
            }
            assertEquals("Failed to encrypt request body", error.message)
            assertEquals(1, handle.sealOptions.size)
        }
    }

    @Test
    fun sealingExceptionPreservesOriginalCause() {
        val failure = IllegalStateException("Seal failed")
        val handle = PreparedHandle().apply { this.failure = failure }
        val error = assertThrows(IllegalStateException::class.java) {
            RadarPreparedFraudPayload(handle).seal(emptyMap())
        }

        assertEquals("Failed to encrypt request body", error.message)
        assertSame(failure, error.cause)
    }

    private fun bridge(hasCompatibilityMarker: Boolean = true): RadarSDKFraud = RadarSDKFraud {
        if (hasCompatibilityMarker) FraudInstance::class.java else FraudWithoutCompatibilityMarker::class.java
    }

    private fun assertFailure(bridge: RadarSDKFraud, expected: Radar.RadarStatus) {
        var calls = 0
        bridge.prepareFraudPayload(context, logger) { status, payload ->
            calls++
            assertEquals(expected, status)
            assertNull(payload)
        }
        assertEquals(1, calls)
    }

    private fun assertLogContains(message: String) {
        assertTrue(ShadowLog.getLogsForTag("RadarLogger").any { it.msg.contains(message) })
    }
}
