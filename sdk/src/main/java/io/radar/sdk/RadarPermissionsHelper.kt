package io.radar.sdk

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

internal class RadarPermissionsHelper {

    companion object {
        fun isPermissionGranted(context: Context, vararg permissions: String): Boolean {
            return permissions.all { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    internal fun fineLocationPermissionGranted(context: Context): Boolean {
        return isPermissionGranted(context, Manifest.permission.ACCESS_FINE_LOCATION)
    }

    internal fun coarseLocationPermissionGranted(context: Context): Boolean {
        return isPermissionGranted(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    internal fun bluetoothPermissionsGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            isPermissionGranted(context, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            isPermissionGranted(
                context,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

}