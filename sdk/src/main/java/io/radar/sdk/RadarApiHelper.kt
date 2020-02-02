package io.radar.sdk

import android.content.Context
import android.os.AsyncTask
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

internal open class RadarApiHelper {

    interface RadarApiCallback {
        fun onComplete(status: Radar.RadarStatus, res: JSONObject? = null)
    }

    internal open fun request(context: Context,
                         method: String,
                         url: URL,
                         headers: Map<String, String>?,
                         params: JSONObject?,
                         callback: RadarApiCallback? = null) {
        DoAsync {
            val urlConnection = url.openConnection() as HttpURLConnection
            if (headers != null) {
                for ((key, value) in headers) {
                    urlConnection.setRequestProperty(key, value)

                }
            }
            urlConnection.requestMethod = method
            urlConnection.connectTimeout = 10000
            urlConnection.readTimeout = 10000

            try {
                if (params != null) {
                    urlConnection.doOutput = true

                    val outputStreamWriter = OutputStreamWriter(urlConnection.outputStream)
                    outputStreamWriter.write(params.toString())
                    outputStreamWriter.close()
                }

                if (urlConnection.responseCode in 200 until 300) {
                    val body = urlConnection.inputStream.readAll()
                    if (body == null) {
                        callback?.onComplete(Radar.RadarStatus.ERROR_SERVER)

                        return@DoAsync
                    }

                    val res = JSONObject(body)

                    callback?.onComplete(Radar.RadarStatus.SUCCESS, res)
                } else {
                    val status = when (urlConnection.responseCode) {
                        400 -> Radar.RadarStatus.ERROR_BAD_REQUEST
                        401 -> Radar.RadarStatus.ERROR_UNAUTHORIZED
                        403 -> Radar.RadarStatus.ERROR_FORBIDDEN
                        429 -> Radar.RadarStatus.ERROR_RATE_LIMIT
                        in (500 until 600) -> Radar.RadarStatus.ERROR_SERVER
                        else -> Radar.RadarStatus.ERROR_UNKNOWN
                    }

                    callback?.onComplete(status)
                }

                urlConnection.disconnect()
            } catch (e: IOException) {
                callback?.onComplete(Radar.RadarStatus.ERROR_NETWORK)
            } catch (e: JSONException) {
                callback?.onComplete(Radar.RadarStatus.ERROR_SERVER)
            } catch (e: Exception) {
                callback?.onComplete(Radar.RadarStatus.ERROR_UNKNOWN)
            }
        }.execute()
    }

    private class DoAsync(val handler: () -> Unit) : AsyncTask<Void, Void, Void>() {
        override fun doInBackground(vararg params: Void?): Void? {
            handler()

            return null
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