package io.radar.sdk.model

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class RadarUserInsightsEventTest {

    @Test
    fun testUserInsightsEventTypesRoundTrip() {
        val cases = mapOf(
            "user.entered_home" to RadarEvent.RadarEventType.USER_ENTERED_HOME,
            "user.exited_home" to RadarEvent.RadarEventType.USER_EXITED_HOME,
            "user.entered_work" to RadarEvent.RadarEventType.USER_ENTERED_WORK,
            "user.exited_work" to RadarEvent.RadarEventType.USER_EXITED_WORK,
            "user.started_traveling" to RadarEvent.RadarEventType.USER_STARTED_TRAVELING,
            "user.stopped_traveling" to RadarEvent.RadarEventType.USER_STOPPED_TRAVELING,
            "user.started_commuting" to RadarEvent.RadarEventType.USER_STARTED_COMMUTING,
            "user.stopped_commuting" to RadarEvent.RadarEventType.USER_STOPPED_COMMUTING
        )

        cases.forEach { (typeString, eventType) ->
            val event = RadarEvent.fromJson(eventJson(typeString))

            assertNotNull(event)
            assertEquals(eventType, event?.type)
            assertEquals(typeString, event?.toJson()?.getString("type"))
        }
    }

    private fun eventJson(type: String): JSONObject = JSONObject()
        .put("_id", "evt_test")
        .put("createdAt", "2026-06-26T12:00:00.000Z")
        .put("actualCreatedAt", "2026-06-26T12:00:00.000Z")
        .put("live", false)
        .put("type", type)
        .put("confidence", 3)
        .put(
            "location",
            JSONObject()
                .put("type", "Point")
                .put("coordinates", JSONArray().put(-73.975365).put(40.783825))
        )
}
