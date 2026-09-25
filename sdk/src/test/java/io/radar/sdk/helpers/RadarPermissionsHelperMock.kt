package io.radar.sdk.helpers

import android.content.Context
import io.radar.sdk.RadarPermissionsHelper

internal class RadarPermissionsHelperMock : RadarPermissionsHelper() {

    internal var mockFineLocationPermissionGranted: Boolean = false

    override fun fineLocationPermissionGranted(context: Context): Boolean = mockFineLocationPermissionGranted
}
