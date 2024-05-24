package io.radar.sdk

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import io.radar.sdk.model.RadarFeatureSettings
import io.radar.sdk.model.RadarSDKConfiguration
import org.json.JSONObject
import java.text.DecimalFormat
import java.util.*


internal object RadarSettings {

    private const val KEY_PUBLISHABLE_KEY = "publishable_key"
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
    private const val KEY_PREVIOUS_TRACKING_OPTIONS = "previous_tracking_options"
    private const val KEY_REMOTE_TRACKING_OPTIONS = "remote_tracking_options"
    private const val KEY_FOREGROUND_SERVICE = "foreground_service"
    private const val KEY_NOTIFICATION_OPTIONS = "notification_options"
    private const val KEY_FEATURE_SETTINGS = "feature_settings"
    private const val KEY_TRIP_OPTIONS = "trip_options"
    private const val KEY_LOG_LEVEL = "log_level"
    private const val KEY_HOST = "host"
    private const val KEY_PERMISSIONS_DENIED = "permissions_denied"
    private const val KEY_LAST_TRACKED_TIME = "last_tracked_time"
    private const val KEY_VERIFIED_HOST = "verified_host"
    private const val KEY_LAST_APP_OPEN_TIME = "last_app_open_time"
    private const val KEY_SHARING = "sharing"
    private const val KEY_X_PLATFORM_SDK_TYPE = "x_platform_sdk_type"
    private const val KEY_X_PLATFORM_SDK_VERSION = "x_platform_sdk_version"

