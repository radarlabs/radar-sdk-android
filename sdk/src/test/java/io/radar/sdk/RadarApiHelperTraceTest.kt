package io.radar.sdk

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.radar.sdk.helpers.RadarApiHelperMock
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowSystemClock

/** Client spans recorded by the real [RadarApiHelper], using a stub connection. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class RadarApiHelperTraceTest {
    @get:Rule val temp = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var manager: RadarTraceManager
    private lateinit var uploads: RadarApiHelperMock

    /** Request headers by path, as each stub connection received them. */
    private val sentHeaders = ConcurrentHashMap<String, MutableMap<String, String>>()

    /** Paths whose requests block in the connection factory until released. */
    private val held = ConcurrentHashMap<String, CountDownLatch>()

    @Before
    fun setUp() {
        val application = ApplicationProvider.getApplicationContext<Context>()
        context = object : ContextWrapper(application) {
            override fun getFilesDir(): File = temp.root
        }
        RadarSettings.setPublishableKey(context, "prj_test_pk_trace")
        val logger = RadarLogger(context)
        uploads = RadarApiHelperMock().apply {
            mockStatus = Radar.RadarStatus.SUCCESS
            mockResponse = JSONObject()
        }
        val apiClient = RadarApiClient(context, logger).also { it.apiHelper = uploads }
        manager = RadarTraceManager(context, apiClient, logger).also { it.overrideSampledForTesting(true) }
        Radar.traceManager = manager
    }

    @After
    fun tearDown() {
        held.values.forEach { it.countDown() }
        Radar.traceManager = null
        manager.shutdownForTesting()
    }

    @Test
    fun tracedRequestSendsTraceparentAndRecordsClientAttributes() {
        val span = clientSpan("GET /v1/test")

        request(helper(), "v1/test", span).await()

        assertEquals("00-${span.traceId}-${span.spanId}-01", sentHeaders["v1/test"]?.get("traceparent"))
        val json = uploadedSpans().getValue("GET /v1/test")
        assertEquals("api.radar.io", attribute(json, "server.address")?.get("stringValue"))
        assertNotNull(attribute(json, "network.connection.type")?.get("stringValue"))
        assertNotNull(attribute(json, "radar.http.queue_ms")?.get("intValue"))
        assertNull(attribute(json, "http.request.resend_count"))
        assertFalse(json.has("status"))
    }

    @Test
    fun failedAttributeReadDoesNotStopRequest() {
        // getNetworkCapabilities throws SecurityException without ACCESS_NETWORK_STATE.
        val throwingContext = object : ContextWrapper(context) {
            override fun getSystemService(name: String): Any? {
                if (name == Context.CONNECTIVITY_SERVICE) throw SecurityException("no network state")
                return super.getSystemService(name)
            }
        }
        val callback = RecordingCallback()

        helper().request(throwingContext, "POST", "v1/test", null, JSONObject(), false, callback, traceSpan = clientSpan("POST /v1/test"))

        callback.await()
        assertNotNull(sentHeaders["v1/test"])
        val json = uploadedSpans().getValue("POST /v1/test")
        assertEquals("api.radar.io", attribute(json, "server.address")?.get("stringValue"))
        assertNull(attribute(json, "network.connection.type"))
    }

    @Test
    fun spanEndsBeforeCallbackIsPosted() {
        val callback = request(helper(), "v1/test", clientSpan("GET /v1/test"))

        // The main looper doesn't run here, so the posted callback can't have run yet.
        waitUntil { uploadedSpans().containsKey("GET /v1/test") }
        assertFalse(callback.completed)
        callback.await()
    }

    @Test
    fun successWithoutCallbackEndsSpan() {
        helper().request(context, "POST", "v1/test", null, JSONObject(), false, null, traceSpan = clientSpan("POST /v1/test"))

        waitUntil { uploadedSpans().containsKey("POST /v1/test") }
        assertFalse(uploadedSpans().getValue("POST /v1/test").has("status"))
    }

    @Test
    fun preparationFailureEndsSpanBeforeCallingHandler() {
        val handled = CountDownLatch(1)
        helper().request(
            context, "POST", "v1/track", null, JSONObject(), false, RecordingCallback(),
            verified = true,
            prepareRequest = { throw IllegalStateException("Failed to prepare fraud payload") },
            preparationFailureHandler = { handled.countDown() },
            traceSpan = clientSpan("POST /v1/track")
        )

        waitUntil { uploadedSpans().containsKey("POST /v1/track") }
        assertEquals(1L, handled.count)
        assertEquals("ERROR_BAD_REQUEST", attribute(uploadedSpans().getValue("POST /v1/track"), "error.type")?.get("stringValue"))
        waitUntil {
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            handled.count == 0L
        }
    }

    @Test
    fun pendingSessionSendsNoTraceparent() {
        manager.overrideSampledForTesting(null)

        request(helper(), "v1/test", clientSpan("GET /v1/test")).await()

        assertNull(sentHeaders["v1/test"]?.get("traceparent"))
    }

    @Test
    fun untracedRequestSendsNoTraceparent() {
        request(helper(), "v1/test", null).await()

        assertNull(sentHeaders["v1/test"]?.get("traceparent"))
    }

    @Test
    fun networkErrorRecordsErrorKind() {
        request(helper(failure = UnknownHostException("api.radar.io")), "v1/test", clientSpan("GET /v1/test"))
            .await(Radar.RadarStatus.ERROR_NETWORK)

        val json = uploadedSpans().getValue("GET /v1/test")
        assertEquals(2, json.getJSONObject("status").getInt("code"))
        assertEquals("ERROR_NETWORK", attribute(json, "error.type")?.get("stringValue"))
        assertEquals("dns_failure", attribute(json, "radar.network.error_kind")?.get("stringValue"))
    }

    @Test
    fun queueTimeIncludesWaitingBehindEarlierRequest() {
        val apiHelper = helper()
        held["v1/first"] = CountDownLatch(1)
        val first = request(apiHelper, "v1/first", null)
        val second = request(apiHelper, "v1/second", clientSpan("GET /v1/second"))

        ShadowSystemClock.advanceBy(Duration.ofMillis(250))
        held.getValue("v1/first").countDown()
        first.await()
        second.await()

        assertEquals("250", attribute(uploadedSpans().getValue("GET /v1/second"), "radar.http.queue_ms")?.get("intValue"))
    }

    @Test
    fun retriedVerifiedRequestRecordsResendCount() {
        val connections = ArrayDeque(listOf(EOFException("Connection closed"), null))
        val apiHelper = RadarApiHelper(connectionFactory = { url -> StubConnection(url, connections.removeFirst()) })
        val callback = RecordingCallback()

        apiHelper.request(
            context, "POST", "v1/track", null, JSONObject(), false, callback,
            verified = true,
            prepareRequest = { JSONObject().put("sealed", true) },
            traceSpan = clientSpan("POST /v1/track")
        )
        callback.await()

        assertEquals("1", attribute(uploadedSpans().getValue("POST /v1/track"), "http.request.resend_count")?.get("intValue"))
    }

    private fun clientSpan(name: String): RadarTraceSpan = RadarTraceSpan.start("root").child(name, RadarSpanKind.CLIENT)

    private fun helper(failure: IOException? = null) = RadarApiHelper(
        connectionFactory = { url ->
            held[url.path.removePrefix("/")]?.await(LATCH_TIMEOUT, TimeUnit.SECONDS)
            StubConnection(url, failure)
        }
    )

    private fun request(apiHelper: RadarApiHelper, path: String, span: RadarTraceSpan?): RecordingCallback {
        val callback = RecordingCallback()
        apiHelper.request(context, "POST", path, null, JSONObject(), false, callback, traceSpan = span)
        return callback
    }

    private fun uploadedSpans(): Map<String, JSONObject> {
        manager.flushAndWaitForTesting()
        return uploads.capturedTraceSpans().associateBy { it.getString("name") }
    }

    private fun attribute(span: JSONObject, key: String): JSONObject? {
        val attributes = span.getJSONArray("attributes")
        return (0 until attributes.length()).map { attributes.getJSONObject(it) }
            .firstOrNull { it.getString("key") == key }
            ?.getJSONObject("value")
    }

    private fun waitUntil(condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(LATCH_TIMEOUT)
        while (!condition() && System.nanoTime() < deadline) {
            Thread.sleep(10)
        }
        assertTrue("Condition not met within $LATCH_TIMEOUT seconds", condition())
    }

    /** Callbacks are posted to the main looper, so [await] runs its tasks. */
    private class RecordingCallback : RadarApiHelper.RadarApiCallback {
        private val latch = CountDownLatch(1)
        var status: Radar.RadarStatus? = null

        val completed: Boolean get() = latch.count == 0L

        override fun onComplete(status: Radar.RadarStatus, res: JSONObject?, throwable: Throwable?) {
            this.status = status
            latch.countDown()
        }

        fun await(expected: Radar.RadarStatus = Radar.RadarStatus.SUCCESS) {
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(LATCH_TIMEOUT)
            while (!completed && System.nanoTime() < deadline) {
                ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
                latch.await(10, TimeUnit.MILLISECONDS)
            }
            assertTrue("Request callback did not complete", completed)
            assertEquals(expected, status)
        }
    }

    /** Returns 200 with an empty JSON body, or throws [failure] when the response is read. */
    private inner class StubConnection(url: URL, private val failure: IOException?) : HttpURLConnection(url) {
        private val path = url.path.removePrefix("/")

        override fun setRequestProperty(key: String, value: String) {
            sentHeaders.getOrPut(path) { ConcurrentHashMap() }[key] = value
        }

        override fun connect() {}
        override fun disconnect() {}
        override fun usingProxy(): Boolean = false
        override fun getOutputStream() = ByteArrayOutputStream()
        override fun getInputStream() = ByteArrayInputStream("{}".toByteArray(Charsets.UTF_8))
        override fun getResponseCode(): Int {
            failure?.let { throw it }
            return 200
        }
    }

    private companion object {
        const val LATCH_TIMEOUT = 5L
    }
}
