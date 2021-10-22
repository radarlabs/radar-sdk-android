package io.radar.sdk

import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import androidx.core.content.edit
import org.json.JSONObject
import java.text.DecimalFormat
import java.util.*

@Suppress("TooManyFunctions")
internal class RadarSettings(val context: RadarApplication) {

    companion object {
        private const val KEY_PUBLISHABLE_KEY = "publishable_key"
        private const val KEY_INSTALL_ID = "install_id"
        private const val KEY_SESSION_ID = "session_id"
        private const val KEY_ID = "radar_user_id"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_DESCRIPTION = "user_description"
        private const val KEY_METADATA = "user_metadata"
        private const val KEY_AD_ID_ENABLED = "ad_id_enabled"
        private const val KEY_TRACKING = "background_tracking"
        private const val KEY_TRACKING_OPTIONS = "tracking_options"
        private const val KEY_TRIP_OPTIONS = "trip_options"
        private const val KEY_LOG_LEVEL = "log_level"
        private const val KEY_CONFIG = "config"
        private const val KEY_HOST = "host"
        private const val KEY_PERMISSIONS_DENIED = "permissions_denied"

        private const val KEY_OLD_UPDATE_INTERVAL = "dwell_delay"
        private const val KEY_OLD_UPDATE_INTERVAL_RESPONSIVE = 60000
        private const val KEY_OLD_SYNC_MODE = "sync_mode"
        private const val KEY_OLD_OFFLINE_MODE = "offline_mode"
    }


    private fun getSharedPreferences(): SharedPreferences {
        return RadarUtils.getSharedPreferences(context)
    }

    internal fun getPublishableKey(): String? {
        return getSharedPreferences().getString(KEY_PUBLISHABLE_KEY, null)
    }

    internal fun setPublishableKey(publishableKey: String?) {
        getSharedPreferences().edit { putString(KEY_PUBLISHABLE_KEY, publishableKey) }
    }

    internal fun getInstallId(): String {
        var installId = getSharedPreferences().getString(KEY_INSTALL_ID, null)
        if (installId == null) {
            installId = UUID.randomUUID().toString()
            getSharedPreferences().edit { putString(KEY_INSTALL_ID, installId) }
        }
        return installId
    }

    internal fun getSessionId(): String {
        return DecimalFormat("#").format(getSharedPreferences().getLong(KEY_SESSION_ID, 0))
    }

    internal fun updateSessionId(): Boolean {
        val timestampSeconds = System.currentTimeMillis() / 1000
        val sessionIdSeconds = getSharedPreferences().getLong(KEY_SESSION_ID, 0)
        if (timestampSeconds - sessionIdSeconds > 300) {
            getSharedPreferences().edit { putLong(KEY_SESSION_ID, timestampSeconds) }
            return true
        }
        return false
    }

    internal fun getId(): String? {
        return getSharedPreferences().getString(KEY_ID, null)
    }

    @Suppress("FunctionParameterNaming")
    internal fun setId(_id: String?) {
        getSharedPreferences().edit { putString(KEY_ID, _id) }
    }

    internal fun getUserId(): String? {
        return getSharedPreferences().getString(KEY_USER_ID, null)
    }

    internal fun setUserId(userId: String?) {
        getSharedPreferences().edit { putString(KEY_USER_ID, userId) }
    }

    internal fun getDescription(): String? {
        return getSharedPreferences().getString(KEY_DESCRIPTION, null)
    }

    internal fun setDescription(description: String?) {
        getSharedPreferences().edit { putString(KEY_DESCRIPTION, description) }
    }

    internal fun getMetadata(): JSONObject? {
        val metadataJson = getSharedPreferences().getString(KEY_METADATA, null) ?: return null
        return JSONObject(metadataJson)
    }

    internal fun setMetadata(metadata: JSONObject?) {
        val metadataJSON = metadata?.toString()
        getSharedPreferences().edit { putString(KEY_METADATA, metadataJSON) }
    }

    internal fun getAdIdEnabled(): Boolean {
        return getSharedPreferences().getBoolean(KEY_AD_ID_ENABLED, false)
    }

