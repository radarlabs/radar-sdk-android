package io.radar.sdk.model

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class RadarResponseFormatTest {

    @Test
    fun missingFormatDefaultsToJws() {
        val response = JSONObject()

        assertEquals(
            RadarResponseFormat.JWS,
            RadarResponseFormat.fromJson(response.opt("format"))
        )
    }

    @Test
    fun parsesSupportedFormats() {
        val formats = mapOf(
            "jws" to RadarResponseFormat.JWS,
            "jwe" to RadarResponseFormat.JWE,
            "id" to RadarResponseFormat.ID
        )

        formats.forEach { (value, expected) ->
            assertEquals(expected, RadarResponseFormat.fromJson(value))
            assertEquals(value, expected.value)
        }
    }

    @Test
    fun rejectsUnsupportedAndNonStringFormats() {
        val invalidValues = listOf(
            "",
            "unknown",
            "JWE",
            " jwe ",
            JSONObject.NULL,
            true,
            0,
            JSONObject(),
            JSONArray()
        )

        invalidValues.forEach { value ->
            assertNull(
                "Unexpectedly accepted format: $value",
                RadarResponseFormat.fromJson(value)
            )
        }
    }
}
