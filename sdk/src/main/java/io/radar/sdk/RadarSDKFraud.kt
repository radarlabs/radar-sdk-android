package io.radar.sdk

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Base64
import java.security.SecureRandom
import org.json.JSONObject

internal open class RadarSDKFraud {
    companion object {
        var shared = RadarSDKFraud()

        fun prepareFraudPayload(
            context: Context,
            logger: RadarLogger,
            location: Location? = null,
            googlePlayProjectNumber: Long? = null,
            callback: (Radar.RadarStatus, RadarPreparedFraudPayload?) -> Unit
        ) {
            shared.prepareFraudPayload(context, logger, location, googlePlayProjectNumber, callback)
        }
    }

    open fun prepareFraudPayload(
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

    // The optional Fraud SDK requires API 21, above the Android versions affected by TrulyRandom.
    @SuppressLint("TrulyRandom")
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
