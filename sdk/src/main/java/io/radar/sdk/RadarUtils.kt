package io.radar.sdk

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.edit
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import java.util.Calendar
import java.util.Date
import java.util.Locale

internal object RadarUtils {

    private const val KEY_AD_ID = "adId"

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences("RadarSDK", Context.MODE_PRIVATE)
    }

    internal val deviceModel = Build.MODEL

    internal val deviceOS = Build.VERSION.RELEASE

    internal val country: String
        get() = Locale.getDefault().country

    internal val sdkVersion: String = BuildConfig.VERSION_NAME

    internal val timeZoneOffset: Int
        get() {
            val timeZone = Calendar.getInstance().timeZone
            var offset = timeZone.rawOffset
            if (timeZone.inDaylightTime(Date())) {
                offset += timeZone.dstSavings
            }
            return offset / 1000
        }

    internal fun loadAdId(context: Context) {
        Thread {
            try {
                val advertisingIdInfo = AdvertisingIdClient.getAdvertisingIdInfo(context)
                val adId = if (advertisingIdInfo.isLimitAdTrackingEnabled) {
                    "OptedOut"
                } else {
                    advertisingIdInfo.id
                }
                getSharedPreferences(context).edit { putString(KEY_AD_ID, adId) }
            } catch (e: Exception) {

            }
        }.start()
    }

    internal fun getAdId(context: Context): String? {
        return getSharedPreferences(context).getString(KEY_AD_ID, null)
    }

    @SuppressLint("HardwareIds")
    internal fun getDeviceId(context: Context): String? {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    internal val deviceType = "Android"

    internal val deviceMake =  Build.MANUFACTURER

    internal fun getLocationEnabled(context: Context): Boolean {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER) || manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    internal fun valid(location: Location): Boolean {
        val latitudeValid = location.latitude != 0.0 && location.latitude > -90.0 && location.latitude < 90.0
        val longitudeValid = location.longitude != 0.0 && location.longitude > -180.0 && location.longitude < 180.0
        val accuracyValid = location.accuracy > 0f
        return latitudeValid && longitudeValid && accuracyValid
    }

    internal fun getUaChannelId(): String? {
        try {
            val urbanAirshipClass = Class.forName("com.urbanairship.UAirship")
            val sharedMethod = urbanAirshipClass.getMethod("shared")
            if (sharedMethod != null) {
                val sharedObj = sharedMethod.invoke(urbanAirshipClass)
                if (sharedObj != null && urbanAirshipClass.isInstance(sharedObj)) {
                    val getPushManagerMethod = urbanAirshipClass.getMethod("getPushManager")
                    if (getPushManagerMethod != null) {
                        val pushManagerClass = Class.forName("com.urbanairship.push.PushManager")
                        val pushManagerObj = getPushManagerMethod.invoke(sharedObj)
                        if (pushManagerObj != null && pushManagerClass.isInstance(pushManagerObj)) {
                            val getChannelIdMethod = pushManagerClass.getMethod("getChannelId")
                            if (getChannelIdMethod != null) {
                                val channelIdObj = getChannelIdMethod.invoke(pushManagerObj)
                                if (channelIdObj != null && channelIdObj is String) {
                                    return channelIdObj
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            return null
        }

        return null
    }

    internal fun getUaNamedUserId(): String? {
        try {
            val urbanAirshipClass = Class.forName("com.urbanairship.UAirship")
            val sharedMethod = urbanAirshipClass.getMethod("shared")
            if (sharedMethod != null) {
                val sharedObj = sharedMethod.invoke(urbanAirshipClass)
                if (sharedObj != null && urbanAirshipClass.isInstance(sharedObj)) {
                    val getNamedUser = urbanAirshipClass.getMethod("getNamedUser")
                    if (getNamedUser != null) {
                        val namedUserClass = Class.forName("com.urbanairship.push.NamedUser")
                        val namedUserObj = getNamedUser.invoke(sharedObj)
                        if (namedUserObj != null && namedUserClass.isInstance(namedUserObj)) {
                            val getIdMethod = namedUserClass.getMethod("getId")
                            if (getIdMethod != null) {
                                val idObj = getIdMethod.invoke(namedUserObj)
                                if (idObj != null && idObj is String) {
                                    return idObj
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            return null
        }

        return null
    }

    internal fun getUaSessionId(): String? {
        try {
            val urbanAirshipClass = Class.forName("com.urbanairship.UAirship")
            val sharedMethod = urbanAirshipClass.getMethod("shared")
            if (sharedMethod != null) {
                val sharedObj = sharedMethod.invoke(urbanAirshipClass)
                if (sharedObj != null && urbanAirshipClass.isInstance(sharedObj)) {
                    val getAnalytics = urbanAirshipClass.getMethod("getAnalytics")
                    if (getAnalytics != null) {
                        val analyticsClass = Class.forName("com.urbanairship.analytics.Analytics")
                        val analyticsObj = getAnalytics.invoke(sharedObj)
                        if (analyticsObj != null && analyticsClass.isInstance(analyticsObj)) {
                            val getSessionIdMethod = analyticsClass.getMethod("getSessionId")
                            if (getSessionIdMethod != null) {
                                val idObj = getSessionIdMethod.invoke(analyticsObj)
                                if (idObj != null && idObj is String) {
                                    return idObj
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            return null
        }

        return null
    }

}