    internal fun setAdIdEnabled(enabled: Boolean) {
        getSharedPreferences().edit { putBoolean(KEY_AD_ID_ENABLED, enabled) }
    }

    internal fun getTracking(): Boolean {
        return getSharedPreferences().getBoolean(KEY_TRACKING, false)
    }

    internal fun setTracking(tracking: Boolean) {
        getSharedPreferences().edit { putBoolean(KEY_TRACKING, tracking) }
    }

    internal fun getTrackingOptions(): RadarTrackingOptions {
        val optionsJson = getSharedPreferences().getString(KEY_TRACKING_OPTIONS, null)
        val options: RadarTrackingOptions?
        if (optionsJson != null) { // v3 tracking options set
            val optionsObj = JSONObject(optionsJson)
            options = RadarTrackingOptions.fromJson(optionsObj)
        } else {
            val oldInterval = getSharedPreferences().getInt(KEY_OLD_UPDATE_INTERVAL, 0)
            if (oldInterval > 0) { // v2 tracking options upgrade
                options = if (oldInterval == KEY_OLD_UPDATE_INTERVAL_RESPONSIVE) {
                    RadarTrackingOptions.RESPONSIVE
                } else {
                    RadarTrackingOptions.EFFICIENT
                }
                val oldSync = getSharedPreferences().getInt(KEY_OLD_SYNC_MODE, 0)
                if (oldSync == -1) {
                    options.sync = RadarTrackingOptions.RadarTrackingOptionsSync.ALL
                }
                val oldOffline = getSharedPreferences().getInt(KEY_OLD_OFFLINE_MODE, 0)
                if (oldOffline == -1) {
                    options.replay = RadarTrackingOptions.RadarTrackingOptionsReplay.NONE
                }
            } else { // no tracking options set
                options = RadarTrackingOptions.EFFICIENT
            }
        }
        return options
    }

    internal fun setTrackingOptions(options: RadarTrackingOptions) {
        val optionsObj = options.toJson()
        val optionsJson = optionsObj.toString()
        getSharedPreferences().edit { putString(KEY_TRACKING_OPTIONS, optionsJson) }
    }

    internal fun getTripOptions(): RadarTripOptions? {
        val optionsJson = getSharedPreferences().getString(KEY_TRIP_OPTIONS, null) ?: return null
        val optionsObj = JSONObject(optionsJson)
        return RadarTripOptions.fromJson(optionsObj)
    }

    internal fun setTripOptions(options: RadarTripOptions?) {
        val optionsObj = options?.toJson()
        val optionsJson = optionsObj?.toString()
        getSharedPreferences().edit { putString(KEY_TRIP_OPTIONS, optionsJson) }
    }

    internal fun getLogLevel(): Radar.RadarLogLevel {
        val logLevelInt = getSharedPreferences().getInt(KEY_LOG_LEVEL, 3)
        return Radar.RadarLogLevel.fromInt(logLevelInt)
    }

    internal fun setLogLevel(level: Radar.RadarLogLevel) {
        val logLevelInt = level.value
        getSharedPreferences().edit { putInt(KEY_LOG_LEVEL, logLevelInt) }
    }

    internal fun setConfig(config: JSONObject?) {
        if (config == null) {
            getSharedPreferences().edit { remove(KEY_CONFIG) }
        } else {
            val configJson = config.toString()
            getSharedPreferences().edit { putString(KEY_CONFIG, configJson) }
        }
    }

    @VisibleForTesting
    internal fun getConfig(): JSONObject? {
        val configJson = getSharedPreferences().getString(KEY_CONFIG, null) ?: return null
        return JSONObject(configJson)
    }

    internal fun getHost(): String {
        return getSharedPreferences().getString(KEY_HOST, null) ?: "https://api.radar.io"
    }

    @VisibleForTesting
    internal fun setHost(host: String?) {
        getSharedPreferences().edit { putString(KEY_HOST, host) }
    }

    internal fun setPermissionsDenied(denied: Boolean) {
        getSharedPreferences().edit { putBoolean(KEY_PERMISSIONS_DENIED, denied) }
    }

    internal fun getPermissionsDenied(): Boolean {
        return getSharedPreferences().getBoolean(KEY_PERMISSIONS_DENIED, false)
    }

}