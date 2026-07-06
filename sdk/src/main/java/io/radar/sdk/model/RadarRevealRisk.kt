package io.radar.sdk.model

import io.radar.sdk.RadarUtils
import java.util.Date
import org.json.JSONObject

/**
 * Represents a user's reveal risk information.
 *
 * @see [](https://radar.com/documentation/fraud)
 */
data class RadarRevealRisk(
    /**
     * A signed JSON Web Token (JWT) containing the user and array of events. Verify the token server-side using your secret key.
     */
    val token: String,

    /**
     * The Radar ID of the reveal risk check.
     */
    val id: String,

    /**
     * The datetime when the token expires.
     */
    val expiresAt: Date?,

    /**
     * The number of seconds until the token expires.
     */
    val expiresIn: Int,

    /**
     * The details risk information for the device
     */
    val risk: Risk,

    /**
     * The information collected about the network
     */
    val network: Network,

    /**
     * The information collected about the device
     */
    val device: Device,

    /**
     * The full JSON value of the token.
     */
    val fullJson: JSONObject
) {
    data class Risk(
        val level: String,
        val failureReasons: Array<String>,
        val score: Double
    )

    data class Geometry(
        val type: String = "point",
        val coordinates: Array<Double>
    )

    data class IpAddress(
        val countryCode: String,
        val country: String,
        val countryFlag: String,
        val state: String,
        val city: String,
        val postalCode: String,
        val latitude: Double,
        val longitude: Double,
        val connectionType: String,
        val stateCode: String,
        val stateConfidence: String,
        val countryConfidence: String,
        val dma: String,
        val dmaCode: String,
        val stateAllowed: Boolean,
        val countryAllowed: Boolean,
        val layer: String,
        val geometry: Geometry
    )

    data class Privacy(
        val hosting: Boolean,
        val proxy: Boolean,
        val relay: Boolean,
        val service: String,
        val tor: Boolean,
        val vpn: Boolean,
        val residentialProxy: Boolean
    )

    data class Asn(
        val asn: String,
        val country: String,
        val domain: String,
        val name: String,
        val network: String,
        val type: String
    )

    data class Network(
        val ipAddress: IpAddress,
        val privacy: Privacy,
        val asn: Asn
    )

    data class Device(
        val deviceId: String,
        val deviceType: String,
        val deviceMake: String,
        val deviceModel: String,
        val deviceOSName: String,
        val deviceOSVersion: String,
        val sdkVersion: String,
        val xPlatformType: String,
        val installId: String,
        val appId: String,
        val appName: String,
        val appVersion: String,
        val appBuild: String
    )

    internal companion object {
        private const val RISK_TITLE = "risk"
        private const val NETWORK_TITLE = "network"
        private const val IP_ADDRESS_TITLE = "ipAddress"
        private const val GEOMETRY_TITLE = "geometry"
        private const val PRIVACY_TITLE = "privacy"
        private const val DEVICE_TITLE = "device"
        private const val ASN_TITLE = "asn"

        // base constant
        private const val FIELD_ID = "_id"
        private const val FIELD_TOKEN = "token"
        private const val FIELD_EXPIRES_IN = "expiresIn"
        private const val FIELD_EXPIRES_AT = "expiresAt"

        // risk constants
        private const val FIELD_FRAUD_LEVEL = "level"
        private const val FIELD_FRAUD_SCORE = "score"
        private const val FIELD_FAILURE_REASONS = "failureReasons"

        // networking constants
        // ip address constants
        private const val FIELD_COUNTRY_CODE = "countryCode"
        private const val FIELD_COUNTRY = "country"
        private const val FIELD_COUNTRY_FLAG = "countryFlag"
        private const val FIELD_STATE = "state"
        private const val FIELD_CITY = "city"
        private const val FIELD_POSTAL_CODE = "postalCode"
        private const val FIELD_LATITUDE = "latitude"
        private const val FIELD_LONGITUDE = "longitude"
        private const val FIELD_CONNECTION_TYPE = "connectionType"
        private const val FIELD_STATE_CODE = "stateCode"
        private const val FIELD_STATE_CONFIDENCE = "stateConfidence"
        private const val FIELD_COUNTRY_CONFIDENCE = "countryConfidence"
        private const val FIELD_DMA = "dma"
        private const val FIELD_DMA_CODE = "dmaCode"
        private const val FIELD_STATE_ALLOWED = "stateAllowed"
        private const val FIELD_COUNTRY_ALLOWED = "countryAllowed"
        private const val FIELD_LAYER = "layer"

        // geometry constants
        private const val FIELD_TYPE = "type"
        private const val FIELD_COORDINATES = "coordinates"

        // privacy constants
        private const val FIELD_HOSTING = "hosting"
        private const val FIELD_PROXY = "proxy"
        private const val FIELD_RELAY = "relay"
        private const val FIELD_SERVICE = "service"
        private const val FIELD_TOR = "tor"
        private const val FIELD_VPN = "vpn"
        private const val FIELD_RESIDENTIAL_PROXY = "residentialProxy"

        // asn constants
        private const val FIELD_ASN = "asn"
        private const val FIELD_DOMAIN = "domain"
        private const val FIELD_NAME = "name"
        private const val FIELD_NETWORK = "network"

        // device constants
        private const val FIELD_DEVICE_ID = "deviceId"
        private const val FIELD_DEVICE_TYPE = "deviceType"
        private const val FIELD_DEVICE_MAKE = "deviceMake"
        private const val FIELD_DEVICE_MODEL = "deviceModel"
        private const val FIELD_DEVICE_OS_NAME = "deviceOSName"
        private const val FIELD_DEVICE_OS_VERSION = "deviceOSVersion"
        private const val FIELD_SDK_VERSION = "sdkVersion"
        private const val FIELD_X_PLATFORM_TYPE = "xPlatformType"
        private const val FIELD_INSTALL_ID = "installId"
        private const val FIELD_APP_ID = "appId"
        private const val FIELD_APP_NAME = "appName"
        private const val FIELD_APP_VERSION = "appVersion"
        private const val FIELD_APP_BUILD = "appBuild"

        fun fromJson(obj: JSONObject?): RadarRevealRisk? {
            if (obj == null) {
                return null
            }

            val riskJson = obj.getJSONObject(RISK_TITLE)
            val risk = Risk(
                level = riskJson.getString(FIELD_FRAUD_LEVEL),
                failureReasons = riskJson.optJSONArray(FIELD_FAILURE_REASONS)?.let { failureReasons ->
                    Array(failureReasons.length()) {
                        failureReasons.optString(it)
                    }
                } ?: emptyArray(),
                score = riskJson.optDouble(FIELD_FRAUD_SCORE)
            )

            val networkJson = obj.optJSONObject(NETWORK_TITLE)
            val ipAddressJson = networkJson!!.optJSONObject(IP_ADDRESS_TITLE)
            val geometryJson = ipAddressJson!!.optJSONObject(GEOMETRY_TITLE)
            val privacyJson = networkJson.optJSONObject(PRIVACY_TITLE)
            val asnJson = networkJson.optJSONObject(ASN_TITLE)

            val geometry = Geometry(
                type = geometryJson!!.getString(FIELD_TYPE),
                coordinates = geometryJson.optJSONArray(FIELD_COORDINATES)?.let { coordinates ->
                    Array(coordinates.length()) {
                        coordinates.optDouble(it)
                    }
                } ?: emptyArray()
            )

            val ipAddress = IpAddress(
                countryCode = ipAddressJson.optString(FIELD_COUNTRY_CODE),
                country = ipAddressJson.optString(FIELD_COUNTRY),
                countryFlag = ipAddressJson.optString(FIELD_COUNTRY_FLAG),
                state = ipAddressJson.optString(FIELD_STATE),
                city = ipAddressJson.optString(FIELD_CITY),
                postalCode = ipAddressJson.optString(FIELD_POSTAL_CODE),
                latitude = ipAddressJson.optDouble(FIELD_LATITUDE),
                longitude = ipAddressJson.optDouble(FIELD_LONGITUDE),
                connectionType = ipAddressJson.getString(FIELD_CONNECTION_TYPE),
                stateCode = ipAddressJson.getString(FIELD_STATE_CODE),
                stateConfidence = ipAddressJson.getString(FIELD_STATE_CONFIDENCE),
                countryConfidence = ipAddressJson.getString(FIELD_COUNTRY_CONFIDENCE),
                dma = ipAddressJson.getString(FIELD_DMA),
                dmaCode = ipAddressJson.getString(FIELD_DMA_CODE),
                stateAllowed = ipAddressJson.getBoolean(FIELD_STATE_ALLOWED),
                countryAllowed = ipAddressJson.getBoolean(FIELD_COUNTRY_ALLOWED),
                layer = ipAddressJson.getString(FIELD_LAYER),
                geometry = geometry
            )

            val privacy = Privacy(
                hosting = privacyJson!!.optBoolean(FIELD_HOSTING),
                proxy = privacyJson.optBoolean(FIELD_PROXY),
                relay = privacyJson.optBoolean(FIELD_RELAY),
                service = privacyJson.optString(FIELD_SERVICE),
                tor = privacyJson.optBoolean(FIELD_TOR),
                vpn = privacyJson.optBoolean(FIELD_VPN),
                residentialProxy = privacyJson.optBoolean(FIELD_RESIDENTIAL_PROXY)
            )

            val asn = Asn(
                asn = asnJson!!.getString(FIELD_ASN),
                country = asnJson.getString(FIELD_COUNTRY),
                domain = asnJson.getString(FIELD_DOMAIN),
                name = asnJson.getString(FIELD_NAME),
                network = asnJson.getString(FIELD_NETWORK),
                type = asnJson.getString(FIELD_TYPE)
            )

            val network = Network(
                ipAddress,
                privacy,
                asn
            )

            val deviceJson = obj.optJSONObject(DEVICE_TITLE)

            val device = Device(
                deviceId = deviceJson!!.getString(FIELD_DEVICE_ID),
                deviceType = deviceJson.getString(FIELD_DEVICE_TYPE),
                deviceMake = deviceJson.getString(FIELD_DEVICE_MAKE),
                deviceModel = deviceJson.getString(FIELD_DEVICE_MODEL),
                deviceOSName = deviceJson.getString(FIELD_DEVICE_OS_NAME),
                deviceOSVersion = deviceJson.getString(FIELD_DEVICE_OS_VERSION),
                sdkVersion = deviceJson.getString(FIELD_SDK_VERSION),
                xPlatformType = deviceJson.getString(FIELD_X_PLATFORM_TYPE),
                installId = deviceJson.getString(FIELD_INSTALL_ID),
                appId = deviceJson.getString(FIELD_APP_ID),
                appName = deviceJson.getString(FIELD_APP_NAME),
                appVersion = deviceJson.getString(FIELD_APP_VERSION),
                appBuild = deviceJson.getString(FIELD_APP_BUILD)
            )

            val token: String? = obj.optString(FIELD_TOKEN)
            val id = obj.optString(FIELD_ID) ?: ""
            val expiresAt: Date? = RadarUtils.isoStringToDate(obj.optString(FIELD_EXPIRES_AT))
            val expiresIn: Int = obj.optInt(FIELD_EXPIRES_IN)

            if (token == null) {
                return null
            }

            return RadarRevealRisk(token, id, expiresAt, expiresIn, risk, network, device, obj)
        }
    }

    fun toJson(): JSONObject = fullJson
}
