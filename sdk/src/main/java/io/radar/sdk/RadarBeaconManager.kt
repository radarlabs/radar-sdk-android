package io.radar.sdk

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.RequiresApi
import io.radar.sdk.Radar.RadarBeaconCallback
import io.radar.sdk.Radar.RadarStatus
import io.radar.sdk.model.RadarBeacon
import java.util.*

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
@SuppressLint("MissingPermission")
internal class RadarBeaconManager(
    private val app: RadarApplication,
    private val permissionsHelper: RadarPermissionsHelper = RadarPermissionsHelper()
) {

    private lateinit var adapter: BluetoothAdapter
    private var started = false
    private val callbacks = Collections.synchronizedList(mutableListOf<RadarBeaconCallback>())
    private var nearbyBeaconIdentifiers = mutableSetOf<String>()
    private var monitoredBeaconIdentifiers = setOf<String>()
    private var beacons = arrayOf<RadarBeacon>()
    private var scanCallback: ScanCallback? = null
    private val handler = Handler(Looper.getMainLooper())

    internal companion object {
        private const val TIMEOUT_TOKEN = "timeout"
    }

    private fun addCallback(callback: RadarBeaconCallback?) {
        if (callback == null) {
            return
        }

        synchronized(callbacks) {
            callbacks.add(callback)
        }
    }

    private fun callCallbacks(nearbyBeacons: Array<String>? = null) {
        synchronized(callbacks) {
            if (callbacks.isEmpty()) {
                return
            }

            app.logger.d("Calling callbacks", "callbacks.size" to callbacks.size)

            for (callback in callbacks) {
                callback.onComplete(RadarStatus.SUCCESS, nearbyBeacons)
            }
            callbacks.clear()
        }
    }

    @Suppress("ReturnCount", "LongMethod")
    @RequiresApi(Build.VERSION_CODES.O)
    fun startMonitoringBeacons(beacons: Array<RadarBeacon>) {
        if (!permissionsHelper.bluetoothPermissionsGranted(app)) {
            app.logger.d("Bluetooth permissions not granted")

            return
        }

        if (!RadarUtils.getBluetoothSupported(app)) {
            app.logger.d("Bluetooth not supported")
            return
        }

        if (!this::adapter.isInitialized) {
            adapter = BluetoothAdapter.getDefaultAdapter()
        }

        if (!adapter.isEnabled) {
            app.logger.d("Bluetooth not enabled")
            return
        }

        val newBeaconIdentifiers = beacons.map { it._id }.toSet()
        if (monitoredBeaconIdentifiers == newBeaconIdentifiers) {
            app.logger.i("Already monitoring beacons")
            return
        }

        this.stopMonitoringBeacons()

        if (beacons.isEmpty()) {
            app.logger.d("No beacons to monitor")
            return
        }

        monitoredBeaconIdentifiers = newBeaconIdentifiers

        val scanFilters = mutableListOf<ScanFilter>()

        for (beacon in beacons) {
            var scanFilter: ScanFilter? = null
            try {
                app.logger.d("Building scan filter for monitoring", "beacon._id" to beacon._id)
                scanFilter = RadarBeaconUtils.getScanFilter(beacon)
            } catch (e: Exception) {
                app.logger.d("Error building scan filter for monitoring", "beacon._id" to beacon._id, e)
            }

            if (scanFilter != null) {
                app.logger.d(
                    "Starting monitoring beacon", mapOf(
                        "_id" to beacon._id,
                        "uuid" to beacon.uuid,
                        "major" to beacon.major,
                        "minor" to beacon.minor
                    )
                )
                scanFilters.add(scanFilter)
            }
        }

        if (scanFilters.size == 0) {
            app.logger.d("No scan filters for monitoring")
            return
        }

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
            .setReportDelay(30000)
            .setMatchMode(ScanSettings.MATCH_MODE_STICKY)
            .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
            .build()

        app.logger.d("Starting monitoring beacons")

        adapter.bluetoothLeScanner.startScan(scanFilters, scanSettings,
            RadarLocationReceiver.getBeaconPendingIntent(app))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun stopMonitoringBeacons() {
        if (!permissionsHelper.bluetoothPermissionsGranted(app) || !RadarUtils.getBluetoothSupported(app)) {
            return
        }

        if (!this::adapter.isInitialized) {
            adapter = BluetoothAdapter.getDefaultAdapter()
        }

        if (adapter.isEnabled) {
            app.logger.d("Stopping monitoring beacons")
            adapter.bluetoothLeScanner.stopScan(RadarLocationReceiver.getBeaconPendingIntent(app))
        } else {
            app.logger.d("Bluetooth not enabled")
        }
    }

    @Suppress("ReturnCount")
    fun rangeBeacons(beacons: Array<RadarBeacon>, callback: RadarBeaconCallback?) {
        if (!permissionsHelper.bluetoothPermissionsGranted(app)) {
            app.logger.d("Bluetooth permissions not granted")
            app.sendError(RadarStatus.ERROR_PERMISSIONS)
            callback?.onComplete(RadarStatus.ERROR_PERMISSIONS)
            return
        }
        if (!RadarUtils.getBluetoothSupported(app)) {
            app.logger.d("Bluetooth not supported")
            app.sendError(RadarStatus.ERROR_BLUETOOTH)
            callback?.onComplete(RadarStatus.ERROR_BLUETOOTH)
            return
        }
        if (!this::adapter.isInitialized) {
            adapter = BluetoothAdapter.getDefaultAdapter()
        }
        if (!adapter.isEnabled) {
            app.logger.d("Bluetooth not enabled")
            app.sendError(RadarStatus.ERROR_BLUETOOTH)
            callback?.onComplete(RadarStatus.ERROR_BLUETOOTH)
            return
        }
        if (beacons.isEmpty()) {
            app.logger.d("No beacons to range")
            callback?.onComplete(RadarStatus.SUCCESS)
            return
        }

        this.addCallback(callback)
        if (this.started) {
            app.logger.d("Already ranging beacons")
            return
        }
        this.beacons = beacons
        this.started = true

        val scanFilters = getScanFilters(beacons)
        if (scanFilters.isEmpty()) {
            app.logger.d("No scan filters for ranging")
            this.callCallbacks()
            return
        }
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .setReportDelay(0)
            .build()

        this.scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)
                this@RadarBeaconManager.handleScanResult(result)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                super.onBatchScanResults(results)
                results?.forEach { result -> this@RadarBeaconManager.handleScanResult(result) }
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                app.logger.d("Scan failed")
                this@RadarBeaconManager.stopRanging()
            }
        }

        adapter.bluetoothLeScanner.startScan(scanFilters, scanSettings, scanCallback)

        handler.postAtTime({
            app.logger.d("Beacon ranging timeout")
            this.stopRanging()
        }, TIMEOUT_TOKEN, SystemClock.uptimeMillis() + 5000L)
    }

    private fun getScanFilters(beacons: Array<RadarBeacon>): List<ScanFilter> {
        val scanFilters = mutableListOf<ScanFilter>()
        for (beacon in beacons) {
            var scanFilter: ScanFilter? = null
            try {
                app.logger.d("Building scan filter for ranging", "_id" to beacon._id)
                scanFilter = RadarBeaconUtils.getScanFilter(beacon)
            } catch (e: Exception) {
                app.logger.d("Error building scan filter for ranging", "_id" to beacon._id, e)
            }

            if (scanFilter != null) {
                app.logger.d(
                    "Starting ranging beacon", mapOf(
                        "_id" to beacon._id,
                        "uuid" to beacon.uuid,
                        "major" to beacon.major,
                        "minor" to beacon.minor
                    )
                )
                scanFilters.add(scanFilter)
            }
        }
        return scanFilters
    }

    private fun stopRanging() {
        if (!permissionsHelper.bluetoothPermissionsGranted(app)) {
            return
        }

        if (!RadarUtils.getBluetoothSupported(app)) {
            return
        }

        app.logger.d("Stopping ranging")

        handler.removeCallbacksAndMessages(TIMEOUT_TOKEN)

        adapter.bluetoothLeScanner.stopScan(scanCallback)
        scanCallback = null

        this.callCallbacks(this.nearbyBeaconIdentifiers.toTypedArray())

        this.beacons = arrayOf()
        this.started = false

        this.nearbyBeaconIdentifiers.clear()
    }

    private fun handleScanResult(result: ScanResult?) {
        app.logger.d("Handling scan result")

        result?.scanRecord?.let { scanRecord -> RadarBeaconUtils.getBeacon(beacons, scanRecord) }?.let { beacon ->
            app.logger.d("Ranged beacon", "beacon._id" to beacon._id)

            nearbyBeaconIdentifiers.add(beacon._id)
        }

        if (this.nearbyBeaconIdentifiers.size == this.beacons.size) {
            app.logger.d("Finished ranging")

            this.stopRanging()
        }
    }

}