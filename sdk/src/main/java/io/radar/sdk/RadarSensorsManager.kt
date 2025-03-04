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
        // Do something with this sensor data.
        val pressureJson = JSONObject()
        pressureJson.put("pressure", millibarsOfPressure)
        // placate the current server implementation, should change if we ever want to merge this
        pressureJson.put("relativeAltitudeTimestamp", System.currentTimeMillis())
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