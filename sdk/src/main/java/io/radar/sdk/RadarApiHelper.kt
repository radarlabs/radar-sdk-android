package io.radar.sdk

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import androidx.core.net.toUri
import io.radar.sdk.Radar.RadarLogType
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.NoRouteToHostException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.util.Scanner
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

internal enum class NetworkErrorKind {
    DNS_FAILURE,
    TIMEOUT,
    SSL_FAILURE,
    CONNECT_REFUSED,
    IO_OTHER;

    companion object {
        fun from(e: IOException): NetworkErrorKind = when (e) {
            is UnknownHostException -> DNS_FAILURE
            is SocketTimeoutException -> TIMEOUT
            is SSLException -> SSL_FAILURE
            is ConnectException -> CONNECT_REFUSED
            else -> IO_OTHER
        }
    }
}

// Android has no exact equivalent of iOS's NSURLErrorNetworkConnectionLost.
// EOF and socket failures may indicate a dropped in-flight request. Exclude
// known connection-setup failures; an eligible request retries once with a fresh seal.
internal fun isRetryableConnectionFailure(e: IOException): Boolean = e is EOFException ||
    (e is SocketException && e !is ConnectException && e !is NoRouteToHostException)

/**
 * The queue a request runs on. Queues are isolated so a request on one never waits behind a request on another.
 */
internal enum class RadarRequestQueue {
    /** One request at a time, in order. For requests like track whose order matters on the server. */
    ORDERED,

    /**
     * One request at a time at background thread priority. For telemetry like traces that must not delay other requests.
     * One at a time limits bandwidth contention with higher priority requests.
     */
    BACKGROUND
}

internal fun networkErrorMessage(host: String, e: Exception, elapsedMs: Long, kind: String): String = "📍 Radar API network error | host = $host; kind = $kind; exception = ${e.javaClass.simpleName}; message = ${e.localizedMessage}; elapsedMs = $elapsedMs"

