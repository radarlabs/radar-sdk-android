package io.radar.sdk

import android.os.Build
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.NoRouteToHostException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.URL
import java.util.ArrayDeque
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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
        var fixedBodyLength: Int? = null

        override fun setFixedLengthStreamingMode(contentLength: Int) {
            fixedBodyLength = contentLength
        }

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
    fun retryClassifierExcludesConnectionSetupFailures() {
        assertTrue(isRetryableConnectionFailure(EOFException("Connection closed")))
        assertFalse(isRetryableConnectionFailure(ConnectException("Connection refused")))
        assertFalse(isRetryableConnectionFailure(NoRouteToHostException("No route to host")))
    }

    class RecordingFraudHandle {
        val sealOptions = mutableListOf<Map<String, Any?>>()

        fun seal(options: Map<String, Any?>): Map<String, Any?> {
            sealOptions.add(options.toMap())
            if (sealOptions.size == 1) {
                SystemClock.sleep(250)
            }
            return mapOf(
                "payload" to JSONObject()
                    .put("encv", 1)
                    .put("ct", "sealed-${sealOptions.size}")
                    .toString()
            )
        }
    }

    private class RecordingCallback : RadarApiHelper.RadarApiCallback {
        val completed = CountDownLatch(1)
        var status: Radar.RadarStatus? = null
        var error: Throwable? = null
        var count = 0

        override fun onComplete(status: Radar.RadarStatus, res: JSONObject?, throwable: Throwable?) {
            this.status = status
            error = throwable
            count++
            completed.countDown()
        }

        fun await() {
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
            while (completed.count > 0 && System.nanoTime() < deadline) {
                ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
                completed.await(10, TimeUnit.MILLISECONDS)
            }
            assertEquals("Request callback did not complete", 0L, completed.count)
            assertEquals(1, count)
        }
    }

    @Test
    fun completeBodyIsPreparedAfterTimingRefreshOnEveryAttempt() {
        SystemClock.sleep(10000)
        val nowMs = SystemClock.elapsedRealtimeNanos() / 1000000
        val params = JSONObject()
            .put("installId", "install-1")
            .put("metadata", JSONObject().put("city", "München"))
            .put("locationMs", nowMs - 1000)
            .put("updatedAtMsDiff", 0)
            .put("replays", JSONArray().put(JSONObject().put("locationMs", nowMs - 2000)))
        val first = StubConnection(SocketException("Connection reset"))
        val second = StubConnection()
        val connections = ArrayDeque<HttpURLConnection>().apply {
            add(first)
            add(second)
        }
        val handle = RecordingFraudHandle()
        val prepared = RadarPreparedFraudPayload(handle)
        val headers = mapOf("Authorization" to "test-key", "X-Radar-Mobile-Origin" to "test-app")
        val callback = RecordingCallback()

        RadarApiHelper(connectionFactory = { connections.removeFirst() }).request(
            context = ApplicationProvider.getApplicationContext(),
            method = "POST",
            path = "v1/track",
            headers = headers,
            params = params,
            sleep = false,
            verified = true,
            verifiedHostOverride = "https://verified.test",
            prepareRequest = { JSONObject(prepared.sealForRequest("v1/track", params, headers)) },
            callback = callback
        )

        callback.await()
        assertEquals(Radar.RadarStatus.SUCCESS, callback.status)
        assertEquals(2, handle.sealOptions.size)
        handle.sealOptions.forEachIndexed { index, options ->
            val body = JSONObject(options["body"] as String)
            assertEquals("install-1", body.getString("installId"))
            assertEquals("München", body.getJSONObject("metadata").getString("city"))
            assertEquals(1000L + index * 250, body.getLong("updatedAtMsDiff"))
            assertEquals(2000L + index * 250, body.getJSONArray("replays").getJSONObject(0).getLong("updatedAtMsDiff"))
            assertFalse(body.has("fraudPayload"))
            assertFalse(body.has("encv"))
            assertFalse(options.containsKey("installId"))
            assertEquals("POST", options["method"])
            assertEquals("/v1/track", options["canonicalRoute"])
            assertEquals("test-key", options["authorization"])
            assertEquals("test-app", options["origin"])
        }
        assertNotEquals(handle.sealOptions[0]["encryptionAttemptId"], handle.sealOptions[1]["encryptionAttemptId"])
        listOf(first, second).forEachIndexed { index, connection ->
            val bytes = connection.writtenBody.toByteArray()
            val body = JSONObject(bytes.toString(Charsets.UTF_8))
            assertEquals(setOf("encv", "ct"), body.keys().asSequence().toSet())
            assertEquals("sealed-${index + 1}", body.getString("ct"))
            assertEquals(bytes.size, connection.fixedBodyLength)
        }
        assertFalse(params.has("fraudPayload"))
        assertFalse(params.has("encv"))
    }

    @Test
    fun sealingFailureStopsBeforeOpeningConnection() {
        var opened = 0
        val params = JSONObject().put("installId", "install-1")
        val failure = IllegalStateException("Cannot encrypt")
        val callback = RecordingCallback()
        RadarApiHelper(connectionFactory = {
            opened++
            StubConnection()
        }).request(
            context = ApplicationProvider.getApplicationContext(),
            method = "POST",
            path = "v1/track",
            headers = emptyMap(),
            params = params,
            sleep = false,
            verified = true,
            verifiedHostOverride = "https://verified.test",
            prepareRequest = { throw failure },
            callback = callback
        )

        callback.await()
        assertEquals(Radar.RadarStatus.ERROR_PLUGIN, callback.status)
        assertEquals(failure, callback.error)
        assertEquals(0, opened)
        assertEquals("""{"installId":"install-1"}""", params.toString())
    }

    @Test
    fun retrySealingFailureDoesNotOpenSecondConnectionOrSendPlaintext() {
        val first = StubConnection(SocketException("Connection reset"))
        var opened = 0
        var seals = 0
        val callback = RecordingCallback()
        RadarApiHelper(connectionFactory = {
            opened++
            first
        }).request(
            context = ApplicationProvider.getApplicationContext(),
            method = "POST",
            path = "v1/track",
            headers = emptyMap(),
            params = JSONObject().put("installId", "install-1"),
            sleep = false,
            verified = true,
            verifiedHostOverride = "https://verified.test",
            prepareRequest = {
                seals++
                check(seals == 1) { "Cannot encrypt retry" }
                JSONObject().put("ct", "sealed-1")
            },
            callback = callback
        )

        callback.await()
        assertEquals(Radar.RadarStatus.ERROR_PLUGIN, callback.status)
        assertEquals(1, opened)
        assertEquals(2, seals)
        assertEquals("""{"ct":"sealed-1"}""", first.writtenBody.toByteArray().toString(Charsets.UTF_8))
        assertEquals(0L, first.disconnected.count)
    }

    @Test
    fun ordinaryRequestSendsCoreBodyAndDoesNotRetryLostConnection() {
        val connection = StubConnection(SocketException("Connection reset"))
        var opened = 0
        val params = JSONObject().put("installId", "install-1").put("metadata", JSONObject().put("enabled", true))
        val callback = RecordingCallback()
        RadarApiHelper(connectionFactory = {
            opened++
            connection
        }).request(
            context = ApplicationProvider.getApplicationContext(),
            method = "POST",
            path = "v1/track",
            headers = emptyMap(),
            params = params,
            sleep = false,
            callback = callback
        )

        callback.await()
        assertEquals(Radar.RadarStatus.ERROR_NETWORK, callback.status)
        assertEquals(1, opened)
        assertEquals(params.toString(), connection.writtenBody.toByteArray().toString(Charsets.UTF_8))
        assertEquals(null, connection.fixedBodyLength)
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
                JSONObject().put("ct", "sealed-$sealCount")
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
                .getString("ct")
        )
        assertEquals(
            "sealed-2",
            JSONObject(second.writtenBody.toByteArray().toString(Charsets.UTF_8))
                .getString("ct")
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
                JSONObject().put("ct", "sealed-$sealCount")
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
                JSONObject().put("ct", "sealed-$sealCount")
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
