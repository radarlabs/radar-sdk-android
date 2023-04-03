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
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.hardware.display.DisplayManagerCompat
import java.text.SimpleDateFormat
import java.util.*

internal object RadarUtils {

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
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
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

    internal fun getLocationAccuracyAuthorization(context: Context): String {
        val olderThanAndroidS = Build.VERSION.SDK_INT < Build.VERSION_CODES.S
        val fineLocationGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        return if (olderThanAndroidS || fineLocationGranted) {
            "FULL"
        } else {
            "REDUCED"
        }
    }

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

    internal fun isScreenSharing(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 17) {
            return false
        }
        val displayManager = DisplayManagerCompat.getInstance(context)
        val multipleDisplays = displayManager.displays.size > 1

        val sharing = RadarSettings.getSharing(context)

        return multipleDisplays || sharing
    }

    internal fun isoStringToDate(str: String?): Date? {
        if (str == null) {
            return null
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        return dateFormat.parse(str)
    }

    internal fun dateToISOString(date: Date?): String? {
        if (date == null) {
            return null
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        return dateFormat.format(date)
    }

}