package io.radar.sdk

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import io.radar.sdk.Radar.RadarLogType
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.io.OutputStreamWriter
import java.net.URL
import java.util.Scanner
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection

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
                    val prevUpdatedAtMsDiff = params.optLong("updatedAtMsDiff", -1L)
                    val replays = params.optJSONArray("replays")

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && (prevUpdatedAtMsDiff != -1L || replays != null)) {
                        val nowMs = SystemClock.elapsedRealtimeNanos() / 1000000
                        val locationMs = params.optLong("locationMs", -1L)

                        if (prevUpdatedAtMsDiff != -1L && locationMs != -1L){
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
                    val outputStreamWriter = OutputStreamWriter(urlConnection.outputStream)
                    outputStreamWriter.write(params.toString())
                    outputStreamWriter.close()
                }

                if (urlConnection.responseCode in 200 until 400) {
                    val body = urlConnection.inputStream.readAll()
                    if (body == null) {
                        handler.post {
                            callback?.onComplete(Radar.RadarStatus.ERROR_SERVER)
                        }

                        return@execute
                    }

                    val res = JSONObject(body)

                    logger?.d("📍 Radar API response | method = $method; url = $url; responseCode = ${urlConnection.responseCode}; res = $res")
                    
                    handler.post {
                        callback?.onComplete(Radar.RadarStatus.SUCCESS, res)
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

                        return@execute
                    }

                    val res = JSONObject(body)

                    logger?.e("📍 Radar API response | method = ${method}; url = ${url}; responseCode = ${urlConnection.responseCode}; res = $res", RadarLogType.SDK_ERROR)
                    
                    handler.post {
                        callback?.onComplete(status)
                    }
                }

                urlConnection.disconnect()
            } catch (e: IOException) {
                handler.post {
                    logger?.d("Error calling API | e = ${e.localizedMessage}")

                    callback?.onComplete(Radar.RadarStatus.ERROR_NETWORK)
                }
            } catch (e: JSONException) {
                logger?.d("Error calling API | e = ${e.localizedMessage}")

                handler.post {
                    callback?.onComplete(Radar.RadarStatus.ERROR_SERVER)
                }
            } catch (e: Exception) {
                logger?.d("Error calling API | e = ${e.localizedMessage}")

                handler.post {
                    callback?.onComplete(Radar.RadarStatus.ERROR_UNKNOWN)
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