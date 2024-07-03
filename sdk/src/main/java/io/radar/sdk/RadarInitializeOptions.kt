package io.radar.sdk

import org.json.JSONObject
import io.radar.sdk.Radar.RadarLocationServicesProvider

data class RadarInitializeOptions(
    /**
     * A boolean indicating whether to enable additional fraud detection signals for location verification.
     */
    var fraud: Boolean = false,

    /**
     * An optional stable unique ID for the user to set upon initialization. No-op if null.
     */
    var userId: String? = null,

    /**
     * An optional set of custom key-value pairs for the user. Must have 16 or fewer keys and values of type string, boolean, or number. No-op if null. 
     */
    var metadata: JSONObject? = null,

    /**
     * An optional receiver for the client-side delivery of events.
     */
    var receiver: RadarReceiver? = null,

    /**
     * The location services provider.
     */
    var provider: RadarLocationServicesProvider = RadarLocationServicesProvider.GOOGLE,
) {

    companion object {
        internal const val KEY_FRAUD = "fraud"
        internal const val KEY_USER_ID = "userId"
        internal const val KEY_METADATA = "metadata"
        internal const val KEY_PROVIDER = "provider"

        @JvmStatic
        fun fromJson(obj: JSONObject): RadarInitializeOptions {
            val initializeOptions = RadarInitializeOptions()
            initializeOptions.fraud = obj.optBoolean(KEY_FRAUD)
            initializeOptions.userId = obj.optString(KEY_USER_ID)
            initializeOptions.metadata = obj.optJSONObject(KEY_METADATA)
            val providerString = obj.optString(KEY_PROVIDER)
            if (!providerString.isNullOrEmpty()) {
                initializeOptions.provider = RadarLocationServicesProvider.valueOf(providerString)
            }
            return initializeOptions
        }
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put(KEY_FRAUD, fraud)
        obj.put(KEY_USER_ID, userId)
        obj.put(KEY_METADATA, metadata)
        obj.put(KEY_PROVIDER, provider.toString().lowercase())
        return obj
    }
}
