package io.radar.sdk

import android.content.Context
import androidx.annotation.VisibleForTesting
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import org.json.JSONArray
import org.json.JSONObject

/**
 * Durable storage and batched OTLP/JSON export for SDK spans.
 *
 * Disk and upload state is confined to a single disk worker thread, and the private functions that
 * touch it run there. [record] and [flush] hand their work to that thread and return immediately.
 *
 * Uploads run on [RadarRequestQueue.BACKGROUND] so they never delay track requests. Like log uploads,
 * flushes are triggered after successful API calls, so failed batches stay on disk and are retried on
 * the next trigger instead of on a timer.
 */
internal class RadarTraceManager(
    private val context: Context,
    private val apiClient: RadarApiClient,
    private val logger: RadarLogger
) {
    companion object {
        // TODO(jordan-nelson): replace hard coded SAMPLED with sampling decision from server.
        private const val SAMPLED = false
        private const val MAX_FILES = 500
        private const val MAX_BYTES = 512 * 1024L
        private val MAX_AGE = 72.hours
        private val STALE_TEMP_FILE_AGE = 1.minutes
        private const val MAX_BATCH_SPANS = 100
        private const val MAX_BATCH_BYTES = 128 * 1024
        private const val DIR = "radar_traces"
    }

    private val disk = Executors.newSingleThreadExecutor()
    private val directory = File(context.filesDir, DIR)
    private val isFlushing = AtomicBoolean(false)

    // Accessed only on the disk worker. inFlightFiles protects a selected batch from pruning during upload.
    private var inFlightFiles: Set<File> = emptySet()
    private var fileSequence = 0L

    @Volatile
    private var sampledOverride: Boolean? = null

    init {
        disk.execute {
            directory.mkdirs()
            prune()
        }
    }

    /** Whether new spans should be recorded. */
    fun isSampled(): Boolean = sampledOverride ?: SAMPLED

    /** Queues a completed span to be written to disk on the disk worker. Never throws into SDK call paths. */
    fun record(span: RadarCompletedSpan, project: String?) {
        try {
            val record = JSONObject()
                .put("project", project)
                .put("sdkVersion", RadarUtils.SDK_VERSION)
                .put("span", span.toJson())
            runOnDisk { write(record) }
        } catch (e: Exception) {
            logger.e("Could not record trace span | message = ${e.message}", Radar.RadarLogType.SDK_ERROR, e)
        }
    }

    /**
     * Uploads batches on the disk worker until the queue is empty or an upload does not succeed. The storage
     * limits keep a full backlog to about 5 batches.
     */
    fun flush() {
        if (!isFlushing.compareAndSet(false, true)) return
        runOnDisk { uploadNextBatch() }
    }

    /** Runs a task on the disk worker. */
    private fun runOnDisk(task: () -> Unit) {
        try {
            disk.execute(task)
        } catch (_: RejectedExecutionException) {
            // Only happens after shutdown (today, only shutdownForTesting()). No disk work can run, so drop the task
            // rather than throw into the caller.
            logger.e("Dropped trace task; disk worker is shut down", Radar.RadarLogType.SDK_ERROR)
        }
    }

    /** Selects and uploads the next batch, continuing from the upload callback. */
    private fun uploadNextBatch() {
        try {
            // Read the key once so the same key selects the batch and authenticates the upload.
            val publishableKey = RadarSettings.getPublishableKey(context)
            val batch = if (publishableKey != null) select(fingerprint(publishableKey)) else null
            if (publishableKey == null || batch == null) {
                finishFlush()
                return
            }
            apiClient.postTraces(
                batch.payload,
                publishableKey,
                object : RadarApiClient.RadarTraceCallback {
                    override fun onComplete(status: Radar.RadarStatus, res: JSONObject?) {
                        runOnDisk {
                            try {
                                if (handleResponse(batch, status, res)) {
                                    uploadNextBatch()
                                } else {
                                    finishFlush()
                                }
                            } catch (e: Exception) {
                                logger.e("Could not handle trace upload response | message = ${e.message}", Radar.RadarLogType.SDK_ERROR, e)
                                finishFlush()
                            }
                        }
                    }
                }
            )
        } catch (e: Exception) {
            logger.e("Could not upload trace batch | message = ${e.message}", Radar.RadarLogType.SDK_ERROR, e)
            // Any exception must end the flush, or isFlushing stays set and uploads stop for the process.
            finishFlush()
        }
    }

    /** Ends the current flush. */
    private fun finishFlush() {
        inFlightFiles = emptySet()
        isFlushing.set(false)
    }

    /** Applies the upload result to the batch. Returns true if the flush should continue with the next batch. */
    private fun handleResponse(batch: Batch, status: Radar.RadarStatus, res: JSONObject?): Boolean {
        inFlightFiles = emptySet()
        when (status) {
            Radar.RadarStatus.SUCCESS -> {
                batch.files.forEach { it.delete() }
                res?.optJSONObject("partialSuccess")?.let {
                    logger.w("Trace batch partially accepted | rejectedSpans = ${it.optInt("rejectedSpans")}; not retrying")
                }
                // If the batch can't be deleted, the next select would upload it again, so stop instead of looping.
                if (batch.files.any { it.exists() }) {
                    logger.e("Could not delete uploaded trace batch; stopping flush", Radar.RadarLogType.SDK_ERROR)
                    return false
                }
                return true
            }

            // Transient: keep the batch for the next flush. A batch the server can never accept is retried until it
            // ages out or is evicted by the storage limits.
            Radar.RadarStatus.ERROR_NETWORK, Radar.RadarStatus.ERROR_RATE_LIMIT, Radar.RadarStatus.ERROR_SERVER -> return false

            // Not retryable (bad request, auth, payload too large, etc.).
            else -> {
                batch.files.forEach { it.delete() }
                logger.e("Dropped trace batch | status = $status; res = ${res?.toString()?.take(1000)}", Radar.RadarLogType.SDK_ERROR)
                return false
            }
        }
    }

    /** Span files selected for one upload, and the OTLP request body built from them. */
    private class Batch(val files: List<File>, val payload: JSONObject)

    /** A span record read from disk. */
    private class StoredSpan(val file: File, val project: String, val sdkVersion: String, val span: JSONObject)

    /** Builds the next OTLP batch from the oldest spans for a project, or null if there are none. */
    private fun select(project: String): Batch? {
        prune()
        val selected = mutableListOf<StoredSpan>()
        var bytes = 0L
        for (file in files()) {
            val stored = read(file) ?: continue
            if (stored.project != project) continue
            if (!fitsInBatch(selected, bytes, stored)) break
            selected.add(stored)
            bytes += file.length()
        }
        if (selected.isEmpty()) return null
        val files = selected.map { it.file }
        inFlightFiles = files.toSet()
        return Batch(files, otlpPayload(selected.map { it.span }, selected.first().sdkVersion))
    }

    /** Reads a span record, deleting it if it is corrupt. */
    private fun read(file: File): StoredSpan? {
        val record = try {
            JSONObject(file.readText())
        } catch (_: Exception) {
            null
        }
        val span = record?.optJSONObject("span")
        if (record == null || span == null) {
            file.delete()
            return null
        }
        return StoredSpan(file, record.optString("project"), record.optString("sdkVersion"), span)
    }

    /**
     * A batch holds spans from one SDK version, up to the span and byte limits. The first span always fits.
     * One version per batch because [otlpPayload] stamps the version on the batch's single resource.
     */
    private fun fitsInBatch(selected: List<StoredSpan>, bytes: Long, next: StoredSpan): Boolean {
        if (selected.isEmpty()) return true
        return next.sdkVersion == selected.first().sdkVersion &&
            selected.size < MAX_BATCH_SPANS &&
            bytes + next.file.length() <= MAX_BATCH_BYTES
    }

    /** Wraps spans in an OTLP ExportTraceServiceRequest. */
    private fun otlpPayload(spans: List<JSONObject>, sdkVersion: String): JSONObject {
        val resource = JSONObject().put(
            "attributes",
            JSONArray()
                .put(attribute("service.name", "radar-client"))
                .put(attribute("os.type", "android"))
                .put(attribute("radar.sdk.version", sdkVersion))
        )
        val scopeSpans = JSONObject()
            .put("scope", JSONObject().put("name", "io.radar.sdk").put("version", sdkVersion))
            .put("spans", JSONArray(spans))
        val resourceSpans = JSONObject()
            .put("resource", resource)
            .put("scopeSpans", JSONArray().put(scopeSpans))
        return JSONObject().put("resourceSpans", JSONArray().put(resourceSpans))
    }

    /**
     * Atomically writes one span record, then enforces the storage limits.
     *
     * Each span is one file, `<directory>/<millis>-<sequence>-<uuid>.json`, containing
     * `{"project": <key fingerprint>, "sdkVersion": <version>, "span": <OTLP span>}`. Names sort oldest
     * first: millis, then a sequence for spans written in the same millisecond. The file is written to a
     * `.tmp` file, synced, and renamed, so readers never see a partial record.
     */
    private fun write(record: JSONObject) {
        var temp: File? = null
        try {
            directory.mkdirs()
            temp = File.createTempFile("span-", ".tmp", directory)
            FileOutputStream(temp).use { stream ->
                stream.write(record.toString().toByteArray(Charsets.UTF_8))
                stream.fd.sync()
            }
            val name = String.format(Locale.US, "%d-%09d-%s.json", System.currentTimeMillis(), fileSequence++, UUID.randomUUID())
            val target = File(directory, name)
            if (!temp.renameTo(target)) throw IllegalStateException("Could not commit trace span")
            prune()
        } catch (e: Exception) {
            logger.e("Could not persist trace span | message = ${e.message}", Radar.RadarLogType.SDK_ERROR, e)
        } finally {
            temp?.delete()
        }
    }

    /** Committed span records, oldest first. */
    private fun files(): List<File> = directory.listFiles()?.filter { it.extension == "json" }?.sortedBy { it.name } ?: emptyList()

    /** Removes stale temp files and expired spans, then evicts the oldest spans until within limits. */
    private fun prune() {
        val now = System.currentTimeMillis()
        // Only remove old temp files left by a crash. A recent one may be an in-progress write by another manager
        directory.listFiles()?.filter { it.extension == "tmp" && now - it.lastModified() > STALE_TEMP_FILE_AGE.inWholeMilliseconds }?.forEach { it.delete() }
        val files = files().toMutableList()
        files.filter { it !in inFlightFiles && now - it.lastModified() > MAX_AGE.inWholeMilliseconds }.forEach { it.delete() }
        files.removeAll { !it.exists() }
        var bytes = files.sumOf { it.length() }
        while (files.size > MAX_FILES || bytes > MAX_BYTES) {
            val index = files.indexOfFirst { it !in inFlightFiles }
            if (index < 0) break
            val oldest = files.removeAt(index)
            bytes -= oldest.length()
            oldest.delete()
        }
    }

    /** Fingerprint of the current publishable key, or null if none is set. */
    internal fun currentProjectFingerprint(): String? = RadarSettings.getPublishableKey(context)?.let { fingerprint(it) }

    /**
     * Tags each span record with its project so spans are only uploaded under the key they were recorded with,
     * without writing the key (or an auth token) to disk.
     */
    private fun fingerprint(key: String): String {
        val hash = MessageDigest.getInstance("SHA-256").digest(key.toByteArray(Charsets.UTF_8))
        return hash.take(8).joinToString("") { String.format(Locale.US, "%02x", it) }
    }

    /** An OTLP string attribute. */
    private fun attribute(key: String, value: String): JSONObject = JSONObject()
        .put("key", key)
        .put("value", JSONObject().put("stringValue", value))

    // Test hooks

    /** Forces spans to be recorded (true) or not (false), e.g. to trace 100% locally. Pass null to restore the default. */
    @VisibleForTesting
    internal fun overrideSampledForTesting(sampled: Boolean?) {
        sampledOverride = sampled
    }

    /** Waits for any flush in progress, then runs a full flush and waits for it to finish. */
    @VisibleForTesting
    internal fun flushAndWaitForTesting() {
        waitForFlushForTesting()
        flush()
        waitForFlushForTesting()
    }

    /** Waits for queued disk work, including span writes, to finish. */
    @VisibleForTesting
    internal fun awaitDiskForTesting() {
        disk.submit {}.get(10, TimeUnit.SECONDS)
    }

    /** Waits until no flush is in progress. Throws after 10 seconds so a stuck flush fails the test. */
    private fun waitForFlushForTesting() {
        val deadline = System.currentTimeMillis() + 10_000
        while (isFlushing.get() && System.currentTimeMillis() < deadline) {
            disk.submit {}.get(1, TimeUnit.SECONDS)
        }
        check(!isFlushing.get()) { "Trace flush did not finish within 10 seconds" }
    }

    /** Stops the disk worker. Later records and flushes are dropped. */
    @VisibleForTesting
    internal fun shutdownForTesting() {
        disk.shutdownNow()
    }
}
