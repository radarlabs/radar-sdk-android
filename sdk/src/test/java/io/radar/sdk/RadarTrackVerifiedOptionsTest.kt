package io.radar.sdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RadarTrackVerifiedOptionsTest {

    @Test
    fun defaultsMatchTheExistingTrackVerifiedDefaults() {
        val options = RadarTrackVerifiedOptions()

        assertFalse(options.beacons)
        assertEquals(RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy.MEDIUM, options.desiredAccuracy)
        assertNull(options.reason)
        assertNull(options.transactionId)
        assertEquals(options, RadarTrackVerifiedOptions.builder().build())
    }

    @Test
    fun builderSetsEveryField() {
        val options = RadarTrackVerifiedOptions.builder()
            .beacons(true)
            .desiredAccuracy(RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy.HIGH)
            .reason("reason")
            .transactionId("transaction")
            .build()

        assertTrue(options.beacons)
        assertEquals(RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy.HIGH, options.desiredAccuracy)
        assertEquals("reason", options.reason)
        assertEquals("transaction", options.transactionId)
        assertEquals(
            RadarTrackVerifiedOptions(
                beacons = true,
                desiredAccuracy = RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy.HIGH,
                reason = "reason",
                transactionId = "transaction"
            ),
            options
        )
    }
}
