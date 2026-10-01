package io.radar.sdk.helpers

import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import io.radar.sdk.RadarContinuousBeaconManager

/** A Bluetooth scanner for `RadarContinuousBeaconManager` that records scans instead of running them. */
internal class RadarFakeBeaconScanner : RadarContinuousBeaconManager.Scanner {
    var available = true
    var availabilityError: Exception? = null
    var starts = 0
    var stops = 0
    var active: ScanCallback? = null

    override fun isAvailable(): Boolean {
        availabilityError?.let { throw it }
        return available
    }

    override fun start(filters: List<ScanFilter>, callback: ScanCallback) {
        starts++
        active = callback
    }

    override fun stop(callback: ScanCallback) {
        stops++
        if (active === callback) {
            active = null
        }
    }
}
