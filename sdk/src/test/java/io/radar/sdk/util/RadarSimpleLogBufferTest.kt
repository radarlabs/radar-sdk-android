package io.radar.sdk.util

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.radar.sdk.Radar
import io.radar.sdk.matchers.RangeMatcher.Companion.isBetween
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.*

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class RadarSimpleLogBufferTest {

    @Test
    fun testLifecycle() {

        // Create the log buffer
        val logBuffer = RadarSimpleLogBuffer()

        // Preconditions
        var flushable = logBuffer.getFlushableLogsStash()
        assertTrue(flushable.get().isEmpty())

        // Log max number of logs before purging
        val beforeLog = Date()
        val logs = mutableListOf<Pair<Radar.RadarLogLevel, String>>()
        repeat(500) {
            val level = Radar.RadarLogLevel.fromInt(it % 5)
            val message = "$it"
            logBuffer.write(level, message)
            logs += level to message
        }
        assertEquals(500, logs.size)
        val afterLog = Date()

        // Verify the log contents
        flushable = logBuffer.getFlushableLogsStash()
        var contents = flushable.get()
        assertEquals(500, contents.size)
        contents.forEachIndexed { index, radarLog ->
            assertEquals(logs[index].second, radarLog.message)
            assertEquals(logs[index].first, radarLog.level)
            assertThat(radarLog.createdAt, isBetween(beforeLog, afterLog))
        }
        // Put logs back
        flushable.onFlush(false)
        // Verify the order was preserved
        flushable = logBuffer.getFlushableLogsStash()
        contents = flushable.get()
        assertEquals(500, logs.size)
        assertEquals(500, contents.size)
        contents.forEachIndexed { index, radarLog ->
            assertEquals(logs[index].second, radarLog.message)
            assertEquals(logs[index].first, radarLog.level)
        }

        // Log 600 more, then put flushed logs back. This will trigger a purge. The most-recent files from the flushable
        // Contents should return to the log buffer.
        repeat(500) {
            val level = Radar.RadarLogLevel.fromInt(it % 5)
            val message = "$it"
            logBuffer.write(level, message)
            logs += level to message
        }
        flushable.onFlush(false)
        flushable = logBuffer.getFlushableLogsStash()
        contents = flushable.get()
        assertEquals(1000, logs.size)
        assertEquals(1000, contents.size)
        flushable.onFlush(false)
        // One more log will cause a purge
        val level = Radar.RadarLogLevel.DEBUG
        var message = UUID.randomUUID().toString()
        logBuffer.write(level, message)
        flushable = logBuffer.getFlushableLogsStash()
        contents = flushable.get()
        // There should be 502 logs remaining - the extras are from the purge message and the log that was being written.
        assertEquals(502, contents.size)
        contents.take(500).forEachIndexed { index, radarLog ->
            assertEquals(logs[index + 500].second, radarLog.message)
            assertEquals(logs[index + 500].first, radarLog.level)
        }
        assertEquals("----- purged oldest logs -----", contents[500].message)
        assertEquals(message, contents[501].message)

        // Test behavior of successful log flush
        message = UUID.randomUUID().toString()
        logBuffer.write(Radar.RadarLogLevel.DEBUG, message)
        flushable.onFlush(true)
        flushable = logBuffer.getFlushableLogsStash()
        contents = flushable.get()
        assertEquals(1, contents.size)
        assertEquals(message, contents[0].message)
    }
}
