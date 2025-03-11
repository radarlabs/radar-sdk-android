package io.radar.sdk
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.HttpsURLConnection

class RadarSensorsManager(context: Context) : SensorEventListener {
    private var sensorManager: SensorManager
    private var pressure: Sensor?
    private val context: Context = context
    private val pressureHistory = mutableListOf<Float>()
    private val maxHistorySize = 400
    private val shortTermWindow = mutableListOf<Float>()
    private val shortTermSize = 50  // 5 seconds at 10 Hz
    private val ngrokUrl = "https://arriving-eagle-magnetic.ngrok-free.app"
    private var lastNgrokSendTime: Long = 0
    private val ngrokSendInterval: Long = 5000  // 5 seconds in milliseconds
    private var lastRawDataSendTime: Long = 0
    private val rawDataSendInterval: Long = 5000  // 1 second in milliseconds
    private val userAgent = "RadarSDK/Android/4.0.0"
    private val executor = Executors.newSingleThreadExecutor()
    private val rawReadings = mutableListOf<Pair<Double, Long>>()  // pressure and timestamp pairs
    private val isRawDataSending = AtomicBoolean(false)  // Lock for raw data sending

    // Exponential decay parameters
    private val smoothingFactor = 0.2  // Higher value means more smoothing (closer to 1.0)
    private var smoothedPressure: Double = 0.0

    // Kalman filter parameters
    private var estimatedPressure: Double = 1000.0
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

    private fun updateExponentialDecay(newPressure: Double, timestamp: Long) {
        val currentTime = System.currentTimeMillis()
        
        
        // Apply exponential decay
        smoothedPressure = smoothingFactor * smoothedPressure + (1 - smoothingFactor) * newPressure
        
        
        //Radar.logger.d("Exponential decay | Raw: $newPressure, Smoothed: $smoothedPressure, smoothingFactor: $smoothingFactor")
    }

    private fun sendRawDataToNgrok() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastRawDataSendTime < rawDataSendInterval) {
            return  // Skip if not enough time has passed
        }

        executor.execute {
            try {
                // Create URL
                val url = URL("$ngrokUrl/raw")
                Radar.logger.d("Sending raw data to ngrok: $url")
                
                // Create JSON array of readings
                val readingsJson = JSONObject()
                val readingsArray = org.json.JSONArray()
                rawReadings.forEach { (pressure, timestamp) ->
                    val reading = JSONObject()
                    reading.put("pressure", pressure)
                    reading.put("timestamp", timestamp)
                    readingsArray.put(reading)
                }
                readingsJson.put("readings", readingsArray)
                
                // Open connection
                val connection = url.openConnection() as HttpsURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.setRequestProperty("User-Agent", userAgent)
                connection.setRequestProperty("ngrok-skip-browser-warning", "true")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                // Write the JSON data
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(readingsJson.toString())
                }

                // Send request and get response
                val responseCode = connection.responseCode
                Radar.logger.d("Raw data ngrok response code: $responseCode")

                // Clean up
                connection.disconnect()
                
                // Update last send time
                lastRawDataSendTime = currentTime
                
                // Clear the buffer after successful send
                rawReadings.clear()
            } catch (e: Exception) {
                Radar.logger.e("Error sending raw data to ngrok: ${e}")
            }
        }
    }

    private fun sendPressureDataToNgrok(pressureJson: JSONObject, timestamp: Long) {

        // Try to acquire the lock



        val currentTime = System.currentTimeMillis()
        if (currentTime - lastNgrokSendTime < ngrokSendInterval) {
            return  // Skip if not enough time has passed
        }

        if (!isRawDataSending.compareAndSet(false, true)) {
            Radar.logger.d("Raw data send already in progress, skipping")
            return
        }

        executor.execute {
            try {
                // Create URL
                val url = URL(ngrokUrl)
                Radar.logger.d("Sending data to ngrok: $url")
                
                // Open connection
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.setRequestProperty("User-Agent", userAgent)
                connection.setRequestProperty("ngrok-skip-browser-warning", "true")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                // Write the JSON data
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(pressureJson.toString())
                }

                // Send request and get response
                val responseCode = connection.responseCode
                Radar.logger.d("Ngrok response code: $responseCode")

                // Clean up
                connection.disconnect()
                
                // Update last send time
                lastNgrokSendTime = currentTime
            } catch (e: Exception) {
                Radar.logger.e("Error sending data to ngrok: ${e}")
            } finally {
                // Always release the lock, even if an error occurred
                isRawDataSending.set(false)
            }
        }
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
        
        //Radar.logger.e("Pressure sensor changed | millibarsOfPressure = $millibarsOfPressure; accuracy = $accuracy")
        
        // Add to raw readings buffer
        rawReadings.add(Pair(millibarsOfPressure.toDouble(), timestamp/1000))
        
        // Update exponential decay smoothing
        updateExponentialDecay(millibarsOfPressure.toDouble(), timestamp)
        
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
        pressureJson.put("shortTermPressure", shortTermAverage)  // Use the 5-second window average
        pressureJson.put("longTermPressure", longTermAverage)  // Store the long-term average as well
        pressureJson.put("kalmanPressure", estimatedPressure)  // Add Kalman filtered value
        pressureJson.put("exponentialPressure", smoothedPressure)  // Add exponentially smoothed value
        pressureJson.put("accuracy", accuracy)

        //pressureJson.put("pressure", millibarsOfPressure)
        pressureJson.put("pressure", shortTermAverage)

        pressureJson.put("absoluteAltitudeTimestamp", timestamp / 1000)

        // Send data to ngrok endpoint (will only send if 5 seconds have passed)
        //sendPressureDataToNgrok(pressureJson, timestamp)
        
        // Send raw data (will only send if 1 second has passed)
        //sendRawDataToNgrok()
        
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