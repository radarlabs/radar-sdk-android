package io.radar.sdk

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.SystemClock
import androidx.annotation.RequiresApi
import io.radar.sdk.model.RadarBeacon
import kotlin.collections.ArrayList

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
@SuppressLint("MissingPermission")
internal class RadarBeaconManager(
    private val context: Context,
    private val logger: RadarLogger,
    internal var permissionsHelper: RadarPermissionsHelper = RadarPermissionsHelper()
) {

    private val adapter = BluetoothAdapter.getDefaultAdapter()
    private var started = false
    private val callbacks = ArrayList<Radar.RadarBeaconCallback>()
    private var nearbyBeaconIdentifiers = mutableSetOf<String>()
    private var beacons = arrayOf<RadarBeacon>()
    private var scanCallback: ScanCallback? = null

    private fun addCallback(callback: Radar.RadarBeaconCallback?) {
        if (callback == null) {
            return
        }

        synchronized(callbacks) {
            callbacks.add(callback)
        }

        Handler().postAtTime({
            synchronized(callbacks) {
                this.stopRanging()
            }
        }, "timeout", SystemClock.uptimeMillis() + 5000L)
    }

    private fun callCallbacks(status: Radar.RadarStatus, nearbyBeacons: Array<String>? = null) {
        synchronized(callbacks) {
            if (callbacks.isEmpty()) {
                return
            }

            logger.d(this.context, "Calling callbacks | callbacks.size = ${callbacks.size}")

            for (callback in callbacks) {
                callback.onComplete(status, nearbyBeacons)
            }
            callbacks.clear()
        }
    }

    fun rangeBeacons(beacons: Array<RadarBeacon>, callback: Radar.RadarBeaconCallback?) {
        if (!permissionsHelper.bluetoothPermissionsGranted(context)) {
            val errorIntent = RadarReceiver.createErrorIntent(Radar.RadarStatus.ERROR_PERMISSIONS)
            Radar.broadcastIntent(errorIntent)

            callback?.onComplete(Radar.RadarStatus.ERROR_PERMISSIONS)

            return
        }

        if (!adapter.isEnabled) {
            val errorIntent = RadarReceiver.createErrorIntent(Radar.RadarStatus.ERROR_BLUETOOTH)
            Radar.broadcastIntent(errorIntent)

            callback?.onComplete(Radar.RadarStatus.ERROR_BLUETOOTH)

            return
        }

        this.addCallback(callback)

        if (this.started) {
            logger.d(this.context, "Already ranging beacons")

            return
        }

        if (beacons.isEmpty()) {
            logger.d(this.context, "No beacons to range")

            return
        }

        this.beacons = beacons
        this.started = true

        val scanFilters = mutableListOf<ScanFilter>()

        for (beacon in beacons) {
            logger.d(this.context, "Starting ranging beacon | _id = ${beacon._id}; uuid = ${beacon.uuid}; major = ${beacon.major}; minor = ${beacon.minor}")

            RadarBeaconUtils.getScanFilter(beacon)?.let { scanFilter -> scanFilters.add(scanFilter) }
        }

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()

        val beaconManager = this

        this.scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)

                beaconManager.handleScanResult(result)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                super.onBatchScanResults(results)

                results?.forEach { result -> beaconManager.handleScanResult(result) }
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)

                logger.d(beaconManager.context, "Scan failed")

                beaconManager.stopRanging()
            }
        }

        adapter.bluetoothLeScanner.startScan(scanFilters, scanSettings, scanCallback)
    }

    private fun stopRanging() {
        logger.d(this.context, "Stopping ranging")

        Handler().removeCallbacksAndMessages("timeout")

        adapter.bluetoothLeScanner.stopScan(scanCallback)
        scanCallback = null

        this.callCallbacks(Radar.RadarStatus.SUCCESS, this.nearbyBeaconIdentifiers.toTypedArray())

        this.beacons = arrayOf()
        this.started = false

        this.nearbyBeaconIdentifiers.clear()
    }

    private fun handleScanResult(result: ScanResult?) {
        logger.d(this.context, "Handling scan result")

        result?.scanRecord?.let { scanRecord -> RadarBeaconUtils.getBeacon(beacons, scanRecord) }?.let { beacon ->
            logger.d(this.context, "Ranged beacon | beacon._id = ${beacon._id}")

            nearbyBeaconIdentifiers.add(beacon._id)
        }

        if (this.nearbyBeaconIdentifiers.size == this.beacons.size) {
            logger.d(this.context, "Finished ranging")

            this.stopRanging()
        }
    }

}