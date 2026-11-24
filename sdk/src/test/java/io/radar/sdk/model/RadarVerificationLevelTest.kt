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
class RadarVerificationLevelTest {

    @Test
    fun parsesSupportedLevels() {
        val levels = mapOf(
            "ip" to RadarVerificationLevel.IP,
            "precise" to RadarVerificationLevel.PRECISE
        )

        levels.forEach { (value, expected) ->
            assertEquals(expected, RadarVerificationLevel.fromJson(value))
            assertEquals(value, expected.value)
        }
    }

    @Test
    fun returnsNullForMissingUnknownAndNonStringLevels() {
        val invalidValues = listOf(
            null,
            "",
            "unknown",
            "IP",
            " precise ",
            JSONObject.NULL,
            true,
            0,
            JSONObject(),
            JSONArray()
        )

        invalidValues.forEach { value ->
            assertNull(
                "Unexpectedly accepted level: $value",
                RadarVerificationLevel.fromJson(value)
            )
        }
    }
}
