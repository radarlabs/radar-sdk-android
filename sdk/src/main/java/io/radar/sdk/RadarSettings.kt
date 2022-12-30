package io.radar.sdk

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.radar.sdk.model.RadarFeatureSettings
import org.json.JSONObject
import java.math.BigInteger
import java.security.SecureRandom
import java.text.DecimalFormat
import java.util.*


internal object RadarSettings {

    private const val KEY_PUBLISHABLE_KEY = "publishable_key"
    private const val KEY_SERVER_PUBLIC_KEY = "server_public_key"
    private const val KEY_CLIENT_PUBLIC_KEY_MOD = "client_public_key_mod"
    private const val KEY_CLIENT_PUBLIC_KEY = "client_public_key"
    private const val KEY_CLIENT_PRIVATE_KEY = "client_private_key"
    private const val KEY_LOCATION_SERVICES_PROVIDER = "provider"
    private const val KEY_INSTALL_ID = "install_id"
    private const val KEY_SESSION_ID = "session_id"
    private const val KEY_ID = "radar_user_id"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_DESCRIPTION = "user_description"
    private const val KEY_METADATA = "user_metadata"
    private const val KEY_ANONYMOUS = "anonymous"
    private const val KEY_AD_ID_ENABLED = "ad_id_enabled"
    private const val KEY_TRACKING = "background_tracking"
    private const val KEY_TRACKING_OPTIONS = "tracking_options"
    private const val KEY_REMOTE_TRACKING_OPTIONS = "remote_tracking_options"
    private const val KEY_FOREGROUND_SERVICE = "foreground_service"
    private const val KEY_FEATURE_SETTINGS = "feature_settings"
    private const val KEY_TRIP_OPTIONS = "trip_options"
    private const val KEY_LOG_LEVEL = "log_level"
    private const val KEY_HOST = "host"
    private const val KEY_PERMISSIONS_DENIED = "permissions_denied"

