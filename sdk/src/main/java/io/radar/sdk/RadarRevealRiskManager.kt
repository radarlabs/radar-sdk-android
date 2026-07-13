package io.radar.sdk

import android.content.Context
import android.location.Location
import android.os.Build
import androidx.annotation.RequiresApi
import io.radar.sdk.Radar.RadarStatus
import io.radar.sdk.model.RadarRevealRiskToken
import kotlin.jvm.functions.Function1

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal class RadarRevealRiskManager(
    private val context: Context,
    private val logger: RadarLogger
) {
    fun revealRisk(
        callback: ((
            status: RadarStatus,
            result: RadarRevealRiskToken?
        ) -> Unit)
    ) {
        val revealRiskManager = this

        revealRiskManager.getFraudPayload { result ->
            val fraudPayload = result?.get("payload") as? String

            if (result?.containsKey("error") == true || fraudPayload == null) {
                val error = result?.get("error") as? String ?: "Unknown error"
                logger.e("Error getting fraud payload: $error", Radar.RadarLogType.SDK_ERROR)
                callback(Radar.RadarStatus.ERROR_PLUGIN, null)
                return@getFraudPayload
            }

            Radar.apiClient.revealRisk(
                fraudPayload,
                callback = callback)
        }
    }

    private fun getFraudPayload(callback: (Map<String, Any?>?) -> Unit) {
        try {
            val fraudClass = Class.forName("io.radar.sdk.fraud.RadarSDKFraud")
            val sharedInstanceMethod = fraudClass.getMethod("sharedInstance")
            val fraudInstance = sharedInstanceMethod.invoke(null)

            // Create adapter callback that matches getFraudPayload's Function1 signature
            val getFraudPayloadCallback = object : Function1<Map<String, Any?>?, Unit> {
                override fun invoke(result: Map<String, Any?>?) {
                    callback(result)
                }
            }

            // Create options map
            val options = mutableMapOf<String, Any?>(
                "context" to context,
                "location" to Location("")
            )

            val getFraudPayloadMethod = fraudClass.getMethod(
                "getFraudPayload",
                java.util.Map::class.java,
                Function1::class.java
            )

            getFraudPayloadMethod.invoke(fraudInstance, options, getFraudPayloadCallback)
        } catch (e: ClassNotFoundException) {
            logger.d("Skipping fraud checks: RadarSDKFraud submodule not available")
            callback(null)
        } catch (e: Exception) {
            logger.e("Error calling fraud detection", Radar.RadarLogType.SDK_EXCEPTION, e)
            callback(mapOf("error" to (e.message ?: "Unknown error")))
        }
    }
}
