package io.radar.example

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.os.Looper
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.engine.MapLibreFusedLocationEngineImpl

class MockableLocationEngine(context: Context): MapLibreFusedLocationEngineImpl(context)  {

    companion object {
        lateinit var engine: MockableLocationEngine
        fun get(context: Context): MockableLocationEngine {
            if (!::engine.isInitialized) {
                engine = MockableLocationEngine(context)
            }
            return engine
        }
    }

    var mockedLocation: Location? = null
    var map = mutableMapOf<LocationListener, LocationListener>()

    @Throws(SecurityException::class)
    override fun requestLocationUpdates(
        request: LocationEngineRequest,
        listener: LocationListener,
        looper: Looper?,
    ) {
        val applyMock = object: LocationListener {
            override fun onLocationChanged(location: Location) {
                val mocked = mockedLocation
                if (mocked != null) {
                    location.latitude = mocked.latitude
                    location.longitude = mocked.longitude
                }
                map[this]?.onLocationChanged(location)
            }
        }

        map[applyMock] = listener
        map[listener] = applyMock
        super.requestLocationUpdates(request, applyMock, looper)
    }

    override fun removeLocationUpdates(listener: LocationListener) {
        val applyMock = map[listener]
        if (applyMock != null) {
            super.removeLocationUpdates(applyMock)
            map.remove(listener)
            map.remove(applyMock)
        }
    }
}
