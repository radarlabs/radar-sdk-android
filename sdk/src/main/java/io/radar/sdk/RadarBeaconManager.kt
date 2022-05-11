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
    private var monitoredBeaconIdentifiers = setOf<String>()
    private var nearbyBeacons = mutableSetOf<RadarBeacon>()
    private var beacons = arrayOf<RadarBeacon>()
    private var beaconUUIDs = arrayOf<String>()
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

    private fun callCallbacks(nearbyBeacons: Array<RadarBeacon>? = null) {
        synchronized(callbacks) {
            if (callbacks.isEmpty()) {
                return
            }

            logger.d("Calling callbacks | callbacks.size = ${callbacks.size}")

            for (callback in callbacks) {
                callback.onComplete(RadarStatus.SUCCESS, nearbyBeacons)
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

        val newBeaconIdentifiers = beacons.mapNotNull { it._id }.toSet()
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

        try {
            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
                .setReportDelay(30000)
                .setMatchMode(ScanSettings.MATCH_MODE_STICKY)
                .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                .build()

            logger.d("Starting monitoring beacons")

            adapter.bluetoothLeScanner.startScan(scanFilters, scanSettings, RadarLocationReceiver.getBeaconPendingIntent(context))
        } catch (e: SecurityException) {
            logger.e("Error starting monitoring beacons", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun startMonitoringBeaconUUIDs(beaconUUIDs: Array<String>) {
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

        val newBeaconIdentifiers = beaconUUIDs.toSet()
        if (monitoredBeaconIdentifiers == newBeaconIdentifiers) {
            logger.i("Already monitoring beacons")

            return
        }

        this.stopMonitoringBeacons()

        if (beacons.isEmpty()) {
            logger.d("No beacon UUIDs to monitor")

            return
        }

        monitoredBeaconIdentifiers = newBeaconIdentifiers

        val scanFilters = mutableListOf<ScanFilter>()

        for (beaconUUID in beaconUUIDs) {
            var scanFilter: ScanFilter? = null
            try {
                logger.d("Building scan filter for monitoring | beaconUUID = $beaconUUID")

                scanFilter = RadarBeaconUtils.getScanFilter(beaconUUID)
            } catch (e: Exception) {
                logger.d("Error building scan filter for monitoring | beaconUUID = $beaconUUID", e)
            }

            if (scanFilter != null) {
                logger.d("Starting monitoring beacon UUID | beaconUUID = $beaconUUID")

                scanFilters.add(scanFilter)
            }
        }

        if (scanFilters.size == 0) {
            logger.d("No scan filters for monitoring")

            return
        }

        try {
            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
                .setReportDelay(30000)
                .setMatchMode(ScanSettings.MATCH_MODE_STICKY)
                .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                .build()

            logger.d("Starting monitoring beacon UUIDs")

            adapter.bluetoothLeScanner.startScan(scanFilters, scanSettings, RadarLocationReceiver.getBeaconPendingIntent(context))
        } catch (e: SecurityException) {
            logger.e("Error starting monitoring beacon UUIDs", e)
        }
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

        monitoredBeaconIdentifiers = setOf()
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

    fun rangeBeaconUUIDs(beaconUUIDs: Array<String>, callback: RadarBeaconCallback?) {
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

        if (beaconUUIDs.isEmpty()) {
            logger.d("No UUIDs to range")

            callback?.onComplete(RadarStatus.SUCCESS)

            return
        }

        this.addCallback(callback)

        if (this.started) {
            logger.d("Already ranging beacons")

            return
        }

        this.beaconUUIDs = beaconUUIDs
        this.started = true

        val scanFilters = mutableListOf<ScanFilter>()

        for (beaconUUID in beaconUUIDs) {
            var scanFilter: ScanFilter? = null
            try {
                logger.d("Building scan filter for ranging | beaconUUID = $beaconUUID")

                scanFilter = RadarBeaconUtils.getScanFilter(beaconUUID)
            } catch (e: Exception) {
                logger.d("Error building scan filter for ranging | beaconUUID = $beaconUUID", e)
            }

            if (scanFilter != null) {
                logger.d("Starting ranging beacon UUID | uuid = $beaconUUID")

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

        this.callCallbacks(this.nearbyBeacons.toTypedArray())

        this.beacons = arrayOf()
        this.started = false

        this.nearbyBeacons.clear()
    }

    internal fun handleScanResults(scanResults: ArrayList<ScanResult>?) {
        if (scanResults == null || scanResults.isEmpty()) {
            logger.d("No scan results to handle")

            return
        }

        scanResults.forEach { scanResult ->
            this.handleScanResult(scanResult, false)
        }
    }

    internal fun handleScanResult(result: ScanResult?, ranging: Boolean = true) {
        logger.d("Handling scan result")

        result?.scanRecord?.let { scanRecord -> RadarBeaconUtils.getBeacon(result, scanRecord) }?.let { beacon ->
            logger.d("Ranged beacon | beacon.uuid = ${beacon.uuid}; beacon.major = ${beacon.major}; beacon.minor = ${beacon.minor}; beacon.rssi = ${beacon.rssi}")

            nearbyBeacons.add(beacon)
        }

        if (this.nearbyBeacons.size == this.beacons.size && ranging) {
            logger.d("Finished ranging")

            this.stopRanging()
        }
    }

}