package io.radar.sdk

import android.content.Context
import android.location.Location
import android.util.Base64
import java.security.SecureRandom
import org.json.JSONObject

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

        fun prepareFraudPayload(
            context: Context,
            logger: RadarLogger,
            location: Location? = null,
            googlePlayProjectNumber: Long? = null,
            callback: (Radar.RadarStatus, RadarPreparedFraudPayload?) -> Unit
        ) {
            try {
                val fraudClass = Class.forName("io.radar.sdk.fraud.RadarSDKFraud")
                val fraudInstance = fraudClass.getMethod("sharedInstance").invoke(null)
                val options = mutableMapOf<String, Any?>(
                    "context" to context,
                    "location" to location
                )
                if (googlePlayProjectNumber != null) {
                    options["googlePlayProjectNumber"] = googlePlayProjectNumber
                }

                val method = fraudClass.getMethod(
                    "prepareFraudPayload",
                    java.util.Map::class.java,
                    Function1::class.java
                )

                val fraudCallback = object : Function1<Map<String, Any?>?, Unit> {
                    override fun invoke(result: Map<String, Any?>?) {
                        val handle = result?.get("preparedPayload")
                        if (handle == null) {
                            val error = result?.get("error") as? String ?: "Unknown error"
                            logger.e("Error preparing fraud payload: $error", Radar.RadarLogType.SDK_ERROR)
                            callback(Radar.RadarStatus.ERROR_PLUGIN, null)
                        } else {
                            callback(Radar.RadarStatus.SUCCESS, RadarPreparedFraudPayload(handle))
                        }
                    }
                }
                method.invoke(fraudInstance, options, fraudCallback)
            } catch (e: ClassNotFoundException) {
                logger.d("Skipping fraud checks: RadarSDKFraud submodule not available")
                callback(Radar.RadarStatus.ERROR_PLUGIN, null)
            } catch (e: Exception) {
                logger.e("Error preparing fraud payload ${e.message ?: ""}", Radar.RadarLogType.SDK_EXCEPTION, e)
                callback(Radar.RadarStatus.ERROR_PLUGIN, null)
            }
        }
    }
}

internal class RadarPreparedFraudPayload(private val handle: Any) {
    fun seal(options: Map<String, Any?>): String {
        val result = handle.javaClass
            .getMethod("seal", java.util.Map::class.java)
            .invoke(handle, options) as? Map<*, *>

        return result?.get("payload") as? String
            ?: throw IllegalStateException(
                result?.get("error") as? String ?: "Failed to encrypt fraud payload"
            )
    }

    fun sealForRequest(
        path: String,
        params: JSONObject,
        headers: Map<String, String>
    ): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        val attemptId = Base64.encodeToString(
            bytes,
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        )

        return seal(
            mapOf(
                "method" to "POST",
                "canonicalRoute" to "/$path",
                "encryptionAttemptId" to attemptId,
                "issuedAt" to System.currentTimeMillis() / 1000L,
                "installId" to params.getString("installId"),
                "origin" to headers["X-Radar-Mobile-Origin"],
                "product" to headers["X-Radar-Product"],
                "sdkVersion" to headers["X-Radar-SDK-Version"],
                "authorization" to headers["Authorization"]
            )
        )
    }
}
