package io.radar.sdk.util

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.radar.sdk.Radar
import io.radar.sdk.matchers.RangeMatcher.Companion.isBetween
import io.radar.sdk.matchers.RangeMatcher.Companion.isGreaterThan
import io.radar.sdk.model.RadarLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.android.util.concurrent.InlineExecutorService
import org.robolectric.annotation.Config
import java.io.File
import java.util.*
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
@Config(sdk=[Build.VERSION_CODES.P])
class RadarLogBufferTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val directory = context.getDir("RadarLogs", Context.MODE_PRIVATE)

    @Test
    fun testLifecycle() {
        //before the buffer is created, ensure this test begins with an empty file directory
        assertEquals(0, directory.listFiles()!!.size)

        //create the log buffer
        val logBuffer = RadarLogBuffer(context, InlineExecutorService())

        //preconditions
        assertEquals(1, directory.listFiles()!!.size)
        var logFile = directory.listFiles()!!.first()
        assertTrue(logFile.exists())
        var flushable = logBuffer.getLogs()
        assertTrue(flushable.get().isEmpty())
        assertEquals(0, logBuffer.size)
        //a file is added when the logs are retrieved
        assertEquals(2, directory.listFiles()!!.size)

        //Flush behavior
        flushable.onFlush(false)
        //No change, since the flush was unsuccessful
        assertEquals(2, directory.listFiles()!!.size)
        assertTrue(logFile.exists())

        //Flush was successful
        flushable.onFlush(true)
        assertEquals(1, directory.listFiles()!!.size)
        assertFalse(logFile.exists())
        logFile = directory.listFiles()!!.first()

        //log max number of logs before purging
        val beforeLog = Date()
        val logs = mutableListOf<Pair<Radar.RadarLogLevel, String>>()
        repeat(500) {
            val level = Radar.RadarLogLevel.fromInt(it%5)
            val message = UUID.randomUUID().toString()
            logBuffer.write(level, message)
            logs += level to message
        }
        assertEquals(500, logBuffer.size)
        assertEquals(500, logs.size)
        val afterLog = Date()

        //Buffer is full, so 1 more operation should cause a purge
        assertEquals(1, directory.listFiles()!!.size)
        assertEquals(logFile.absolutePath, directory.listFiles()!!.first().absolutePath)

        //Verify the log contents
        var contents = logBuffer.getLogs().get()
        assertEquals(500, contents.size)
        contents.forEachIndexed { index, radarLog ->
            assertEquals(logs[index].first, radarLog.level)
            assertEquals(logs[index].second, radarLog.message)
            assertThat(radarLog.createdAt, isBetween(beforeLog, afterLog))
        }
        //getLogs() creates a new file
        assertEquals(2, directory.listFiles()!!.size)

        //log one more, to trigger a purge
        val message = UUID.randomUUID().toString()
        logBuffer.write(Radar.RadarLogLevel.DEBUG, message)
        //new log file was created.
        assertEquals(3, directory.listFiles()!!.size)
        //New buffer contains the message written plus a log message to specify that a purge occurred
        assertEquals(2, logBuffer.size)
        flushable = logBuffer.getLogs()
        //getLogs creates a new file for logging new messages
        assertEquals(4, directory.listFiles()!!.size)
        contents = flushable.get()
        //contains the 2 logs in currentFile plus all the logs in last-used file
        assertEquals(502, contents.size)

        //Successful flush will delete the extra files and update the logBuffer size
        flushable.onFlush(true)
        assertEquals(0, logBuffer.size)
        assertEquals(1, directory.listFiles()!!.size)
    }

    @Test
    fun testConstructor() {
        //the constructor initializes the data, and may do some file cleanup.
        var size = writeNewFile()
        assertEquals(1, directory.listFiles()!!.size)

        var logBuffer = RadarLogBuffer(context, InlineExecutorService())
        //precondition for the rest of this test
        Assume.assumeThat(logBuffer.getLastModified(directory.listFiles()!![0]), isGreaterThan(0L))

        //Keeps latest file as log file
        assertEquals(1, directory.listFiles()!!.size)
        assertEquals(size, logBuffer.size)

        //Keeps latest file as log file
        size = writeNewFile()
        assertEquals(2, directory.listFiles()!!.size)
        logBuffer = RadarLogBuffer(context, InlineExecutorService())
        assertEquals(2, directory.listFiles()!!.size)
        assertEquals(size, logBuffer.size)

        //Any additional files causes a purge at start
        writeNewFile()
        size = writeNewFile()
        assertEquals(4, directory.listFiles()!!.size)
        logBuffer = RadarLogBuffer(context, InlineExecutorService())
        //two will be left, since the latest file will be kept and a new one will be created.
        assertEquals(2, directory.listFiles()!!.size)
        //size will be 1, because the purge methods writes a log to the buffer
        assertEquals(1, logBuffer.size)
        assertEquals(size + 1, logBuffer.getLogs().get().size)
    }

    private fun writeNewFile(): Int {
        //Add a little wait between log file creation, since the tests rely on lastModified timestamps
        Thread.sleep(1000)
        val name = "${UUID.randomUUID()}.log"
        val size = Random.nextInt(10)
        val file = File(directory, name)
        file.createNewFile()
        val radarLog = RadarLog(Radar.RadarLogLevel.DEBUG, name)
        repeat(size) { file.appendText("${radarLog.toJson()}\n") }
        return size
    }

}