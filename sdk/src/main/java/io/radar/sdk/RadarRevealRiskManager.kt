package io.radar.sdk

import android.content.Context
import android.location.Location
import android.os.Build
import android.os.SystemClock
import androidx.annotation.RequiresApi
import io.radar.sdk.Radar.RadarStatus
import io.radar.sdk.model.RadarConfig
import io.radar.sdk.model.RadarEvent
import io.radar.sdk.model.RadarRevealRiskToken
import io.radar.sdk.model.RadarUser
import kotlin.jvm.functions.Function1
import org.json.JSONObject

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal class RadarRevealRiskManager(
    private val context: Context,
    private val logger: RadarLogger
) {
    var started = false

    private var lastToken: RadarRevealRiskToken? = null
    private var lastTokenElapsedRealtime: Long = 0L
    private var lastTokenBeacons: Boolean = false
    private var expectedCountryCode: String? = null
    private var expectedStateCode: String? = null

    fun revealRisk(
        reason: String? = null,
        transactionId: String? = null,
        callback: ((
            status: RadarStatus,
            token: RadarRevealRiskToken?
        ) -> Unit)? = null
    ) {
        val revealRiskManager = this

        revealRiskManager.getFraudPayload { result ->
            val fraudPayload = result?.get("payload") as? String

            if (result?.containsKey("error") == true || fraudPayload == null) {
                val error = result?.get("error") as? String ?: "Unknown error"
                logger.e("Error getting fraud payload: $error", Radar.RadarLogType.SDK_ERROR)
                callback?.invoke(Radar.RadarStatus.ERROR_PLUGIN, null)
                return@getFraudPayload
            }

            Radar.apiClient.revealRisk(
                RadarActivityLifecycleCallbacks.foreground,
                false,
                revealRiskManager.expectedCountryCode,
                revealRiskManager.expectedStateCode,
                reason ?: "manual",
                transactionId,
                fraudPayload,
                callback = {
                        status: Radar.RadarStatus,
                        res: JSONObject?,
                        token: RadarRevealRiskToken?
                    ->
                    if (token != null) {
                        revealRiskManager.lastToken = token
                        revealRiskManager.lastTokenElapsedRealtime = SystemClock.elapsedRealtime()
                        revealRiskManager.lastTokenBeacons = lastTokenBeacons
                    }
                    Radar.handler.post {
                        if (status != Radar.RadarStatus.SUCCESS) {
                            Radar.sendError(status)
                        }
                        callback?.invoke(status, token)
                    }
                })
        }
    }

    fun setExpectedJurisdiction(countryCode: String?, stateCode: String?) {
        this.expectedCountryCode = countryCode
        this.expectedStateCode = stateCode
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
