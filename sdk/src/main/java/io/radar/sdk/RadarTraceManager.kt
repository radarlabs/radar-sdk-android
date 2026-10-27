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
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
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
 *
 * The server decides sampling per session (`tracingEnabled` in `/v1/config` and `/v1/track` responses). Spans can
 * end before that decision arrives, so they are written as pending, tagged with their session, and are not
 * uploaded. [onSamplingDecision] then makes them uploadable or deletes them. A replaced session can still get
 * its decision from a request that was in flight when the session changed, so its pending spans are kept util
 * the next [init] call. Pending spans from sessions other than the current one are deleted when the manager starts.
 */
internal class RadarTraceManager(
    private val context: Context,
    private val apiClient: RadarApiClient,
    private val logger: RadarLogger
) {
    companion object {
        private const val MAX_FILES = 500
        private const val MAX_BYTES = 512 * 1024L
        private val MAX_AGE = 72.hours
        private val STALE_TEMP_FILE_AGE = 1.minutes
        private const val MAX_BATCH_SPANS = 100
        private const val MAX_BATCH_BYTES = 128 * 1024
        private const val DIR = "radar_traces"
        private const val READY_EXTENSION = "json"
        private const val PENDING_EXTENSION = "pending"

        // Decisions remembered per process. Sampling is based on session ID. Session ID can change in the middle of
        // a trace resulting in a new sampling decision. A minimum of two decisions need to be retained. Retaining 5
        // allows for cases where multiple session changes could occur between trace start and end.
        private const val MAX_DECISIONS = 5

        // Spans waiting for the disk worker. Past this, storing a new span would only evict an older one.
        private const val MAX_PENDING_WRITES = MAX_FILES
    }

    private val disk = Executors.newSingleThreadExecutor()
    private val directory = File(context.filesDir, DIR)
    private val isFlushing = AtomicBoolean(false)
    private val pendingWrites = AtomicInteger(0)
    private val droppedWrites = AtomicInteger(0)

    // Accessed only on the disk worker. inFlightFiles protects a selected batch from pruning during upload.
    private var inFlightFiles: Set<File> = emptySet()
    private var fileSequence = 0L

    @Volatile
    private var sampledOverride: Boolean? = null

    // The server's decisions by session, oldest first. A trace keeps the session it started in, so spans that end
    // after the session changes still need the previous session's decision.
    private val decisions = object : LinkedHashMap<String, Boolean>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?): Boolean = size > MAX_DECISIONS
    }

    /** Whether spans in a session are uploaded, dropped, or kept until the server decides. */
    enum class SamplingState { SAMPLED, NOT_SAMPLED, PENDING }

    init {
        disk.execute {
            directory.mkdirs()
            deletePendingFromOtherSessions()
            prune()
        }
    }

    /**
     * The sampling state for spans in a session. A decision only covers the session it was made for, so a new
     * session is pending until its own decision arrives.
     */
    fun samplingState(sessionId: String): SamplingState {
        sampledOverride?.let { return if (it) SamplingState.SAMPLED else SamplingState.NOT_SAMPLED }
        return when (synchronized(decisions) { decisions[sessionId] }) {
            null -> SamplingState.PENDING
            true -> SamplingState.SAMPLED
            false -> SamplingState.NOT_SAMPLED
        }
    }

    /**
     * Applies the server's sampling decision for [sessionId], which must be the session sent with the request,
     * not the current one: the session can change while the request is in flight. Pending spans from that
     * session become uploadable if sampled and are deleted otherwise.
     */
    fun onSamplingDecision(sessionId: String, sampled: Boolean) {
        synchronized(decisions) {
            // Return early if there was already a sampling decision for this session. This prevents the sampling
            // decision from changing mid session. While the server should return a stable decision for the same session,
            // in the case the sampling percentage was changed or could not be loaded, the client should respect the
            // initial decision.
            if (decisions.containsKey(sessionId)) {
                return
            }
            decisions[sessionId] = sampled
        }
        runOnDisk { resolvePending(sessionId, sampled) }
        if (sampled) {
            flush()
        }
    }

    /**
     * Queues a completed span to be written to disk on the disk worker. Never throws into SDK call paths.
     * [sessionId] is the session the span's trace started in, which decides whether it is sampled.
     */
    fun record(span: RadarCompletedSpan, project: String?, sessionId: String) {
        try {
            val record = JSONObject()
                .put("project", project)
                .put("sdkVersion", RadarUtils.SDK_VERSION)
                .put("span", span.toJson())
            // If spans are created faster than they can be written, drop new ones instead of queuing without limit.
            if (pendingWrites.incrementAndGet() > MAX_PENDING_WRITES) {
                pendingWrites.decrementAndGet()
                if (droppedWrites.incrementAndGet() == 1) {
                    logger.e("Trace write queue is full; dropping new spans | limit = $MAX_PENDING_WRITES", Radar.RadarLogType.SDK_ERROR)
                }
                return
            }
            runOnDisk {
                try {
                    // Checked on the disk worker, which runs tasks in order. onSamplingDecision stores the decision
                    // before queueing resolvePending, so this write either sees the decision or writes a pending file
                    // that resolvePending runs after.
                    when (samplingState(sessionId)) {
                        SamplingState.SAMPLED -> write(record, pendingSessionId = null)
                        SamplingState.PENDING -> write(record, pendingSessionId = sessionId)
                        SamplingState.NOT_SAMPLED -> Unit
                    }
                } finally {
                    finishWrite()
                }
            }
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

    /** Called on the disk worker after each write. Logs how many spans were dropped once the queue drains. */
    private fun finishWrite() {
        if (pendingWrites.decrementAndGet() == 0) {
            val dropped = droppedWrites.getAndSet(0)
            if (dropped > 0) {
                logger.e("Dropped trace spans while the write queue was full | dropped = $dropped", Radar.RadarLogType.SDK_ERROR)
            }
        }
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

    /** Makes a session's pending spans uploadable if it is sampled, or deletes them. */
    private fun resolvePending(sessionId: String, sampled: Boolean) {
        try {
            pendingFiles().filter { parsePendingName(it).second == sessionId }.forEach { file ->
                if (!sampled) {
                    file.delete()
                } else if (!file.renameTo(File(directory, "${parsePendingName(file).first}.$READY_EXTENSION"))) {
                    logger.e("Could not make pending trace span uploadable", Radar.RadarLogType.SDK_ERROR)
                }
            }
        } catch (e: Exception) {
            logger.e("Could not apply trace sampling decision | message = ${e.message}", Radar.RadarLogType.SDK_ERROR, e)
        }
    }

    /**
     * Deletes pending spans left by a previous session during init. Decisions for those sessions will never arrive.
     * Pending spans from the current session are kept, because a request in this process can still return its decision.
     */
    private fun deletePendingFromOtherSessions() {
        try {
            val currentSessionId = RadarSettings.getSessionId(context)
            pendingFiles().filter { parsePendingName(it).second != currentSessionId }.forEach { it.delete() }
        } catch (e: Exception) {
            logger.e("Could not delete pending trace spans | message = ${e.message}", Radar.RadarLogType.SDK_ERROR, e)
        }
    }

    /** Splits `<name>.<sessionId>.pending` into the record name and session. */
    private fun parsePendingName(file: File): Pair<String, String> {
        val base = file.nameWithoutExtension
        return base.substringBeforeLast('.') to base.substringAfterLast('.')
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
     * `{"project": <key fingerprint>, "sdkVersion": <version>, "span": <OTLP span>}`. A span waiting for its
     * session's sampling decision is written as `<millis>-<sequence>-<uuid>.<sessionId>.pending` instead and
     * renamed when the decision arrives. Names sort oldest first: millis, then a sequence for spans written in
     * the same millisecond. The file is written to a `.tmp` file, synced, and renamed, so readers never see a
     * partial record.
     */
    private fun write(record: JSONObject, pendingSessionId: String?) {
        var temp: File? = null
        try {
            directory.mkdirs()
            temp = File.createTempFile("span-", ".tmp", directory)
            FileOutputStream(temp).use { stream ->
                stream.write(record.toString().toByteArray(Charsets.UTF_8))
                stream.fd.sync()
            }
            val name = String.format(Locale.US, "%d-%09d-%s", System.currentTimeMillis(), fileSequence++, UUID.randomUUID())
            val suffix = if (pendingSessionId == null) READY_EXTENSION else "$pendingSessionId.$PENDING_EXTENSION"
            val target = File(directory, "$name.$suffix")
            if (!temp.renameTo(target)) throw IllegalStateException("Could not commit trace span")
            prune()
        } catch (e: Exception) {
            logger.e("Could not persist trace span | message = ${e.message}", Radar.RadarLogType.SDK_ERROR, e)
        } finally {
            temp?.delete()
        }
    }

    /** Uploadable span records, oldest first. */
    private fun files(): List<File> = records(READY_EXTENSION)

    /** Span records waiting for a sampling decision, oldest first. */
    private fun pendingFiles(): List<File> = records(PENDING_EXTENSION)

    /** Committed span records with the given extensions, oldest first. */
    private fun records(vararg extensions: String): List<File> = directory.listFiles()?.filter { it.extension in extensions }?.sortedBy { it.name } ?: emptyList()

    /**
     * Removes stale temp files and expired spans, then evicts the oldest spans until within limits. Pending spans
     * count toward the limits, so a decision that never arrives can't grow storage.
     */
    private fun prune() {
        val now = System.currentTimeMillis()
        // Only remove old temp files left by a crash. A recent one may be an in-progress write by another manager
        directory.listFiles()?.filter { it.extension == "tmp" && now - it.lastModified() > STALE_TEMP_FILE_AGE.inWholeMilliseconds }?.forEach { it.delete() }
        val files = records(READY_EXTENSION, PENDING_EXTENSION).toMutableList()
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

    /** The current session, which new traces are sampled under. */
    internal fun currentSessionId(): String = RadarSettings.getSessionId(context)

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

    /**
     * Forces every session to be sampled (true) or not (false), e.g. to trace 100% locally. Pass null to use the
     * server's decision.
     */
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
            try {
                disk.submit {}.get(1, TimeUnit.SECONDS)
            } catch (_: TimeoutException) {
                // The worker is still busy, for example with a long queue of writes. Keep waiting until the deadline.
            }
        }
        check(!isFlushing.get()) { "Trace flush did not finish within 10 seconds" }
    }

    /** Stops the disk worker. Later records and flushes are dropped. */
    @VisibleForTesting
    internal fun shutdownForTesting() {
        disk.shutdownNow()
    }
}
