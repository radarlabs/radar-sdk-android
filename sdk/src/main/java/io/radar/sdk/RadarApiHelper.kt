package io.radar.sdk

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import io.radar.sdk.Radar.RadarLogType
import java.io.IOException
import java.io.InputStream
import java.io.OutputStreamWriter
import java.net.ConnectException
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.util.Scanner
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLException
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509ExtendedTrustManager
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

// // For debugging local development server trackVerified
// import javax.net.ssl.SSLContext
// import javax.net.ssl.TrustManager
// import javax.net.ssl.X509TrustManager
// import javax.net.ssl.HostnameVerifier
// import java.security.cert.X509Certificate

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

internal fun networkErrorMessage(host: String, e: Exception, elapsedMs: Long, kind: String): String = "📍 Radar API network error | host = $host; kind = $kind; exception = ${e.javaClass.simpleName}; message = ${e.localizedMessage}; elapsedMs = $elapsedMs"

internal open class RadarApiHelper(
    private var logger: RadarLogger? = null
) {

    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    // // For debugging local development server trackVerified
    // // Custom TrustManager that accepts all certificates
    // private val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
    //     override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    //     override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) {}
    //     override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) {}
    // })

    // // SSLContext that uses the custom TrustManager
    // private val sslContext: SSLContext by lazy {
    //     val context = SSLContext.getInstance("TLS")
    //     context.init(null, trustAllCerts, java.security.SecureRandom())
    //     context
    // }

    // // Custom HostnameVerifier that accepts all hostnames
    // private val hostnameVerifier = HostnameVerifier { _, _ -> true }

    /**
     * Builds an [SSLSocketFactory] that logs the server's presented certificate chain during the
     * TLS handshake and then delegates to the platform default trust manager so real validation is
     * preserved. Unlike reading [HttpsURLConnection.getServerCertificates] after the fact, this
     * captures the chain even when the handshake ultimately fails (untrusted root, expired cert,
     * etc.) — which is the case worth debugging. Intended for verified requests only.
     *
     * Extends [X509ExtendedTrustManager] (API 24+) and forwards every overload, including the
     * hostname-aware `Socket`/`SSLEngine` variants. Android's Network Security Config
     * (`RootTrustManager`) rejects a plain [X509TrustManager] with "Domain specific configurations
     * require that hostname aware checkServerTrusted(...) is used", so the extended variants must be
     * delegated for validation to pass.
     */
    // Delegates every check to the platform default trust manager, so validation is preserved;
    // the custom trust manager exists only to log the presented chain mid-handshake.
    @SuppressLint("CustomX509TrustManager")
    @RequiresApi(Build.VERSION_CODES.N)
    private fun loggingSslSocketFactory(host: String): SSLSocketFactory {
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(null as KeyStore?)
        val defaultTrustManager = tmf.trustManagers.filterIsInstance<X509ExtendedTrustManager>().first()

        val loggingTrustManager = object : X509ExtendedTrustManager() {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = defaultTrustManager.checkClientTrusted(chain, authType)

            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String, socket: Socket?) = defaultTrustManager.checkClientTrusted(chain, authType, socket)

            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String, engine: SSLEngine?) = defaultTrustManager.checkClientTrusted(chain, authType, engine)

            // Log the presented chain, then delegate to the platform's hostname-aware validation so a
            // bad chain still throws CertificateException — but only after we've logged it.
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                logServerCertificateChain(chain, host)
                defaultTrustManager.checkServerTrusted(chain, authType)
            }

            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String, socket: Socket?) {
                logServerCertificateChain(chain, host)
                defaultTrustManager.checkServerTrusted(chain, authType, socket)
            }

            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String, engine: SSLEngine?) {
                logServerCertificateChain(chain, host)
                defaultTrustManager.checkServerTrusted(chain, authType, engine)
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> = defaultTrustManager.acceptedIssuers
        }

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf<TrustManager>(loggingTrustManager), null)
        return sslContext.socketFactory
    }

    private fun logServerCertificateChain(certs: Array<X509Certificate>, host: String) {
        try {
            if (certs.isEmpty()) {
                logger?.i("📍 Radar API TLS chain | host = $host; no X509 certificates")
                return
            }

            val chain = certs.mapIndexed { index, cert ->
                val sha256 = cert.encoded.sha256Hex()
                listOf(
                    "[$index]",
                    "  subject = ${cert.subjectX500Principal.name}",
                    "  issuer = ${cert.issuerX500Principal.name}",
                    "  notBefore = ${cert.notBefore}",
                    "  notAfter = ${cert.notAfter}",
                    "  serial = ${cert.serialNumber}",
                    "  sha256 = $sha256"
                ).joinToString("\n")
            }.joinToString("\n")

            logger?.i("📍 Radar API TLS chain | host = $host\n$chain")
        } catch (e: Exception) {
            logger?.i("📍 Radar API TLS chain unavailable | host = $host; exception = ${e.javaClass.simpleName}; message = ${e.localizedMessage}")
        }
    }

    private fun ByteArray.sha256Hex(): String = MessageDigest.getInstance("SHA-256")
        .digest(this)
        .joinToString(":") { "%02X".format(it) }

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
        verifiedHostOverride: String? = null
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

        executor.execute {
            val startMs = SystemClock.elapsedRealtime()
            try {
                val urlConnection = url.openConnection() as HttpsURLConnection
                if (verified && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    // Log the server's certificate chain during the TLS handshake (validation preserved).
                    urlConnection.sslSocketFactory = loggingSslSocketFactory(host)
                }
                if (headers != null) {
                    for ((key, value) in headers) {
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

                if (params != null) {
                    val prevUpdatedAtMsDiff = params.optLong("updatedAtMsDiff", -1L)
                    val replays = params.optJSONArray("replays")

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && (prevUpdatedAtMsDiff != -1L || replays != null)) {
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

                    urlConnection.doOutput = true
                    OutputStreamWriter(urlConnection.outputStream).use { writer ->
                        writer.write(params.toString())
                    }
                }

                if (urlConnection.responseCode in 200 until 400) {
                    if (callback != null) {
                        val body = urlConnection.inputStream.readAll()
                        if (body == null) {
                            handler.post {
                                callback.onComplete(Radar.RadarStatus.ERROR_SERVER)
                            }

                            return@execute
                        }

                        val res = JSONObject(body)

                        logger?.d("📍 Radar API response | method = $method; url = $url; responseCode = ${urlConnection.responseCode}; res = $res")

                        handler.post {
                            callback.onComplete(Radar.RadarStatus.SUCCESS, res)
                        }
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
                        callback?.onComplete(Radar.RadarStatus.ERROR_SERVER)
                        imageCallback?.onComplete(Radar.RadarStatus.ERROR_SERVER)

                        return@execute
                    }

                    val res = JSONObject(body)

                    logger?.e("📍 Radar API response | method = $method; url = $url; responseCode = ${urlConnection.responseCode}; res = $res", RadarLogType.SDK_ERROR)

                    handler.post {
                        callback?.onComplete(status, res)
                        imageCallback?.onComplete(status)
                    }
                }

                urlConnection.disconnect()
            } catch (e: IOException) {
                logNetworkError(host, e, startMs, NetworkErrorKind.from(e).name)
                handler.post {
                    callback?.onComplete(Radar.RadarStatus.ERROR_NETWORK, throwable = e)
                    imageCallback?.onComplete(Radar.RadarStatus.ERROR_NETWORK)
                }
            } catch (e: JSONException) {
                logNetworkError(host, e, startMs, "JSON_PARSE")
                handler.post {
                    callback?.onComplete(Radar.RadarStatus.ERROR_SERVER, throwable = e)
                    imageCallback?.onComplete(Radar.RadarStatus.ERROR_SERVER)
                }
            } catch (e: Exception) {
                logNetworkError(host, e, startMs, "UNKNOWN")
                handler.post {
                    callback?.onComplete(Radar.RadarStatus.ERROR_UNKNOWN, throwable = e)
                    imageCallback?.onComplete(Radar.RadarStatus.ERROR_UNKNOWN)
                }
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
