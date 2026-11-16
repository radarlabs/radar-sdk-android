package io.radar.sdk.model

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class RadarEventTest {

    @Test
    fun testLocationParsed() {
        val event = RadarEvent.fromJson(eventJson())

        assertNotNull(event?.location)
        assertEquals(-73.975365, event!!.location!!.longitude, 0.000001)
        assertEquals(40.783825, event.location!!.latitude, 0.000001)
        assertNotNull(event.actualCreatedAt)

        val json = event.toJson()
        assertTrue(json.has("location"))
        assertTrue(json.has("actualCreatedAt"))
    }

    @Test
    fun testLocationWithAltitudeParsed() {
        val obj = eventJson().put("location", point(JSONArray().put(-73.975365).put(40.783825).put(10.0)))

        assertNotNull(RadarEvent.fromJson(obj)?.location)
    }

    @Test
    fun testMissingLocationIsNull() {
        val obj = eventJson()
        obj.remove("location")

        val event = RadarEvent.fromJson(obj)

        assertNotNull(event)
        assertNull(event?.location)
        assertFalse(event!!.toJson().has("location"))
    }

    @Test
    fun testJsonNullLocationIsNull() {
        val event = RadarEvent.fromJson(eventJson().put("location", JSONObject.NULL))

        assertNotNull(event)
        assertNull(event?.location)
    }

    @Test
    fun testMalformedLocationFailsEvent() {
        val malformed = listOf<Any>(
            "not an object",
            42,
            JSONArray().put(-73.975365).put(40.783825),
            JSONObject().put("type", "Point"),
            point("not an array"),
            point(JSONArray()),
            point(JSONArray().put(-73.975365)),
            point(JSONArray().put(1.0).put(2.0).put(3.0).put(4.0)),
            point(JSONArray().put("-73.975365").put(40.783825)),
            point(JSONArray().put(-73.975365).put(JSONObject.NULL))
        )

        malformed.forEach { location ->
            assertNull("location $location", RadarEvent.fromJson(eventJson().put("location", location)))
        }
    }

    @Test
    fun testMalformedLocationDropsOnlyThatEventFromArray() {
        val arr = JSONArray()
            .put(eventJson().put("_id", "evt_good"))
            .put(eventJson().put("_id", "evt_bad").put("location", "not an object"))
            .put(eventJson().put("_id", "evt_no_location").apply { remove("location") })

        val events = RadarEvent.fromJson(arr)

        assertEquals(listOf("evt_good", "evt_no_location"), events?.map { it._id })
    }

    @Test
    fun testMissingActualCreatedAtIsNull() {
        val obj = eventJson()
        obj.remove("actualCreatedAt")

        val event = RadarEvent.fromJson(obj)

        assertNotNull(event)
        assertNull(event?.actualCreatedAt)
        assertFalse(event!!.toJson().has("actualCreatedAt"))
    }

    @Test
    fun testInvalidActualCreatedAtIsNull() {
        val event = RadarEvent.fromJson(eventJson().put("actualCreatedAt", "not a date"))

        assertNotNull(event)
        assertNull(event?.actualCreatedAt)
    }

    @Test
    fun testConversionResponseParsed() {
        // The documented response of POST /v1/events has no location and no actualCreatedAt.
        val obj = JSONObject()
            .put("_id", "evt_conversion")
            .put("createdAt", "2026-06-26T12:00:00.000Z")
            .put("live", false)
            .put("type", "in_app_purchase")
            .put("confidence", 3)
            .put(
                "user",
                JSONObject()
                    .put("_id", "usr_test")
                    .put("userId", "user_test")
                    .put("deviceId", "device_test")
            )

        val event = RadarEvent.fromJson(obj)

        assertNotNull(event)
        assertEquals("evt_conversion", event?._id)
        assertEquals(RadarEvent.RadarEventType.CONVERSION, event?.type)
        assertEquals("in_app_purchase", event?.conversionName)
        assertEquals(RadarEvent.RadarEventConfidence.HIGH, event?.confidence)
        assertFalse(event!!.live)
        assertNull(event.location)
        assertNull(event.actualCreatedAt)

        val json = event.toJson()
        assertFalse(json.has("location"))
        assertFalse(json.has("actualCreatedAt"))
        assertTrue(json.has("createdAt"))
    }

    private fun point(coordinates: Any): JSONObject = JSONObject()
        .put("type", "Point")
        .put("coordinates", coordinates)

    private fun eventJson(): JSONObject = JSONObject()
        .put("_id", "evt_test")
        .put("createdAt", "2026-06-26T12:00:00.000Z")
        .put("actualCreatedAt", "2026-06-26T12:00:00.000Z")
        .put("live", false)
        .put("type", "user.entered_geofence")
        .put("confidence", 3)
        .put("location", point(JSONArray().put(-73.975365).put(40.783825)))
}
