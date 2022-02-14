package io.radar.sdk

import io.mockk.every
import io.mockk.spyk

internal class RadarPermissionsHelperMock {

    var mockFineLocationPermissionGranted: Boolean = false
    val helper = spyk(RadarPermissionsHelper())

    init {
        every { helper.fineLocationPermissionGranted(allAny()) } answers { mockFineLocationPermissionGranted }
    }

}