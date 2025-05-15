package io.radar.sdk

import org.json.JSONObject
import java.util.Date

/**
 * An options class used to configure local notifications.
 *
 * @see [](https://radar.com/documentation/sdk/android)
 */
data class RadarNotificationOptions(
    /**
     * Determines the name of the asset to be used for notifications. Optional, defaults to app icon.  
     */
    val iconString: String? = null,

    /**
     * Determines the background color of used for notifications. Optional.
     */
    val iconColor: String? = null,

    /**
     * Determines the name of the asset to be used for forgroundService notifications. Optional, defaults to iconString.
     */
    val foregroundServiceIconString: String? = null,

    /**
     * Determines the name of the asset to be used for forgroundService notifications. Optional, defaults to iconString.
     */
    val foregroundServiceIconColor: String? = null,

    /**
     * Determines the name of the asset to be used for event notifications. Optional, defaults to iconString.
     */
    val eventIconString:String? = null,

    /**
     * Determines the name of the asset to be used for event notifications. Optional, defaults to iconString.
     */
    val eventIconColor: String? = null,

    /**
     * Determines the deep link to be used for event notifications. Optional.
     */
    val deepLink: String? = null,
) {

    companion object {

        internal const val KEY_ICON_STRING = "iconString"
        internal const val KEY_ICON_COLOR = "iconColor"
        internal const val KEY_FOREGROUNDSERVICE_ICON_STRING = "foregroundServiceIconString"
        internal const val KEY_FOREGROUNDSERVICE_ICON_COLOR = "foregroundServiceIconColor"
        internal const val KEY_EVENT_ICON_STRING = "eventIconString"
        internal const val KEY_EVENT_ICON_COLOR = "eventIconColor"
        internal const val KEY_DEEPLINK = "deepLink"

        @JvmStatic
        fun fromJson(obj: JSONObject): RadarNotificationOptions {
            val iconString = if (obj.isNull(KEY_ICON_STRING)) null else obj.optString(KEY_ICON_STRING)
            val iconColor = if (obj.isNull(KEY_ICON_COLOR)) null else obj.optString(KEY_ICON_COLOR)
            val foregroundServiceIconString = if (obj.isNull(KEY_FOREGROUNDSERVICE_ICON_STRING)) null else obj.optString(KEY_FOREGROUNDSERVICE_ICON_STRING)
            val foregroundServiceIconColor = if (obj.isNull(KEY_FOREGROUNDSERVICE_ICON_COLOR)) null else obj.optString(KEY_FOREGROUNDSERVICE_ICON_COLOR)
            val eventIconString = if (obj.isNull(KEY_EVENT_ICON_STRING)) null else obj.optString(KEY_EVENT_ICON_STRING)
            val eventIconColor = if (obj.isNull(KEY_EVENT_ICON_COLOR)) null else obj.optString(KEY_EVENT_ICON_COLOR)
            val deepLink = if (obj.isNull(KEY_DEEPLINK)) null else obj.optString(KEY_DEEPLINK)
            return RadarNotificationOptions(iconString, iconColor, foregroundServiceIconString, foregroundServiceIconColor, eventIconString, eventIconColor, deepLink)
        }
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put(KEY_ICON_STRING, iconString)
        obj.put(KEY_ICON_COLOR, iconColor)
        obj.put(KEY_FOREGROUNDSERVICE_ICON_STRING, foregroundServiceIconString)
        obj.put(KEY_FOREGROUNDSERVICE_ICON_COLOR, foregroundServiceIconColor)
        obj.put(KEY_EVENT_ICON_STRING, eventIconString)
        obj.put(KEY_EVENT_ICON_COLOR, eventIconColor)
        obj.put(KEY_DEEPLINK, deepLink)
        return obj
    }

    fun getForegroundServiceIcon(): String? {
        return foregroundServiceIconString ?: iconString
    }

    fun getForegroundServiceColor(): String? {
        return foregroundServiceIconColor ?: iconColor
    }

    fun getEventIcon(): String? {
        return eventIconString ?: iconString
    }

    fun getEventColor(): String? {
        return eventIconColor ?: iconColor
    }

}
