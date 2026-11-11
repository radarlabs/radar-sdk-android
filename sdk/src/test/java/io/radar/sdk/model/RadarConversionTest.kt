package io.radar.sdk.model

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.radar.sdk.RadarUtils
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
class RadarConversionTest {

    private val createdAtString = "2026-07-20T12:00:00.000Z"
    private val actualCreatedAtString = "2026-07-20T12:00:01.000Z"

    // The fields every conversion needs: `_id`, `type`, and `createdAt`.
    private fun baseConversionJson(): JSONObject = JSONObject()
        .put("_id", "conversion-id")
        .put("type", "opened_app")
        .put("createdAt", createdAtString)

    @Test
    fun testParsesEveryField() {
        val json = baseConversionJson()
            .put("actualCreatedAt", actualCreatedAtString)
            .put("live", true)
            .put("metadata", JSONObject().put("campaign", "spring"))

        val conversion = RadarConversion.fromJson(json)!!

        assertEquals("conversion-id", conversion._id)
        assertEquals("opened_app", conversion.name)
        assertEquals(RadarUtils.isoStringToDate(createdAtString), conversion.createdAt)
        assertEquals(RadarUtils.isoStringToDate(actualCreatedAtString), conversion.actualCreatedAt)
        assertTrue(conversion.live)
        assertEquals("spring", conversion.metadata?.getString("campaign"))
    }

    @Test
    fun testLeavesOptionalFieldsEmpty() {
        val conversion = RadarConversion.fromJson(baseConversionJson())!!

        assertNull(conversion.actualCreatedAt)
        assertFalse(conversion.live)
        assertNull(conversion.metadata)
    }

    @Test
    fun testIgnoresWronglyTypedOptionalFields() {
        val json = baseConversionJson()
            .put("actualCreatedAt", 12)
            .put("metadata", JSONArray().put("not").put("an").put("object"))

        val conversion = RadarConversion.fromJson(json)!!

        assertNull(conversion.actualCreatedAt)
        assertNull(conversion.metadata)
    }

    @Test
    fun testParsesLiveFromNumberOrString() {
        assertTrue(RadarConversion.fromJson(baseConversionJson().put("live", 1))!!.live)
        assertFalse(RadarConversion.fromJson(baseConversionJson().put("live", 0))!!.live)
        assertTrue(RadarConversion.fromJson(baseConversionJson().put("live", "true"))!!.live)
        assertFalse(RadarConversion.fromJson(baseConversionJson().put("live", "yes"))!!.live)
    }

    @Test
    fun testParsesMetadataFromJsonObjectString() {
        val json = baseConversionJson().put("metadata", "{\"revenue\": 0.2}")

        assertEquals(0.2, RadarConversion.fromJson(json)!!.metadata!!.getDouble("revenue"), 0.0)
    }

    @Test
    fun testReturnsNullWithoutRequiredFields() {
        assertNull(RadarConversion.fromJson(null))

        for (key in listOf("_id", "type", "createdAt")) {
            val json = baseConversionJson()
            json.remove(key)
            assertNull("missing $key", RadarConversion.fromJson(json))
        }

        assertNull(RadarConversion.fromJson(baseConversionJson().put("createdAt", "yesterday")))
        assertNull(RadarConversion.fromJson(baseConversionJson().put("type", 3)))
    }

    @Test
    fun testToJsonWritesHybridKeysAndName() {
        val json = baseConversionJson()
            .put("actualCreatedAt", actualCreatedAtString)
            .put("live", true)
            .put("metadata", JSONObject().put("campaign", "spring"))

        val obj = RadarConversion.fromJson(json)!!.toJson()

        assertEquals("conversion-id", obj.getString("_id"))
        assertEquals("custom", obj.getString("type"))
        assertEquals("opened_app", obj.getString("name"))
        assertTrue(obj.getBoolean("live"))
        assertEquals("spring", obj.getJSONObject("metadata").getString("campaign"))
        assertEquals(createdAtString, obj.getString("createdAt"))
        assertEquals(actualCreatedAtString, obj.getString("actualCreatedAt"))
        for (key in listOf("location", "confidence", "replayed")) {
            assertFalse("$key must be omitted", obj.has(key))
        }
    }

    @Test
    fun testToJsonOmitsMissingFields() {
        val obj = RadarConversion.fromJson(baseConversionJson())!!.toJson()

        assertFalse(obj.has("metadata"))
        assertFalse(obj.has("actualCreatedAt"))
        assertFalse(obj.getBoolean("live"))
    }

    @Test
    fun testParsesDocumentedSampleResponse() {
        // The sample "Log a conversion" response in the API docs. It has no `actualCreatedAt` and no `location`.
        val json = JSONObject()
            .put("_id", "56db1f4613012711002229f6")
            .put("createdAt", "2016-06-10T13:44:10.535Z")
            .put("live", true)
            .put("type", "conversion")
            .put("confidence", 3)
            .put("user", JSONObject().put("_id", "56db1f4613012711002229f4").put("userId", "user123"))

        val conversion = RadarConversion.fromJson(json)

        assertNotNull(conversion)
        assertEquals("conversion", conversion!!.name)
        assertNull(conversion.actualCreatedAt)
        assertTrue(conversion.live)
    }
}
