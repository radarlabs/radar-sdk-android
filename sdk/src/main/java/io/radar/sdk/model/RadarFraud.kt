package io.radar.sdk.model

import org.json.JSONObject

/**
 * Learned fraud state for the user
 */
data class RadarFraud(
    /**
     * A boolean indicating whether the user's IP address is a known proxy. May be `null` if Fraud is not enabled.
     */
    val proxy: Boolean?,

    /**
     * A boolean indicating whether or not the user's location is being mocked, such as in a simulation. May be
     * `null` if Fraud is not enabled.
     */
    val mocked: Boolean?
) {
    companion object {
        private const val PROXY = "proxy"
        private const val MOCKED = "mocked"

        @JvmStatic
        fun fromJson(json: JSONObject?): RadarFraud {
            return RadarFraud(
                proxy = json?.optBoolean(PROXY),
                mocked = json?.optBoolean(MOCKED)
            )
        }
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            putOpt(PROXY, proxy)
            putOpt(MOCKED, mocked)
        }
    }
}
