package io.radar.sdk

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.RequiresApi
import io.radar.sdk.Radar.RadarBeaconCallback
import io.radar.sdk.Radar.RadarLogType
import io.radar.sdk.Radar.RadarStatus
import io.radar.sdk.model.RadarBeacon
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
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
    private var beaconUIDs = arrayOf<String>()
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

    fun startMonitoringBeacons(beacons: Array<RadarBeacon>) {
        if (RadarSettings.getFeatureSettings(context).useRadarModifiedBeacon) {
            return
        }

        if (!permissionsHelper.bluetoothPermissionsGranted(context)) {
            logger.d("Bluetooth permissions not granted")

            return
        }

        if (!isBluetoothSupported(context)) {
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
            logger.d("Already monitoring beacons")

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

                scanFilter = RadarBeaconUtils.getScanFilterForBeacon(beacon)
            } catch (e: Exception) {
                logger.d("Error building scan filter for monitoring | _id = ${beacon._id}", RadarLogType.SDK_EXCEPTION, e)
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
            val scanSettings = getScanSettings(ScanSettings.SCAN_MODE_LOW_POWER)

            logger.d("Starting monitoring beacons")

            adapter.bluetoothLeScanner.startScan(scanFilters, scanSettings, RadarLocationReceiver.getBeaconPendingIntent(context))
        } catch (e: Exception) {
            logger.e("Error starting monitoring beacons", RadarLogType.SDK_EXCEPTION, e)
        }
    }

    fun startMonitoringBeaconUUIDs(beaconUUIDs: Array<String>?, beaconUIDs: Array<String>?) {
        if (RadarSettings.getFeatureSettings(context).useRadarModifiedBeacon) {
            return
        }

        if (!permissionsHelper.bluetoothPermissionsGranted(context)) {
            logger.d("Bluetooth permissions not granted")

            return
        }

        if (!isBluetoothSupported(context)) {
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

        val newBeaconIdentifiers = mutableSetOf<String>()
        if (beaconUUIDs != null) {
            newBeaconIdentifiers.addAll(beaconUUIDs)
        }
        if (beaconUIDs != null) {
            newBeaconIdentifiers.addAll(beaconUIDs)
        }
        if (monitoredBeaconIdentifiers == newBeaconIdentifiers) {
            logger.d("Already monitoring beacons")

            return
        }

        this.stopMonitoringBeacons()

        if (beaconUUIDs.isNullOrEmpty() && beaconUIDs.isNullOrEmpty()) {
            logger.d("No beacon UUIDs or UIDs to monitor")

            return
        }

        monitoredBeaconIdentifiers = newBeaconIdentifiers

        val scanFilters = mutableListOf<ScanFilter>()

        if (beaconUUIDs != null) {
            for (beaconUUID in beaconUUIDs) {
                var scanFilter: ScanFilter? = null
                try {
                    logger.d("Building scan filter for monitoring | beaconUUID = $beaconUUID")

                    scanFilter = RadarBeaconUtils.getScanFilterForBeacon(beaconUUID)
                } catch (e: Exception) {
                    logger.d("Error building scan filter for monitoring | beaconUUID = $beaconUUID", RadarLogType.SDK_EXCEPTION, e)
                }

                if (scanFilter != null) {
                    logger.d("Starting monitoring beacon UUID | beaconUUID = $beaconUUID")

                    scanFilters.add(scanFilter)
                }
            }
        }

        if (beaconUIDs != null) {
            for (beaconUID in beaconUIDs) {
                var scanFilter: ScanFilter? = null
                try {
                    logger.d("Building scan filter for monitoring | beaconUID = $beaconUID")

                    scanFilter = RadarBeaconUtils.getScanFilterForBeaconUID(beaconUID)
                } catch (e: Exception) {
                    logger.d("Error building scan filter for monitoring | beaconUID = $beaconUID", RadarLogType.SDK_EXCEPTION, e)
                }

                if (scanFilter != null) {
                    logger.d("Starting monitoring beacon UID | beaconUID = $beaconUID")

                    scanFilters.add(scanFilter)
                }
            }
        }

        if (scanFilters.size == 0) {
            logger.d("No scan filters for monitoring")

            return
        }

        try {
            val scanSettings = getScanSettings(ScanSettings.SCAN_MODE_LOW_POWER)

            logger.d("Starting monitoring beacon UUIDs")

            adapter.bluetoothLeScanner.startScan(scanFilters, scanSettings, RadarLocationReceiver.getBeaconPendingIntent(context))
        } catch (e: Exception) {
            logger.e("Error starting monitoring beacon UUIDs", RadarLogType.SDK_EXCEPTION, e)
        }
    }

    fun stopMonitoringBeacons() {
        if (RadarSettings.getFeatureSettings(context).useRadarModifiedBeacon) {
            return
        }

        if (!permissionsHelper.bluetoothPermissionsGranted(context)) {
            return
        }

        if (!isBluetoothSupported(context)) {
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

        try {
            adapter.bluetoothLeScanner.stopScan(RadarLocationReceiver.getBeaconPendingIntent(context))
        } catch (e: Exception) {
            logger.d("Error stopping monitoring beacons", RadarLogType.SDK_EXCEPTION, e)
        }

        monitoredBeaconIdentifiers = setOf()
    }

    fun rangeBeacons(beacons: Array<RadarBeacon>, background: Boolean, callback: RadarBeaconCallback?) {
        if (!permissionsHelper.bluetoothPermissionsGranted(context)) {
            logger.d("Bluetooth permissions not granted")

            Radar.sendError(RadarStatus.ERROR_PERMISSIONS)

            callback?.onComplete(RadarStatus.ERROR_PERMISSIONS)

            return
        }

        if (!isBluetoothSupported(context)) {
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

            callback?.onComplete(RadarStatus.SUCCESS, emptyArray())

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

                scanFilter = RadarBeaconUtils.getScanFilterForBeacon(beacon)
            } catch (e: Exception) {
                logger.d("Error building scan filter for ranging | _id = ${beacon._id}", RadarLogType.SDK_EXCEPTION, e)
            }

            if (scanFilter != null) {
                logger.d("Starting ranging beacon | type = ${beacon.type}; _id = ${beacon._id}; uuid = ${beacon.uuid}; major = ${beacon.major}; minor = ${beacon.minor}")

                scanFilters.add(scanFilter)
            }
        }

        if (scanFilters.size == 0) {
            logger.d("No scan filters for ranging")

            this.callCallbacks()

            return
        }

        val scanMode = if (background) ScanSettings.SCAN_MODE_LOW_POWER else ScanSettings.SCAN_MODE_LOW_LATENCY
        val scanSettings = getScanSettings(scanMode)

        val beaconManager = this

        this.scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)

                beaconManager.handleScanResult(callbackType, result)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                super.onBatchScanResults(results)

                results?.forEach { result -> beaconManager.handleScanResult(ScanSettings.CALLBACK_TYPE_FIRST_MATCH, result) }
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)

                logger.d("Scan failed")

                beaconManager.stopRanging()
            }
        }

        try {
            adapter.bluetoothLeScanner.startScan(scanFilters, scanSettings, scanCallback)
        } catch (e: Exception) {
            logger.e("Error starting ranging beacons", RadarLogType.SDK_EXCEPTION, e)
        }

        handler.postAtTime({
            logger.d("Beacon ranging timeout")

            this.stopRanging()
        }, TIMEOUT_TOKEN, SystemClock.uptimeMillis() + 5000L)
    }

    fun rangeBeaconUUIDs(beaconUUIDs: Array<String>?, beaconUIDs: Array<String>?, background: Boolean, callback: RadarBeaconCallback?) {
        if (!permissionsHelper.bluetoothPermissionsGranted(context)) {
            logger.d("Bluetooth permissions not granted")

            Radar.sendError(RadarStatus.ERROR_PERMISSIONS)

            callback?.onComplete(RadarStatus.ERROR_PERMISSIONS)

            return
        }

        if (!isBluetoothSupported(context)) {
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

        if (beaconUUIDs.isNullOrEmpty() && beaconUIDs.isNullOrEmpty()) {
            logger.d("No beacon UUIDs or UIDs to range")

            callback?.onComplete(RadarStatus.SUCCESS, emptyArray())

            return
        }

        this.addCallback(callback)

        if (this.started) {
            logger.d("Already ranging beacons")

            return
        }

        this.beaconUUIDs = beaconUUIDs ?: arrayOf()
        this.beaconUIDs = beaconUIDs ?: arrayOf()
        this.started = true

        val scanFilters = mutableListOf<ScanFilter>()

        if (beaconUUIDs != null) {
            for (beaconUUID in beaconUUIDs) {
                var scanFilter: ScanFilter? = null
                try {
                    logger.d("Building scan filter for ranging | beaconUUID = $beaconUUID")

                    scanFilter = RadarBeaconUtils.getScanFilterForBeacon(beaconUUID)
                } catch (e: Exception) {
                    logger.d("Error building scan filter for ranging | beaconUUID = $beaconUUID", RadarLogType.SDK_EXCEPTION, e)
                }

                if (scanFilter != null) {
                    logger.d("Starting ranging beacon UUID | beaconUUID = $beaconUUID")

                    scanFilters.add(scanFilter)
                }
            }
        }

        if (beaconUIDs != null) {
            for (beaconUID in beaconUIDs) {
                var scanFilter: ScanFilter? = null
                try {
                    logger.d("Building scan filter for ranging | beaconUID = $beaconUID")

                    scanFilter = RadarBeaconUtils.getScanFilterForBeaconUID(beaconUID)
                } catch (e: Exception) {
                    logger.d("Error building scan filter for ranging | beaconUID = $beaconUID", RadarLogType.SDK_EXCEPTION, e)
                }

                if (scanFilter != null) {
                    logger.d("Starting ranging beacon UID | beaconUID = $beaconUID")

                    scanFilters.add(scanFilter)
                }
            }
        }

        if (scanFilters.size == 0) {
            logger.d("No scan filters for ranging")

            this.callCallbacks()

            return
        }

        val scanMode = if (background) ScanSettings.SCAN_MODE_LOW_POWER else ScanSettings.SCAN_MODE_LOW_LATENCY
        val scanSettings = getScanSettings(scanMode)

        val beaconManager = this

        this.scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)

                beaconManager.handleScanResult(callbackType, result)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                super.onBatchScanResults(results)

                results?.forEach { result -> beaconManager.handleScanResult(ScanSettings.CALLBACK_TYPE_FIRST_MATCH, result) }
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)

                logger.d("Scan failed")

                beaconManager.stopRanging()
            }
        }

        try {
            adapter.bluetoothLeScanner.startScan(scanFilters, scanSettings, scanCallback)
        } catch (e: Exception) {
            logger.e("Error starting ranging beacon UUIDs", RadarLogType.SDK_EXCEPTION, e)
        }

        handler.postAtTime({
            logger.d("Beacon ranging timeout")

            this.stopRanging()
        }, TIMEOUT_TOKEN, SystemClock.uptimeMillis() + 5000L)
    }

    private fun stopRanging() {
        if (!permissionsHelper.bluetoothPermissionsGranted(context)) {
            return
        }

        if (!isBluetoothSupported(context)) {
            return
        }

        if (!this::adapter.isInitialized) {
            adapter = BluetoothAdapter.getDefaultAdapter()
        }

        logger.d("Stopping ranging")

        handler.removeCallbacksAndMessages(TIMEOUT_TOKEN)

        try {
            adapter.bluetoothLeScanner.stopScan(scanCallback)
        } catch (e: Exception) {
            logger.d("Error stopping ranging beacons", RadarLogType.SDK_EXCEPTION, e)
        }
        
        scanCallback = null

        this.callCallbacks(this.nearbyBeacons.toTypedArray())

        this.beacons = arrayOf()
        this.started = false

        this.nearbyBeacons.clear()
    }

    internal fun handleBeacons(beacons: Array<RadarBeacon>?, source: Radar.RadarLocationSource) {
        if (beacons.isNullOrEmpty()) {
            logger.d("No beacons to handle")

            return
        }

        beacons.forEach { beacon ->
            if (source == Radar.RadarLocationSource.BEACON_EXIT) {
                logger.d("Handling beacon exit | beacon.type = ${beacon.type}; beacon.uuid = ${beacon.uuid}; beacon.major = ${beacon.major}; beacon.minor = ${beacon.minor}; beacon.rssi = ${beacon.rssi}")

                nearbyBeacons.remove(beacon)
            } else {
                logger.d("Handling beacon entry | beacon.type = ${beacon.type}; beacon.uuid = ${beacon.uuid}; beacon.major = ${beacon.major}; beacon.minor = ${beacon.minor}; beacon.rssi = ${beacon.rssi}")

                nearbyBeacons.add(beacon)
            }
        }
    }

    internal fun handleScanResult(callbackType: Int, result: ScanResult?, ranging: Boolean = true) {
        logger.d("Handling scan result")

        try {
            result?.scanRecord?.let { scanRecord -> RadarBeaconUtils.getBeacon(result, scanRecord) }?.let { beacon ->
                logger.d("Ranged beacon | beacon.type = ${beacon.type}; beacon.uuid = ${beacon.uuid}; beacon.major = ${beacon.major}; beacon.minor = ${beacon.minor}; beacon.rssi = ${beacon.rssi}")

                if (callbackType == ScanSettings.CALLBACK_TYPE_MATCH_LOST) {
                    logger.d("Handling beacon exit | beacon.type = ${beacon.type}; beacon.uuid = ${beacon.uuid}; beacon.major = ${beacon.major}; beacon.minor = ${beacon.minor}; beacon.rssi = ${beacon.rssi}")

                    nearbyBeacons.remove(beacon)
                } else {
                    logger.d("Handling beacon entry | beacon.type = ${beacon.type}; beacon.uuid = ${beacon.uuid}; beacon.major = ${beacon.major}; beacon.minor = ${beacon.minor}; beacon.rssi = ${beacon.rssi}")

                    nearbyBeacons.add(beacon)
                }
            }
        } catch (e: Exception) {
            logger.e("Error handling scan result", RadarLogType.SDK_EXCEPTION, e)
        }

        if (this.nearbyBeacons.size == this.beacons.size && ranging) {
            logger.d("Finished ranging")

            this.stopRanging()
        }
    }

    private fun isBluetoothSupported(context: Context): Boolean {
        if(!this::adapter.isInitialized) {
            val defaultAdapter = BluetoothAdapter.getDefaultAdapter()
            if (defaultAdapter != null) {
                adapter = defaultAdapter
            }
        }

        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH) && adapter != null && adapter.bluetoothLeScanner != null
    }

    private fun getScanSettings(scanMode: Int): ScanSettings {
        return ScanSettings.Builder()
            .setScanMode(scanMode)
            .build()
    }

}