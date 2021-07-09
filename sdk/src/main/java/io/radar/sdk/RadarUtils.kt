package io.radar.sdk

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import java.util.*

internal object RadarUtils {

    private const val KEY_AD_ID = "adId"

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences("RadarSDK", Context.MODE_PRIVATE)
    }

    internal val deviceModel = Build.MODEL

    internal val deviceOS = Build.VERSION.RELEASE

    internal val country: String
        get() = Locale.getDefault().country

    internal const val sdkVersion: String = BuildConfig.VERSION_NAME

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

    internal const val deviceType = "Android"

    internal val deviceMake =  Build.MANUFACTURER

    internal fun getLocationAuthorization(context: Context): String {
        var locationAuthorization = "NOT_DETERMINED"
        if (RadarSettings.getPermissionsDenied(context)) {
            locationAuthorization = "DENIED"
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationAuthorization = "GRANTED_FOREGROUND"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) {
            locationAuthorization = "GRANTED_BACKGROUND"
        }
        return locationAuthorization
    }

    internal fun getBluetoothSupported(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
    }

    internal fun getLocationEnabled(context: Context): Boolean {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER) || manager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    internal fun valid(location: Location): Boolean {
        val latitudeValid = location.latitude != 0.0 && location.latitude > -90.0 && location.latitude < 90.0
        val longitudeValid = location.longitude != 0.0 && location.longitude > -180.0 && location.longitude < 180.0
        val accuracyValid = location.accuracy > 0f
        return latitudeValid && longitudeValid && accuracyValid
    }

    // based on https://github.com/flutter/plugins/tree/master/packages/device_info/device_info
    internal fun isEmulator(): Boolean {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator");
    }

}