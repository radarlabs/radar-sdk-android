package io.radar.sdk.helpers

import android.content.Context
import io.radar.sdk.RadarPermissionsHelper

internal class RadarPermissionsHelperMock : RadarPermissionsHelper() {

    internal var mockFineLocationPermissionGranted: Boolean = false

    // When `null`, checks the real permissions.
    internal var mockBluetoothPermissionsGranted: Boolean? = null

    override fun fineLocationPermissionGranted(context: Context): Boolean = mockFineLocationPermissionGranted

    override fun bluetoothPermissionsGranted(context: Context): Boolean = mockBluetoothPermissionsGranted ?: super.bluetoothPermissionsGranted(context)
}
