package io.radar.sdk

import android.content.Context
import android.location.Location

internal class RadarSDKFraud {
    companion object {
        fun getFraudPayload(context: Context, logger: RadarLogger, location: Location? = null, googlePlayProjectNumber: Long? = null, callback: (Radar.RadarStatus, String) -> Unit) {
            try {
                val fraudClass = Class.forName("io.radar.sdk.fraud.RadarSDKFraud")
                val sharedInstanceMethod = fraudClass.getMethod("sharedInstance")
                val fraudInstance = sharedInstanceMethod.invoke(null)

                // Create adapter callback that matches getFraudPayload's Function1 signature
                val getFraudPayloadCallback = object : Function1<Map<String, Any?>?, Unit> {
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
                val options = mutableMapOf<String, Any?>(
                    "context" to context,
                    "location" to location
                )

                // Add integrity-related parameters if available
                if (googlePlayProjectNumber != null) {
                    options["googlePlayProjectNumber"] = googlePlayProjectNumber
                }

                val getFraudPayloadMethod = fraudClass.getMethod(
                    "getFraudPayload",
                    java.util.Map::class.java,
                    Function1::class.java
                )

                getFraudPayloadMethod.invoke(fraudInstance, options, getFraudPayloadCallback)
            } catch (e: ClassNotFoundException) {
                logger.d("Skipping fraud checks: RadarSDKFraud submodule not available")
                callback(Radar.RadarStatus.ERROR_PLUGIN, "")
            } catch (e: Exception) {
                logger.e("Error calling fraud detection ${e.message ?: ""}", Radar.RadarLogType.SDK_EXCEPTION, e)
                callback(Radar.RadarStatus.ERROR_PLUGIN, "")
            }
        }

        fun setRevealRiskId(revealRiskId: String?, logger: RadarLogger) {
            try {
                val fraudClass = Class.forName("io.radar.sdk.fraud.RadarSDKFraud")
                val sharedInstanceMethod = fraudClass.getMethod("sharedInstance")
                val fraudInstance = sharedInstanceMethod.invoke(null)

                val setRevealRiskMethod = fraudClass.getMethod(
                    "setRevealRiskId",
                    String::class.java
                )

                setRevealRiskMethod.invoke(fraudInstance, revealRiskId)
            } catch (e: ClassNotFoundException) {
                logger.d("Skipping fraud checks: RadarSDKFraud submodule not available")
            } catch (e: Exception) {
                logger.e("Error calling fraud detection ${e.message ?: ""}", Radar.RadarLogType.SDK_EXCEPTION, e)
            }
        }
    }
}
