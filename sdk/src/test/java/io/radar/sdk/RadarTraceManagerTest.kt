package io.radar.sdk

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.radar.sdk.helpers.RadarApiHelperMock
import java.io.File
import java.util.Collections
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class RadarTraceManagerTest {
    @get:Rule val temp = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var logger: RadarLogger
    private lateinit var manager: RadarTraceManager
    private lateinit var apiClient: RadarApiClient
    private lateinit var apiHelper: RadarApiHelperMock

    @Before
    fun setUp() {
        val application = ApplicationProvider.getApplicationContext<Context>()
        context = object : ContextWrapper(application) {
            override fun getFilesDir(): File = temp.root
        }
        RadarSettings.setPublishableKey(context, PUBLISHABLE_KEY)
        logger = RadarLogger(context)
        apiHelper = RadarApiHelperMock().apply {
            mockStatus = Radar.RadarStatus.SUCCESS
            mockResponse = JSONObject()
        }
        apiClient = RadarApiClient(context, logger).also { it.apiHelper = apiHelper }
        // Most tests are about storage and upload, so every session is sampled unless a test clears this.
        manager = RadarTraceManager(context, apiClient, logger).also { it.overrideSampledForTesting(true) }
    }

    @After
    fun tearDown() {
        manager.shutdownForTesting()
    }

    // Upload

    @Test
    fun persistsAndExportsOtlpEnvelopeThenDeletesAcknowledgedFiles() {
        manager.record(span("one", spanId(1)), manager.currentProjectFingerprint(), manager.currentSessionId())
        manager.record(span("two", spanId(2)), manager.currentProjectFingerprint(), manager.currentSessionId())
        manager.awaitDiskForTesting()
        assertEquals(2, traceFiles().size)

        manager.flushAndWaitForTesting()

        assertTrue(traceFiles().isEmpty())
        val payload = apiHelper.capturedTracePayloads.single()
        assertEquals("radar-client", resourceAttribute(payload, "service.name"))
        assertEquals("android", resourceAttribute(payload, "os.type"))
        assertEquals(RadarUtils.SDK_VERSION, resourceAttribute(payload, "radar.sdk.version"))
        assertEquals(Build.VERSION.RELEASE, resourceAttribute(payload, "os.version"))
        assertEquals(Build.MANUFACTURER, resourceAttribute(payload, "device.manufacturer"))
        assertEquals(Build.MODEL, resourceAttribute(payload, "device.model.identifier"))
        val scope = payload.getJSONArray("resourceSpans").getJSONObject(0).getJSONArray("scopeSpans").getJSONObject(0).getJSONObject("scope")
        assertEquals("io.radar.sdk", scope.getString("name"))
        assertEquals(listOf(spanId(1), spanId(2)), apiHelper.capturedTraceSpans().map { it.getString("spanId") })
    }

    @Test
    fun uploadsOnBackgroundQueueWithPublishableKey() {
        recordSpans(1)

        manager.flushAndWaitForTesting()

        assertEquals(RadarRequestQueue.BACKGROUND, apiHelper.lastCapturedTraceQueue)
        assertEquals(PUBLISHABLE_KEY, apiHelper.lastCapturedTraceHeaders?.get("Authorization"))
    }

    @Test
    fun transientFailureRetriesBatch() {
        apiHelper.queueMockStatuses("v1/traces", listOf(Radar.RadarStatus.ERROR_NETWORK))
        recordSpans(150)

        manager.flushAndWaitForTesting()

        assertEquals(150, traceFiles().size)
        assertEquals(listOf(100), batchSizes())

        manager.flushAndWaitForTesting()

        assertTrue(traceFiles().isEmpty())
        // The initial failed batch of 100 spans is retried, followed by the next batch of 50
        assertEquals(listOf(100, 100, 50), batchSizes())
        val payloads = apiHelper.capturedTracePayloads
        assertEquals(spanIds(payloads[0]), spanIds(payloads[1]))
    }

    @Test
    fun rateLimitKeepsBatchOnDisk() {
        apiHelper.queueMockStatuses("v1/traces", listOf(Radar.RadarStatus.ERROR_RATE_LIMIT))
        recordSpans(150)

        manager.flushAndWaitForTesting()

        assertEquals(150, traceFiles().size)
        assertEquals(listOf(100), batchSizes())
    }

    @Test
    fun nonRetryableFailureDropsBatch() {
        apiHelper.queueMockStatuses("v1/traces", listOf(Radar.RadarStatus.ERROR_BAD_REQUEST))
        recordSpans(150)

        manager.flushAndWaitForTesting()

        // The rejected batch (the oldest 100) is deleted. The flush stops, so the other 50 stay for the next flush.
        val rejected = (1..100).map { spanId(it) }
        assertEquals(listOf(100), batchSizes())
        assertEquals(rejected, spanIds(apiHelper.capturedTracePayloads.single()))
        assertTrue(persistedSpanIds().none { it in rejected })
        assertEquals((101..150).map { spanId(it) }.toSet(), persistedSpanIds())
    }

    @Test
    fun serverErrorsKeepBatchOnDisk() {
        apiHelper.queueMockStatuses("v1/traces", List(5) { Radar.RadarStatus.ERROR_SERVER })
        recordSpans(150)

        // Each flush retries the oldest batch and stops. Repeated server errors never drop spans.
        repeat(5) { manager.flushAndWaitForTesting() }
        assertEquals(150, traceFiles().size)
        assertEquals(List(5) { 100 }, batchSizes())
        assertTrue(apiHelper.capturedTracePayloads.all { spanIds(it) == (1..100).map { id -> spanId(id) } })

        // Once the server recovers, the whole queue uploads.
        manager.flushAndWaitForTesting()
        assertTrue(traceFiles().isEmpty())
        assertEquals(List(5) { 100 } + listOf(100, 50), batchSizes())
    }

    @Test
    fun partialSuccessDeletesBatch() {
        apiHelper.mockResponse = JSONObject().put("partialSuccess", JSONObject().put("rejectedSpans", 1))
        recordSpans(2)

        manager.flushAndWaitForTesting()

        assertTrue(traceFiles().isEmpty())
        assertEquals(1, apiHelper.capturedTracePayloads.size)
    }

    @Test
    fun flushDrainsQueueInBatches() {
        recordSpans(450)

        manager.flushAndWaitForTesting()

        assertEquals(listOf(100, 100, 100, 100, 50), batchSizes())
        assertEquals((1..100).map { spanId(it) }, spanIds(apiHelper.capturedTracePayloads.first()))
        assertTrue(traceFiles().isEmpty())
    }

    @Test
    fun flushStopsWhenUploadedBatchCannotBeDeleted() {
        recordSpans(150)
        val directory = traceDirectory()
        directory.setWritable(false)
        try {
            // Root ignores directory permissions, so deletes would still succeed.
            assumeFalse(directory.canWrite())

            manager.flushAndWaitForTesting()

            assertEquals(listOf(100), batchSizes())
            assertEquals(150, traceFiles().size)
        } finally {
            directory.setWritable(true)
        }
    }

    @Test
    fun batchIsSplitAtByteLimit() {
        // Each record is about 20 KB, so six fit under the 128 KiB batch limit.
        recordSpans(10, attributes = mapOf("payload" to "x".repeat(20_000)))

        manager.flushAndWaitForTesting()

        assertEquals(listOf(6, 4), batchSizes())
        assertTrue(traceFiles().isEmpty())
    }

    @Test
    fun spansFromDifferentSdkVersionsAreUploadedInSeparateBatches() {
        writeRecordFile("1000-000000001-a.json", spanId(1), sdkVersion = "3.37.0")
        writeRecordFile("1000-000000002-a.json", spanId(2), sdkVersion = "3.37.0")
        writeRecordFile("1000-000000003-a.json", spanId(3), sdkVersion = "3.38.0")

        manager.flushAndWaitForTesting()

        assertEquals(listOf("3.37.0", "3.38.0"), apiHelper.capturedTracePayloads.map { resourceAttribute(it, "radar.sdk.version") })
        assertEquals(listOf(2, 1), batchSizes())
    }

    @Test
    fun secondFlushWhileUploadInProgressIsIgnored() {
        val holdingHelper = HoldingApiHelper()
        apiClient.apiHelper = holdingHelper
        recordSpans(1)

        manager.flush()
        manager.awaitDiskForTesting()
        manager.flush()
        manager.awaitDiskForTesting()

        assertEquals(1, holdingHelper.payloads.size)
        holdingHelper.nextCallback().onComplete(Radar.RadarStatus.SUCCESS, JSONObject())
        manager.awaitDiskForTesting()
        assertTrue(traceFiles().isEmpty())
    }

    @Test
    fun exceptionDuringUploadEndsFlushSoLaterFlushesRun() {
        apiClient.apiHelper = object : RadarApiHelper() {
            override fun request(
                context: Context,
                method: String,
                path: String,
                headers: Map<String, String>?,
                params: JSONObject?,
                sleep: Boolean,
                callback: RadarApiCallback?,
                extendedTimeout: Boolean,
                stream: Boolean,
                logPayload: Boolean,
                verified: Boolean,
                imageCallback: RadarImageApiCallback?,
                verifiedHostOverride: String?,
                prepareRequest: (() -> JSONObject)?,
                preparationFailureHandler: ((Exception) -> Unit)?,
                queue: RadarRequestQueue,
                traceSpan: RadarTraceSpan?
            ): Unit = throw IllegalStateException("upload failed")
        }
        recordSpans(1)

        manager.flushAndWaitForTesting()
        assertEquals(1, traceFiles().size)

        apiClient.apiHelper = apiHelper
        manager.flushAndWaitForTesting()
        assertTrue(traceFiles().isEmpty())
    }

    @Test
    fun queuedSpansAreNotUploadedUnderAnotherProjectKey() {
        recordSpans(1)

        RadarSettings.setPublishableKey(context, "prj_test_pk_other")
        manager.flushAndWaitForTesting()
        assertEquals(1, traceFiles().size)
        assertTrue(apiHelper.capturedTracePayloads.isEmpty())

        RadarSettings.setPublishableKey(context, PUBLISHABLE_KEY)
        manager.flushAndWaitForTesting()
        assertTrue(traceFiles().isEmpty())
        assertEquals(1, apiHelper.capturedTracePayloads.size)
    }

    // Storage

    @Test
    fun fileLimitEvictsOldestSpans() {
        // Record in two steps so the write queue (also 500) never fills; this test is about the file limit.
        recordSpans(500)
        recordSpans(10, firstId = 501)

        assertEquals(500, traceFiles().size)
        assertEquals((11..510).map { spanId(it) }.toSet(), persistedSpanIds())
    }

    @Test
    fun byteLimitEvictsOldestSpans() {
        recordSpans(30, attributes = mapOf("payload" to "x".repeat(20_000)))

        val files = traceFiles()
        assertTrue(files.sumOf { it.length() } <= 512 * 1024)
        assertTrue(files.size < 30)
        // Whatever survives must be the newest spans.
        assertEquals((1..30).map { spanId(it) }.takeLast(files.size).toSet(), persistedSpanIds())
    }

    @Test
    fun expiredSpanIsDroppedBeforeUpload() {
        recordSpans(1)
        traceFiles().single().setLastModified(System.currentTimeMillis() - 73 * 60 * 60 * 1000L)

        manager.flushAndWaitForTesting()

        assertTrue(traceFiles().isEmpty())
        assertTrue(apiHelper.capturedTracePayloads.isEmpty())
    }

    @Test
    fun inFlightBatchIsNotEvictedWhileUploading() {
        val holdingHelper = HoldingApiHelper()
        apiClient.apiHelper = holdingHelper
        recordSpans(100)

        manager.flush()
        manager.awaitDiskForTesting()
        val inFlight = (1..100).map { spanId(it) }

        // Exceed the file limit while the first batch is uploading. The oldest spans are in flight,
        // so the next oldest are evicted instead.
        recordSpans(500, firstId = 101)
        assertEquals(500, traceFiles().size)
        assertTrue(persistedSpanIds().containsAll(inFlight))

        holdingHelper.nextCallback().onComplete(Radar.RadarStatus.SUCCESS, JSONObject())
        manager.awaitDiskForTesting()

        assertEquals(inFlight, spanIds(holdingHelper.payloads.first()))
        assertEquals((201..600).map { spanId(it) }.toSet(), persistedSpanIds())
    }

    @Test
    fun corruptRecordsAreDeletedAndValidRecordsUploaded() {
        val directory = traceDirectory()
        File(directory, "1000-000000001-a.json").writeText("not json")
        File(directory, "1000-000000002-a.json").writeText(JSONObject().put("project", manager.currentProjectFingerprint()).toString())
        writeRecordFile("1000-000000003-a.json", spanId(3))

        manager.flushAndWaitForTesting()

        assertTrue(traceFiles().isEmpty())
        assertEquals(listOf(spanId(3)), apiHelper.capturedTraceSpans().map { it.getString("spanId") })
    }

    @Test
    fun staleTempFilesAreRemovedOnStartup() {
        val leftover = File(traceDirectory(), "span-123.tmp").apply {
            writeText("partial")
            setLastModified(System.currentTimeMillis() - 2 * 60 * 1000L)
        }

        val restarted = RadarTraceManager(context, apiClient, logger)
        restarted.awaitDiskForTesting()
        restarted.shutdownForTesting()

        assertFalse(leftover.exists())
    }

    @Test
    fun recentTempFileIsNotRemovedByPrune() {
        // Simulates an in-progress write by another manager on the same directory.
        val inProgress = File(traceDirectory(), "span-456.tmp").apply { writeText("partial") }

        recordSpans(1)
        val restarted = RadarTraceManager(context, apiClient, logger)
        restarted.awaitDiskForTesting()
        restarted.shutdownForTesting()

        assertTrue(inProgress.exists())
    }

    @Test
    fun spansPersistedBeforeProcessDeathAreUploadedByNewManager() {
        recordSpans(3)
        manager.shutdownForTesting()

        manager = RadarTraceManager(context, apiClient, logger)
        manager.flushAndWaitForTesting()

        assertTrue(traceFiles().isEmpty())
        assertEquals((1..3).map { spanId(it) }.toSet(), apiHelper.capturedTraceSpans().map { it.getString("spanId") }.toSet())
    }

    @Test
    fun spansBeyondWriteQueueLimitAreDropped() {
        // Hold the disk worker so writes queue up. A flush reads the publishable key on the worker, so block there.
        val held = CountDownLatch(1)
        val release = CountDownLatch(1)
        val testThread = Thread.currentThread()
        val holdingContext = object : ContextWrapper(context) {
            override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences {
                if (Thread.currentThread() != testThread && release.count > 0) {
                    held.countDown()
                    release.await(10, TimeUnit.SECONDS)
                }
                return super.getSharedPreferences(name, mode)
            }
        }
        manager.shutdownForTesting()
        manager = RadarTraceManager(holdingContext, apiClient, logger).also { it.overrideSampledForTesting(true) }
        manager.flush()
        assertTrue("Disk worker was not held", held.await(5, TimeUnit.SECONDS))

        // The queue holds 500 writes, so the newest 100 spans are dropped. Without the limit, all 600 would be
        // written and the storage limits would evict the oldest 100 instead.
        (1..600).forEach { manager.record(span("span", spanId(it)), manager.currentProjectFingerprint(), manager.currentSessionId()) }
        release.countDown()
        manager.flushAndWaitForTesting()

        assertEquals((1..500).map { spanId(it) }, apiHelper.capturedTraceSpans().map { it.getString("spanId") })

        // Once the queue drains, new spans are accepted again.
        recordSpans(1, firstId = 601)
        manager.flushAndWaitForTesting()

        assertEquals(spanId(601), apiHelper.capturedTraceSpans().last().getString("spanId"))
    }

    @Test
    fun recordAndFlushAfterShutdownDoNotThrow() {
        manager.shutdownForTesting()

        manager.record(span("span", spanId(1)), manager.currentProjectFingerprint(), manager.currentSessionId())
        manager.flush()
    }

    // Sampling

    @Test
    fun spansWaitForSamplingDecisionBeforeUpload() {
        useServerSampling(sessionId = 1000)
        recordSpans(2)

        manager.flushAndWaitForTesting()
        assertTrue(apiHelper.capturedTracePayloads.isEmpty())
        assertEquals(2, pendingFiles().size)
        assertEquals(RadarTraceManager.SamplingState.PENDING, manager.samplingState("1000"))

        manager.onSamplingDecision("1000", sampled = true)
        manager.flushAndWaitForTesting()

        assertEquals(listOf(spanId(1), spanId(2)), spanIds(apiHelper.capturedTracePayloads.single()))
        assertTrue(pendingFiles().isEmpty())
        assertTrue(traceFiles().isEmpty())
    }

    @Test
    fun spansAfterSampledDecisionAreWrittenUploadable() {
        useServerSampling(sessionId = 1000)
        manager.onSamplingDecision("1000", sampled = true)

        recordSpans(1)

        assertTrue(pendingFiles().isEmpty())
        assertEquals(1, traceFiles().size)
    }

    @Test
    fun notSampledDecisionDeletesPendingSpansAndDropsLaterSpans() {
        useServerSampling(sessionId = 1000)
        recordSpans(2)

        manager.onSamplingDecision("1000", sampled = false)
        recordSpans(1, firstId = 3)
        manager.flushAndWaitForTesting()

        assertEquals(RadarTraceManager.SamplingState.NOT_SAMPLED, manager.samplingState("1000"))
        assertTrue(pendingFiles().isEmpty())
        assertTrue(traceFiles().isEmpty())
        assertTrue(apiHelper.capturedTracePayloads.isEmpty())
    }

    @Test
    fun replacedSessionKeepsPendingSpansForItsOwnDecision() {
        // Session 1000 is replaced before its decision arrives, e.g. from a track that was in flight when the app
        // was resumed. Session 2000's decision must not delete session 1000's spans.
        useServerSampling(sessionId = 1000)
        recordSpans(2)
        setSessionId(2000)
        recordSpans(1, firstId = 3)

        manager.onSamplingDecision("2000", sampled = true)
        manager.flushAndWaitForTesting()
        assertEquals(listOf(spanId(3)), apiHelper.capturedTraceSpans().map { it.getString("spanId") })
        assertEquals(2, pendingFiles().size)

        manager.onSamplingDecision("1000", sampled = true)
        manager.flushAndWaitForTesting()
        assertEquals(setOf(spanId(1), spanId(2), spanId(3)), apiHelper.capturedTraceSpans().map { it.getString("spanId") }.toSet())
        assertTrue(pendingFiles().isEmpty())
    }

    @Test
    fun spansFromDecidedSessionUseItsDecisionAfterSessionChanges() {
        // A trace started in session 1000 ends after the session changed and the new session was decided.
        useServerSampling(sessionId = 1000)
        manager.onSamplingDecision("1000", sampled = true)
        setSessionId(2000)
        manager.onSamplingDecision("2000", sampled = false)

        manager.record(span("span", spanId(1)), manager.currentProjectFingerprint(), "1000")
        manager.record(span("span", spanId(2)), manager.currentProjectFingerprint(), "2000")
        manager.flushAndWaitForTesting()

        assertEquals(listOf(spanId(1)), apiHelper.capturedTraceSpans().map { it.getString("spanId") })
        assertTrue(pendingFiles().isEmpty())
    }

    @Test
    fun firstDecisionForSessionIsFinal() {
        useServerSampling(sessionId = 1000)
        manager.onSamplingDecision("1000", sampled = true)

        // E.g. a track response after the sampling rate was lowered, or a sporadic false.
        manager.onSamplingDecision("1000", sampled = false)
        recordSpans(1)
        manager.flushAndWaitForTesting()

        assertEquals(RadarTraceManager.SamplingState.SAMPLED, manager.samplingState("1000"))
        assertEquals(listOf(spanId(1)), apiHelper.capturedTraceSpans().map { it.getString("spanId") })
    }

    @Test
    fun firstNotSampledDecisionIsFinal() {
        useServerSampling(sessionId = 1000)
        manager.onSamplingDecision("1000", sampled = false)

        manager.onSamplingDecision("1000", sampled = true)
        recordSpans(1)
        manager.flushAndWaitForTesting()

        assertEquals(RadarTraceManager.SamplingState.NOT_SAMPLED, manager.samplingState("1000"))
        assertTrue(apiHelper.capturedTracePayloads.isEmpty())
        assertTrue(pendingFiles().isEmpty())
    }

    @Test
    fun startupDeletesPendingSpansFromOtherSessions() {
        // The previous process's session was replaced, so no request can carry its decision anymore.
        useServerSampling(sessionId = 1000)
        recordSpans(2)
        manager.shutdownForTesting()
        setSessionId(2000)

        manager = RadarTraceManager(context, apiClient, logger)
        manager.awaitDiskForTesting()

        assertTrue(pendingFiles().isEmpty())
    }

    @Test
    fun lateDecisionForReplacedSessionKeepsCurrentSessionPending() {
        // The config request was sent in session 1000, and the session changed before the response arrived.
        useServerSampling(sessionId = 1000)
        recordSpans(1)
        setSessionId(2000)
        recordSpans(1, firstId = 2)

        manager.onSamplingDecision("1000", sampled = true)
        manager.flushAndWaitForTesting()

        assertEquals(listOf(spanId(1)), apiHelper.capturedTraceSpans().map { it.getString("spanId") })
        assertEquals(RadarTraceManager.SamplingState.PENDING, manager.samplingState("2000"))
        assertEquals(1, pendingFiles().size)

        manager.onSamplingDecision("2000", sampled = false)
        manager.awaitDiskForTesting()
        assertTrue(pendingFiles().isEmpty())
    }

    @Test
    fun newSessionIsPendingUntilItsOwnDecision() {
        useServerSampling(sessionId = 1000)
        manager.onSamplingDecision("1000", sampled = true)
        assertEquals(RadarTraceManager.SamplingState.SAMPLED, manager.samplingState(manager.currentSessionId()))

        setSessionId(2000)

        assertEquals(RadarTraceManager.SamplingState.PENDING, manager.samplingState(manager.currentSessionId()))
    }

    @Test
    fun pendingSpansAreResolvedAfterProcessDeathInTheSameSession() {
        useServerSampling(sessionId = 1000)
        recordSpans(2)
        manager.shutdownForTesting()

        manager = RadarTraceManager(context, apiClient, logger)
        manager.onSamplingDecision("1000", sampled = true)
        manager.flushAndWaitForTesting()

        assertEquals(setOf(spanId(1), spanId(2)), apiHelper.capturedTraceSpans().map { it.getString("spanId") }.toSet())
        assertTrue(pendingFiles().isEmpty())
    }

    @Test
    fun pendingSpansCountTowardStorageLimits() {
        useServerSampling(sessionId = 1000)
        recordSpans(500)
        recordSpans(10, firstId = 501)

        assertEquals(500, pendingFiles().size)
    }

    /** Uses the server's sampling decisions instead of sampling every session, starting in [sessionId]. */
    private fun useServerSampling(sessionId: Long) {
        manager.overrideSampledForTesting(null)
        setSessionId(sessionId)
    }

    private fun setSessionId(sessionId: Long) {
        context.getSharedPreferences("RadarSDK", Context.MODE_PRIVATE).edit().putLong("session_id", sessionId).commit()
    }

    private fun recordSpans(count: Int, firstId: Int = 1, attributes: Map<String, Any> = emptyMap()) {
        (firstId until firstId + count).forEach { manager.record(span("span", spanId(it), attributes = attributes), manager.currentProjectFingerprint(), manager.currentSessionId()) }
        manager.awaitDiskForTesting()
    }

    private fun writeRecordFile(name: String, spanId: String, sdkVersion: String = RadarUtils.SDK_VERSION) {
        val directory = traceDirectory()
        val record = JSONObject()
            .put("project", manager.currentProjectFingerprint())
            .put("sdkVersion", sdkVersion)
            .put("span", span("span", spanId).toJson())
        File(directory, name).writeText(record.toString())
    }

    private fun traceDirectory(): File = File(temp.root, "radar_traces").apply { mkdirs() }

    private fun traceFiles(): List<File> = traceDirectory().listFiles()?.filter { it.extension == "json" } ?: emptyList()

    private fun pendingFiles(): List<File> = traceDirectory().listFiles()?.filter { it.extension == "pending" } ?: emptyList()

    private fun persistedSpanIds(): Set<String> = traceFiles().map { JSONObject(it.readText()).getJSONObject("span").getString("spanId") }.toSet()

    private fun spanId(index: Int): String = String.format(Locale.US, "%016x", index)

    private fun spans(payload: JSONObject) = payload.getJSONArray("resourceSpans").getJSONObject(0)
        .getJSONArray("scopeSpans").getJSONObject(0).getJSONArray("spans")

    /** Span count of each upload so far, in upload order. */
    private fun batchSizes(): List<Int> = apiHelper.capturedTracePayloads.map { spans(it).length() }

    /** Span IDs in upload order. */
    private fun spanIds(payload: JSONObject): List<String> {
        val spans = spans(payload)
        return (0 until spans.length()).map { spans.getJSONObject(it).getString("spanId") }
    }

    private fun resourceAttribute(payload: JSONObject, key: String): String? {
        val attributes = payload.getJSONArray("resourceSpans").getJSONObject(0).getJSONObject("resource").getJSONArray("attributes")
        return (0 until attributes.length()).map { attributes.getJSONObject(it) }
            .firstOrNull { it.getString("key") == key }
            ?.getJSONObject("value")?.getString("stringValue")
    }

    private fun span(
        name: String,
        spanId: String,
        attributes: Map<String, Any> = emptyMap()
    ): RadarCompletedSpan = RadarCompletedSpan(
        name,
        "0123456789abcdef0123456789abcdef",
        spanId,
        null,
        RadarSpanKind.INTERNAL,
        1_780_000_000_000_000_000,
        1_780_000_000_100_000_000,
        Radar.RadarStatus.SUCCESS,
        attributes
    )

    /** Holds trace upload callbacks so tests can act while an upload is in flight. */
    private class HoldingApiHelper : RadarApiHelper() {
        val payloads: MutableList<JSONObject> = Collections.synchronizedList(mutableListOf())
        private val callbacks = LinkedBlockingQueue<RadarApiCallback>()

        /** The next held upload callback. Fails instead of hanging if no upload starts. */
        fun nextCallback(): RadarApiCallback {
            val callback = callbacks.poll(5, TimeUnit.SECONDS)
            assertNotNull("No trace upload started", callback)
            return callback!!
        }

        override fun request(
            context: Context,
            method: String,
            path: String,
            headers: Map<String, String>?,
            params: JSONObject?,
            sleep: Boolean,
            callback: RadarApiCallback?,
            extendedTimeout: Boolean,
            stream: Boolean,
            logPayload: Boolean,
            verified: Boolean,
            imageCallback: RadarImageApiCallback?,
            verifiedHostOverride: String?,
            prepareRequest: (() -> JSONObject)?,
            preparationFailureHandler: ((Exception) -> Unit)?,
            queue: RadarRequestQueue,
            traceSpan: RadarTraceSpan?
        ) {
            params?.let { payloads.add(it) }
            callback?.let { callbacks.add(it) }
        }
    }

    private companion object {
        const val PUBLISHABLE_KEY = "prj_test_pk_trace"
    }
}
