package io.radar.sdk

import android.content.Context
import android.os.Build
import android.os.Process
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class RadarApiHelperQueueTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    /** Paths whose requests block in the connection factory until [release] is called. */
    private val held = ConcurrentHashMap<String, CountDownLatch>()

    /** Paths in the order their requests started running. */
    private val started: MutableList<String> = Collections.synchronizedList(mutableListOf())
    private val threads = ConcurrentHashMap<String, Thread>()
    private val priorities = ConcurrentHashMap<String, Int>()

    private val apiHelper = RadarApiHelper(
        connectionFactory = { url ->
            val path = url.path.removePrefix("/")
            started.add(path)
            threads[path] = Thread.currentThread()
            priorities[path] = Process.getThreadPriority(Process.myTid())
            held[path]?.await(LATCH_TIMEOUT, TimeUnit.SECONDS)
            StubConnection(url)
        }
    )

    @After
    fun tearDown() {
        held.values.forEach { it.countDown() }
    }

    @Test
    fun backgroundRequestDoesNotWaitBehindOrderedRequest() {
        hold("v1/track")
        val track = request("v1/track", RadarRequestQueue.ORDERED)
        val traces = request("v1/traces", RadarRequestQueue.BACKGROUND)

        traces.await()
        assertFalse("Ordered request should still be in flight", track.isComplete())

        release("v1/track")
        track.await()
    }

    @Test
    fun orderedRequestsRunOneAtATimeInOrder() {
        hold("v1/first")
        val first = request("v1/first", RadarRequestQueue.ORDERED)
        val second = request("v1/second", RadarRequestQueue.ORDERED)

        assertSecondWaitsForFirst("v1/first" to first, "v1/second" to second)
    }

    @Test
    fun backgroundRequestsRunOneAtATimeInOrder() {
        hold("v1/first")
        val first = request("v1/first", RadarRequestQueue.BACKGROUND)
        val second = request("v1/second", RadarRequestQueue.BACKGROUND)

        assertSecondWaitsForFirst("v1/first" to first, "v1/second" to second)
    }

    @Test
    fun requestsUseOrderedQueueByDefault() {
        hold("v1/explicit")
        val explicit = request("v1/explicit", RadarRequestQueue.ORDERED)
        val default = RecordingCallback()
        apiHelper.request(context, "POST", "v1/default", null, JSONObject(), false, default)

        // The default request queues behind the held ORDERED request.
        assertSecondWaitsForFirst("v1/explicit" to explicit, "v1/default" to default)
    }

    @Test
    fun backgroundRequestsRunAtBackgroundPriority() {
        request("v1/track", RadarRequestQueue.ORDERED).await()
        request("v1/traces", RadarRequestQueue.BACKGROUND).await()

        assertEquals(Process.THREAD_PRIORITY_BACKGROUND, priorities["v1/traces"])
        assertNotEquals(Process.THREAD_PRIORITY_BACKGROUND, priorities["v1/track"])
        assertNotEquals(threads["v1/track"], threads["v1/traces"])
    }

    /** Asserts [second] doesn't start until the held [first] request finishes, and both run on one thread. */
    private fun assertSecondWaitsForFirst(first: Pair<String, RecordingCallback>, second: Pair<String, RecordingCallback>) {
        val (firstPath, firstCallback) = first
        val (secondPath, secondCallback) = second
        waitUntil { started.contains(firstPath) }
        // Give the second request time to start if the queue wrongly ran requests concurrently.
        Thread.sleep(100)
        assertEquals(listOf(firstPath), started.toList())
        assertFalse(secondCallback.isComplete())

        release(firstPath)
        firstCallback.await()
        secondCallback.await()
        assertEquals(listOf(firstPath, secondPath), started.toList())
        assertEquals(threads[firstPath], threads[secondPath])
    }

    private fun hold(path: String) {
        held[path] = CountDownLatch(1)
    }

    private fun release(path: String) {
        held.getValue(path).countDown()
    }

    private fun request(path: String, queue: RadarRequestQueue): RecordingCallback {
        val callback = RecordingCallback()
        apiHelper.request(context, "POST", path, null, JSONObject(), false, callback, queue = queue)
        return callback
    }

    private fun waitUntil(condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(LATCH_TIMEOUT)
        while (!condition() && System.nanoTime() < deadline) {
            Thread.sleep(10)
        }
        assertTrue("Condition not met within $LATCH_TIMEOUT seconds", condition())
    }

    /** Callbacks are posted to the main looper, so waiting runs its tasks. */
    private class RecordingCallback : RadarApiHelper.RadarApiCallback {
        private val completed = CountDownLatch(1)
        var status: Radar.RadarStatus? = null

        override fun onComplete(status: Radar.RadarStatus, res: JSONObject?, throwable: Throwable?) {
            this.status = status
            completed.countDown()
        }

        fun isComplete(): Boolean {
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            return completed.count == 0L
        }

        fun await() {
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(LATCH_TIMEOUT)
            while (!isComplete() && System.nanoTime() < deadline) {
                completed.await(10, TimeUnit.MILLISECONDS)
            }
            assertTrue("Request callback did not complete", isComplete())
            assertEquals(Radar.RadarStatus.SUCCESS, status)
        }
    }

    /** Returns 200 with an empty JSON body. */
    private class StubConnection(url: URL) : HttpURLConnection(url) {
        override fun connect() {}
        override fun disconnect() {}
        override fun usingProxy(): Boolean = false
        override fun getOutputStream() = ByteArrayOutputStream()
        override fun getInputStream() = ByteArrayInputStream("{}".toByteArray(Charsets.UTF_8))
        override fun getResponseCode(): Int = 200
    }

    private companion object {
        const val LATCH_TIMEOUT = 5L
    }
}
