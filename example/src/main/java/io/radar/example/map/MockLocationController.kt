package io.radar.example.map

import android.content.Context
import android.location.Location
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.location.LocationServices
import io.radar.example.MockableLocationEngine
import org.maplibre.android.geometry.LatLng

/**
 * Drops a mock location through the fused location provider + [MockableLocationEngine] so the
 * blue dot and the SDK follow a long-pressed map point. Preserved from the original example —
 * invaluable on Android where there's no simulator location simulator.
 */
class MockLocationController(context: Context) {

    private val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)
    private val engine = MockableLocationEngine.get(context.applicationContext)

    var mocking by mutableStateOf(false)
        private set

    fun setMock(latLng: LatLng, onSuccess: (Location) -> Unit = {}) {
        client.setMockMode(true).addOnSuccessListener {
            val mock = Location("mock").apply {
                latitude = latLng.latitude
                longitude = latLng.longitude
                accuracy = 5f
                time = System.currentTimeMillis()
                elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            }
            client.setMockLocation(mock)
            engine.mockedLocation = mock
            mocking = true
            onSuccess(mock)
        }
    }

    fun clear() {
        client.setMockMode(false).addOnSuccessListener {
            engine.mockedLocation = null
            mocking = false
        }
    }
}
