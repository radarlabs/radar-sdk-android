package io.radar.sdk

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RadarTrackingOptionsTest {

    @Test
    fun testEquals() {
        assertEquals(RadarTrackingOptions.EFFICIENT, RadarTrackingOptions.fromJson(RadarTrackingOptions.EFFICIENT.toJson()))
        assertEquals(RadarTrackingOptions.RESPONSIVE, RadarTrackingOptions.fromJson(RadarTrackingOptions.RESPONSIVE.toJson()))
        assertEquals(RadarTrackingOptions.CONTINUOUS, RadarTrackingOptions.fromJson(RadarTrackingOptions.CONTINUOUS.toJson()))
    }
}