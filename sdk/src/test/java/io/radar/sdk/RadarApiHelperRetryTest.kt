package io.radar.sdk

import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.URL
import java.util.ArrayDeque
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class RadarApiHelperRetryTest {
    private class StubConnection(
        private val failure: IOException? = null
    ) : HttpURLConnection(URL("https://verified.test")) {
        val writtenBody = ByteArrayOutputStream()
        val disconnected = CountDownLatch(1)

        override fun connect() {}
        override fun disconnect() {
            disconnected.countDown()
        }
        override fun usingProxy(): Boolean = false
        override fun getOutputStream() = writtenBody
        override fun getInputStream() = ByteArrayInputStream("{}".toByteArray(Charsets.UTF_8))
        override fun getResponseCode(): Int {
            failure?.let { throw it }
            return 200
        }
    }

    @Test
    fun lostConnectionResealsAndRetriesOnce() {
        val first = StubConnection(SocketException("Connection reset"))
        val second = StubConnection()
        val connections = ArrayDeque<HttpURLConnection>().apply {
            add(first)
            add(second)
        }
        val helper = RadarApiHelper(connectionFactory = { connections.removeFirst() })
        val params = JSONObject().put("installId", "install-1")
        var sealCount = 0
        var callbackCount = 0
        var callbackStatus: Radar.RadarStatus? = null

        helper.request(
            context = ApplicationProvider.getApplicationContext(),
            method = "POST",
            path = "v1/reveal/risk",
            headers = emptyMap(),
            params = params,
            sleep = false,
            verified = true,
            verifiedHostOverride = "https://verified.test",
            prepareRequest = {
                sealCount++
                params.put("fraudPayload", "sealed-$sealCount")
            },
            callback = object : RadarApiHelper.RadarApiCallback {
                override fun onComplete(
                    status: Radar.RadarStatus,
                    res: JSONObject?,
                    throwable: Throwable?
                ) {
                    callbackCount++
                    callbackStatus = status
                }
            }
        )

        assertTrue(second.disconnected.await(5, TimeUnit.SECONDS))
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertEquals(2, sealCount)
        assertEquals(1, callbackCount)
        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)
        assertEquals(
            "sealed-1",
            JSONObject(first.writtenBody.toByteArray().toString(Charsets.UTF_8))
                .getString("fraudPayload")
        )
        assertEquals(
            "sealed-2",
            JSONObject(second.writtenBody.toByteArray().toString(Charsets.UTF_8))
                .getString("fraudPayload")
        )
    }

    @Test
    fun secondLostConnectionStopsAfterOneRetry() {
        val firstFailure = SocketException("first reset")
        val secondFailure = SocketException("second reset")
        val first = StubConnection(firstFailure)
        val second = StubConnection(secondFailure)
        val connections = ArrayDeque<HttpURLConnection>().apply {
            add(first)
            add(second)
        }
        val helper = RadarApiHelper(connectionFactory = { connections.removeFirst() })
        val params = JSONObject().put("installId", "install-1")
        var sealCount = 0
        var callbackCount = 0
        var callbackStatus: Radar.RadarStatus? = null
        var callbackError: Throwable? = null

        helper.request(
            context = ApplicationProvider.getApplicationContext(),
            method = "POST",
            path = "v1/reveal/risk",
            headers = emptyMap(),
            params = params,
            sleep = false,
            verified = true,
            verifiedHostOverride = "https://verified.test",
            prepareRequest = {
                sealCount++
                params.put("fraudPayload", "sealed-$sealCount")
            },
            callback = object : RadarApiHelper.RadarApiCallback {
                override fun onComplete(
                    status: Radar.RadarStatus,
                    res: JSONObject?,
                    throwable: Throwable?
                ) {
                    callbackCount++
                    callbackStatus = status
                    callbackError = throwable
                }
            }
        )

        assertTrue(second.disconnected.await(5, TimeUnit.SECONDS))
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertEquals(2, sealCount)
        assertEquals(1, callbackCount)
        assertEquals(Radar.RadarStatus.ERROR_NETWORK, callbackStatus)
        assertEquals(secondFailure, callbackError)
    }

    @Test
    fun timeoutDoesNotRetryOrReseal() {
        val first = StubConnection(SocketTimeoutException("timed out"))
        val second = StubConnection()
        val connections = ArrayDeque<HttpURLConnection>().apply {
            add(first)
            add(second)
        }
        val helper = RadarApiHelper(connectionFactory = { connections.removeFirst() })
        val params = JSONObject().put("installId", "install-1")
        var sealCount = 0
        var callbackCount = 0
        var callbackStatus: Radar.RadarStatus? = null

        helper.request(
            context = ApplicationProvider.getApplicationContext(),
            method = "POST",
            path = "v1/reveal/risk",
            headers = emptyMap(),
            params = params,
            sleep = false,
            verified = true,
            verifiedHostOverride = "https://verified.test",
            prepareRequest = {
                sealCount++
                params.put("fraudPayload", "sealed-$sealCount")
            },
            callback = object : RadarApiHelper.RadarApiCallback {
                override fun onComplete(
                    status: Radar.RadarStatus,
                    res: JSONObject?,
                    throwable: Throwable?
                ) {
                    callbackCount++
                    callbackStatus = status
                }
            }
        )

        assertTrue(first.disconnected.await(5, TimeUnit.SECONDS))
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertEquals(1, sealCount)
        assertEquals(1, callbackCount)
        assertEquals(Radar.RadarStatus.ERROR_NETWORK, callbackStatus)
        assertEquals(1, connections.size)
    }
}
