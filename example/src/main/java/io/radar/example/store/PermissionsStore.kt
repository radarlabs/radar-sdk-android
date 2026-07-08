package io.radar.example.store

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat

/**
 * Runtime-permission status snapshot for the settings sheet. Mirrors the iOS
 * `PermissionsStore` (minus the iOS-only pending-notification count).
 */
class PermissionsStore {

    var fineLocationGranted by mutableStateOf(false)
        private set
    var backgroundLocationGranted by mutableStateOf(false)
        private set
    var notificationsGranted by mutableStateOf(false)
        private set
    var activityRecognitionGranted by mutableStateOf(false)
        private set

    fun refresh(context: Context) {
        fineLocationGranted = granted(context, Manifest.permission.ACCESS_FINE_LOCATION)
        backgroundLocationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            granted(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            fineLocationGranted
        }
        notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            granted(context, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }
        activityRecognitionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            granted(context, Manifest.permission.ACTIVITY_RECOGNITION)
        } else {
            true
        }
    }

    private fun granted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    companion object {
        fun openAppSettings(context: Context) {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null),
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(intent)
        }
    }
}
