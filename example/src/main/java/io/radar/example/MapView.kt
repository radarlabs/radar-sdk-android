package io.radar.example

import android.Manifest
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.location.Location
import android.os.FileObserver
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
//import com.google.android.gms.location.LocationCallback
//import com.google.android.gms.location.LocationRequest
//import com.google.android.gms.location.LocationResult
//import com.google.android.gms.location.LocationServices
//import com.google.android.gms.location.Priority
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.engine.LocationEngine
import org.maplibre.android.location.engine.LocationEngineCallback
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.engine.LocationEngineResult
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import java.io.File
import kotlin.math.cos

fun createCirclePolygon(
    lat: Double,
    lng: Double,
    radiusInMeters: Double,
    points: Int = 64,
    altitude: Double = 0.0,
): Polygon {
    val coords = mutableListOf<Point>()
    for (i in 0 until points) {
        val angle = 2 * Math.PI * i / points
        val dx = radiusInMeters * Math.cos(angle)
        val dy = radiusInMeters * Math.sin(angle)

        // approximate meter-to-lat/lng
        val deltaLat = dy / 111320.0
        val deltaLng = dx / (111320.0 * cos(Math.toRadians(lat)))

        coords.add(Point.fromLngLat(lng + deltaLng, lat + deltaLat, altitude))
    }
    // close ring
    if (coords.isNotEmpty()) coords.add(coords[0])
    return Polygon.fromLngLats(listOf(coords))
}

fun getFeatureCollection(file: File): FeatureCollection {
    try {
        if (!file.exists()) throw Exception()
        val json = JSONObject(file.readBytes().toString(Charsets.UTF_8))

        val geofences = json.getJSONArray("geofences")
        val features = mutableListOf<Feature>()

        for (i in 0 until geofences.length()) {
            val geofenceJSON = geofences.get(i) as JSONObject

            val center = geofenceJSON.getJSONObject("geometryCenter")
            val radius = geofenceJSON.getDouble("geometryRadius")

            val coord = center.getJSONArray("coordinates")
            val polygon = createCirclePolygon(coord.getDouble(1), coord.getDouble(0), radius)

            features.add(Feature.fromGeometry(polygon))
        }

        println("getFeatureCollection ${features.size}")

        return FeatureCollection.fromFeatures(features)
    } catch (e: Exception) {
        println("getFeatureCollection UH OH, ERROR CREATING FEATURE COLLECTION $e")
        return FeatureCollection.fromFeatures(arrayOf<Feature>())
    }
}

@Composable
fun MapView(disabled: Boolean) {
//    val client = LocationServices.getFusedLocationProviderClient(LocalContext.current)
    val file = File(File(LocalContext.current.filesDir, "RadarSDK"), "offlineData.json")
    var offlineData by remember { mutableStateOf(getFeatureCollection(file)) } // GeoJson of the geofence circles
    val fileObserver = object : FileObserver(file.absolutePath, (CLOSE_WRITE or MODIFY or CREATE)) {
        override fun onEvent(event: Int, path: String?) {
            offlineData = getFeatureCollection(file)
        }
    }.apply {
        startWatching()
    }

    AndroidView(
        modifier = Modifier.graphicsLayer { alpha = if (disabled) 0f else 1f },
        factory = { context ->
            val style = "radar-default-v1"
            val styleURL = "$HOST/maps/styles/$style?publishableKey=$PUBLISHABLE_KEY"

            // init MapLibre
            MapLibre.getInstance(context)

            // init layout view
            val mapView = MapView(context)

            mapView.getMapAsync { map ->
                // callback for map done loading
                map.uiSettings.isLogoEnabled = false

                // anchor attribution to bottom right
                map.uiSettings.isTiltGesturesEnabled = false
                map.uiSettings.attributionGravity = Gravity.END + Gravity.BOTTOM
                map.uiSettings.setAttributionMargins(0,0,24,24)

                map.addOnMapLongClickListener { point ->
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return@addOnMapLongClickListener false
                    }
//                    client.setMockMode(true).addOnSuccessListener {
//                        val mockLoc = Location("mock").apply {
//                            latitude = point.latitude
//                            longitude = point.longitude
//                            accuracy = 5f
//                            time = System.currentTimeMillis()
//                            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
//                        }
//                        client.setMockLocation(mockLoc)
//                        println("Succesfully set mock $mockLoc")
//                    }.addOnFailureListener {
//                        println("Failed to set mock")
//                    }
                    return@addOnMapLongClickListener true
                }

                map.setStyle(styleURL) { style ->

                    val offlineGeofences = GeoJsonSource("offlineGeofences")
                    offlineGeofences.setGeoJson(offlineData)
                    style.addSource(offlineGeofences)
                    style.addLayer(FillLayer("offlineCircles", "offlineGeofences").apply {
                        setProperties(
                            PropertyFactory.fillOpacity(0.1f),
                            PropertyFactory.fillColor(Color.Green.toArgb()),
                        )
                    })
                    style.addLayer(LineLayer("offlineOutline", "offlineGeofences").apply {
                        setProperties(
                            PropertyFactory.lineColor(Color.Green.toArgb()),
                            PropertyFactory.lineWidth(1f)
                        )
                    })

                    val locationComponent = map.locationComponent
                    // Build LocationComponent activation options
                    val locCompOpts = LocationComponentActivationOptions.builder(context, style)
//                        .useDefaultLocationEngine(false)
//                        .locationEngine(GmsMapLibreEngine(context))
                        .build()
                    locationComponent.activateLocationComponent(locCompOpts)
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return@setStyle
                    }
                    locationComponent.isLocationComponentEnabled = true

                    val location = map.locationComponent.lastKnownLocation ?: return@setStyle
                    map.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(location.latitude, location.longitude))
                        .zoom(13.0)
                        .build()
                }
            }
            mapView
        },
        update = { mapView ->
            mapView.getMapAsync { map ->
                val style = map.style ?: return@getMapAsync

                val offlineGeofences = style.getSource("offlineGeofences") as GeoJsonSource
                offlineGeofences.setGeoJson(offlineData)

                map.uiSettings.isScrollGesturesEnabled = !disabled
                map.uiSettings.isZoomGesturesEnabled = !disabled
                map.uiSettings.isRotateGesturesEnabled = !disabled
            }

        },
        onReset = { mapView ->
            // Reset transient state: listeners, animations, scroll positions, etc.
//            view.resetTransientState()
        },
        onRelease = { mapView ->
            // Cleanup if it's going away permanently
//            view.cleanup()
        },
    )
}