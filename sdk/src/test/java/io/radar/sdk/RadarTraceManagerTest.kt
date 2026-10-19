package io.radar.sdk

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.radar.sdk.helpers.RadarApiHelperMock
import java.io.File
import java.util.Collections
import java.util.Locale
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
        manager = RadarTraceManager(context, apiClient, logger)
    }

    @After
    fun tearDown() {
        manager.shutdownForTesting()
    }

    // Upload

    @Test
    fun persistsAndExportsOtlpEnvelopeThenDeletesAcknowledgedFiles() {
        manager.record(span("one", spanId(1)), manager.currentProjectFingerprint())
        manager.record(span("two", spanId(2)), manager.currentProjectFingerprint())
        manager.awaitDiskForTesting()
        assertEquals(2, traceFiles().size)

        manager.flushAndWaitForTesting()

        assertTrue(traceFiles().isEmpty())
        val payload = apiHelper.capturedTracePayloads.single()
        assertEquals("radar-client", resourceAttribute(payload, "service.name"))
        assertEquals("android", resourceAttribute(payload, "os.type"))
        assertEquals(RadarUtils.SDK_VERSION, resourceAttribute(payload, "radar.sdk.version"))
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
    fun repeatedServerErrorsDropBatch() {
        apiHelper.queueMockStatuses("v1/traces", List(3) { Radar.RadarStatus.ERROR_SERVER })
        recordSpans(1)

        manager.flushAndWaitForTesting()
        manager.flushAndWaitForTesting()
        // Batch is persisted after two failures
        assertEquals(1, traceFiles().size)

        manager.flushAndWaitForTesting()
        // Batch is dropped after 3rd failure
        assertTrue(traceFiles().isEmpty())
        assertEquals(3, apiHelper.capturedTracePayloads.size)
    }

    @Test
    fun networkErrorsDoNotCountTowardServerErrors() {
        apiHelper.queueMockStatuses(
            "v1/traces",
            listOf(Radar.RadarStatus.ERROR_SERVER, Radar.RadarStatus.ERROR_NETWORK, Radar.RadarStatus.ERROR_SERVER, Radar.RadarStatus.ERROR_SERVER)
        )
        recordSpans(1)

        repeat(3) { manager.flushAndWaitForTesting() }
        assertEquals(1, traceFiles().size)

        manager.flushAndWaitForTesting()
        assertTrue(traceFiles().isEmpty())
        assertEquals(4, apiHelper.capturedTracePayloads.size)
    }

    @Test
    fun successResetsServerErrorCount() {
        apiHelper.queueMockStatuses(
            "v1/traces",
            listOf(Radar.RadarStatus.ERROR_SERVER, Radar.RadarStatus.ERROR_SERVER, Radar.RadarStatus.SUCCESS, Radar.RadarStatus.ERROR_SERVER, Radar.RadarStatus.ERROR_SERVER)
        )
        recordSpans(1)
        repeat(3) { manager.flushAndWaitForTesting() }
        assertTrue(traceFiles().isEmpty())

        // Without the reset, the next server error would be the third in a row and drop this span.
        recordSpans(1, firstId = 2)
        repeat(2) { manager.flushAndWaitForTesting() }
        assertEquals(setOf(spanId(2)), persistedSpanIds())
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
                queue: RadarRequestQueue
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
        recordSpans(510)

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
    fun recordAndFlushAfterShutdownDoNotThrow() {
        manager.shutdownForTesting()

        manager.record(span("span", spanId(1)), manager.currentProjectFingerprint())
        manager.flush()
    }

    private fun recordSpans(count: Int, firstId: Int = 1, attributes: Map<String, Any> = emptyMap()) {
        (firstId until firstId + count).forEach { manager.record(span("span", spanId(it), attributes = attributes), manager.currentProjectFingerprint()) }
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
            queue: RadarRequestQueue
        ) {
            params?.let { payloads.add(it) }
            callback?.let { callbacks.add(it) }
        }
    }

    private companion object {
        const val PUBLISHABLE_KEY = "prj_test_pk_trace"
    }
}
