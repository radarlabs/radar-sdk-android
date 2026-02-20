package io.radar.sdk

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.net.toUri
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

// // For debugging local development server trackVerified
// import javax.net.ssl.SSLContext
// import javax.net.ssl.TrustManager
// import javax.net.ssl.X509TrustManager
// import javax.net.ssl.HostnameVerifier
// import java.security.cert.X509Certificate

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

    interface RadarApiCallback {
        fun onComplete(status: Radar.RadarStatus, res: JSONObject? = null)
    }

    interface RadarImageApiCallback {
        fun onComplete(status: Radar.RadarStatus, bitmap: android.graphics.Bitmap? = null)
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
                              verified: Boolean = false,
                              fraudOptions: Map<String, Any?>? = null,
                              imageCallback: RadarImageApiCallback? = null) {
        val host = if (verified) {
            RadarSettings.getVerifiedHost(context)
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
            // Helper function to update params with updatedAtMsDiff and replays
            fun updateParamsTiming(params: JSONObject) {
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
            }
            
            // Nested function to handle response parsing
            fun handleResponse(body: String?, responseCode: Int, error: Exception?) {
                if (error != null) {
                    // Determine status based on exception type
                    val status = when (error) {
                        is IOException -> Radar.RadarStatus.ERROR_NETWORK
                        is JSONException -> Radar.RadarStatus.ERROR_SERVER
                        else -> Radar.RadarStatus.ERROR_NETWORK
                    }
                    handler.post {
                        logger?.d("Error calling API | e = ${error.localizedMessage}")
                        callback?.onComplete(status)
                        imageCallback?.onComplete(status)
                    }
                    return
                }
                
                if (body == null) {
                    handler.post {
                        callback?.onComplete(Radar.RadarStatus.ERROR_SERVER)
                        imageCallback?.onComplete(Radar.RadarStatus.ERROR_SERVER)
                    }
                    return
                }
                
                try {
                    val res = JSONObject(body as String)
                    
                    // Map response to RadarStatus
                    val status = when (responseCode) {
                        in 200 until 400 -> Radar.RadarStatus.SUCCESS
                        400 -> Radar.RadarStatus.ERROR_BAD_REQUEST
                        401 -> Radar.RadarStatus.ERROR_UNAUTHORIZED
                        402 -> Radar.RadarStatus.ERROR_PAYMENT_REQUIRED
                        403 -> Radar.RadarStatus.ERROR_FORBIDDEN
                        404 -> Radar.RadarStatus.ERROR_NOT_FOUND
                        429 -> Radar.RadarStatus.ERROR_RATE_LIMIT
                        in 500 until 600 -> Radar.RadarStatus.ERROR_SERVER
                        else -> Radar.RadarStatus.ERROR_UNKNOWN
                    }
                    
                    if (status == Radar.RadarStatus.SUCCESS) {
                        logger?.d("📍 Radar API response | method = $method; url = $url; responseCode = $responseCode; res = $res")
                    } else {
                        logger?.e("📍 Radar API response | method = $method; url = $url; responseCode = $responseCode; res = $res", RadarLogType.SDK_ERROR)
                    }
                    
                    handler.post {
                        callback?.onComplete(status, res)
                        imageCallback?.onComplete(status)
                    }
                } catch (e: JSONException) {
                    logger?.d("Error calling API | e = ${e.localizedMessage}")
                    handler.post {
                        callback?.onComplete(Radar.RadarStatus.ERROR_SERVER)
                        imageCallback?.onComplete(Radar.RadarStatus.ERROR_SERVER)
                    }
                }
            }
            
            try {
                val urlConnection = url.openConnection() as HttpsURLConnection
                // // For debugging local development server trackVerified
                // // Configure SSL to accept any certificate and hostname
                // urlConnection.sslSocketFactory = sslContext.socketFactory
                // urlConnection.hostnameVerifier = hostnameVerifier
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

                // Check if verified and fraudOptions provided - pass to fraud SDK
                if (verified && fraudOptions != null && params != null) {
                    try {
                        val fraudClass = Class.forName("io.radar.sdk.fraud.RadarSDKFraud")
                        val sharedInstanceMethod = fraudClass.getMethod("sharedInstance")
                        val fraudInstance = sharedInstanceMethod.invoke(null)
                        
                        // Update params with updatedAtMsDiff if needed (before passing to fraud SDK)
                        updateParamsTiming(params)
                        
                        // Add connection, params, and context to fraudOptions
                        val fraudOptionsWithRequest = fraudOptions.toMutableMap()
                        fraudOptionsWithRequest["connection"] = urlConnection
                        fraudOptionsWithRequest["params"] = params
                        fraudOptionsWithRequest["context"] = context
                        
                        // Call fraud SDK's trackVerified
                        val trackVerifiedMethod = fraudClass.getMethod("trackVerified", 
                            Map::class.java,
                            kotlin.jvm.functions.Function1::class.java)
                        
                        val fraudCallback = object : kotlin.jvm.functions.Function1<Map<String, Any?>?, Unit> {
                            override fun invoke(result: Map<String, Any?>?): Unit {
                                if (sleep) {
                                    Thread.sleep(1000)
                                }
                                
                                val body = result?.get("body") as? String
                                val responseCode = result?.get("responseCode") as? Int ?: -1
                                val error = result?.get("error") as? Exception
                                handleResponse(body, responseCode, error)
                                urlConnection.disconnect()
                            }
                        }
                        
                        trackVerifiedMethod.invoke(fraudInstance, fraudOptionsWithRequest, fraudCallback)
                        return@execute
                    } catch (e: ClassNotFoundException) {
                        // Fraud SDK not available - fall through to normal execution
                    } catch (e: Exception) {
                        logger?.d("Error calling fraud SDK | e = ${e.localizedMessage}")
                        // Fall through to normal execution
                    }
                }

                if (params != null) {
                    // Update params with updatedAtMsDiff if needed
                    updateParamsTiming(params)

                    urlConnection.doOutput = true
                    val outputStreamWriter = OutputStreamWriter(urlConnection.outputStream)
                    outputStreamWriter.write(params.toString())
                    outputStreamWriter.close()
                }

                if (urlConnection.responseCode in 200 until 400) {
                    if (imageCallback != null) {
                        val inputStream = urlConnection.inputStream
                        val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                        inputStream.close()

                        logger?.d("📍 Radar API image response | method = $method; url = $url; responseCode = ${urlConnection.responseCode}")
                        
                        handler.post {
                            imageCallback.onComplete(Radar.RadarStatus.SUCCESS, bitmap)
                        }
                    } else if (callback != null) {
                        val body = urlConnection.inputStream.readAll()
                        handleResponse(body, urlConnection.responseCode, null)
                    }
                } else {
                    val body = urlConnection.errorStream.readAll()
                    handleResponse(body, urlConnection.responseCode, null)
                }

                urlConnection.disconnect()
            } catch (e: Exception) {
                handleResponse(null, -1, e)
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

    internal open fun requestImage(context: Context,
                               method: String,
                               urlString: String,
                               headers: Map<String, String>?,
                               callback: RadarImageApiCallback? = null) {
        request(context, method, urlString, headers, null, false, imageCallback=callback)
    }
}