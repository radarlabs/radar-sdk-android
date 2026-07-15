package io.radar.sdk.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Represents device and network risk signals for a reveal risk check.
 *
 * @see [](https://radar.com/documentation/fraud)
 */
class RadarRevealRiskToken(
    /**
     * The Radar ID of the reveal risk check.
     */
    val id: String,

    /**
     * The risk information for the reveal risk check.
     */
    val risk: Risk,

    /**
     * The network information for the reveal risk check.
     */
    val network: Network,

    /**
     * The device information for the reveal risk check.
     */
    val device: Device,

    /**
     * A signed JSON Web Token (JWT) containing the risk, network, and device information. Verify the token server-side using your secret key.
     */
    val token: String? = null,

    /**
     * The datetime when the token expires.
     */
    val expiresAt: String? = null,

    /**
     * The number of seconds until the token expires.
     */
    val expiresIn: Int? = null,

    /**
     * The full JSON value of the response.
     */
    val fullJson: JSONObject
) {

    /**
     * Represents the risk level for a reveal risk check.
     */
    enum class RiskLevel {
        NONE,
        LOW,
        MEDIUM,
        HIGH
    }

    /**
     * Represents the risk information for a reveal risk check.
     */
    class Risk(
        /**
         * The risk level.
         */
        val level: RiskLevel,

        /**
         * An array of risk signals.
         */
        val reasons: Array<String>
    ) {
        internal companion object {
            private const val FIELD_LEVEL = "level"
            private const val FIELD_REASONS = "reasons"

            fun fromJson(obj: JSONObject?): Risk? {
                if (obj == null) {
                    return null
                }

                val level = when (obj.optString(FIELD_LEVEL)) {
                    "low" -> RiskLevel.LOW
                    "medium" -> RiskLevel.MEDIUM
                    "high" -> RiskLevel.HIGH
                    else -> RiskLevel.NONE
                }
                val reasons = stringArrayFromJson(obj.optJSONArray(FIELD_REASONS)) ?: emptyArray()

                return Risk(level, reasons)
            }
        }
    }

    /**
     * Represents the network information for a reveal risk check.
     */
    class Network(
        /**
         * The IP address and IP geolocation for the reveal risk check.
         */
        val ipAddress: IpAddress? = null,

        /**
         * The IP privacy information for the reveal risk check.
         */
        val privacy: Privacy? = null,

        /**
         * The IP ASN information for the reveal risk check.
         */
        val asn: Asn? = null
    ) {
        internal companion object {
            private const val FIELD_IP_ADDRESS = "ipAddress"
            private const val FIELD_PRIVACY = "privacy"
            private const val FIELD_ASN = "asn"

            fun fromJson(obj: JSONObject?): Network? {
                if (obj == null) {
                    return null
                }

                val ipAddress = IpAddress.fromJson(obj.optJSONObject(FIELD_IP_ADDRESS))
                val privacy = Privacy.fromJson(obj.optJSONObject(FIELD_PRIVACY))
                val asn = Asn.fromJson(obj.optJSONObject(FIELD_ASN))

                return Network(ipAddress, privacy, asn)
            }
        }
    }

    /**
     * Represents the device information for a reveal risk check.
     */
    class Device(
        val deviceId: String? = null,
        val deviceType: String? = null,
        val deviceMake: String? = null,
        val deviceModel: String? = null,
        val deviceOSName: String? = null,
        val deviceOSVersion: String? = null,
        val sdkVersion: String? = null,
        val xPlatformType: String? = null,
        val installId: String? = null,
        val appId: String? = null,
        val appName: String? = null,
        val appVersion: String? = null,
        val appBuild: String? = null,
        val userAgent: String? = null,
        val browserName: String? = null,
        val browserVersion: String? = null,
        val browserEngine: String? = null,
        val browserEngineVersion: String? = null
    ) {
        internal companion object {
            fun fromJson(obj: JSONObject?): Device? {
                if (obj == null) {
                    return null
                }

                return Device(
                    obj.optStringOrNull("deviceId"),
                    obj.optStringOrNull("deviceType"),
                    obj.optStringOrNull("deviceMake"),
                    obj.optStringOrNull("deviceModel"),
                    obj.optStringOrNull("deviceOSName"),
                    obj.optStringOrNull("deviceOSVersion"),
                    obj.optStringOrNull("sdkVersion"),
                    obj.optStringOrNull("xPlatformType"),
                    obj.optStringOrNull("installId"),
                    obj.optStringOrNull("appId"),
                    obj.optStringOrNull("appName"),
                    obj.optStringOrNull("appVersion"),
                    obj.optStringOrNull("appBuild"),
                    obj.optStringOrNull("userAgent"),
                    obj.optStringOrNull("browserName"),
                    obj.optStringOrNull("browserVersion"),
                    obj.optStringOrNull("browserEngine"),
                    obj.optStringOrNull("browserEngineVersion")
                )
            }
        }
    }

    /**
     * Represents the IP address and IP geolocation for a reveal risk check.
     */
    class IpAddress(
        val ip: String? = null,
        val addressLabel: String? = null,
        val borough: String? = null,
        val city: String? = null,
        val confidence: String? = null,
        val country: String? = null,
        val countryCode: String? = null,
        val countryFlag: String? = null,
        val county: String? = null,
        val distance: Double? = null,
        val formattedAddress: String? = null,
        val geometry: String? = null,
        val latitude: Double? = null,
        val longitude: Double? = null,
        val neighborhood: String? = null,
        val number: String? = null,
        val placeLabel: String? = null,
        val postalCode: String? = null,
        val state: String? = null,
        val stateCode: String? = null,
        val dma: String? = null,
        val dmaCode: String? = null,
        val street: String? = null,
        val debug: String? = null,
        val layer: String? = null,
        val stateAllowed: Boolean? = null,
        val countryAllowed: Boolean? = null,
        val timeZone: String? = null,
        val connectionType: String? = null,
        val stateConfidence: String? = null,
        val countryConfidence: String? = null,
        val categories: Array<String>? = null,
        val chainSlug: String? = null,
        val ssid: String? = null,
        val bssid: String? = null
    ) {
        internal companion object {
            fun fromJson(obj: JSONObject?): IpAddress? {
                if (obj == null) {
                    return null
                }

                return IpAddress(
                    obj.optStringOrNull("ip"),
                    obj.optStringOrNull("addressLabel"),
                    obj.optStringOrNull("borough"),
                    obj.optStringOrNull("city"),
                    obj.optStringOrNull("confidence"),
                    obj.optStringOrNull("country"),
                    obj.optStringOrNull("countryCode"),
                    obj.optStringOrNull("countryFlag"),
                    obj.optStringOrNull("county"),
                    obj.optDoubleOrNull("distance"),
                    obj.optStringOrNull("formattedAddress"),
                    obj.optStringOrNull("geometry"),
                    obj.optDoubleOrNull("latitude"),
                    obj.optDoubleOrNull("longitude"),
                    obj.optStringOrNull("neighborhood"),
                    obj.optStringOrNull("number"),
                    obj.optStringOrNull("placeLabel"),
                    obj.optStringOrNull("postalCode"),
                    obj.optStringOrNull("state"),
                    obj.optStringOrNull("stateCode"),
                    obj.optStringOrNull("dma"),
                    obj.optStringOrNull("dmaCode"),
                    obj.optStringOrNull("street"),
                    obj.optStringOrNull("debug"),
                    obj.optStringOrNull("layer"),
                    obj.optBooleanOrNull("stateAllowed"),
                    obj.optBooleanOrNull("countryAllowed"),
                    obj.optStringOrNull("timeZone"),
                    obj.optStringOrNull("connectionType"),
                    obj.optStringOrNull("stateConfidence"),
                    obj.optStringOrNull("countryConfidence"),
                    stringArrayFromJson(obj.optJSONArray("categories")),
                    obj.optStringOrNull("chainSlug"),
                    obj.optStringOrNull("ssid"),
                    obj.optStringOrNull("bssid")
                )
            }
        }
    }

    /**
     * Represents the IP privacy information for a reveal risk check.
     */
    class Privacy(
        val vpn: Boolean? = null,
        val proxy: Boolean? = null,
        val tor: Boolean? = null,
        val relay: Boolean? = null,
        val hosting: Boolean? = null,
        val service: String? = null,
        val residentialProxy: Boolean? = null
    ) {
        internal companion object {
            fun fromJson(obj: JSONObject?): Privacy? {
                if (obj == null) {
                    return null
                }

                return Privacy(
                    obj.optBooleanOrNull("vpn"),
                    obj.optBooleanOrNull("proxy"),
                    obj.optBooleanOrNull("tor"),
                    obj.optBooleanOrNull("relay"),
                    obj.optBooleanOrNull("hosting"),
                    obj.optStringOrNull("service"),
                    obj.optBooleanOrNull("residentialProxy")
                )
            }
        }
    }

    /**
     * Represents the IP ASN information for a reveal risk check.
     */
    class Asn(
        val asn: String? = null,
        val name: String? = null,
        val domain: String? = null,
        val route: String? = null,
        val type: String? = null,
        val country: String? = null,
        val network: String? = null
    ) {
        internal companion object {
            fun fromJson(obj: JSONObject?): Asn? {
                if (obj == null) {
                    return null
                }

                return Asn(
                    obj.optStringOrNull("asn"),
                    obj.optStringOrNull("name"),
                    obj.optStringOrNull("domain"),
                    obj.optStringOrNull("route"),
                    obj.optStringOrNull("type"),
                    obj.optStringOrNull("country"),
                    obj.optStringOrNull("network")
                )
            }
        }
    }

    internal companion object {
        private const val FIELD_ID = "_id"
        private const val FIELD_RISK = "risk"
        private const val FIELD_NETWORK = "network"
        private const val FIELD_DEVICE = "device"
        private const val FIELD_TOKEN = "token"
        private const val FIELD_EXPIRES_AT = "expiresAt"
        private const val FIELD_EXPIRES_IN = "expiresIn"

        private fun JSONObject.optStringOrNull(name: String): String? = if (has(name) && !isNull(name)) optString(name) else null

        private fun JSONObject.optDoubleOrNull(name: String): Double? = if (has(name) && !isNull(name)) optDouble(name) else null

        private fun JSONObject.optBooleanOrNull(name: String): Boolean? = if (has(name) && !isNull(name)) optBoolean(name) else null

        private fun stringArrayFromJson(arr: JSONArray?): Array<String>? {
            if (arr == null) {
                return null
            }

            return Array(arr.length()) { arr.optString(it) }
        }

        fun fromJson(obj: JSONObject?): RadarRevealRiskToken? {
            if (obj == null) {
                return null
            }

            val id = obj.optString(FIELD_ID)
            val risk = Risk.fromJson(obj.optJSONObject(FIELD_RISK)) ?: return null
            val network = Network.fromJson(obj.optJSONObject(FIELD_NETWORK)) ?: return null
            val device = Device.fromJson(obj.optJSONObject(FIELD_DEVICE)) ?: return null
            val token = obj.optStringOrNull(FIELD_TOKEN)
            val expiresAt = obj.optStringOrNull(FIELD_EXPIRES_AT)
            val expiresIn = if (obj.has(FIELD_EXPIRES_IN) && !obj.isNull(FIELD_EXPIRES_IN)) {
                obj.optInt(FIELD_EXPIRES_IN)
            } else {
                null
            }

            obj.remove("meta")

            return RadarRevealRiskToken(id, risk, network, device, token, expiresAt, expiresIn, obj)
        }
    }

    fun toJson(): JSONObject = fullJson
}
