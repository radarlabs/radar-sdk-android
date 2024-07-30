package io.radar.sdk

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStreamWriter
import java.net.URL
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.util.*
import java.util.concurrent.Executors
import io.radar.sdk.Radar.RadarLogType
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory

internal open class RadarApiHelper(
    private var logger: RadarLogger? = null
) {
  
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    interface RadarApiCallback {
        fun onComplete(status: Radar.RadarStatus, res: JSONObject? = null)
    }

    internal open fun request(context: Context,
                              method: String,
                              path: String,
                              headers: Map<String, String>?,
                              params: JSONObject?,
                              sleep: Boolean,
                              callback: RadarApiCallback? = null,
                              extendedTimeout: Boolean = false,
                              stream: Boolean = false,
                              logPayload: Boolean = true,
                              verified: Boolean = false) {
        val host = if (verified) {
            RadarSettings.getVerifiedHost(context)
        } else {
            RadarSettings.getHost(context)
        }
        val uri = Uri.parse(host).buildUpon()
            .appendEncodedPath(path)
            .build()
        val url = URL(uri.toString())

        request(context, url, method, headers, params?.toString(), sleep, extendedTimeout, stream, logPayload) { status, res ->
            callback?.onComplete(status, res?.let { JSONObject(it) });
        }
    }
    internal open fun request(context: Context,
                              url: URL,
                              method: String,
                              headers: Map<String, String>?,
                              params: String?,
                              sleep: Boolean,
                              extendedTimeout: Boolean = false,
                              stream: Boolean = false,
                              logPayload: Boolean = true,
                              block: (status: Radar.RadarStatus, res: String?) -> Unit) {
        if (logPayload) {
            logger?.d("📍 Radar API request | method = $method; url = $url; headers = $headers; params = $params")
        } else {
            logger?.d("📍 Radar API request | method = $method; url = $url; headers = $headers")
        }
        
        executor.execute {
            try {
                val urlConnection = url.openConnection() as HttpsURLConnection
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
                urlConnection.connectTimeout = 10000
                if (extendedTimeout) {
                    urlConnection.readTimeout = 25000
                } else {
                    urlConnection.readTimeout = 10000
                }
                if (stream) {
                    urlConnection.setChunkedStreamingMode(1024)
                }

                if (params != null) {
                    urlConnection.doOutput = true

                    val outputStreamWriter = OutputStreamWriter(urlConnection.outputStream)
                    outputStreamWriter.write(params)
                    outputStreamWriter.close()
                }

                if (urlConnection.responseCode in 200 until 400) {
                    val body = urlConnection.inputStream.readAll()
                    if (body == null) {
                        handler.post {
                            block(Radar.RadarStatus.ERROR_SERVER, null)
                        }

                        return@execute
                    }

                    logger?.d("📍 Radar API response | method = $method; url = $url; responseCode = ${urlConnection.responseCode}; res = $body")
                    
                    handler.post {
                        block(Radar.RadarStatus.SUCCESS, body)
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
                        block(Radar.RadarStatus.ERROR_SERVER, null)

                        return@execute
                    }

                    val res = JSONObject(body)

                    logger?.e("📍 Radar API response | method = ${method}; url = ${url}; responseCode = ${urlConnection.responseCode}; res = $res", RadarLogType.SDK_ERROR)
                    
                    handler.post {
                        block(status, null)
                    }
                }

                urlConnection.disconnect()
            } catch (e: IOException) {
                handler.post {
                    logger?.d("Error calling API | e = ${e.localizedMessage}")

                    block(Radar.RadarStatus.ERROR_NETWORK, null)
                }
            } catch (e: JSONException) {
                logger?.d("Error calling API | e = ${e.localizedMessage}")

                handler.post {
                    block(Radar.RadarStatus.ERROR_SERVER, null)
                }
            } catch (e: Exception) {
                logger?.d("Error calling API | e = ${e.localizedMessage}")

                handler.post {
                    block(Radar.RadarStatus.ERROR_UNKNOWN, null)
                }
            }

            if (sleep) {
                Thread.sleep(1000)
            }
        }
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

}