    private const val KEY_OLD_UPDATE_INTERVAL = "dwell_delay"
    private const val KEY_OLD_UPDATE_INTERVAL_RESPONSIVE = 60000
    private const val KEY_OLD_SYNC_MODE = "sync_mode"
    private const val KEY_OLD_OFFLINE_MODE = "offline_mode"

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences("RadarSDK", Context.MODE_PRIVATE)
    }

    private fun getEncryptedSharedPreferences(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            "RadarSDK_encrypted",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    internal fun getPublishableKey(context: Context): String? {
        return getSharedPreferences(context).getString(KEY_PUBLISHABLE_KEY, null)
    }

    internal fun setPublishableKey(context: Context, publishableKey: String?) {
        getSharedPreferences(context).edit { putString(KEY_PUBLISHABLE_KEY, publishableKey) }
    }

    internal fun getServerPublicKey(context: Context): BigInteger? {
        val serverPublicKeyLong = getSharedPreferences(context).getLong(KEY_SERVER_PUBLIC_KEY, 0L)
        if (serverPublicKeyLong == 0L) {
            return null
        }
        return serverPublicKeyLong.toBigInteger()
    }

    internal fun setServerPublicKey(context: Context, base64ServerPublicKey: String?) {
        val serverPublicKeyBytes = Base64.decode(base64ServerPublicKey, 0)
        val serverPublicKey = BigInteger(serverPublicKeyBytes)
        getEncryptedSharedPreferences(context).edit { putLong(KEY_SERVER_PUBLIC_KEY, serverPublicKey.toLong()) }
    }

    internal fun setClientPublicAndPrivateKeys(context: Context) {
        val random = SecureRandom()
        val clientPublicKeyMod = BigInteger.probablePrime(2048, random)
        val clientPrime = BigInteger.probablePrime(2048, random)
        val clientPrivateKey = BigInteger.probablePrime(2048, random)
        val clientPublicKey = clientPrime.modPow(clientPrivateKey, clientPublicKeyMod)
        getEncryptedSharedPreferences(context).edit { putLong(KEY_CLIENT_PRIVATE_KEY, clientPrivateKey.toLong()) }
        getEncryptedSharedPreferences(context).edit { putLong(KEY_CLIENT_PUBLIC_KEY, clientPublicKey.toLong()) }
        getEncryptedSharedPreferences(context).edit { putLong(KEY_CLIENT_PUBLIC_KEY_MOD, clientPublicKeyMod.toLong()) }
    }

    internal fun getClientPrivateKey(context: Context): BigInteger {
        var clientPrivateKey = getSharedPreferences(context).getLong(KEY_CLIENT_PRIVATE_KEY, 0L)
        if (clientPrivateKey == 0L) {
            setClientPublicAndPrivateKeys(context)
            clientPrivateKey = getSharedPreferences(context).getLong(KEY_CLIENT_PRIVATE_KEY, 0L)
        }
        return clientPrivateKey.toBigInteger()
    }

    internal fun getClientPublicKey(context: Context): BigInteger {
        var clientPublicKey = getSharedPreferences(context).getLong(KEY_CLIENT_PUBLIC_KEY, 0L)
        if (clientPublicKey == 0L) {
            setClientPublicAndPrivateKeys(context)
            clientPublicKey = getSharedPreferences(context).getLong(KEY_CLIENT_PUBLIC_KEY, 0L)
        }
        return clientPublicKey.toBigInteger()
    }

    internal fun getClientPublicKeyMod(context: Context): BigInteger {
        var clientPublicKeyMod = getSharedPreferences(context).getLong(KEY_CLIENT_PUBLIC_KEY_MOD, 0L)
        if (clientPublicKeyMod == 0L) {
            setClientPublicAndPrivateKeys(context)
            clientPublicKeyMod = getSharedPreferences(context).getLong(KEY_CLIENT_PUBLIC_KEY_MOD, 0L)
        }
        return clientPublicKeyMod.toBigInteger()
    }

    internal fun getLocationServicesProvider(context: Context): String? {
        return getSharedPreferences(context).getString(KEY_LOCATION_SERVICES_PROVIDER, null)
    }

    internal fun setLocationServicesProvider(context: Context, provider: Radar.RadarLocationServicesProvider) {
        getSharedPreferences(context).edit { putString(KEY_LOCATION_SERVICES_PROVIDER, provider.name) }
    }

    internal fun getInstallId(context: Context): String {
        var installId = getSharedPreferences(context).getString(KEY_INSTALL_ID, null)
        if (installId == null) {
            installId = UUID.randomUUID().toString()
            getSharedPreferences(context).edit { putString(KEY_INSTALL_ID, installId) }
        }
        return installId
    }

    internal fun getSessionId(context: Context): String {
        return DecimalFormat("#").format(getSharedPreferences(context).getLong(KEY_SESSION_ID, 0))
    }

    internal fun updateSessionId(context: Context): Boolean {
        val timestampSeconds = System.currentTimeMillis() / 1000
        val sessionIdSeconds = getSharedPreferences(context).getLong(KEY_SESSION_ID, 0)
        if (timestampSeconds - sessionIdSeconds > 300) {
            getSharedPreferences(context).edit { putLong(KEY_SESSION_ID, timestampSeconds) }

            Radar.logger.d("New session | sessionId = ${this.getSessionId(context)}")

            return true
        }
        return false
    }

    internal fun getId(context: Context): String? {
        return getSharedPreferences(context).getString(KEY_ID, null)
    }

    internal fun setId(context: Context, _id: String?) {
        getSharedPreferences(context).edit { putString(KEY_ID, _id) }
    }

    internal fun getUserId(context: Context): String? {
        return getSharedPreferences(context).getString(KEY_USER_ID, null)
    }

    internal fun setUserId(context: Context, userId: String?) {
        getSharedPreferences(context).edit { putString(KEY_USER_ID, userId) }
    }

    internal fun getDescription(context: Context): String? {
        return getSharedPreferences(context).getString(KEY_DESCRIPTION, null)
    }

    internal fun setDescription(context: Context, description: String?) {
        getSharedPreferences(context).edit { putString(KEY_DESCRIPTION, description) }
    }

    internal fun getMetadata(context: Context): JSONObject? {
        val metadataJson = getSharedPreferences(context).getString(KEY_METADATA, null) ?: return null
        return JSONObject(metadataJson)
    }

    internal fun setMetadata(context: Context, metadata: JSONObject?) {
        val metadataJSON = metadata?.toString()
        getSharedPreferences(context).edit { putString(KEY_METADATA, metadataJSON) }
    }

    internal fun getAnonymousTrackingEnabled(context: Context): Boolean {
        return getSharedPreferences(context).getBoolean(KEY_ANONYMOUS, false)
    }

    internal fun setAnonymousTrackingEnabled(context: Context, enabled: Boolean) {
        getSharedPreferences(context).edit { putBoolean(KEY_ANONYMOUS, enabled) }
    }

    internal fun getAdIdEnabled(context: Context): Boolean {
        return getSharedPreferences(context).getBoolean(KEY_AD_ID_ENABLED, false)
    }

    internal fun setAdIdEnabled(context: Context, enabled: Boolean) {
        getSharedPreferences(context).edit { putBoolean(KEY_AD_ID_ENABLED, enabled) }
    }

    internal fun getTracking(context: Context): Boolean {
        return getSharedPreferences(context).getBoolean(KEY_TRACKING, false)
    }

    internal fun setTracking(context: Context, tracking: Boolean) {
        getSharedPreferences(context).edit { putBoolean(KEY_TRACKING, tracking) }
    }

    internal fun getTrackingOptions(context: Context): RadarTrackingOptions {
        return getTrackingOptionsByKey(context, KEY_TRACKING_OPTIONS)
    }

    private fun getTrackingOptionsByKey(context: Context, key: String): RadarTrackingOptions {
        val optionsJson = getSharedPreferences(context).getString(key, null)
        val options: RadarTrackingOptions?
        if (optionsJson != null) { // v3 tracking options set
            val optionsObj = JSONObject(optionsJson)
            options = RadarTrackingOptions.fromJson(optionsObj)
        } else {
            val oldInterval = getSharedPreferences(context).getInt(KEY_OLD_UPDATE_INTERVAL, 0)
            if (oldInterval > 0) { // v2 tracking options upgrade
                options = if (oldInterval == KEY_OLD_UPDATE_INTERVAL_RESPONSIVE) {
                    RadarTrackingOptions.RESPONSIVE
                } else {
                    RadarTrackingOptions.EFFICIENT
                }
                val oldSync = getSharedPreferences(context).getInt(KEY_OLD_SYNC_MODE, 0)
                if (oldSync == -1) {
                    options.sync = RadarTrackingOptions.RadarTrackingOptionsSync.ALL
                }
                val oldOffline = getSharedPreferences(context).getInt(KEY_OLD_OFFLINE_MODE, 0)
                if (oldOffline == -1) {
                    options.replay = RadarTrackingOptions.RadarTrackingOptionsReplay.NONE
                }
            } else { // no tracking options set
                options = RadarTrackingOptions.EFFICIENT
            }
        }
        return options
    }

    internal fun setTrackingOptions(context: Context, options: RadarTrackingOptions) {
        val optionsObj = options.toJson()
        val optionsJson = optionsObj.toString()
        getSharedPreferences(context).edit { putString(KEY_TRACKING_OPTIONS, optionsJson) }
    }

    internal fun getRemoteTrackingOptions(context: Context): RadarTrackingOptions? {
        val keyExists = getSharedPreferences(context).contains(KEY_REMOTE_TRACKING_OPTIONS)
        return if (keyExists) getTrackingOptionsByKey(context, KEY_REMOTE_TRACKING_OPTIONS) else null
    }

    internal fun setRemoteTrackingOptions(context: Context, options: RadarTrackingOptions) {
        val optionsJson = options.toJson().toString()
        getSharedPreferences(context).edit { putString(KEY_REMOTE_TRACKING_OPTIONS, optionsJson) }
    }

    internal fun removeRemoteTrackingOptions(context: Context) {
        getSharedPreferences(context).edit { remove(KEY_REMOTE_TRACKING_OPTIONS) }
    }

    internal fun getForegroundService(context: Context): RadarTrackingOptions.RadarTrackingOptionsForegroundService {
        val foregroundJson = getSharedPreferences(context).getString(KEY_FOREGROUND_SERVICE, null)
        var foregroundService: RadarTrackingOptions.RadarTrackingOptionsForegroundService? = null
        if (foregroundJson != null) {
            val foregroundObj = JSONObject(foregroundJson)
            foregroundService = RadarTrackingOptions.RadarTrackingOptionsForegroundService.fromJson(foregroundObj)
        }
        if (foregroundService == null) {
            foregroundService = RadarTrackingOptions.RadarTrackingOptionsForegroundService()
        }
        return foregroundService
    }

    internal fun setForegroundService(
        context: Context,
        foregroundService: RadarTrackingOptions.RadarTrackingOptionsForegroundService
    ) {
        val foregroundJson = foregroundService.toJson().toString()
        getSharedPreferences(context).edit { putString(KEY_FOREGROUND_SERVICE, foregroundJson) }
    }

    internal fun getTripOptions(context: Context): RadarTripOptions? {
        val optionsJson = getSharedPreferences(context).getString(KEY_TRIP_OPTIONS, null) ?: return null
        val optionsObj = JSONObject(optionsJson)
        return RadarTripOptions.fromJson(optionsObj)
    }

    internal fun setTripOptions(context: Context, options: RadarTripOptions?) {
        val optionsObj = options?.toJson()
        val optionsJson = optionsObj?.toString()
        getSharedPreferences(context).edit { putString(KEY_TRIP_OPTIONS, optionsJson) }
    }

    fun setFeatureSettings(context: Context, featureSettings: RadarFeatureSettings) {
        getSharedPreferences(context).edit { putString(KEY_FEATURE_SETTINGS, featureSettings.toJson().toString()) }
    }

    fun getFeatureSettings(context: Context): RadarFeatureSettings {
        val optionsJson = getSharedPreferences(context).getString(KEY_FEATURE_SETTINGS, null)
            ?: return RadarFeatureSettings.default()
        return RadarFeatureSettings.fromJson(JSONObject(optionsJson))
    }

    internal fun getLogLevel(context: Context): Radar.RadarLogLevel {
        val logLevelInt = getSharedPreferences(context).getInt(KEY_LOG_LEVEL, 3)
        return Radar.RadarLogLevel.fromInt(logLevelInt)
    }

    internal fun setLogLevel(context: Context, level: Radar.RadarLogLevel) {
        val logLevelInt = level.value
        getSharedPreferences(context).edit { putInt(KEY_LOG_LEVEL, logLevelInt) }
    }

    internal fun getHost(context: Context): String {
        return getSharedPreferences(context).getString(KEY_HOST, null) ?: "https://api.radar.io"
    }

    internal fun setPermissionsDenied(context: Context, denied: Boolean) {
        getSharedPreferences(context).edit { putBoolean(KEY_PERMISSIONS_DENIED, denied) }
    }

    internal fun getPermissionsDenied(context: Context): Boolean {
        return getSharedPreferences(context).getBoolean(KEY_PERMISSIONS_DENIED, false)
    }

}