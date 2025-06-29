package io.radar.sdk

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import kotlin.math.pow
import kotlin.random.Random

internal class RadarAPIRetryWrapper(private var radarAPIHelper: RadarApiHelper) {
    private val handler = Handler(Looper.getMainLooper())
    private val maxRetries = 5
    private val timeout = 200
    private val timeoutBase = 2.0


    internal fun requestWithRetry(context: Context,
                                  method: String,
                                  path: String,
                                  headers: Map<String, String>?,
                                  params: JSONObject?,
                                  sleep: Boolean,
                                  callback: RadarApiHelper.RadarApiCallback? = null,
                                  extendedTimeout: Boolean = false,
                                  stream: Boolean = false,
                                  logPayload: Boolean = true,
                                  verified: Boolean = false,
                                  retriesLeft: Int = maxRetries) {
        val retriesLeftSanitized = minOf(retriesLeft, maxRetries)
        if (retriesLeftSanitized == 0) {
            handler.post {
                callback?.onComplete(Radar.RadarStatus.ERROR_UNKNOWN)
            }
            return
        }
        val newCallback = object : RadarApiHelper.RadarApiCallback {
            override fun onComplete(status: Radar.RadarStatus, res: JSONObject?) {
                if ((status !== Radar.RadarStatus.SUCCESS && retriesLeftSanitized === 1) || (status === Radar.RadarStatus.SUCCESS)) {
                    handler.post {
                        callback?.onComplete(status, res)
                    }
                } else {
                    // Calculate retry duration with jitter.
                    val retryDuration = (timeout * timeoutBase.pow((maxRetries - retriesLeft + 1).toDouble()) + Random.nextInt(1000)).toLong()
                    Thread.sleep(retryDuration)
                    requestWithRetry(context, method, path, headers, params, sleep, callback, extendedTimeout, stream, logPayload, verified, retriesLeftSanitized - 1)
                }
            }
        }
        radarAPIHelper.request(context, method, path, headers, params, sleep, newCallback, extendedTimeout, stream, logPayload, verified)

    }


}