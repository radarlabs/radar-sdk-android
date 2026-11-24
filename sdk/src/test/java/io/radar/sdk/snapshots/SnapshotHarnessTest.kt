package io.radar.sdk.snapshots

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.radar.sdk.model.RadarAddress
import io.radar.sdk.model.RadarCoordinate
import java.util.Date
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Self-tests for the snapshot harness: the canonical JSON writer and [assertFullyPopulated]. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class SnapshotHarnessTest {

    @Test
    fun canonicalJson_formatsValues() {
        val obj = JSONObject()
            .put("b", 42.0)
            .put("a", 42)
            .put("long", 1737479963000L)
            .put("float", 0.1f as Any) // Android's org.json has no put(String, float)
            .put("tiny", 1.0e-7)
            .put("s", "a/b \"q\" \\ \n\t\u0001 🇺🇸")
            .put("t", true)
            .put("n", JSONObject.NULL)
            .put("emptyObj", JSONObject())
            .put("emptyArr", JSONArray())
            .put("arr", JSONArray().put(1).put("x"))
        val expected = """
            {
              "a": 42,
              "arr": [
                1,
                "x"
              ],
              "b": 42.0,
              "emptyArr": [],
              "emptyObj": {},
              "float": 0.1,
              "long": 1737479963000,
              "n": null,
              "s": "a/b \"q\" \\ \n\t\u0001 🇺🇸",
              "t": true,
              "tiny": 1.0e-7
            }
        """.trimIndent() + "\n"
        assertEquals(expected, CanonicalJson.write(obj))
    }

    @Test
    fun canonicalJson_sortsByCodePoint() {
        // U+FF61 sorts before U+1F600 by code point, but after it by UTF-16 code unit.
        val obj = JSONObject().put("😀", 1).put("｡", 2)
        assertEquals("{\n  \"｡\": 2,\n  \"😀\": 1\n}\n", CanonicalJson.write(obj))
    }

    @Test
    fun canonicalJson_failsOnUnknownType() {
        val obj = JSONObject().put("outer", JSONObject().put("date", Date(0)))
        val error = assertThrows(IllegalArgumentException::class.java) { CanonicalJson.write(obj) }
        assertTrue(error.message!!, error.message!!.contains("java.util.Date at $.outer.date"))
    }

    @Test
    fun lineDiff_marksChangedLines() {
        assertEquals("  a\n- b\n+ c\n  d\n", Snapshots.lineDiff("a\nb\nd", "a\nc\nd"))
    }

    private class Holder(
        val name: String?,
        val coordinate: RadarCoordinate?,
        val tags: List<String>
    )

    @Test
    fun assertFullyPopulated_passesWhenSet() {
        assertFullyPopulated(Holder("x", RadarCoordinate(1.0, 2.0), listOf("t")))
    }

    @Test
    fun assertFullyPopulated_reportsNullsAndEmpties() {
        val error = assertThrows(AssertionError::class.java) {
            assertFullyPopulated(Holder("", null, emptyList()))
        }
        val message = error.message!!
        assertTrue(message, message.contains("Holder.name is empty string"))
        assertTrue(message, message.contains("Holder.coordinate is null"))
        assertTrue(message, message.contains("Holder.tags is empty list"))
    }

    @Test
    fun assertFullyPopulated_catchesFieldsMissingFromGeocodeFixture() {
        // geocode.json leaves out several fields. optString turns them into "", so the check
        // has to treat empty strings as unset to see them.
        val address = RadarAddress.fromJson(Snapshots.fixture("geocode.json").getJSONArray("addresses").getJSONObject(0))!!
        val message = assertThrows(AssertionError::class.java) { assertFullyPopulated(address) }.message!!
        assertTrue(message, message.contains("RadarAddress.dma is empty string"))
        assertTrue(message, message.contains("RadarAddress.categories is null"))
        assertTrue(message, message.contains("RadarAddress.resultId is null"))
    }

    @Test
    fun assertFullyPopulated_honorsAllowedNull() {
        assertFullyPopulated(Holder("x", null, listOf("t")), allowedNull = setOf("Holder.coordinate"))
    }
}
