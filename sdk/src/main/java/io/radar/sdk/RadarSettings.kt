package io.radar.sdk

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONObject
import java.util.UUID

internal object RadarSettings {

    private const val KEY_PUBLISHABLE_KEY = "publishable_key"
    private const val KEY_INSTALL_ID = "install_id"
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

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences("RadarSDK", Context.MODE_PRIVATE)
    }

    internal fun getPublishableKey(context: Context): String? {
        return getSharedPreferences(context).getString(KEY_PUBLISHABLE_KEY, null)
    }

    internal fun setPublishableKey(context: Context, publishableKey: String?) {
        getSharedPreferences(context).edit { putString(KEY_PUBLISHABLE_KEY, publishableKey) }
    }

    internal fun getInstallId(context: Context): String {
        var installId = getSharedPreferences(context).getString(KEY_INSTALL_ID, null)
        if (installId == null) {
            installId = UUID.randomUUID().toString()
            getSharedPreferences(context).edit { putString(KEY_INSTALL_ID, installId) }
        }
        return installId
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
        val optionsJson = getSharedPreferences(context).getString(KEY_TRACKING_OPTIONS, null) ?: return RadarTrackingOptions.EFFICIENT
        val optionsObj = JSONObject(optionsJson)
        return RadarTrackingOptions.fromJson(optionsObj)
    }

    internal fun setTrackingOptions(context: Context, options: RadarTrackingOptions) {
        val optionsObj = options.toJson()
        val optionsJson = optionsObj.toString()
        getSharedPreferences(context).edit { putString(KEY_TRACKING_OPTIONS, optionsJson) }
    }

    internal fun getTripOptions(context: Context): RadarTripOptions? {
        val optionsJson = getSharedPreferences(context).getString(KEY_TRIP_OPTIONS, null) ?: return null
        val optionsObj = JSONObject(optionsJson)
        return RadarTripOptions.fromJson(optionsObj)
    }

    internal fun setTripOptions(context: Context, options: RadarTripOptions?) {
        val optionsObj = options?.toJson()
        val optionsJson = optionsObj?.toString()
        print(optionsJson)
        getSharedPreferences(context).edit { putString(KEY_TRIP_OPTIONS, optionsJson) }
    }

    internal fun getLogLevel(context: Context): Radar.RadarLogLevel {
        val logLevelInt = getSharedPreferences(context).getInt(KEY_LOG_LEVEL, 0)
        return Radar.RadarLogLevel.fromInt(logLevelInt)
    }

    internal fun setLogLevel(context: Context, level: Radar.RadarLogLevel) {
        val logLevelInt = level.value
        getSharedPreferences(context).edit { putInt(KEY_LOG_LEVEL, logLevelInt) }
    }

    internal fun setConfig(context: Context, config: JSONObject?) {
        val configJson = config.toString()
        getSharedPreferences(context).edit { putString(KEY_CONFIG, configJson) }
    }

    internal fun getHost(context: Context): String {
        return getSharedPreferences(context).getString(KEY_HOST, null) ?: "https://api.radar.io"
    }

}