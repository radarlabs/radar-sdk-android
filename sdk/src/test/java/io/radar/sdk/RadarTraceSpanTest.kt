package io.radar.sdk

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.radar.sdk.helpers.RadarApiHelperMock
import java.io.File
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class RadarTraceSpanTest {
    @get:Rule val temp = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var logger: RadarLogger
    private lateinit var manager: RadarTraceManager
    private lateinit var apiHelper: RadarApiHelperMock
    private val defaultWallClock = RadarTraceSpan.wallClockMs

    @Before
    fun setUp() {
        val application = ApplicationProvider.getApplicationContext<Context>()
        context = object : ContextWrapper(application) {
            override fun getFilesDir(): File = temp.root
        }
        RadarSettings.setPublishableKey(context, "prj_test_pk_trace")
        logger = RadarLogger(context)
        apiHelper = RadarApiHelperMock().apply {
            mockStatus = Radar.RadarStatus.SUCCESS
            mockResponse = JSONObject()
        }
        val apiClient = RadarApiClient(context, logger).also { it.apiHelper = apiHelper }
        manager = RadarTraceManager(context, apiClient, logger).also { it.overrideSampledForTesting(true) }
        Radar.traceManager = manager
    }

    @After
    fun tearDown() {
        RadarTraceSpan.wallClockMs = defaultWallClock
        Radar.traceManager = null
        manager.shutdownForTesting()
    }

    @Test
    fun childSpansShareTraceAndLinkToParent() {
        val root = RadarTraceSpan.start("root")
        val internal = root.child("internal")
        val client = root.child("HTTP POST /v1/test", RadarSpanKind.CLIENT)
        client.end(Radar.RadarStatus.SUCCESS)
        internal.end(Radar.RadarStatus.SUCCESS)
        root.end(Radar.RadarStatus.SUCCESS)

        val spans = uploadedSpans()
        val rootJson = spans.getValue("root")
        assertFalse(rootJson.has("parentSpanId"))
        assertEquals(RadarSpanKind.INTERNAL.otlpValue, rootJson.getInt("kind"))
        listOf("internal", "HTTP POST /v1/test").forEach { name ->
            assertEquals(rootJson.getString("traceId"), spans.getValue(name).getString("traceId"))
            assertEquals(rootJson.getString("spanId"), spans.getValue(name).getString("parentSpanId"))
        }
        assertEquals(RadarSpanKind.INTERNAL.otlpValue, spans.getValue("internal").getInt("kind"))
        assertEquals(RadarSpanKind.CLIENT.otlpValue, spans.getValue("HTTP POST /v1/test").getInt("kind"))
        assertEquals("00-${client.traceId}-${client.spanId}-01", client.traceparent())
    }

    @Test
    fun idsAreLowercaseHexAndUnique() {
        val spans = (1..50).map { RadarTraceSpan.start("root") }

        spans.forEach {
            assertTrue(it.traceId, it.traceId.matches(Regex("[0-9a-f]{32}")))
            assertTrue(it.spanId, it.spanId.matches(Regex("[0-9a-f]{16}")))
        }
        assertEquals(50, spans.map { it.traceId }.toSet().size)
        assertEquals(50, spans.map { it.spanId }.toSet().size)
    }

    @Test
    fun endIsIdempotent() {
        val span = RadarTraceSpan.start("root")
        span.end(Radar.RadarStatus.SUCCESS)
        span.end(Radar.RadarStatus.ERROR_SERVER)

        manager.flushAndWaitForTesting()

        // Recorded once, with the first end's status.
        val json = apiHelper.capturedTraceSpans().single()
        assertFalse(json.has("status"))
        assertNull(attribute(json, "error.type"))
    }

    @Test
    fun attributesSetAfterEndAreIgnored() {
        val span = RadarTraceSpan.start("root")
        span.setAttribute("before", "1")
        span.end(Radar.RadarStatus.SUCCESS)
        span.setAttribute("after", "2")

        val json = uploadedSpans().getValue("root")
        assertNotNull(attribute(json, "before"))
        assertNull(attribute(json, "after"))
    }

    @Test
    fun attributesAreEncodedAsOtlpValues() {
        val span = RadarTraceSpan.start("root", mapOf("bool" to true))
        span.setAttribute("string", "medium")
        span.setAttribute("long", 42L)
        span.setAttribute("double", 1.5)
        span.end(Radar.RadarStatus.SUCCESS)

        val json = uploadedSpans().getValue("root")
        assertEquals("medium", attribute(json, "string")?.get("stringValue"))
        // OTLP/JSON encodes int64 as a decimal string. get() checks the type, unlike getString(), which converts.
        assertEquals("42", attribute(json, "long")?.get("intValue"))
        assertEquals(1.5, attribute(json, "double")!!.getDouble("doubleValue"), 0.0)
        assertTrue(attribute(json, "bool")!!.getBoolean("boolValue"))
    }

    @Test
    fun errorStatusSetsOtlpErrorAndErrorType() {
        RadarTraceSpan.start("root").end(Radar.RadarStatus.ERROR_NETWORK)

        val json = uploadedSpans().getValue("root")
        assertEquals(2, json.getJSONObject("status").getInt("code"))
        assertEquals("ERROR_NETWORK", attribute(json, "error.type")?.getString("stringValue"))
    }

    @Test
    fun timestampsAreUnixNanoStrings() {
        RadarTraceSpan.wallClockMs = { 1_780_000_000_000L }
        val span = RadarTraceSpan.start("root")
        ShadowSystemClock.advanceBy(Duration.ofMillis(250))
        span.end(Radar.RadarStatus.SUCCESS)

        // OTLP/JSON encodes fixed64 timestamps as decimal strings. get() checks the type, unlike getString().
        val json = uploadedSpans().getValue("root")
        assertEquals("1780000000000000000", json.get("startTimeUnixNano"))
        assertEquals("1780000000250000000", json.get("endTimeUnixNano"))
    }

    @Test
    fun durationUsesMonotonicClock() {
        val span = RadarTraceSpan.start("root")
        ShadowSystemClock.advanceBy(Duration.ofMillis(1500))
        span.end(Radar.RadarStatus.SUCCESS)

        val json = uploadedSpans().getValue("root")
        assertEquals(1_500_000_000L, json.getString("endTimeUnixNano").toLong() - json.getString("startTimeUnixNano").toLong())
    }

    @Test
    fun unsampledSpanIsNoOp() {
        manager.overrideSampledForTesting(false)

        val root = RadarTraceSpan.start("root")
        val child = root.child("HTTP POST /v1/test", RadarSpanKind.CLIENT)
        child.end(Radar.RadarStatus.SUCCESS)
        root.end(Radar.RadarStatus.SUCCESS)

        assertNull(root.traceparent())
        assertNull(child.traceparent())
        assertSame(root, child)
        assertSame(root, RadarTraceSpan.start("other"))
        manager.flushAndWaitForTesting()
        assertTrue(apiHelper.capturedTracePayloads.isEmpty())
    }

    @Test
    fun spanIsNoOpWithoutManager() {
        Radar.traceManager = null

        val root = RadarTraceSpan.start("root")
        root.end(Radar.RadarStatus.SUCCESS)

        assertNull(root.traceparent())
        assertNull(root.child("child").traceparent())
    }

    @Test
    fun spanIsNoOpWithoutPublishableKey() {
        RadarSettings.setPublishableKey(context, null)

        val root = RadarTraceSpan.start("root")
        root.end(Radar.RadarStatus.SUCCESS)

        assertNull(root.traceparent())
        manager.awaitDiskForTesting()
        assertTrue(File(temp.root, "radar_traces").listFiles()?.none { it.extension == "json" } ?: true)
    }

    @Test
    fun attributeUpdatesRacingEndDoNotDropSpans() {
        // Writers keep adding attributes while end() serializes the span. Each new key changes the map's structure,
        // which breaks an unsynchronized serialization and drops the span. A race test only fails when it hits the
        // window, so it runs many spans and ends each one while the writers are mid-way through.
        val writers = 3
        val spans = 200
        val pool = Executors.newFixedThreadPool(writers)
        try {
            repeat(spans) { i ->
                val span = RadarTraceSpan.start("span-$i")
                val writing = CountDownLatch(writers)
                val writes = (0 until writers).map { w ->
                    pool.submit {
                        // Writers keep going through serialization; writes after end() are ignored. Keys repeat after
                        // 200 so a span holds at most 600 attributes and stays well under the storage limits.
                        repeat(2_000) { k ->
                            span.setAttribute("w$w-${k % 200}", k.toLong())
                            if (k == 20) writing.countDown()
                        }
                    }
                }
                writing.await(5, TimeUnit.SECONDS)
                span.end(Radar.RadarStatus.SUCCESS)
                writes.forEach { it.get(5, TimeUnit.SECONDS) }
                // Upload as we go so the spans stay well under the storage limits.
                if (i % 10 == 9) manager.flushAndWaitForTesting()
            }
        } finally {
            pool.shutdownNow()
        }

        assertEquals((0 until spans).map { "span-$it" }.toSet(), apiHelper.capturedTraceSpans().map { it.getString("name") }.toSet())
    }

    private fun uploadedSpans(): Map<String, JSONObject> {
        manager.flushAndWaitForTesting()
        return apiHelper.capturedTraceSpans().associateBy { it.getString("name") }
    }

    private fun attribute(span: JSONObject, key: String): JSONObject? {
        val attributes = span.getJSONArray("attributes")
        return (0 until attributes.length()).map { attributes.getJSONObject(it) }
            .firstOrNull { it.getString("key") == key }
            ?.getJSONObject("value")
    }
}
