package io.radar.sdk
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import org.json.JSONObject
import kotlin.math.abs

class RadarSensorsManager(context: Context) : SensorEventListener {
    private var sensorManager: SensorManager
    private var pressure: Sensor?
    private val context: Context = context
    private val pressureHistory = mutableListOf<Float>()
    private val maxHistorySize = 400
    private val shortTermWindow = mutableListOf<Float>()
    private val shortTermSize = 50  // 5 seconds at 10 Hz

    // Kalman filter parameters
    private var estimatedPressure: Double = 0.0
    private var estimatedError: Double = 1.0
    private val measurementNoise: Double = 1.0    // Noise magnitude of 10^0
    private val processNoise: Double = 0.01       // Expected changes of 10^-1 per second
    private var lastPressure: Double = 0.0
    private var lastTimestamp: Long = 0

    init {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        pressure = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        Radar.logger.d("SensorManager initialized: $sensorManager")
        Radar.logger.d("Pressure sensor: ${pressure?.let { 
            "Found - Name: ${it.name}, Vendor: ${it.vendor}, Power: ${it.power}, Resolution: ${it.resolution}"
        } ?: "Not available on device"}")
    }

    private fun updateKalmanFilter(measurement: Double, timestamp: Long) {
        // Time step between measurements
        val dt = if (lastTimestamp == 0L) 0.1 else (timestamp - lastTimestamp) / 1000.0  // Convert to seconds
        lastTimestamp = timestamp

        // Predict
        // Since pressure changes are relatively slow, we can assume the state doesn't change much
        // This is a simple prediction step - could be made more sophisticated if needed
        val predictedPressure = estimatedPressure
        val predictedError = estimatedError + processNoise * dt

        // Update
        val kalmanGain = predictedError / (predictedError + measurementNoise)
        estimatedPressure = predictedPressure + kalmanGain * (measurement - predictedPressure)
        estimatedError = (1 - kalmanGain) * predictedError

        // Log the filter's behavior
        Radar.logger.d("Kalman filter | Measurement: $measurement, Estimated: $estimatedPressure, Gain: $kalmanGain, Error: $estimatedError")
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Right now do nothing here if sensor accuracy changes.
    }

    override fun onSensorChanged(event: SensorEvent) {
        val millibarsOfPressure = event.values[0]
        val accuracy = event.accuracy
        val timestamp = System.currentTimeMillis()
        
        Radar.logger.e("Pressure sensor changed | millibarsOfPressure = $millibarsOfPressure; accuracy = $accuracy")
        
        // Update Kalman filter
        updateKalmanFilter(millibarsOfPressure.toDouble(), timestamp)
        
        // Add new pressure reading to both histories
        pressureHistory.add(millibarsOfPressure)
        if (pressureHistory.size > maxHistorySize) {
            pressureHistory.removeAt(0)
        }
        
        shortTermWindow.add(millibarsOfPressure)
        if (shortTermWindow.size > shortTermSize) {
            shortTermWindow.removeAt(0)
        }
        
        // Calculate average of both windows
        val longTermAverage = pressureHistory.average()
        val shortTermAverage = shortTermWindow.average()
        
        val pressureJson = JSONObject()
        pressureJson.put("pressure", shortTermAverage)  // Use the 5-second window average
        pressureJson.put("longTermPressure", longTermAverage)  // Store the long-term average as well
        pressureJson.put("kalmanPressure", estimatedPressure)  // Add Kalman filtered value
        pressureJson.put("accuracy", accuracy)
        pressureJson.put("rawPressure", millibarsOfPressure)

        // placate the current server implementation, should change if we ever want to merge this
        pressureJson.put("relativeAltitudeTimestamp", timestamp / 1000)
        RadarState.setLastPressure(context, pressureJson)
    }

    fun onResume() {
        if (pressure != null) {
            sensorManager.registerListener(this, pressure, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun onPause() {
        if (pressure != null) {
            sensorManager.unregisterListener(this)
        }
    }
}