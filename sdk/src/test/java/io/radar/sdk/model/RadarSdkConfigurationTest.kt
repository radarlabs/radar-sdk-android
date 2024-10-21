package io.radar.sdk.model

import io.radar.sdk.Radar
import io.radar.sdk.RadarTrackingOptions
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    private var useOfflineRTOUpdates = false

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
            "useOfflineRTOUpdates":$useOfflineRTOUpdates,
            "inGeofenceTrackingOptions": {
                "desiredStoppedUpdateInterval": 0,
                "fastestStoppedUpdateInterval": 0,
                "desiredMovingUpdateInterval": 150,
                "fastestMovingUpdateInterval": 30,
                "desiredSyncInterval": 20,
                "desiredAccuracy": "medium",
                "stopDuration": 140,
                "stopDistance": 70,

                "replay": "stops",
                "sync": "all",
                "useStoppedGeofence": true,
                "stoppedGeofenceRadius": 100,
                "useMovingGeofence": true,
                "movingGeofenceRadius": 100,
                "syncGeofences": true,
                "syncGeofencesLimit": 10,
                "foregroundServiceEnabled": false,
                "beacons": false
            }
            ,
            "defaultTrackingOptions": {
                "desiredStoppedUpdateInterval": 3600,
                "fastestStoppedUpdateInterval": 1200,
                "desiredMovingUpdateInterval": 1200,
                "fastestMovingUpdateInterval": 360,
                "desiredSyncInterval": 140,
                "desiredAccuracy": "medium",
                "stopDuration": 140,
                "stopDistance": 70,

                "replay": "stops",
                "sync": "all",
                "useStoppedGeofence": false,
                "stoppedGeofenceRadius": 0,
                "useMovingGeofence": false,
                "movingGeofenceRadius": 0,
                "syncGeofences": true,
                "syncGeofencesLimit": 10,
                "foregroundServiceEnabled": false,
                "beacons": false
            }
            ,
            "onTripTrackingOptions": {
                "desiredStoppedUpdateInterval": 30,
                "fastestStoppedUpdateInterval": 30,
                "desiredMovingUpdateInterval": 30,
                "fastestMovingUpdateInterval": 30,
                "desiredSyncInterval": 20,
                "desiredAccuracy": "high",
                "stopDuration": 140,
                "stopDistance": 70,

                "replay": "none",
                "sync": "all",
                "useStoppedGeofence": false,
                "stoppedGeofenceRadius": 0,
                "useMovingGeofence": false,
                "movingGeofenceRadius": 0,
                "syncGeofences": true,
                "syncGeofencesLimit": 0,
                "foregroundServiceEnabled": true,
                "beacons": false
            }
            ,
            "inGeofenceTrackingOptionsTags": ["venue"]

        }""".trimIndent()
    }

    @Test
    fun testToJson() {
        assertEquals(
            JSONObject(jsonString).toMap(),
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
                useOfflineRTOUpdates,
                RadarTrackingOptions.RESPONSIVE,
                RadarTrackingOptions.EFFICIENT,
                RadarTrackingOptions.CONTINUOUS,
                setOf("venue")
            ).toJson().toMap()
        )
    }

    fun JSONObject.toMap(): Map<String, Any?> = keys().asSequence().associateWith { key ->
        when (val value = this[key]) {
            is JSONArray -> value.toList()
            is JSONObject -> value.toMap()
            else -> value
        }
    }

    fun JSONArray.toList(): List<Any?> = (0 until length()).map { index ->
        when (val value = get(index)) {
            is JSONArray -> value.toList()
            is JSONObject -> value.toMap()
            else -> value
        }
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
        assertEquals(useOfflineRTOUpdates, settings.useOfflineRTOUpdates)
        assertEquals(RadarTrackingOptions.RESPONSIVE, settings.inGeofenceTrackingOptions)
        assertEquals(RadarTrackingOptions.EFFICIENT, settings.defaultTrackingOptions)
        assertEquals(RadarTrackingOptions.CONTINUOUS, settings.onTripTrackingOptions)
        assertEquals(setOf("venue"), settings.inGeofenceTrackingOptionsTags)
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
        assertFalse(settings.useOfflineRTOUpdates)
        assertNull(settings.inGeofenceTrackingOptions)
        assertNull(settings.defaultTrackingOptions)
        assertNull(settings.onTripTrackingOptions)
        assertEquals(emptySet<String>(), settings.inGeofenceTrackingOptionsTags)
    }

    private fun String.removeWhitespace(): String = replace("\\s".toRegex(), "")

}
