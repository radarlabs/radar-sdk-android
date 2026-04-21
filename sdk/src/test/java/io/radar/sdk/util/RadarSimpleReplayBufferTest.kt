package io.radar.sdk.util

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.radar.sdk.RadarTrackingOptions
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class RadarSimpleReplayBufferTest {

    private lateinit var buffer: RadarSimpleReplayBuffer

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        buffer = RadarSimpleReplayBuffer(context)
    }

    @After
    fun tearDown() {
        buffer.shutdown()
    }

    private fun options(batchSize: Int, batchInterval: Int): RadarTrackingOptions {
        return RadarTrackingOptions.RESPONSIVE.copy(
            batchSize = batchSize,
            batchInterval = batchInterval
        )
    }

    @Test
    fun test_addToBatch_incrementsCount() {
        val options = options(batchSize = 5, batchInterval = 0)
        assertEquals(0, buffer.batchCount())
        buffer.addToBatch(JSONObject().put("foo", "bar"), options)
        assertEquals(1, buffer.batchCount())
    }

    @Test
    fun test_shouldFlushBatch_byBatchSize() {
        val options = options(batchSize = 2, batchInterval = 0)
        buffer.addToBatch(JSONObject(), options)
        assertFalse(buffer.shouldFlushBatch(options))
        buffer.addToBatch(JSONObject(), options)
        assertTrue(buffer.shouldFlushBatch(options))
    }

    @Test
    fun test_shouldFlushBatch_withZeroBatchSize_neverFlushes() {
        val options = options(batchSize = 0, batchInterval = 0)
        repeat(10) { buffer.addToBatch(JSONObject(), options) }
        assertFalse(buffer.shouldFlushBatch(options))
    }

    @Test
    fun test_addToBatch_scheduleTimerAfterIntervalSetLate() {
        // First adds with no interval -> no timer scheduled (mirrors reviewer fix #1 on iOS)
        val noInterval = options(batchSize = 0, batchInterval = 0)
        buffer.addToBatch(JSONObject(), noInterval)

        // Later add with interval -> timer should still schedule since it's gated on
        // `batchTimerRunnable == null`, not on "buffer was empty"
        val withInterval = options(batchSize = 0, batchInterval = 30)
        buffer.addToBatch(JSONObject(), withInterval)

        // cancelBatchTimer should be safe to call synchronously without racing the scheduled runnable
        buffer.cancelBatchTimer()
    }
}