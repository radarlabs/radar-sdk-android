package io.radar.sdk

import android.content.Context

internal class RadarPermissionsHelperMock : RadarPermissionsHelper() {

    internal var mockFineLocationPermissionGranted: Boolean = false

    override fun locationPermissionGranted(context: Context): Boolean {
        return mockFineLocationPermissionGranted
    }

}