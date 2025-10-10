//package io.radar.example
//
//import android.Manifest
//import android.app.PendingIntent
//import android.content.Context
//import android.os.Looper
//import androidx.annotation.RequiresPermission
//import com.google.android.gms.location.LastLocationRequest
//import com.google.android.gms.location.LocationAvailability
//import com.google.android.gms.location.LocationCallback
//import com.google.android.gms.location.LocationRequest
//import com.google.android.gms.location.LocationResult
//import com.google.android.gms.location.LocationServices
//import com.google.android.gms.location.Priority
//import org.maplibre.android.location.engine.LocationEngine
//import org.maplibre.android.location.engine.LocationEngineCallback
//import org.maplibre.android.location.engine.LocationEngineRequest
//import org.maplibre.android.location.engine.LocationEngineResult
//
//class GmsMapLibreEngine(context: Context): LocationEngine {
//    private val callbackMap = HashMap<LocationEngineCallback<LocationEngineResult>, LocationCallback>()
//    private val client = LocationServices.getFusedLocationProviderClient(context)
//
//    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
//    override fun getLastLocation(p0: LocationEngineCallback<LocationEngineResult>) {
//        println("ENGINE: getLastLocation called")
//
//        client.setMockMode(false)
//
//        client.getLastLocation(LastLocationRequest.Builder().build()).addOnSuccessListener { location ->
//            print("ENGINE: getLastLocation success $location")
//            p0.onSuccess(LocationEngineResult.create(location))
//        }.addOnFailureListener { exception ->
//            print("ENGINE: getLastLocation failed $exception")
//            p0.onFailure(exception)
//        }
//    }
//
//    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
//    override fun requestLocationUpdates(
//        p0: LocationEngineRequest,
//        p1: LocationEngineCallback<LocationEngineResult>,
//        p2: Looper?,
//    ) {
//        println("ENGINE: requestLocationUpdates called ${p0.priority} ${p0.interval} ${p0.fastestInterval}")
//        val priority = when (p0.priority) {
//            LocationEngineRequest.PRIORITY_HIGH_ACCURACY -> Priority.PRIORITY_HIGH_ACCURACY
//            LocationEngineRequest.PRIORITY_NO_POWER -> Priority.PRIORITY_PASSIVE
//            LocationEngineRequest.PRIORITY_LOW_POWER -> Priority.PRIORITY_LOW_POWER
//            LocationEngineRequest.PRIORITY_BALANCED_POWER_ACCURACY -> Priority.PRIORITY_BALANCED_POWER_ACCURACY
//            else -> Priority.PRIORITY_HIGH_ACCURACY
//        }
//        val request = LocationRequest.Builder(priority, p0.interval).setMaxUpdateDelayMillis(p0.maxWaitTime).build()
//        val callback = object : LocationCallback() {
//            override fun onLocationResult(var1: LocationResult) {
//                println("ENGINE: location updated")
//                p1.onSuccess(LocationEngineResult.create(var1.locations))
//            }
//
//            override fun onLocationAvailability(p0: LocationAvailability) {
//                println("ENGINE: location available")
//            }
//        }
//        callbackMap[p1] = callback
//        client.requestLocationUpdates(request, callback, p2)
//    }
//
//    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
//    override fun requestLocationUpdates(
//        p0: LocationEngineRequest,
//        p1: PendingIntent?,
//    ) {
//        println("ENGINE: requestLocationUpdates intent called $p1")
//        if (p1 == null) {
//            return
//        }
//        val priority = when (p0.priority) {
//            LocationEngineRequest.PRIORITY_HIGH_ACCURACY -> Priority.PRIORITY_HIGH_ACCURACY
//            LocationEngineRequest.PRIORITY_NO_POWER -> Priority.PRIORITY_PASSIVE
//            LocationEngineRequest.PRIORITY_LOW_POWER -> Priority.PRIORITY_LOW_POWER
//            LocationEngineRequest.PRIORITY_BALANCED_POWER_ACCURACY -> Priority.PRIORITY_BALANCED_POWER_ACCURACY
//            else -> Priority.PRIORITY_HIGH_ACCURACY
//        }
//        val request = LocationRequest.Builder(priority, p0.interval).setMaxUpdateDelayMillis(p0.maxWaitTime).build()
//        client.requestLocationUpdates(request, p1)
//    }
//
//    override fun removeLocationUpdates(p0: LocationEngineCallback<LocationEngineResult>) {
//        println("ENGINE: removeLocationUpdates")
//        val callback = callbackMap[p0]
//        if (callback != null) {
//            client.removeLocationUpdates(callback)
//        }
//    }
//
//    override fun removeLocationUpdates(p0: PendingIntent?) {
//        println("ENGINE: removeLocationUpdates intent $p0")
//        if (p0 != null) {
//            client.removeLocationUpdates(p0)
//        }
//    }
//}