package io.radar.sdk

import android.content.Context
import android.location.Location
import java.lang.reflect.Method

internal open class RadarSDKFraud(
    val instance: Any? = null,
    val getFraudPayload: Method? = null,
) {
    companion object {
        val shared: RadarSDKFraud by lazy {
            try {
                val fraudClass = Class.forName("io.radar.sdk.fraud.RadarSDKFraud")
                val sharedInstanceMethod = fraudClass.getMethod("sharedInstance")
                val fraudInstance = sharedInstanceMethod.invoke(null)

                // check getFraudPayload function exist
                val getFraudPayload = fraudClass.getMethod(
                    "getFraudPayload",
                    java.util.Map::class.java,
                    Function1::class.java
                )

                return@lazy RadarSDKFraud(fraudInstance, getFraudPayload)
            } catch (_: Exception) {
                return@lazy RadarSDKFraud()
            }
        }
    }

    open fun getFraudPayload(context: Context, logger: RadarLogger, location: Location? = null, googlePlayProjectNumber: Long? = null, callback: (Radar.RadarStatus, String) -> Unit) {
        if (instance == null || getFraudPayload == null) {
            callback(Radar.RadarStatus.ERROR_PLUGIN, "")
            return
        }
        try {
            // Create adapter callback that matches getFraudPayload's Function1 signature
            val callback = object : Function1<Map<String, Any?>?, Unit> {
                override fun invoke(result: Map<String, Any?>?) {
                    val fraudPayload = result?.get("payload") as? String

                    if (result?.containsKey("error") == true || fraudPayload == null) {
                        val error = result?.get("error") as? String ?: "Unknown error"
                        logger.e("Error getting fraud payload: $error", Radar.RadarLogType.SDK_ERROR)
                        callback(Radar.RadarStatus.ERROR_PLUGIN, "")
                    } else {
                        callback(Radar.RadarStatus.SUCCESS, fraudPayload)
                    }
                }
            }

            // Create options map
            val options = mutableMapOf(
                "context" to context,
                "location" to location
            )

            // Add integrity-related parameters if available
            if (googlePlayProjectNumber != null) {
                options["googlePlayProjectNumber"] = googlePlayProjectNumber
            }

            getFraudPayload.invoke(instance, options, callback)
        } catch (e: Exception) {
            logger.e("Error calling fraud detection ${e.message ?: ""}", Radar.RadarLogType.SDK_EXCEPTION, e)
            callback(Radar.RadarStatus.ERROR_PLUGIN, "")
        }
    }
}
