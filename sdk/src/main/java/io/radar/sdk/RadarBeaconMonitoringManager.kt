package io.radar.sdk

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import io.radar.sdk.Radar.RadarLogType
import io.radar.sdk.model.RadarBeacon

/**
 * Monitors beacons in the background with a low-power scan that delivers enter and exit events
 * to `RadarLocationReceiver`. The scan runs until `stopMonitoringBeacons()` is called, and
 * survives the app process being killed.
 *
 * Events are handled by `RadarLocationManager.handleBeacons`, which seeds the beacons into the next
 * one-shot ranging result with `RadarOneShotBeaconManager.seedNearbyBeacons` and then tracks.
 */
@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("MissingPermission")
internal class RadarBeaconMonitoringManager(
    private val context: Context,
    private val logger: RadarLogger,
    @SuppressLint("VisibleForTests")
    internal var permissionsHelper: RadarPermissionsHelper = RadarPermissionsHelper()
) {

    private lateinit var adapter: BluetoothAdapter
    private var monitoredBeaconIdentifiers = setOf<String>()

    fun startMonitoringBeacons(beacons: Array<RadarBeacon>) {
        if (RadarSettings.getSdkConfiguration(context).useRadarModifiedBeacon) {
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
        if (RadarSettings.getSdkConfiguration(context).useRadarModifiedBeacon) {
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
        if (RadarSettings.getSdkConfiguration(context).useRadarModifiedBeacon) {
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

    private fun isBluetoothSupported(context: Context): Boolean {
        if (!this::adapter.isInitialized) {
            val defaultAdapter = BluetoothAdapter.getDefaultAdapter()
            if (defaultAdapter != null) {
                adapter = defaultAdapter
            }
        }

        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH) && adapter != null && adapter.bluetoothLeScanner != null
    }

    private fun getScanSettings(scanMode: Int): ScanSettings = ScanSettings.Builder()
        .setScanMode(scanMode)
        .build()
}
