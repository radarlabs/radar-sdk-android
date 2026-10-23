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
 * Where the Bluetooth controller supports hardware filtering, the scan reports a first match and a
 * loss per beacon, like iOS beacon region enter and exit. Otherwise it reports every sighting, and
 * `RadarLocationManager.handleBeacons` treats a beacon as entered when it hasn't been seen recently.
 *
 * Events are handled by `RadarLocationManager.handleBeacons`, which keeps the beacons the user is
 * inside in `RadarState` and tracks only when a beacon is entered or exited.
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

        if (!adapter.isEnabled) {
            logger.d("Bluetooth not enabled")

            return
        }

        val newBeaconIdentifiers = beacons.mapNotNull { it._id }.toSet()
        if (monitoredBeaconIdentifiers == newBeaconIdentifiers) {
            logger.d("Already monitoring beacons")

            return
        }

        this.stopScan()

        if (beacons.isEmpty()) {
            logger.d("No beacons to monitor")

            return
        }

        monitoredBeaconIdentifiers = newBeaconIdentifiers

        val monitoredKeys = beacons.map { RadarBeaconUtils.beaconKey(it) }.toSet()
        val insideBeacons = RadarState.getInsideBeacons(context)
        if (insideBeacons.keys.retainAll(monitoredKeys)) {
            RadarState.setInsideBeacons(context, insideBeacons)
        }

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

        logger.d("Starting monitoring beacons")

        startScan(scanFilters)
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

        this.stopScan()

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

        logger.d("Starting monitoring beacon UUIDs")

        startScan(scanFilters)
    }

    fun stopMonitoringBeacons() {
        if (RadarSettings.getSdkConfiguration(context).useRadarModifiedBeacon) {
            return
        }

        // Monitoring has stopped, so the next sighting of any beacon is an entry.
        RadarState.setInsideBeacons(context, emptyMap())

        stopScan()
    }

    /**
     * Called when a first match and loss scan fails after it started. Later scans report every sighting.
     */
    fun handleScanError(errorCode: Int) {
        logger.e("Beacon monitoring scan failed | errorCode = $errorCode; transitions = ${RadarState.getBeaconMonitoringTransitions(context)}", RadarLogType.SDK_ERROR)

        if (RadarState.getBeaconMonitoringTransitions(context)) {
            RadarState.setBeaconTransitionsUnsupported(context, true)
        }

        // Restart monitoring the next time beacons are searched.
        monitoredBeaconIdentifiers = setOf()
    }

    private fun startScan(scanFilters: List<ScanFilter>) {
        val transitionsSupported = adapter.isOffloadedFilteringSupported && !RadarState.getBeaconTransitionsUnsupported(context)

        logger.d("Starting monitoring scan | offloadedFilteringSupported = ${adapter.isOffloadedFilteringSupported}; transitions = $transitionsSupported")

        if (transitionsSupported && startScan(scanFilters, transitions = true)) {
            return
        }

        if (transitionsSupported) {
            logger.d("First match and loss scan failed, monitoring every sighting")

            RadarState.setBeaconTransitionsUnsupported(context, true)
        }

        startScan(scanFilters, transitions = false)
    }

    private fun startScan(scanFilters: List<ScanFilter>, transitions: Boolean): Boolean = try {
        val result = adapter.bluetoothLeScanner.startScan(scanFilters, getScanSettings(transitions), RadarLocationReceiver.getBeaconPendingIntent(context))
        if (result == 0) {
            RadarState.setBeaconMonitoringTransitions(context, transitions)
        } else {
            logger.d("Error starting monitoring scan | result = $result; transitions = $transitions")
        }

        result == 0
    } catch (e: Exception) {
        logger.e("Error starting monitoring scan | transitions = $transitions", RadarLogType.SDK_EXCEPTION, e)

        false
    }

    private fun stopScan() {
        if (!permissionsHelper.bluetoothPermissionsGranted(context)) {
            return
        }

        if (!isBluetoothSupported(context)) {
            return
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
            RadarBeaconUtils.getBluetoothAdapter(context)?.let { adapter = it }
        }

        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH) && this::adapter.isInitialized && adapter.bluetoothLeScanner != null
    }

    internal companion object {
        /**
         * With [transitions], the scan reports the first sighting and the loss of each beacon, using
         * sticky matching to avoid flapping. Without it, the scan reports every sighting.
         */
        internal fun getScanSettings(transitions: Boolean): ScanSettings {
            val builder = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)

            if (transitions) {
                builder
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH or ScanSettings.CALLBACK_TYPE_MATCH_LOST)
                    .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                    .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
            }

            return builder.build()
        }
    }
}
