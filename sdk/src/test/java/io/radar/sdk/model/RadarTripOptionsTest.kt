package io.radar.sdk.model

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.radar.sdk.RadarTrackingOptions
import io.radar.sdk.RadarTripOptions
import junit.framework.Assert.assertNull
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.*

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class RadarTripOptionsTest {

    @Test
    fun testToFromJsonWithNonZeroApproachingThreshold() {
        val tripOptions = RadarTripOptions(
            externalId = "externalId",
            destinationGeofenceTag = "destinationGeofenceTag",
            destinationGeofenceExternalId = "destinationGeofenceExternalId",
            approachingThreshold = 5
        )
        val jsonObject = tripOptions.toJson()

        assertEquals("externalId", jsonObject["externalId"])
        assertEquals("destinationGeofenceTag", jsonObject["destinationGeofenceTag"])
        assertEquals("destinationGeofenceExternalId", jsonObject["destinationGeofenceExternalId"])
        assertEquals(5, jsonObject["approachingThreshold"])

        // Deserialize it and compare them again
        val deserializedTripOptions = RadarTripOptions.fromJson(jsonObject)
        assertEquals(tripOptions.externalId, deserializedTripOptions.externalId)
        assertEquals(tripOptions.destinationGeofenceTag, deserializedTripOptions.destinationGeofenceTag)
        assertEquals(tripOptions.destinationGeofenceExternalId, deserializedTripOptions.destinationGeofenceExternalId)
        assertEquals(tripOptions.approachingThreshold, deserializedTripOptions.approachingThreshold)
    }

    @Test
    fun testToFromJsonWithZeroApproachingThresholdIgnoresThreshold() {
        val tripOptions = RadarTripOptions(
            externalId = "externalId",
            destinationGeofenceTag = "destinationGeofenceTag",
            destinationGeofenceExternalId = "destinationGeofenceExternalId",
            approachingThreshold = 0 // values less than 1 aren't serialized
        )
        val jsonObject = tripOptions.toJson()

        assertEquals("externalId", jsonObject["externalId"])
        assertEquals("destinationGeofenceTag", jsonObject["destinationGeofenceTag"])
        assertEquals("destinationGeofenceExternalId", jsonObject["destinationGeofenceExternalId"])
        assertFalse(jsonObject.has("approachingThreshold"))

        // Deserialize it and compare them again
        val deserializedTripOptions = RadarTripOptions.fromJson(jsonObject)
        assertEquals(tripOptions.externalId, deserializedTripOptions.externalId)
        assertEquals(tripOptions.destinationGeofenceTag, deserializedTripOptions.destinationGeofenceTag)
        assertEquals(tripOptions.destinationGeofenceExternalId, deserializedTripOptions.destinationGeofenceExternalId)
        assertEquals(tripOptions.approachingThreshold, deserializedTripOptions.approachingThreshold) // both are 0
    }

    @Test
    fun testToFromJsonWithNegativeApproachingThresholdIgnoresThreshold() {
        val tripOptions = RadarTripOptions(
            externalId = "externalId",
            destinationGeofenceTag = "destinationGeofenceTag",
            destinationGeofenceExternalId = "destinationGeofenceExternalId",
            approachingThreshold = -5 // negative values aren't serialized
        )
        val jsonObject = tripOptions.toJson()

        assertEquals("externalId", jsonObject["externalId"])
        assertEquals("destinationGeofenceTag", jsonObject["destinationGeofenceTag"])
        assertEquals("destinationGeofenceExternalId", jsonObject["destinationGeofenceExternalId"])
        assertFalse(jsonObject.has("approachingThreshold"))

        // Deserialize it and compare them again
        val deserializedTripOptions = RadarTripOptions.fromJson(jsonObject)
        assertEquals(tripOptions.externalId, deserializedTripOptions.externalId)
        assertEquals(tripOptions.destinationGeofenceTag, deserializedTripOptions.destinationGeofenceTag)
        assertEquals(tripOptions.destinationGeofenceExternalId, deserializedTripOptions.destinationGeofenceExternalId)
        assertNotEquals(tripOptions.approachingThreshold, deserializedTripOptions.approachingThreshold)
    }

    @Test
    fun testIsEqualsWithDifferentApproachingThresholdsReturnsFalse() {
        val tripOptions1 = RadarTripOptions(
            externalId = "externalId",
            destinationGeofenceTag = "destinationGeofenceTag",
            destinationGeofenceExternalId = "destinationGeofenceExternalId",
            approachingThreshold = -5
        )

        val tripOptions2 = RadarTripOptions(
            externalId = "externalId",
            destinationGeofenceTag = "destinationGeofenceTag",
            destinationGeofenceExternalId = "destinationGeofenceExternalId",
            approachingThreshold = 11
        )

        assertNotEquals(tripOptions1, tripOptions2)
    }

    @Test
    fun testIsEqualsWithUnsetApproachingThresholdsReturnsFalse() {
        val tripOptions1 = RadarTripOptions(
            externalId = "externalId",
            destinationGeofenceTag = "destinationGeofenceTag",
            destinationGeofenceExternalId = "destinationGeofenceExternalId",
        )

        val tripOptions2 = RadarTripOptions(
            externalId = "externalId",
            destinationGeofenceTag = "destinationGeofenceTag",
            destinationGeofenceExternalId = "destinationGeofenceExternalId",
            approachingThreshold = 11
        )

        assertNotEquals(tripOptions1, tripOptions2)
    }

    @Test
    fun testIsEqualsWithSameApproachingThresholdsReturnsTrue() {
        val tripOptions1 = RadarTripOptions(
            externalId = "externalId",
            destinationGeofenceTag = "destinationGeofenceTag",
            destinationGeofenceExternalId = "destinationGeofenceExternalId",
            approachingThreshold = 11
        )

        val tripOptions2 = RadarTripOptions(
            externalId = "externalId",
            destinationGeofenceTag = "destinationGeofenceTag",
            destinationGeofenceExternalId = "destinationGeofenceExternalId",
            approachingThreshold = 11
        )

        assertEquals(tripOptions1, tripOptions2)
    }

}