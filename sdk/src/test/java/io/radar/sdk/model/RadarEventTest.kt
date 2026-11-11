package io.radar.sdk.model

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.radar.sdk.RadarUtils
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class RadarEventTest {

    private fun eventJson(): JSONObject = JSONObject()
        .put("_id", "event-id")
        .put("createdAt", "2026-07-20T12:00:00.000Z")
        .put("actualCreatedAt", "2026-07-20T12:00:01.000Z")
        .put("live", true)
        .put("type", "user.entered_geofence")
        .put("location", locationJson(JSONArray().put(-73.975365).put(40.783825)))

    private fun locationJson(coordinates: Any): JSONObject = JSONObject()
        .put("type", "Point")
        .put("coordinates", coordinates)

    @Test
    fun testParsesRequiredFields() {
        val event = RadarEvent.fromJson(eventJson())!!

        assertEquals("event-id", event._id)
        assertEquals(RadarUtils.isoStringToDate("2026-07-20T12:00:00.000Z"), event.createdAt)
        assertEquals(RadarUtils.isoStringToDate("2026-07-20T12:00:01.000Z"), event.actualCreatedAt)
        assertEquals(-73.975365, event.location.longitude, 0.0)
        assertEquals(40.783825, event.location.latitude, 0.0)
        assertEquals(event.createdAt.time, event.location.time)
    }

    @Test
    fun testParsesMetadataFromObjectOrString() {
        val fromObject = RadarEvent.fromJson(eventJson().put("metadata", JSONObject().put("foo", "bar")))
        val fromString = RadarEvent.fromJson(eventJson().put("metadata", "{\"foo\": \"bar\"}"))

        assertEquals("bar", fromObject!!.metadata!!.getString("foo"))
        assertEquals("bar", fromString!!.metadata!!.getString("foo"))
        assertNull(RadarEvent.fromJson(eventJson())!!.metadata)
    }

    @Test
    fun testReturnsNullWithoutRequiredFields() {
        assertNull(RadarEvent.fromJson(null as JSONObject?))

        for (key in listOf("_id", "createdAt", "actualCreatedAt", "location")) {
            val json = eventJson()
            json.remove(key)
            assertNull("missing $key", RadarEvent.fromJson(json))
        }
    }

    @Test
    fun testReturnsNullForInvalidDates() {
        assertNull(RadarEvent.fromJson(eventJson().put("createdAt", "yesterday")))
        assertNull(RadarEvent.fromJson(eventJson().put("actualCreatedAt", "yesterday")))
        assertNull(RadarEvent.fromJson(eventJson().put("actualCreatedAt", 12)))
    }

    @Test
    fun testReturnsNullForMalformedLocation() {
        val badLocations = listOf(
            JSONObject().put("type", "Point"),
            locationJson(JSONArray()),
            locationJson(JSONArray().put(-73.975365)),
            locationJson(JSONArray().put("-73.975365").put("40.783825")),
            locationJson(JSONArray().put(-73.975365).put(JSONObject.NULL)),
            locationJson(JSONArray().put(1).put(2).put(3).put(4)),
            locationJson("not an array")
        )

        for (location in badLocations) {
            assertNull("location $location", RadarEvent.fromJson(eventJson().put("location", location)))
        }
    }

    @Test
    fun testParsesLocationWithAltitude() {
        val json = eventJson().put("location", locationJson(JSONArray().put(-73.975365).put(40.783825).put(10)))

        assertNotNull(RadarEvent.fromJson(json))
    }

    @Test
    fun testConversionResponseIsNotAnEvent() {
        // A `POST /v1/events` response has no location, so it parses only as `RadarConversion`.
        val json = JSONObject()
            .put("_id", "56db1f4613012711002229f6")
            .put("createdAt", "2016-06-10T13:44:10.535Z")
            .put("actualCreatedAt", "2016-06-10T13:44:10.535Z")
            .put("live", true)
            .put("type", "conversion")

        assertNull(RadarEvent.fromJson(json))
        assertNotNull(RadarConversion.fromJson(json))
    }

    @Test
    fun testArrayDropsEventsThatDoNotParse() {
        val invalid = eventJson()
        invalid.remove("location")
        val arr = JSONArray().put(eventJson()).put(invalid)

        val events = RadarEvent.fromJson(arr)

        assertEquals(1, events!!.size)
        assertEquals("event-id", events[0]._id)
    }

    @Test
    fun testToJsonRoundTrip() {
        val obj = RadarEvent.fromJson(eventJson())!!.toJson()

        assertEquals("event-id", obj.getString("_id"))
        assertEquals("2026-07-20T12:00:00.000Z", obj.getString("createdAt"))
        assertEquals("2026-07-20T12:00:01.000Z", obj.getString("actualCreatedAt"))
        val coordinates = obj.getJSONObject("location").getJSONArray("coordinates")
        assertEquals(-73.975365, coordinates.getDouble(0), 0.0)
        assertEquals(40.783825, coordinates.getDouble(1), 0.0)
        assertNotNull(RadarEvent.fromJson(obj))
    }
}
