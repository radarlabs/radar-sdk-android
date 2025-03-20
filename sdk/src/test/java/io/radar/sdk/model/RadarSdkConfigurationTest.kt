package io.radar.sdk.model

import io.radar.sdk.Radar
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.random.Random

/**
 * Unit test [RadarFeatureSettings]
 */
@RunWith(JUnit4::class)
class RadarSdkConfigurationTest {

    private var maxConcurrentJobs = -1
    private var requiresNetwork = false
    private var usePersistence = true
    private var extendFlushReplays = false
    private var useLogPersistence = true
    private var useRadarModifiedBeacon = false
    private lateinit var jsonString: String
    private var logLevel = Radar.RadarLogLevel.INFO
    private var startTrackingOnInitialize = false
    private var trackOnceOnAppOpen = false
    private var useLocationMetadata = false
    private var useOpenedAppConversion = false
    private var useForegroundLocationUpdatedAtMsDiff = false
    private var locationManagerTimeout = 123456

    @Before
    fun setUp() {
        maxConcurrentJobs = Random.nextInt(11)
        requiresNetwork = Random.nextBoolean()
        extendFlushReplays = Random.nextBoolean()
        jsonString = """{
            "networkAny":$requiresNetwork,
            "maxConcurrentJobs":$maxConcurrentJobs,
            "usePersistence":$usePersistence,
            "useRadarModifiedBeacon":$useRadarModifiedBeacon,
            "useLogPersistence":$useLogPersistence,
            "extendFlushReplays":$extendFlushReplays,
            "logLevel":"info",
            "startTrackingOnInitialize":$startTrackingOnInitialize,
            "trackOnceOnAppOpen":$trackOnceOnAppOpen,
            "useLocationMetadata":$useLocationMetadata,
            "useOpenedAppConversion":$useOpenedAppConversion,
            "useForegroundLocationUpdatedAtMsDiff":$useForegroundLocationUpdatedAtMsDiff,
            "locationManagerTimeout":$locationManagerTimeout
        }""".trimIndent()
    }

    @Test
    fun testToJson() {
        assertEquals(
            JSONObject(jsonString).toString(),
            RadarSdkConfiguration(
                maxConcurrentJobs,
                requiresNetwork,
                usePersistence,
                extendFlushReplays,
                useLogPersistence,
                useRadarModifiedBeacon,
                logLevel,
                startTrackingOnInitialize,
                trackOnceOnAppOpen,
                useLocationMetadata,
                useOpenedAppConversion,
                useForegroundLocationUpdatedAtMsDiff,
                locationManagerTimeout
            ).toJson().toString()
        )
    }

    @Test
    fun testFromJson() {
        val settings = RadarSdkConfiguration.fromJson(JSONObject(jsonString))
        assertEquals(maxConcurrentJobs, settings.maxConcurrentJobs)
        assertEquals(requiresNetwork, settings.schedulerRequiresNetwork)
        assertEquals(usePersistence, settings.usePersistence)
        assertEquals(extendFlushReplays, settings.extendFlushReplays)
        assertEquals(useLogPersistence, settings.useLogPersistence)
        assertEquals(useRadarModifiedBeacon, settings.useRadarModifiedBeacon)
        assertEquals(logLevel, settings.logLevel)
        assertEquals(startTrackingOnInitialize, settings.startTrackingOnInitialize)
        assertEquals(trackOnceOnAppOpen, settings.trackOnceOnAppOpen)
        assertEquals(useLocationMetadata, settings.useLocationMetadata)
        assertEquals(useOpenedAppConversion, settings.useOpenedAppConversion)
        assertEquals(useForegroundLocationUpdatedAtMsDiff, settings.useForegroundLocationUpdatedAtMsDiff)
        assertEquals(locationManagerTimeout, settings.locationManagerTimeout)
    }

    @Test
    fun testDefault() {
        val settings = RadarSdkConfiguration.fromJson(null)
        assertEquals(1, settings.maxConcurrentJobs)
        assertFalse(settings.schedulerRequiresNetwork)
        assertFalse(settings.usePersistence)
        assertFalse(settings.extendFlushReplays)
        assertFalse(settings.useLogPersistence)
        assertFalse(settings.useRadarModifiedBeacon)
        assertEquals(Radar.RadarLogLevel.INFO, settings.logLevel)
        assertFalse(settings.startTrackingOnInitialize)
        assertFalse(settings.trackOnceOnAppOpen)
        assertFalse(settings.useLocationMetadata)
        assertTrue(settings.useOpenedAppConversion)
        assertFalse(settings.useForegroundLocationUpdatedAtMsDiff)
        assertEquals(0, settings.locationManagerTimeout)
    }

    private fun String.removeWhitespace(): String = replace("\\s".toRegex(), "")

}
