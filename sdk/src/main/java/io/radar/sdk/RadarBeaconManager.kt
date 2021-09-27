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
    private val context: Context,
    private val logger: RadarLogger,
    @SuppressLint("VisibleForTests")
    internal var permissionsHelper: RadarPermissionsHelper = RadarPermissionsHelper()
) {

    private lateinit var adapter: BluetoothAdapter
    private var started = false
    private val callbacks = Collections.synchronizedList(mutableListOf<RadarBeaconCallback>())
    private var nearbyBeaconIdentifiers = mutableSetOf<String>()
    private var nearbyBeaconRSSI = mutableMapOf<String, Int>()
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

    private fun callCallbacks(nearbyBeacons: Array<String>? = null, nearbyBeaconRSSI: Map<String, Int>? = null) {
        synchronized(callbacks) {
            if (callbacks.isEmpty()) {
                return
            }

            logger.d("Calling callbacks | callbacks.size = ${callbacks.size}")

            for (callback in callbacks) {
                callback.onComplete(RadarStatus.SUCCESS, nearbyBeacons, nearbyBeaconRSSI)
            }
            callbacks.clear()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun startMonitoringBeacons(beacons: Array<RadarBeacon>) {
        if (!permissionsHelper.bluetoothPermissionsGranted(context)) {
            logger.d("Bluetooth permissions not granted")

            return
        }

        if (!RadarUtils.getBluetoothSupported(context)) {
            logger.d("Bluetooth not supported")

            return
        }

        if (!this::adapter.isInitialized) {
            adapter = BluetoothAdapter.getDefaultAdapter()
        }

        if (!adapter.isEnabled) {
            logger.d("Bluetooth not enabled")

            return
        }

        val newBeaconIdentifiers = beacons.map { it._id }.toSet()
        if (monitoredBeaconIdentifiers == newBeaconIdentifiers) {
            logger.i("Already monitoring beacons")

            return
        }

        this.stopMonitoringBeacons()

        if (beacons.isEmpty()) {
            logger.d("No beacons to monitor")

            return
        }

        monitoredBeaconIdentifiers = newBeaconIdentifiers

        val scanFilters = mutableListOf<ScanFilter>()

        for (beacon in beacons) {
            var scanFilter: ScanFilter? = null
            try {
                logger.d("Building scan filter for monitoring | _id = ${beacon._id}")

                scanFilter = RadarBeaconUtils.getScanFilter(beacon)
            } catch (e: Exception) {
                logger.d("Error building scan filter for monitoring | _id = ${beacon._id}", e)
            }

            if (scanFilter != null) {
                logger.d("Starting monitoring beacon | _id = ${beacon._id}; uuid = ${beacon.uuid}; major = ${beacon.major}; minor = ${beacon.minor}")

                scanFilters.add(scanFilter)
            }
        }

        if (scanFilters.size == 0) {
            logger.d("No scan filters for monitoring")

            return
        }

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
            .setReportDelay(30000)
            .setMatchMode(ScanSettings.MATCH_MODE_STICKY)
            .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
            .build()

        logger.d("Starting monitoring beacons")

        adapter.bluetoothLeScanner.startScan(scanFilters, scanSettings, RadarLocationReceiver.getBeaconPendingIntent(context))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun stopMonitoringBeacons() {
        if (!permissionsHelper.bluetoothPermissionsGranted(context)) {
            return
        }

        if (!RadarUtils.getBluetoothSupported(context)) {
            return
        }

        if (!this::adapter.isInitialized) {
            adapter = BluetoothAdapter.getDefaultAdapter()
        }

        if (!adapter.isEnabled) {
            logger.d("Bluetooth not enabled")

            return
        }

        logger.d("Stopping monitoring beacons")

        adapter.bluetoothLeScanner.stopScan(RadarLocationReceiver.getBeaconPendingIntent(context))
    }

    fun rangeBeacons(beacons: Array<RadarBeacon>, callback: RadarBeaconCallback?) {
        if (!permissionsHelper.bluetoothPermissionsGranted(context)) {
            logger.d("Bluetooth permissions not granted")

            Radar.sendError(RadarStatus.ERROR_PERMISSIONS)

            callback?.onComplete(RadarStatus.ERROR_PERMISSIONS)

            return
        }

        if (!RadarUtils.getBluetoothSupported(context)) {
            logger.d("Bluetooth not supported")

            Radar.sendError(RadarStatus.ERROR_BLUETOOTH)

            callback?.onComplete(RadarStatus.ERROR_BLUETOOTH)

            return
        }

        if (!this::adapter.isInitialized) {
            adapter = BluetoothAdapter.getDefaultAdapter()
        }

        if (!adapter.isEnabled) {
            logger.d("Bluetooth not enabled")

            Radar.sendError(RadarStatus.ERROR_BLUETOOTH)

            callback?.onComplete(RadarStatus.ERROR_BLUETOOTH)

            return
        }

        if (beacons.isEmpty()) {
            logger.d("No beacons to range")

            callback?.onComplete(RadarStatus.SUCCESS)

            return
        }

        this.addCallback(callback)

        if (this.started) {
            logger.d("Already ranging beacons")

            return
        }

        this.beacons = beacons
        this.started = true

        val scanFilters = mutableListOf<ScanFilter>()

        for (beacon in beacons) {
            var scanFilter: ScanFilter? = null
            try {
                logger.d("Building scan filter for ranging | _id = ${beacon._id}")

                scanFilter = RadarBeaconUtils.getScanFilter(beacon)
            } catch (e: Exception) {
                logger.d("Error building scan filter for ranging | _id = ${beacon._id}", e)
            }

            if (scanFilter != null) {
                logger.d("Starting ranging beacon | _id = ${beacon._id}; uuid = ${beacon.uuid}; major = ${beacon.major}; minor = ${beacon.minor}")

                scanFilters.add(scanFilter)
            }
        }

        if (scanFilters.size == 0) {
            logger.d("No scan filters for ranging")

            this.callCallbacks()

            return
        }

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
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

                logger.d("Scan failed")

                beaconManager.stopRanging()
            }
        }

        adapter.bluetoothLeScanner.startScan(scanFilters, scanSettings, scanCallback)

        handler.postAtTime({
            logger.d("Beacon ranging timeout")

            this.stopRanging()
        }, TIMEOUT_TOKEN, SystemClock.uptimeMillis() + 5000L)
    }

    private fun stopRanging() {
        if (!permissionsHelper.bluetoothPermissionsGranted(context)) {
            return
        }

        if (!RadarUtils.getBluetoothSupported(context)) {
            return
        }

        logger.d("Stopping ranging")

        handler.removeCallbacksAndMessages(TIMEOUT_TOKEN)

        adapter.bluetoothLeScanner.stopScan(scanCallback)
        scanCallback = null

        this.callCallbacks(this.nearbyBeaconIdentifiers.toTypedArray(), this.nearbyBeaconRSSI)

        this.beacons = arrayOf()
        this.started = false

        this.nearbyBeaconIdentifiers.clear()
        this.nearbyBeaconRSSI.clear()
    }

    private fun handleScanResult(result: ScanResult?) {
        logger.d("Handling scan result")

        result?.scanRecord?.let { scanRecord -> RadarBeaconUtils.getBeacon(beacons, scanRecord) }?.let { beacon ->
            logger.d("Ranged beacon | beacon._id = ${beacon._id}")

            nearbyBeaconIdentifiers.add(beacon._id)
            nearbyBeaconRSSI.put(beacon._id, result.rssi)
        }

        if (this.nearbyBeaconIdentifiers.size == this.beacons.size) {
            logger.d("Finished ranging")

            this.stopRanging()
        }
    }

}