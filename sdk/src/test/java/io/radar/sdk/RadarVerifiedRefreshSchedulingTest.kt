package io.radar.sdk

import android.content.Context
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.radar.sdk.model.RadarVerifiedLocationToken
import java.time.Duration
import java.util.concurrent.TimeUnit
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class RadarVerifiedRefreshSchedulingTest {

    @Test
    fun idUsesConfiguredIntervalWithoutTokenExpiry() {
        assertScheduledAfter(
            token = token("id"),
            elapsedSeconds = 0,
            expectedSeconds = 50
        )
    }

    @Test
    fun jwsAndJweStillUseRemainingTokenLifetime() {
        listOf("jws", "jwe").forEach { format ->
            assertScheduledAfter(
                token = token(format),
                elapsedSeconds = 5,
                expectedSeconds = 15
            )
        }
    }

    private fun token(format: String): RadarVerifiedLocationToken {
        val obj = JSONObject()
            .put("format", format)
            .put("_id", "evaluation-id")

        if (format != "id") {
            obj.put("token", "opaque-token")
            obj.put("expiresAt", "2030-01-01T00:00:00.000Z")
            obj.put("expiresIn", 30)
            obj.put("passed", true)
        }

        if (format == "jws") {
            obj.put("user", JSONObject().put("_id", "user-id"))
            obj.put("events", JSONArray())
        }

        return requireNotNull(RadarVerifiedLocationToken.fromJson(obj))
    }

    private fun assertScheduledAfter(
        token: RadarVerifiedLocationToken,
        elapsedSeconds: Long,
        expectedSeconds: Long
    ) {
        val context: Context = ApplicationProvider.getApplicationContext()
        val logger = RadarLogger(context)
        Radar.logger = logger

        val manager = RadarVerificationManager(
            context,
            logger,
            RadarRevealRiskManager(context, logger)
        )
        manager.started = true

        val receivedAt = SystemClock.elapsedRealtime()
        ShadowSystemClock.advanceBy(Duration.ofSeconds(elapsedSeconds))

        setField(manager, "startedInterval", 60)
        setField(manager, "lastToken", token)
        setField(manager, "lastTokenElapsedRealtime", receivedAt)

        // Observe the scheduled callback without starting location or network work.
        var firings = 0
        setField(manager, "runnable", Runnable { firings++ })

        manager.scheduleNextIntervalWithLastToken()

        val looper = shadowOf(Looper.getMainLooper())
        looper.idleFor(expectedSeconds - 1, TimeUnit.SECONDS)
        assertEquals("Refresh fired too early", 0, firings)

        looper.idleFor(1, TimeUnit.SECONDS)
        assertEquals("Refresh must fire at the expected interval", 1, firings)
    }

    private fun setField(
        manager: RadarVerificationManager,
        name: String,
        value: Any
    ) {
        RadarVerificationManager::class.java.getDeclaredField(name).apply {
            isAccessible = true
            set(manager, value)
        }
    }
}
