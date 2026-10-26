package io.radar.sdk.model

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class RadarTripTest {

    private fun tripJson(extra: String = ""): JSONObject = JSONObject("{\"_id\":\"tripId\",\"externalId\":\"externalId\",\"status\":\"started\"$extra}")

    @Test
    fun testToJsonWithoutEtaDoesNotThrow() {
        val trip = RadarTrip.fromJson(tripJson())!!

        assertNull(trip.etaDistance)
        assertNull(trip.etaDuration)

        val obj = trip.toJson()
        assertFalse(obj.has("eta"))

        val roundTripped = RadarTrip.fromJson(obj)!!
        assertNull(roundTripped.etaDistance)
        assertNull(roundTripped.etaDuration)
    }

    @Test
    fun testToJsonWithEmptyEtaDoesNotThrow() {
        val trip = RadarTrip.fromJson(tripJson(",\"eta\":{}"))!!

        assertNull(trip.etaDistance)
        assertNull(trip.etaDuration)
        assertFalse(trip.toJson().has("eta"))
    }

    @Test
    fun testToJsonWithNullEtaValuesDoesNotThrow() {
        val trip = RadarTrip.fromJson(tripJson(",\"eta\":{\"distance\":null,\"duration\":null}"))!!

        assertNull(trip.etaDistance)
        assertNull(trip.etaDuration)
        assertFalse(trip.toJson().has("eta"))
    }

    @Test
    fun testToJsonWithNaNEtaDoesNotThrow() {
        val trip = RadarTrip(
            _id = "tripId",
            externalId = "externalId",
            metadata = null,
            destinationGeofenceTag = null,
            destinationGeofenceExternalId = null,
            destinationLocation = null,
            mode = null,
            etaDistance = Double.NaN,
            etaDuration = Double.NaN,
            status = RadarTrip.RadarTripStatus.STARTED
        )

        assertFalse(trip.toJson().has("eta"))
    }

    @Test
    fun testToFromJsonWithEta() {
        val trip = RadarTrip.fromJson(tripJson(",\"eta\":{\"distance\":1000.5,\"duration\":12.25}"))!!

        assertEquals(1000.5, trip.etaDistance!!, 0.0)
        assertEquals(12.25, trip.etaDuration!!, 0.0)

        val roundTripped = RadarTrip.fromJson(trip.toJson())!!
        assertEquals(1000.5, roundTripped.etaDistance!!, 0.0)
        assertEquals(12.25, roundTripped.etaDuration!!, 0.0)
    }

    @Test
    fun testFromJsonWithoutDestinationReturnsNullDestination() {
        val trip = RadarTrip.fromJson(tripJson())!!

        assertNull(trip.destinationGeofenceTag)
        assertNull(trip.destinationGeofenceExternalId)
        assertNull(trip.destinationLocation)

        val roundTripped = RadarTrip.fromJson(trip.toJson())!!
        assertNull(roundTripped.destinationGeofenceTag)
        assertNull(roundTripped.destinationGeofenceExternalId)
    }

    @Test
    fun testFromJsonWithDestination() {
        val trip = RadarTrip.fromJson(
            tripJson(
                ",\"destinationGeofenceTag\":\"store\",\"destinationGeofenceExternalId\":\"123\"," +
                    "\"destinationLocation\":{\"type\":\"Point\",\"coordinates\":[-73.99,40.73]}"
            )
        )!!

        assertEquals("store", trip.destinationGeofenceTag)
        assertEquals("123", trip.destinationGeofenceExternalId)
        assertNotNull(trip.destinationLocation)
        assertEquals(40.73, trip.destinationLocation!!.latitude, 0.0)
        assertEquals(-73.99, trip.destinationLocation!!.longitude, 0.0)
    }

    @Test
    fun testFromJsonWithEmptyDestinationCoordinatesReturnsNullLocation() {
        val trip = RadarTrip.fromJson(tripJson(",\"destinationLocation\":{\"coordinates\":[]}"))!!

        assertNull(trip.destinationLocation)
        trip.toJson()
    }
}
