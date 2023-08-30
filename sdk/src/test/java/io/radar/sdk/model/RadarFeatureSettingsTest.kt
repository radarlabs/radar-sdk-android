package io.radar.sdk.model

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.random.Random

/**
 * Unit test [RadarFeatureSettings]
 */
@RunWith(JUnit4::class)
class RadarFeatureSettingsTest {

    private var maxConcurrentJobs = -1
    private var requiresNetwork = false
    private var usePersistence = true
    private var extendFlushReplays = false
    private lateinit var jsonString: String

    @Before
    fun setUp() {
        maxConcurrentJobs = Random.nextInt(11)
        requiresNetwork = Random.nextBoolean()
        extendFlushReplays = Random.nextBoolean()
        jsonString = """{
            "networkAny":$requiresNetwork,
            "maxConcurrentJobs":$maxConcurrentJobs,
            "usePersistence":$usePersistence,
            "extendFlushReplays":$extendFlushReplays
        }""".trimIndent()
    }

    @Test
    fun testToJson() {
        assertEquals(
            jsonString.removeWhitespace(),
            RadarFeatureSettings(maxConcurrentJobs, requiresNetwork, usePersistence, extendFlushReplays).toJson().toString().removeWhitespace()
        )
    }

    @Test
    fun testFromJson() {
        val settings = RadarFeatureSettings.fromJson(JSONObject(jsonString))
        assertEquals(maxConcurrentJobs, settings.maxConcurrentJobs)
        assertEquals(requiresNetwork, settings.schedulerRequiresNetwork)
        assertEquals(usePersistence, settings.usePersistence)
        assertEquals(extendFlushReplays, settings.extendFlushReplays)
    }

    @Test
    fun testDefault() {
        val settings = RadarFeatureSettings.default()
        assertEquals(1, settings.maxConcurrentJobs)
        assertFalse(settings.schedulerRequiresNetwork)
        assertFalse(settings.usePersistence)
        assertFalse(settings.extendFlushReplays)
    }

    private fun String.removeWhitespace(): String = replace("\\s".toRegex(), "")

}
