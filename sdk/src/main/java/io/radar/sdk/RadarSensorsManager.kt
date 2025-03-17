package io.radar.sdk
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import org.json.JSONObject

class RadarSensorsManager(context: Context) : SensorEventListener {
    private var sensorManager: SensorManager
    private var pressure: Sensor?
    private val context: Context = context
    private val shortTermWindow = mutableListOf<Float>()
    private val shortTermSize = 50  // 5 seconds at 10 Hz
    private val userAgent = "RadarSDK/Android/4.0.0"

    init {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        pressure = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        Radar.logger.d("SensorManager initialized: $sensorManager")
        Radar.logger.d("Pressure sensor: ${pressure?.let { 
            "Found - Name: ${it.name}, Vendor: ${it.vendor}, Power: ${it.power}, Resolution: ${it.resolution}"
        } ?: "Not available on device"}")
    }


    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Right now do nothing here if sensor accuracy changes.
    }

    override fun onSensorChanged(event: SensorEvent) {
        val millibarsOfPressure = event.values[0]
        val accuracy = event.accuracy
        val timestamp = System.currentTimeMillis()
        
        
        shortTermWindow.add(millibarsOfPressure)
        if (shortTermWindow.size > shortTermSize) {
            shortTermWindow.removeAt(0)
        }
        
        // Calculate average of both windows
        val shortTermAverage = shortTermWindow.average()
        
        val pressureJson = JSONObject()

        pressureJson.put("accuracy", accuracy)

        pressureJson.put("pressure", shortTermAverage)

        pressureJson.put("absoluteAltitudeTimestamp", timestamp / 1000)


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