package io.radar.sdk

import android.os.Handler
import android.os.Looper
import io.radar.sdk.util.RadarHandler
import io.radar.sdk.util.RadarPostable
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

internal class RadarApiHelper(
    private var logger: RadarLogger? = null,
    private val executor: ExecutorService = Executors.newSingleThreadExecutor(),
    private val handler: RadarPostable = RadarHandler(Handler(Looper.getMainLooper()))
) {

    interface RadarApiCallback {
        fun onComplete(status: Radar.RadarStatus, res: JSONObject? = null)
    }

    private fun connect(request: RadarApiRequest): HttpURLConnection {
        val urlConnection = request.url.openConnection() as HttpURLConnection
        request.headers?.forEach { (key, value) ->
            try {
                urlConnection.setRequestProperty(key, value)
            } catch (ignored: Exception) {
            }
        }
        urlConnection.requestMethod = request.method
        urlConnection.connectTimeout = 10000
        urlConnection.readTimeout = 10000
        return urlConnection
    }

    @Suppress("LongMethod")
    internal fun request(request: RadarApiRequest) {
        logger?.d("📍 Radar API request", mapOf("method" to request.method, "url" to request.url,
            "headers" to request.headers, "params" to request.params))
        executor.execute {
            var urlConnection: HttpURLConnection? = null
            try {
                urlConnection = connect(request)
                if (request.params != null) {
                    urlConnection.doOutput = true
                    val outputStreamWriter = OutputStreamWriter(urlConnection.outputStream)
                    outputStreamWriter.write(request.params.toString())
                    outputStreamWriter.close()
                }
                if (urlConnection.responseCode in 200 until 400) {
                    val body = urlConnection.inputStream.readAll()
                    if (body == null) {
                        handler.post {
                            request.callback?.onComplete(Radar.RadarStatus.ERROR_SERVER)
                        }
                        return@execute
                    }
                    val res = JSONObject(body)
                    logger?.d("📍 Radar API response", mapOf("method" to request.method, "url" to request.url,
                        "responseCode" to urlConnection.responseCode, "res" to res))
                    handler.post {
                        request.callback?.onComplete(Radar.RadarStatus.SUCCESS, res)
                    }
                } else {
                    val status = convertToRadarStatus(urlConnection.responseCode)
                    val body = urlConnection.errorStream.readAll()
                    if (body == null) {
                        request.callback?.onComplete(Radar.RadarStatus.ERROR_SERVER)
                        return@execute
                    }
                    logger?.d("📍 Radar API response", mapOf(
                        "responseCode" to urlConnection.responseCode,
                        "res" to body))
                    if (body == "Access denied") {
                        handler.post {
                            request.callback?.onComplete(Radar.RadarStatus.ERROR_UNAUTHORIZED)
                        }
                    } else {
                        handler.post {
                            request.callback?.onComplete(status)
                        }
                    }
                }
            } catch (e: IOException) {
                handler.post {
                    request.callback?.onComplete(Radar.RadarStatus.ERROR_NETWORK)
                }
            } catch (e: JSONException) {
                handler.post {
                    request.callback?.onComplete(Radar.RadarStatus.ERROR_SERVER)
                }
            } catch (e: Exception) {
                handler.post {
                    request.callback?.onComplete(Radar.RadarStatus.ERROR_UNKNOWN)
                }
            } finally {
                try {
                    urlConnection?.disconnect()
                } catch(ignored: Exception) {
                }
            }
            if (request.sleep) {
                Thread.sleep(1000)
            }
        }
    }

    private fun convertToRadarStatus(responseCode: Int): Radar.RadarStatus {
        return when (responseCode) {
            400 -> Radar.RadarStatus.ERROR_BAD_REQUEST
            401 -> Radar.RadarStatus.ERROR_UNAUTHORIZED
            402 -> Radar.RadarStatus.ERROR_PAYMENT_REQUIRED
            403 -> Radar.RadarStatus.ERROR_FORBIDDEN
            404 -> Radar.RadarStatus.ERROR_NOT_FOUND
            429 -> Radar.RadarStatus.ERROR_RATE_LIMIT
            in (500 until 600) -> Radar.RadarStatus.ERROR_SERVER
            else -> Radar.RadarStatus.ERROR_UNKNOWN
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