internal open class RadarApiHelper(
    private var logger: RadarLogger? = null,
    private val connectionFactory: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection }
) {

    private val orderedExecutor = Executors.newSingleThreadExecutor()
    private val backgroundExecutor = ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS, LinkedBlockingQueue()) { runnable ->
        Thread {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            runnable.run()
        }
    }.apply { allowCoreThreadTimeOut(true) }
    private val handler = Handler(Looper.getMainLooper())

    interface RadarApiCallback {
        fun onComplete(status: Radar.RadarStatus, res: JSONObject? = null, throwable: Throwable? = null)
    }

    interface RadarImageApiCallback {
        fun onComplete(status: Radar.RadarStatus, bitmap: android.graphics.Bitmap? = null)
    }

    internal open fun request(
        context: Context,
        method: String,
        path: String,
        headers: Map<String, String>?,
        params: JSONObject?,
        sleep: Boolean,
        callback: RadarApiCallback? = null,
        extendedTimeout: Boolean = false,
        stream: Boolean = false,
        logPayload: Boolean = true,
        verified: Boolean = false,
        imageCallback: RadarImageApiCallback? = null,
        verifiedHostOverride: String? = null,
        prepareRequest: (() -> JSONObject)? = null,
        preparationFailureHandler: ((Exception) -> Unit)? = null,
        queue: RadarRequestQueue = RadarRequestQueue.ORDERED,
        traceSpan: RadarTraceSpan? = null
    ) {
        val host = if (verified) {
            verifiedHostOverride ?: RadarSettings.getVerifiedHost(context)
        } else {
            RadarSettings.getHost(context)
        }
        val uri = host.toUri().buildUpon()
            .appendEncodedPath(path)
            .build()
        val url = URL(uri.toString())

        if (logPayload) {
            logger?.d("📍 Radar API request | method = $method; url = $url; headers = $headers; params = $params")
        } else {
            logger?.d("📍 Radar API request | method = $method; url = $url; headers = $headers")
        }

        val executor = when (queue) {
            RadarRequestQueue.ORDERED -> orderedExecutor
            RadarRequestQueue.BACKGROUND -> backgroundExecutor
        }
        // A traced request's client span ends on the executor, before its callback is posted, so time waiting for the
        // main thread counts toward the caller's span instead.
        val enqueuedAtMs = SystemClock.elapsedRealtime()
        executor.execute {
            val startMs = SystemClock.elapsedRealtime()
            // Queue time includes waiting behind earlier requests on this queue, and their post-request sleeps.
            traceSpan?.requestStarted(context, url.host, startMs - enqueuedAtMs)
            val requestHeaders = tracedHeaders(headers, traceSpan)
            val retryEncryptedRequest = verified && prepareRequest != null
            // Attempt 2 is reached only via the eligible IOException `continue` below.
            for (attempt in 0..1) {
                if (attempt > 0) {
                    traceSpan?.resendCount(attempt)
                }
                val requestParams = try {
                    if (params != null) {
                        val prevUpdatedAtMsDiff = params.optLong("updatedAtMsDiff", -1L)
                        val replays = params.optJSONArray("replays")

                        if (prevUpdatedAtMsDiff != -1L || replays != null) {
                            val nowMs = SystemClock.elapsedRealtimeNanos() / 1000000
                            val locationMs = params.optLong("locationMs", -1L)

                            if (prevUpdatedAtMsDiff != -1L && locationMs != -1L) {
                                val updatedAtMsDiff = nowMs - locationMs
                                params.put("updatedAtMsDiff", updatedAtMsDiff)
                            }

                            if (replays != null) {
                                val updatedReplays = mutableListOf<JSONObject>()
                                for (i in 0 until replays.length()) {
                                    val replay = replays.optJSONObject(i)
                                    replay?.let {
                                        val replayLocationMs = it.optLong("locationMs", -1L)
                                        if (replayLocationMs != -1L) {
                                            val replayUpdatedAtMsDiff = nowMs - replayLocationMs
                                            it.put("updatedAtMsDiff", replayUpdatedAtMsDiff)
                                        }
                                        updatedReplays.add(it)
                                    }
                                }
                                params.put("replays", JSONArray(updatedReplays))
                            }
                        }
                    }

                    prepareRequest?.invoke() ?: params
                } catch (e: Exception) {
                    logger?.e("Failed to prepare Radar API request: ${e.message ?: "Unknown error"}", RadarLogType.SDK_ERROR, e)
                    traceSpan?.end(Radar.RadarStatus.ERROR_BAD_REQUEST)
                    handler.post {
                        if (preparationFailureHandler != null) {
                            preparationFailureHandler(e)
                        } else {
                            callback?.onComplete(Radar.RadarStatus.ERROR_BAD_REQUEST, throwable = e)
                        }
                    }
                    return@execute
                }

                var connectionToClose: HttpURLConnection? = null
                try {
                    val urlConnection = connectionFactory(url)
                    connectionToClose = urlConnection
                    if (requestHeaders != null) {
                        for ((key, value) in requestHeaders) {
                            try {
                                urlConnection.setRequestProperty(key, value)
                            } catch (e: Exception) {
                                logger?.d("Error setting request property | key = $key; value = $value")
                            }
                        }
                    }
                    urlConnection.requestMethod = method
                    val timeoutMs = RadarSettings.getNetworkTimeoutMs(context)
                    urlConnection.connectTimeout = timeoutMs
                    // Preserve historical 2.5x read timeout for long-running requests (was 25s vs 10s)
                    urlConnection.readTimeout = if (extendedTimeout) {
                        (timeoutMs * 2.5).toInt()
                    } else {
                        timeoutMs
                    }
                    if (stream) {
                        urlConnection.setChunkedStreamingMode(1024)
                    }

                    if (requestParams != null) {
                        val body = requestParams.toString().toByteArray(Charsets.UTF_8)
                        if (verified && prepareRequest != null && !stream) {
                            urlConnection.setFixedLengthStreamingMode(body.size)
                        }
                        urlConnection.doOutput = true
                        urlConnection.outputStream.use { it.write(body) }
                    }

                    if (urlConnection.responseCode in 200 until 400) {
                        if (callback != null) {
                            val body = urlConnection.inputStream.readAll()
                            if (body == null) {
                                traceSpan?.end(Radar.RadarStatus.ERROR_SERVER)
                                handler.post {
                                    callback.onComplete(Radar.RadarStatus.ERROR_SERVER)
                                }

                                return@execute
                            }

                            val res = JSONObject(body)

                            logger?.d("📍 Radar API response | method = $method; url = $url; responseCode = ${urlConnection.responseCode}; res = $res")

                            traceSpan?.end(Radar.RadarStatus.SUCCESS)
                            handler.post {
                                callback.onComplete(Radar.RadarStatus.SUCCESS, res)
                            }
                        } else {
                            traceSpan?.end(Radar.RadarStatus.SUCCESS)
                        }
                        if (imageCallback != null) {
                            val inputStream = urlConnection.inputStream
                            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                            inputStream.close()

                            logger?.d("📍 Radar API image response | method = $method; url = $url; responseCode = ${urlConnection.responseCode}")

                            handler.post {
                                imageCallback.onComplete(Radar.RadarStatus.SUCCESS, bitmap)
                            }
                        }
                    } else {
                        val status = when (urlConnection.responseCode) {
                            400 -> Radar.RadarStatus.ERROR_BAD_REQUEST
                            401 -> Radar.RadarStatus.ERROR_UNAUTHORIZED
                            402 -> Radar.RadarStatus.ERROR_PAYMENT_REQUIRED
                            403 -> Radar.RadarStatus.ERROR_FORBIDDEN
                            404 -> Radar.RadarStatus.ERROR_NOT_FOUND
                            429 -> Radar.RadarStatus.ERROR_RATE_LIMIT
                            in (500 until 600) -> Radar.RadarStatus.ERROR_SERVER
                            else -> Radar.RadarStatus.ERROR_UNKNOWN
                        }

                        val body = urlConnection.errorStream.readAll()
                        if (body == null) {
                            traceSpan?.end(Radar.RadarStatus.ERROR_SERVER)
                            callback?.onComplete(Radar.RadarStatus.ERROR_SERVER)
                            imageCallback?.onComplete(Radar.RadarStatus.ERROR_SERVER)

                            return@execute
                        }

                        val res = JSONObject(body)

                        logger?.e("📍 Radar API response | method = $method; url = $url; responseCode = ${urlConnection.responseCode}; res = $res", RadarLogType.SDK_ERROR)

                        traceSpan?.end(status)
                        handler.post {
                            callback?.onComplete(status, res)
                            imageCallback?.onComplete(status)
                        }
                    }
                } catch (e: IOException) {
                    if (attempt == 0 && retryEncryptedRequest && isRetryableConnectionFailure(e)) {
                        logger?.d("📍 Radar API retrying after lost connection | url = $url")
                        continue
                    }
                    logNetworkError(host, e, startMs, NetworkErrorKind.from(e).name)
                    traceSpan?.networkError(e)?.end(Radar.RadarStatus.ERROR_NETWORK)
                    handler.post {
                        callback?.onComplete(Radar.RadarStatus.ERROR_NETWORK, throwable = e)
                        imageCallback?.onComplete(Radar.RadarStatus.ERROR_NETWORK)
                    }
                } catch (e: JSONException) {
                    logNetworkError(host, e, startMs, "JSON_PARSE")
                    traceSpan?.end(Radar.RadarStatus.ERROR_SERVER)
                    handler.post {
                        callback?.onComplete(Radar.RadarStatus.ERROR_SERVER, throwable = e)
                        imageCallback?.onComplete(Radar.RadarStatus.ERROR_SERVER)
                    }
                } catch (e: Exception) {
                    logNetworkError(host, e, startMs, "UNKNOWN")
                    traceSpan?.end(Radar.RadarStatus.ERROR_UNKNOWN)
                    handler.post {
                        callback?.onComplete(Radar.RadarStatus.ERROR_UNKNOWN, throwable = e)
                        imageCallback?.onComplete(Radar.RadarStatus.ERROR_UNKNOWN)
                    }
                } finally {
                    connectionToClose?.disconnect()
                }
                break
            }

            if (sleep) {
                Thread.sleep(1000)
            }
        }
    }

    private fun logNetworkError(host: String, e: Exception, startMs: Long, kind: String) {
        val elapsedMs = SystemClock.elapsedRealtime() - startMs
        logger?.e(networkErrorMessage(host, e, elapsedMs, kind), RadarLogType.SDK_ERROR, e)
    }

    private fun InputStream.readAll(): String? {
        val scanner = Scanner(this, "UTF-8").useDelimiter("\\A")
        val body = if (scanner.hasNext()) {
            scanner.next()
        } else {
            null
        }
        close()

        return body
    }

    internal open fun requestImage(
        context: Context,
        method: String,
        urlString: String,
        headers: Map<String, String>?,
        callback: RadarImageApiCallback? = null
    ) {
        request(context, method, urlString, headers, null, false, imageCallback = callback)
    }
}
