package io.radar.sdk

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Handler
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresApi
import io.radar.sdk.Radar.RadarBeaconCallback
import io.radar.sdk.Radar.RadarStatus
import io.radar.sdk.model.RadarBeacon
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.util.*
import java.util.zip.GZIPOutputStream

@SuppressLint("MissingPermission")
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal class RadarIndoorSurveyManager(
    private val context: Context,
    private val logger: RadarLogger,
    private val locationManager: RadarLocationManager,
    private val apiClient: RadarApiClient
) : SensorEventListener {

    interface RadarIndoorSurveyCallback {
        fun onComplete(status: RadarStatus, payload: String)
    }

    private var isScanning = false
    private lateinit var placeLabel: String
    private lateinit var callback: RadarIndoorSurveyCallback
    private val bluetoothReadings = mutableListOf<String>()
    private var isWhereAmIScan = false
    private lateinit var scanId: String
    private var locationAtTimeOfSurveyStart: Location? = null
    private var lastMagnetometerData: SensorEvent? = null

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private lateinit var sensorManager: SensorManager

   internal fun start(
        placeLabel: String,
        surveyLengthSeconds: Int,
        knownLocation: Location?,
        isWhereAmIScan: Boolean,
        callback: RadarIndoorSurveyCallback
    ) {
        logger.d("start called with placeLabel: $placeLabel, surveyLengthSeconds: $surveyLengthSeconds, isWhereAmIScan: $isWhereAmIScan")
        logger.d("isScanning: $isScanning")

        if (isScanning) {
            logger.e("Error: start called while already scanning")
            callback.onComplete(RadarStatus.ERROR_UNKNOWN, "Error: start called while already scanning")
            return
        }

        isScanning = true
        this.placeLabel = placeLabel
        this.callback = callback
        this.isWhereAmIScan = isWhereAmIScan
        scanId = UUID.randomUUID().toString()

        if (isWhereAmIScan && knownLocation == null) {
            logger.e("Error: start called with isWhereAmIScan but no knownLocation")
            callback.onComplete(RadarStatus.ERROR_UNKNOWN, "Error: start called with isWhereAmIScan but no knownLocation")
            isScanning = false
            return
        } else if (isWhereAmIScan && knownLocation != null) {
            locationAtTimeOfSurveyStart = knownLocation
            kickOffMotionAndBluetooth(surveyLengthSeconds)
        } else if (!isWhereAmIScan) {
            logger.d("Calling RadarLocationManager getLocationWithDesiredAccuracy")
            locationManager.getLocation(object : Radar.RadarLocationCallback { 
                override fun onComplete(status: RadarStatus, location: Location?, stopped: Boolean) {
                    if (status != RadarStatus.SUCCESS || location == null) {
                        callback.onComplete(status, "")
                        isScanning = false
                        return
                    }

                    logger.d("location: ${location.latitude}, ${location.longitude}")
                    logger.d(location.toString())

                    locationAtTimeOfSurveyStart = location
                    kickOffMotionAndBluetooth(surveyLengthSeconds)
                }
            })
        }
    }

    private fun kickOffMotionAndBluetooth(surveyLengthSeconds: Int) {
        logger.d("Kicking off SensorManager")
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL)

        logger.d("Kicking off BluetoothLeScanner")
        logger.d("time: ${System.currentTimeMillis() / 1000.0}")

        bluetoothAdapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        startScanning()

        Handler(Looper.getMainLooper()).postDelayed({ stopScanning() }, surveyLengthSeconds * 1000L)
    }

    private fun startScanning() {
        logger.d("startScanning called --- calling startScan")
        bluetoothLeScanner.startScan(scanCallback)
    }

    private fun stopScanning() {
        logger.d("stopScanning called")
        logger.d("time: ${System.currentTimeMillis() / 1000.0}")

        bluetoothLeScanner.stopScan(scanCallback)

        val payload = bluetoothReadings.joinToString("\n")

        logger.d("payload length: ${payload.length}")

        val compressedData = gzip(payload)
        logger.d("compressedData length: ${compressedData.size}")

        val compressedDataBase64 = Base64.getEncoder().encodeToString(compressedData)
        logger.d("compressedDataBase64 length: ${compressedDataBase64.length}")

        logger.d("isWhereAmIScan $isWhereAmIScan")

        if (isWhereAmIScan) {
            callback.onComplete(RadarStatus.SUCCESS, compressedDataBase64)
        } else {
            // TODO: Implement POST request to server using apiClient
        }

        logger.d("Calling clear, resetting scanId, etc.")

        bluetoothReadings.clear()
        scanId = ""
        locationAtTimeOfSurveyStart = null
        lastMagnetometerData = null

        logger.d("stopScanning end")

        isScanning = false
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val rssi = result.rssi
            val scanRecord = result.scanRecord

            val manufacturerId = scanRecord?.manufacturerSpecificData?.keyAt(0)?.toString() ?: ""
            val name = device.name ?: "(no name)"

            val timestamp = System.currentTimeMillis() / 1000.0

            val serviceUuids = scanRecord?.serviceUuids?.joinToString(",") { it.uuid.toString() } ?: "(no services)"

            val verticalAccuracy = locationAtTimeOfSurveyStart?.let { location ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && location.hasVerticalAccuracy() && !location.verticalAccuracyMeters.isNaN()) {
                    location.verticalAccuracyMeters
                } else {
                    0.0
                }
            }

            val queryItems = listOf(
                "time" to timestamp.toString(),
                "label" to placeLabel,
                "peripheral.identifier" to device.address,
                "peripheral.name" to name,
                "rssi" to rssi.toString(),
                "manufacturerId" to manufacturerId,
                "scanId" to scanId,
                "serviceUUIDs" to serviceUuids,
                "location.coordinate.latitude" to locationAtTimeOfSurveyStart?.latitude.toString(),
                "location.coordinate.longitude" to locationAtTimeOfSurveyStart?.longitude.toString(),
                "location.horizontalAccuracy" to locationAtTimeOfSurveyStart?.accuracy.toString(),
                "location.verticalAccuracy" to verticalAccuracy.toString(),
                "location.altitude" to locationAtTimeOfSurveyStart?.altitude.toString(),
                "location.ellipsoidalAltitude" to "0.0", // Not available in Android
                "location.timestamp" to locationAtTimeOfSurveyStart?.time.toString(),
                "location.floor" to "0", // Not available in Android
                "sdkVersion" to RadarUtils.sdkVersion,
                "deviceType" to "Android",
                "deviceMake" to android.os.Build.MANUFACTURER,
                "deviceModel" to android.os.Build.MODEL,
                "deviceOS" to android.os.Build.VERSION.RELEASE,
                "magnetometer.field.x" to lastMagnetometerData?.values?.get(0).toString(),
                "magnetometer.field.y" to lastMagnetometerData?.values?.get(1).toString(),
                "magnetometer.field.z" to lastMagnetometerData?.values?.get(2).toString(),
                "magnetometer.timestamp" to lastMagnetometerData?.timestamp.toString(),
                // "magnetometer.field.magnitude" to lastMagnetometerData?.let {
                //     Math.sqrt(it.values[0].toDouble().pow(2) + it.values[1].toDouble().pow(2) + it.values[2].toDouble().pow(2))
                // }.toString(),
                // "isConnectable" to scanRecord?.isConnectable().toString()
            )

            val queryString = queryItems.joinToString("&") { (key, value) ->
                "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
            }

            bluetoothReadings.add("?$queryString")
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            lastMagnetometerData = event
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun gzip(input: String): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(input.toByteArray(Charsets.UTF_8)) }
        return bos.toByteArray()
    }
}