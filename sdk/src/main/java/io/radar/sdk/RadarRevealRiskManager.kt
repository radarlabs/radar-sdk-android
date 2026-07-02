package io.radar.sdk

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.SystemClock
import androidx.annotation.RequiresApi
import io.radar.sdk.model.RadarConfig
import io.radar.sdk.model.RadarEvent
import io.radar.sdk.model.RadarUser
import io.radar.sdk.model.RadarRevealRiskToken
import org.json.JSONObject
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Enumeration
import kotlin.jvm.functions.Function1

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal class RadarRevealRiskManager(
    private val context: Context,
    private val logger: RadarLogger,
) {

    var started = false

    private val handler = Handler(this.context.mainLooper)
    private val connectivityManager = this.context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var startedInterval = 0
    private var runnable: Runnable? = null
    private var lastToken: RadarRevealRiskToken? = null
    private var lastTokenElapsedRealtime: Long = 0L
    private var lastTokenBeacons: Boolean = false
    private var lastIPs: String? = null
    private var lastIpChangedDeliveredAtMs: Long = 0L
    private var expectedCountryCode: String? = null
    private var expectedStateCode: String? = null

    fun revealRisk(
        reason: String? = null,
        transactionId: String? = null,
        callback: Radar.RadarRevealRiskCallback? = null
    ) {
        val revealRiskManager = this
        val usage = "revealRisk"

        val continueWithConfig = { status: Radar.RadarStatus, config: RadarConfig?, chosenVerifiedHost: String? ->
            if (status != Radar.RadarStatus.SUCCESS || config == null) {
                Radar.handler.post {
                    if (status != Radar.RadarStatus.SUCCESS) {
                        Radar.sendError(status)
                    }

                    callback?.onComplete(status)
                }
            } else {
                val googlePlayProjectNumber = config.googlePlayProjectNumber

                revealRiskManager.getFraudPayload(googlePlayProjectNumber) { result ->
                    val fraudPayload = result?.get("payload") as? String

                    if (result?.containsKey("error") == true || fraudPayload == null) {
                        val error = result?.get("error") as? String ?: "Unknown error"
                        logger.e("Error getting fraud payload: $error", Radar.RadarLogType.SDK_ERROR)
                        callback?.onComplete(Radar.RadarStatus.ERROR_PLUGIN)
                        return@getFraudPayload
                    }

                    val callRevealRiskApi = {
                        Radar.apiClient.revealRisk(
                            RadarActivityLifecycleCallbacks.foreground,
                            false,
                            revealRiskManager.expectedCountryCode,
                            revealRiskManager.expectedStateCode,
                            reason ?: "manual",
                            transactionId,
                            fraudPayload,
                            callback = object : RadarApiClient.RadarRevealRiskApiCallback {
                                override fun onComplete(
                                    status: Radar.RadarStatus,
                                    res: JSONObject?,
                                    events: Array<RadarEvent>?,
                                    user: RadarUser?,
                                    config: RadarConfig?,
                                    token: RadarRevealRiskToken?
                                ) {
                                    if (token != null) {
                                        revealRiskManager.lastToken = token
                                        revealRiskManager.lastTokenElapsedRealtime = SystemClock.elapsedRealtime()
                                        revealRiskManager.lastTokenBeacons = lastTokenBeacons
                                    }
                                    Radar.handler.post {
                                        if (status != Radar.RadarStatus.SUCCESS) {
                                            Radar.sendError(status)
                                        }

                                        callback?.onComplete(status, token)
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }

        Radar.apiClient.getConfig(usage = usage, verified = true, callback = object : RadarApiClient.RadarGetConfigApiCallback {
            override fun onComplete(status: Radar.RadarStatus, config: RadarConfig?) {
                continueWithConfig(status, config, null)
            }
        })
    }

    private fun callRevealRisk(reason: String?) {
        val revealRiskManager = this

        if (!revealRiskManager.started) {
            return
        }

        revealRiskManager.revealRisk(  reason, null, object : Radar.RadarRevealRiskCallback {
            override fun onComplete(
                status: Radar.RadarStatus,
                token: RadarRevealRiskToken?
            ) {
            }
        })
    }

    fun setExpectedJurisdiction(countryCode: String?, stateCode: String?) {
        this.expectedCountryCode = countryCode
        this.expectedStateCode = stateCode
    }

    private fun getFraudPayload(googlePlayProjectNumber: Long?, callback: (Map<String, Any?>?) -> Unit) {
        try {
            val fraudClass = Class.forName("io.radar.sdk.fraud.RadarSDKFraud")
            val sharedInstanceMethod = fraudClass.getMethod("sharedInstance")
            val fraudInstance = sharedInstanceMethod.invoke(null)

            // Create adapter callback that matches getFraudPayload's Function1 signature
            val getFraudPayloadCallback = object : Function1<Map<String, Any?>?, Unit> {
                override fun invoke(result: Map<String, Any?>?): Unit {
                    callback(result)
                }
            }

            // Create options map
            val options = mutableMapOf<String, Any?>(
                "context" to context
            )

            // Add integrity-related parameters if available
            if (googlePlayProjectNumber != null) {
                options["googlePlayProjectNumber"] = googlePlayProjectNumber
            }

            val getFraudPayloadMethod = fraudClass.getMethod("getFraudPayload",
                java.util.Map::class.java,
                Function1::class.java)

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
