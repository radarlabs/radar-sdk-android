package io.radar.sdk

import android.content.Context
import android.location.Location
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.radar.sdk.model.RadarEvent
import io.radar.sdk.model.RadarUser
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.android.util.concurrent.InlineExecutorService
import org.robolectric.annotation.Config
import java.util.*

/**
 * Unit tests [RadarLogger]
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class RadarLoggerTest {

    private val app = mockk<RadarApplication>()
    private val settings = mockk<RadarSettings>()
    private val logger = RadarLogger(app, InlineExecutorService())
    private var logLevel = Radar.RadarLogLevel.NONE
    private var message: String? = null
    private val receiver = object : RadarReceiver() {
        override fun onEventsReceived(context: Context, events: Array<RadarEvent>, user: RadarUser?) = Unit

        override fun onLocationUpdated(context: Context, location: Location, user: RadarUser) = Unit

        override fun onClientLocationUpdated(
            context: Context,
            location: Location,
            stopped: Boolean,
            source: Radar.RadarLocationSource
        ) = Unit

        override fun onError(context: Context, status: Radar.RadarStatus) = Unit

        override fun onLog(context: Context, message: String) {
            this@RadarLoggerTest.message = message
        }
    }

    @Before
    fun setUp() {
        every { app.settings } answers { settings }
        every { app.receiver } answers { receiver }
        every { settings.getLogLevel() } answers { logLevel }
        every { app.isTestKey() } answers { false }
    }

    @After
    fun tearDown() {
        message = null
    }

    @Test
    fun testDebug() {
        verifyLogging(listOf(Radar.RadarLogLevel.DEBUG)) { text, props ->
            if (props == null) {
                logger.d(text)
            } else {
                logger.d(text, props)
            }
        }
    }

    @Test
    fun testInfo() {
        verifyLogging(listOf(Radar.RadarLogLevel.INFO, Radar.RadarLogLevel.DEBUG)) { text, props ->
            if (props == null) {
                logger.i(text)
            } else {
                logger.i(text, props)
            }
        }
    }

    @Test
    fun testWarn() {
        verifyLogging(
            listOf(Radar.RadarLogLevel.WARNING, Radar.RadarLogLevel.INFO, Radar.RadarLogLevel.DEBUG)
        ) { text, props ->
            if (props == null) {
                logger.w(text)
            } else {
                logger.w(text, props)
            }
        }
    }

    @Test
    fun testError() {
        verifyLogging(
            listOf(
                Radar.RadarLogLevel.ERROR,
                Radar.RadarLogLevel.WARNING,
                Radar.RadarLogLevel.INFO,
                Radar.RadarLogLevel.DEBUG
            )
        ) { text, props ->
            if (props == null) {
                logger.e(text)
            } else {
                logger.e(text, props)
            }
        }
    }

    /**
     * Checks that the given log levels produce a log message in [RadarReceiver]. This also checks that the log levels
     * NOT contained in the given list do not produce a log message.
     *
     * @param [expectedLogLevels] log levels that are expected to produce a log result in [RadarReceiver]
     * @param [logBlock] the logger method to invoke. If the given pair is null, this should invoke the text-only
     * logger function.
     */
    private fun verifyLogging(
        expectedLogLevels: List<Radar.RadarLogLevel>,
        logBlock: (String, Pair<String, Any?>?) -> Unit
    ) {
        val skippedLogLevels = Radar.RadarLogLevel.values().toMutableList()
        skippedLogLevels.removeAll { expectedLogLevels.contains(it) }
        assertNull(message)
        var text = UUID.randomUUID().toString()
        var key = UUID.randomUUID().toString()
        var value = UUID.randomUUID()
        skippedLogLevels.forEach { level ->
            logLevel = level
            //just text
            logBlock.invoke(text, null)
            assertNull(message)
            //text with params
            logBlock.invoke(text, key to value)
            assertNull(message)
        }
        expectedLogLevels.forEach { level ->
            logLevel = level
            text = UUID.randomUUID().toString()
            //just text
            logBlock.invoke(text, null)
            assertEquals(text, message)
            //text with params
            key = UUID.randomUUID().toString()
            value = UUID.randomUUID()
            logBlock.invoke(text, key to value)
            assertEquals("$text | $key = $value", message)
        }
    }
}