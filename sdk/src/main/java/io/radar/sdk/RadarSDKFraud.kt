package io.radar.sdk

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Base64
import java.lang.reflect.InvocationTargetException
import java.security.SecureRandom
import org.json.JSONObject

internal open class RadarSDKFraud(
    private val fraudClassProvider: () -> Class<*> = { Class.forName("io.radar.sdk.fraud.RadarSDKFraud") }
) {
    companion object {
        private const val REQUIRED_CORE_COMPATIBILITY_VERSION = 1
        private const val REQUIRED_FRAUD_SDK_MESSAGE =
            "A compatible Radar Fraud SDK is required for verified requests."
        private const val INCOMPATIBLE_FRAUD_SDK_MESSAGE =
            "Installed Radar Fraud SDK is incompatible with this Radar SDK. " +
                "Update to compatible Radar SDK and Radar Fraud SDK versions."

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
        val (fraudInstance, method) = try {
            val fraudClass = fraudClassProvider()
            check(
                fraudClass.getMethod("coreCompatibilityVersion").invoke(null) == REQUIRED_CORE_COMPATIBILITY_VERSION
            ) {
                INCOMPATIBLE_FRAUD_SDK_MESSAGE
            }
            val method = fraudClass.getMethod(
                "prepareFraudPayload",
                java.util.Map::class.java,
                Function1::class.java
            )
            val instance = checkNotNull(fraudClass.getMethod("sharedInstance").invoke(null))
            instance to method
        } catch (e: ClassNotFoundException) {
            logger.e(REQUIRED_FRAUD_SDK_MESSAGE, Radar.RadarLogType.SDK_ERROR)
            callback(Radar.RadarStatus.ERROR_PLUGIN, null)
            return
        } catch (e: Exception) {
            logger.e(INCOMPATIBLE_FRAUD_SDK_MESSAGE, Radar.RadarLogType.SDK_EXCEPTION, e)
            callback(Radar.RadarStatus.ERROR_PLUGIN, null)
            return
        } catch (e: LinkageError) {
            logger.e(INCOMPATIBLE_FRAUD_SDK_MESSAGE, Radar.RadarLogType.SDK_EXCEPTION, e)
            callback(Radar.RadarStatus.ERROR_PLUGIN, null)
            return
        }

        val options = mutableMapOf<String, Any?>(
            "context" to context,
            "location" to location
        )
        if (googlePlayProjectNumber != null) {
            options["googlePlayProjectNumber"] = googlePlayProjectNumber
        }

        val fraudCallback = object : Function1<Map<String, Any?>?, Unit> {
            override fun invoke(result: Map<String, Any?>?) {
                val prepared = try {
                    check(result?.get("error") == null) {
                        result?.get("error") as? String ?: "Failed to prepare fraud payload"
                    }
                    val handle = checkNotNull(result?.get("preparedPayload")) {
                        "Failed to prepare fraud payload"
                    }
                    RadarPreparedFraudPayload(handle)
                } catch (e: Exception) {
                    logger.e("Failed to prepare fraud payload", Radar.RadarLogType.SDK_ERROR, e)
                    callback(Radar.RadarStatus.ERROR_BAD_REQUEST, null)
                    return
                }
                callback(Radar.RadarStatus.SUCCESS, prepared)
            }
        }
        try {
            method.invoke(fraudInstance, options, fraudCallback)
        } catch (e: Exception) {
            val cause = (e as? InvocationTargetException)?.targetException ?: e
            logger.e("Failed to prepare fraud payload", Radar.RadarLogType.SDK_EXCEPTION, cause)
            callback(Radar.RadarStatus.ERROR_BAD_REQUEST, null)
        }
    }
}

internal class RadarPreparedFraudPayload(private val handle: Any) {
    private val sealMethod = handle.javaClass.getMethod("seal", java.util.Map::class.java)

    fun seal(options: Map<String, Any?>): String {
        val result = try {
            sealMethod.invoke(handle, options) as? Map<*, *>
        } catch (e: Exception) {
            val cause = (e as? InvocationTargetException)?.targetException ?: e
            throw IllegalStateException("Failed to encrypt request body", cause)
        }
        val payload = result?.get("payload") as? String
        if (result?.get("error") != null || payload.isNullOrEmpty()) {
            throw IllegalStateException("Failed to encrypt request body")
        }

        return payload
    }

    // The optional Fraud SDK requires API 21, above the Android versions affected by TrulyRandom.
    @SuppressLint("TrulyRandom")
    fun sealForRequest(
        path: String,
        params: JSONObject,
        headers: Map<String, String>
    ): String {
        val attemptId = try {
            val bytes = ByteArray(16)
            SecureRandom().nextBytes(bytes)
            Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to generate encryption attempt ID", e)
        }

        return seal(
            mapOf(
                "body" to params.toString(),
                "method" to "POST",
                "canonicalRoute" to "/$path",
                "encryptionAttemptId" to attemptId,
                "issuedAt" to System.currentTimeMillis() / 1000L,
                "origin" to headers["X-Radar-Mobile-Origin"],
                "product" to headers["X-Radar-Product"],
                "sdkVersion" to headers["X-Radar-SDK-Version"],
                "authorization" to headers["Authorization"]
            )
        )
    }
}