    private const val KEY_OLD_UPDATE_INTERVAL = "dwell_delay"
    private const val KEY_OLD_UPDATE_INTERVAL_RESPONSIVE = 60000
    private const val KEY_OLD_SYNC_MODE = "sync_mode"
    private const val KEY_OLD_OFFLINE_MODE = "offline_mode"
    private const val KEY_USER_DEBUG = "user_debug"

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences("RadarSDK", Context.MODE_PRIVATE)
    }

    internal fun getPublishableKey(context: Context): String? {
        return getSharedPreferences(context).getString(KEY_PUBLISHABLE_KEY, null)
    }

    internal fun setPublishableKey(context: Context, publishableKey: String?) {
        getSharedPreferences(context).edit { putString(KEY_PUBLISHABLE_KEY, publishableKey) }
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

        val settings = RadarSettings.getFeatureSettings(context)
        if (settings.extendFlushReplays) {
            Radar.logger.d("Flushing replays from updateSessionId")
            Radar.flushReplays()
        }

        if (timestampSeconds - sessionIdSeconds > 300) {
            getSharedPreferences(context).edit { putLong(KEY_SESSION_ID, timestampSeconds) }

            Radar.logOpenedAppConversion()

            Radar.logger.d("New session | sessionId = ${this.getSessionId(context)}")

            setSharing(context, false)

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

    internal fun getPreviousTrackingOptions(context: Context): RadarTrackingOptions? {
        val keyExists = getSharedPreferences(context).contains(KEY_PREVIOUS_TRACKING_OPTIONS)
        return if (keyExists) getTrackingOptionsByKey(context, KEY_PREVIOUS_TRACKING_OPTIONS) else null
    }

    internal fun setPreviousTrackingOptions(context: Context, options: RadarTrackingOptions) {
        val optionsObj = options.toJson()
        val optionsJson = optionsObj.toString()
        getSharedPreferences(context).edit { putString(KEY_PREVIOUS_TRACKING_OPTIONS, optionsJson) }
    }

    internal fun removePreviousTrackingOptions(context: Context) {
        getSharedPreferences(context).edit { remove(KEY_PREVIOUS_TRACKING_OPTIONS) }
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

    internal fun setNotificationOptions(context:Context,notificationOptions:RadarNotificationOptions){
        val notificationOptionsJson = notificationOptions.toJson().toString()
        getSharedPreferences(context).edit { putString(KEY_NOTIFICATION_OPTIONS, notificationOptionsJson) }
        // Update foregroundServiceOptions as well.
        var previousValue = getForegroundService(context)
        setForegroundService(context,RadarTrackingOptions.RadarTrackingOptionsForegroundService(
            previousValue.text,
            previousValue.title,
            previousValue.icon,
            previousValue.updatesOnly,
            previousValue.activity,
            previousValue.importance,
            previousValue.id,
            previousValue.channelName,
            notificationOptions.getForegroundServiceIcon()?:previousValue.iconString,
            notificationOptions.getForegroundServiceColor()?:previousValue.iconColor
        ))
    }

    internal fun getNotificationOptions(context: Context):RadarNotificationOptions?{
        val optionsJson = getSharedPreferences(context).getString(KEY_NOTIFICATION_OPTIONS, null) ?: return null
        val optionsObj = JSONObject(optionsJson)
        return RadarNotificationOptions.fromJson(optionsObj)
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
        // Previous values of iconColor and iconString are preserved if new fields are null.
        val previousValue = getForegroundService(context)
        if (foregroundService.iconString == null) {
           foregroundService.iconString = previousValue.iconString 
        }
        if (foregroundService.iconColor == null) {
           foregroundService.iconColor = previousValue.iconColor 
        }
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
        Radar.setLogPersistenceFeatureFlag(featureSettings.useLogPersistence)
        val optionsJson = featureSettings.toJson().toString()

        getSharedPreferences(context).edit { putString(KEY_FEATURE_SETTINGS, optionsJson) }
    }

    fun getFeatureSettings(context: Context): RadarFeatureSettings {
        val sharedPrefFeatureSettings = getSharedPreferences(context).getString(KEY_FEATURE_SETTINGS, null)
        // The log buffer singleton is initialized before the logger, but requires calling this method
        // to obtain its feature flag. Thus we cannot call the logger yet as its not yet initialized.
        try {
            Radar.logger.d("getFeatureSettings | featureSettings = $sharedPrefFeatureSettings")
        } catch (e: Exception) {
            // Do nothing for now
        }
        val optionsJson = sharedPrefFeatureSettings ?: return RadarFeatureSettings.default()
        return RadarFeatureSettings.fromJson(JSONObject(optionsJson))
    }

    fun setSDKConfiguration(context: Context, configuration: RadarSDKConfiguration) {
        Radar.logger.d("set SDK Configuration | sdkConfiguration = $configuration")
        if (configuration.logLevel != null) {
            setLogLevel(context, configuration.logLevel);
        }
    }

    fun getSDKConfiguration(context: Context): RadarSDKConfiguration {
        val logLevelInt = getSharedPreferences(context).getInt(KEY_LOG_LEVEL, 3)
        return RadarSDKConfiguration(Radar.RadarLogLevel.fromInt(logLevelInt));
    }

    internal fun getLogLevel(context: Context): Radar.RadarLogLevel {
        val logLevelInt = getSharedPreferences(context).getInt(KEY_LOG_LEVEL, 3)
        val userDebug = getUserDebug(context)
        return if (userDebug) Radar.RadarLogLevel.DEBUG else Radar.RadarLogLevel.fromInt(logLevelInt)
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

    internal fun updateLastTrackedTime(context: Context) {
        val timestampSeconds = System.currentTimeMillis() / 1000
        getSharedPreferences(context).edit { putLong(KEY_LAST_TRACKED_TIME, timestampSeconds) }
    }

    internal fun getLastTrackedTime(context: Context): Long {
        return getSharedPreferences(context).getLong(KEY_LAST_TRACKED_TIME, 0)
    }

    internal fun getVerifiedHost(context: Context): String {
        return getSharedPreferences(context).getString(KEY_VERIFIED_HOST, null) ?: "https://api-verified.radar.io"
    }

    internal fun getUserDebug(context: Context): Boolean {
        return getSharedPreferences(context).getBoolean(KEY_USER_DEBUG, true)
    }

    internal fun setUserDebug(context: Context, userDebug: Boolean) {
        getSharedPreferences(context).edit { putBoolean(KEY_USER_DEBUG, userDebug) }
    }

    internal fun getLastAppOpenTimeMillis(context: Context): Long {
        return getSharedPreferences(context).getLong(KEY_LAST_APP_OPEN_TIME, 0)
    }

    internal fun updateLastAppOpenTimeMillis(context: Context) {
        val timestampSeconds = System.currentTimeMillis()
        getSharedPreferences(context).edit { putLong(KEY_LAST_APP_OPEN_TIME, timestampSeconds) }
    }

    internal fun getSharing(context: Context): Boolean {
        return getSharedPreferences(context).getBoolean(KEY_SHARING, false)
    }

    internal fun setSharing(context: Context, sharing: Boolean) {
        getSharedPreferences(context).edit { putBoolean(KEY_SHARING, sharing) }
    }

    internal fun isXPlatform(context: Context): Boolean {
        return getSharedPreferences(context).contains(KEY_X_PLATFORM_SDK_TYPE) &&
                getSharedPreferences(context).contains(KEY_X_PLATFORM_SDK_VERSION)
    }

    internal fun getXPlatformSDKType(context: Context): String? {
        return getSharedPreferences(context).getString(KEY_X_PLATFORM_SDK_TYPE, null);
    }

    internal fun getXPlatformSDKVersion(context: Context): String? {
        return getSharedPreferences(context).getString(KEY_X_PLATFORM_SDK_VERSION, null);
    }

}