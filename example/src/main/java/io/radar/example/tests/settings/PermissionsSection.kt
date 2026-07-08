package io.radar.example.tests.settings

import android.Manifest
import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import io.radar.example.components.ControlRow
import io.radar.example.store.LocalPermissionsStore
import io.radar.example.store.PermissionsStore

@Composable
fun PermissionsSection() {
    val perms = LocalPermissionsStore.current
    val context = LocalContext.current

    LaunchedEffect(Unit) { perms.refresh(context) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Permissions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

        PermissionRow("Location (fine)", perms.fineLocationGranted) {
            requestPermission(context, Manifest.permission.ACCESS_FINE_LOCATION, 1)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            PermissionRow("Background location", perms.backgroundLocationGranted) {
                requestPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION, 2)
            }
            PermissionRow("Activity recognition", perms.activityRecognitionGranted) {
                requestPermission(context, Manifest.permission.ACTIVITY_RECOGNITION, 3)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionRow("Notifications", perms.notificationsGranted) {
                requestPermission(context, Manifest.permission.POST_NOTIFICATIONS, 4)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { perms.refresh(context) }) { Text("Refresh") }
            TextButton(onClick = { PermissionsStore.openAppSettings(context) }) { Text("App settings") }
        }
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, onRequest: () -> Unit) {
    ControlRow(
        label = label,
        value = if (granted) "Granted" else "Not granted",
        trailing = {
            if (!granted) {
                TextButton(onClick = onRequest) { Text("Request") }
            }
        },
    )
}

private fun requestPermission(context: Context, permission: String, code: Int) {
    (context as? Activity)?.let {
        ActivityCompat.requestPermissions(it, arrayOf(permission), code)
    }
}
