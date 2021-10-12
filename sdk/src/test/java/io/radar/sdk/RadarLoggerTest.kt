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
import org.robolectric.annotation.Config
import java.util.*

/**
 * Unit tests [RadarLogger]
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk=[Build.VERSION_CODES.P])
class RadarLoggerTest {

    private val app = mockk<RadarApplication>()
    private val settings = mockk<RadarSettings>()
    private val logger = RadarLogger(app)
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
    }

    @After
    fun tearDown() {
        message = null
    }

    @Test
    fun testDebug() {
        assertNull(message)
        val text = UUID.randomUUID().toString()
        listOf(Radar.RadarLogLevel.NONE, Radar.RadarLogLevel.ERROR, Radar.RadarLogLevel.WARNING,
            Radar.RadarLogLevel.INFO).forEach {
            logLevel = it
            logger.d(text)
            assertNull(message)
        }
        logLevel = Radar.RadarLogLevel.DEBUG
        logger.d(text)
        assertEquals(text, message)
    }

    @Test
    fun testInfo() {
        assertNull(message)
        var text = UUID.randomUUID().toString()
        listOf(Radar.RadarLogLevel.NONE, Radar.RadarLogLevel.ERROR, Radar.RadarLogLevel.WARNING).forEach {
            logLevel = it
            logger.i(text)
            assertNull(message)
        }
        listOf(Radar.RadarLogLevel.INFO, Radar.RadarLogLevel.DEBUG).forEach {
            logLevel = it
            text = UUID.randomUUID().toString()
            logger.i(text)
            assertEquals(text, message)
        }
    }

    @Test
    fun testWarn() {
        assertNull(message)
        var text = UUID.randomUUID().toString()
        listOf(Radar.RadarLogLevel.NONE, Radar.RadarLogLevel.ERROR).forEach {
            logLevel = it
            logger.w(text)
            assertNull(message)
        }
        listOf(Radar.RadarLogLevel.WARNING, Radar.RadarLogLevel.INFO, Radar.RadarLogLevel.DEBUG).forEach {
            logLevel = it
            text = UUID.randomUUID().toString()
            logger.w(text)
            assertEquals(text, message)
        }
    }

    @Test
    fun testError() {
        assertNull(message)
        var text = UUID.randomUUID().toString()
        logLevel = Radar.RadarLogLevel.NONE
        logger.e(text)
        assertNull(message)

        listOf(Radar.RadarLogLevel.ERROR, Radar.RadarLogLevel.WARNING, Radar.RadarLogLevel.INFO,
                Radar.RadarLogLevel.DEBUG).forEach {
            logLevel = it
            text = UUID.randomUUID().toString()
            logger.e(text)
            assertEquals(text, message)
        }
    }
}