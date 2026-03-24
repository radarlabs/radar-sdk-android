package io.radar.example

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.FileObserver
import android.os.SystemClock
import android.view.Gravity
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.location.LocationServices
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.engine.LocationEngineProxy
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
import kotlin.math.sin

fun getMapHost(): String = io.radar.sdk.Radar.getHost() ?: "https://api.radar.io"

fun createCirclePolygon(
    lat: Double,
    lng: Double,
    radiusInMeters: Double,
    points: Int = 64,
    altitude: Double = 0.0,
): Polygon {
    val coordinates = mutableListOf<Point>()
    for (i in 0 until points) {
        val angle = 2 * Math.PI * i / points
        val dx = radiusInMeters * cos(angle)
        val dy = radiusInMeters * sin(angle)

        // approximate meter-to-lat/lng
        val deltaLat = dy / 111320.0
        val deltaLng = dx / (111320.0 * cos(Math.toRadians(lat)))

        coordinates.add(Point.fromLngLat(lng + deltaLng, lat + deltaLat, altitude))
    }
    // close ring
    if (coordinates.isNotEmpty()) coordinates.add(coordinates[0])
    return Polygon.fromLngLats(listOf(coordinates))
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

            val coordinates = center.getJSONArray("coordinates")
            val polygon = createCirclePolygon(coordinates.getDouble(1), coordinates.getDouble(0), radius)

            features.add(Feature.fromGeometry(polygon))
        }

        return FeatureCollection.fromFeatures(features)
    } catch (e: Exception) {
        println("getFeatureCollection error: $e")
        return FeatureCollection.fromFeatures(arrayOf<Feature>())
    }
}

data class SyncRegionData(
    val region: FeatureCollection,
    val geofences: FeatureCollection,
    val places: FeatureCollection,
    val beacons: FeatureCollection,
)

private val emptySyncRegionData = SyncRegionData(
    region = FeatureCollection.fromFeatures(emptyList()),
    geofences = FeatureCollection.fromFeatures(emptyList()),
    places = FeatureCollection.fromFeatures(emptyList()),
    beacons = FeatureCollection.fromFeatures(emptyList()),
)

fun parseSyncRegionData(file: File): SyncRegionData {
    try {
        if (!file.exists()) return emptySyncRegionData
        val json = JSONObject(file.readText())

        val regionFeatures = mutableListOf<Feature>()
        val centerObj = json.optJSONObject("syncedRegionCenter")
        val radius = json.optDouble("syncedRegionRadius", 0.0)
        if (centerObj != null && radius > 0) {
            val coords = centerObj.optJSONArray("coordinates")
            if (coords != null) {
                val polygon = createCirclePolygon(coords.getDouble(1), coords.getDouble(0), radius)
                regionFeatures.add(Feature.fromGeometry(polygon))
            }
        }

        val geofenceFeatures = mutableListOf<Feature>()
        json.optJSONArray("syncedGeofences")?.let { arr ->
            for (i in 0 until arr.length()) {
                val gf = arr.optJSONObject(i) ?: continue
                val gfType = gf.optString("type", "")

                if (gfType.equals("Polygon", ignoreCase = true) || gfType.equals("isochrone", ignoreCase = true)) {
                    val coordsArr = gf.optJSONArray("coordinates")
                    val ring = coordsArr?.optJSONArray(0)
                    if (ring != null && ring.length() > 0) {
                        val points = mutableListOf<Point>()
                        for (j in 0 until ring.length()) {
                            val pair = ring.optJSONArray(j) ?: continue
                            points.add(Point.fromLngLat(pair.getDouble(0), pair.getDouble(1)))
                        }
                        if (points.isNotEmpty()) {
                            if (points.first() != points.last()) points.add(points.first())
                            geofenceFeatures.add(Feature.fromGeometry(Polygon.fromLngLats(listOf(points))))
                        }
                    }
                } else {
                    val center = gf.optJSONObject("geometryCenter") ?: continue
                    val gfRadius = gf.optDouble("geometryRadius", 0.0)
                    if (gfRadius <= 0) continue
                    val coords = center.optJSONArray("coordinates") ?: continue
                    val polygon = createCirclePolygon(coords.getDouble(1), coords.getDouble(0), gfRadius)
                    geofenceFeatures.add(Feature.fromGeometry(polygon))
                }
            }
        }

        val placeFeatures = mutableListOf<Feature>()
        json.optJSONArray("syncedPlaces")?.let { arr ->
            for (i in 0 until arr.length()) {
                val place = arr.optJSONObject(i) ?: continue
                val loc = place.optJSONObject("location") ?: continue
                val coords = loc.optJSONArray("coordinates") ?: continue
                val point = Point.fromLngLat(coords.getDouble(0), coords.getDouble(1))
                placeFeatures.add(Feature.fromGeometry(point))
            }
        }

        val beaconFeatures = mutableListOf<Feature>()
        json.optJSONArray("syncedBeacons")?.let { arr ->
            for (i in 0 until arr.length()) {
                val beacon = arr.optJSONObject(i) ?: continue
                val geo = beacon.optJSONObject("geometry") ?: continue
                val coords = geo.optJSONArray("coordinates") ?: continue
                val point = Point.fromLngLat(coords.getDouble(0), coords.getDouble(1))
                beaconFeatures.add(Feature.fromGeometry(point))
            }
        }

        return SyncRegionData(
            region = FeatureCollection.fromFeatures(regionFeatures),
            geofences = FeatureCollection.fromFeatures(geofenceFeatures),
            places = FeatureCollection.fromFeatures(placeFeatures),
            beacons = FeatureCollection.fromFeatures(beaconFeatures),
        )
    } catch (e: Exception) {
        println("parseSyncRegionData error: $e")
        return emptySyncRegionData
    }
}

@Composable
fun MapView(disabled: Boolean) {
    val client = LocationServices.getFusedLocationProviderClient(LocalContext.current)
    val radarDir = File(LocalContext.current.filesDir, "RadarSDK")

    // TODO: re-enable offline geofences after QA testing
    // val file = File(radarDir, "offlineData.json")
    // var offlineData by remember { mutableStateOf(getFeatureCollection(file)) }
    // val fileObserver = object : FileObserver(file.absolutePath, (CLOSE_WRITE or MODIFY or CREATE)) {
    //     override fun onEvent(event: Int, path: String?) {
    //         offlineData = getFeatureCollection(file)
    //     }
    // }.apply {
    //     startWatching()
    // }

    val syncFile = File(radarDir, "radar_sync_state.json")
    var syncData by remember { mutableStateOf(parseSyncRegionData(syncFile)) }
    val syncFileObserver = object : FileObserver(syncFile.absolutePath, (CLOSE_WRITE or MODIFY or CREATE)) {
        override fun onEvent(event: Int, path: String?) {
            syncData = parseSyncRegionData(syncFile)
        }
    }.apply {
        startWatching()
    }

    val locationEngine = MockableLocationEngine.get(LocalContext.current)
    var mocking by remember { mutableStateOf(false) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, mapViewRef) {
        val observer = LifecycleEventObserver { _, event ->
            val mv = mapViewRef ?: return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_START -> mv.onStart()
                Lifecycle.Event.ON_RESUME -> mv.onResume()
                Lifecycle.Event.ON_PAUSE -> mv.onPause()
                Lifecycle.Event.ON_STOP -> mv.onStop()
                Lifecycle.Event.ON_DESTROY -> mv.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box {
        AndroidView(
            modifier = Modifier.graphicsLayer { alpha = if (disabled) 0f else 1f },
            factory = { context ->
                val style = "radar-default-v1"
                val styleURL = "${getMapHost()}/maps/styles/$style?publishableKey=$PUBLISHABLE_KEY"

                MapLibre.getInstance(context)

                val mapView = MapView(context)
                mapView.onCreate(null)
                mapView.onStart()
                mapView.onResume()
                mapViewRef = mapView

                mapView.getMapAsync { map ->
                    // callback for map done loading
                    map.uiSettings.isLogoEnabled = false

                    // anchor attribution to bottom right
                    map.uiSettings.isTiltGesturesEnabled = false
                    map.uiSettings.attributionGravity = Gravity.END + Gravity.BOTTOM
                    map.uiSettings.setAttributionMargins(0, 0, 24, 24)

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
                        client.setMockMode(true).addOnSuccessListener {
                            val mockLoc = Location("mock").apply {
                                latitude = point.latitude
                                longitude = point.longitude
                                accuracy = 5f
                                time = System.currentTimeMillis()
                                elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                            }
                            client.setMockLocation(mockLoc)
                            locationEngine.mockedLocation = mockLoc
                            println("Successfully set mock $mockLoc")
                            mocking = true
                        }.addOnFailureListener {
                            println("Failed to set mock")
                        }
                        return@addOnMapLongClickListener true
                    }

                    map.setStyle(styleURL) { style ->

                        // TODO: re-enable offline geofences after QA testing
                        // val offlineGeofences = GeoJsonSource("offlineGeofences")
                        // offlineGeofences.setGeoJson(offlineData)
                        // style.addSource(offlineGeofences)
                        // style.addLayer(FillLayer("offlineCircles", "offlineGeofences").apply {
                        //     setProperties(
                        //         PropertyFactory.fillOpacity(0.1f),
                        //         PropertyFactory.fillColor(Color.Green.toArgb()),
                        //     )
                        // })
                        // style.addLayer(LineLayer("offlineOutline", "offlineGeofences").apply {
                        //     setProperties(
                        //         PropertyFactory.lineColor(Color.Green.toArgb()),
                        //         PropertyFactory.lineWidth(1f)
                        //     )
                        // })

                        val syncRegionSource = GeoJsonSource("syncRegion")
                        syncRegionSource.setGeoJson(syncData.region)
                        style.addSource(syncRegionSource)
                        style.addLayer(FillLayer("syncRegionFill", "syncRegion").apply {
                            setProperties(
                                PropertyFactory.fillOpacity(0.06f),
                                PropertyFactory.fillColor(Color.Blue.toArgb()),
                            )
                        })
                        style.addLayer(LineLayer("syncRegionOutline", "syncRegion").apply {
                            setProperties(
                                PropertyFactory.lineColor(Color.Blue.toArgb()),
                                PropertyFactory.lineWidth(1.5f),
                                PropertyFactory.lineDasharray(arrayOf(4f, 3f)),
                            )
                        })

                        val syncedGeofencesSource = GeoJsonSource("syncedGeofences")
                        syncedGeofencesSource.setGeoJson(syncData.geofences)
                        style.addSource(syncedGeofencesSource)
                        style.addLayer(FillLayer("syncedGeofencesFill", "syncedGeofences").apply {
                            setProperties(
                                PropertyFactory.fillOpacity(0.15f),
                                PropertyFactory.fillColor(Color(0xFF6A0DAD).toArgb()),
                            )
                        })
                        style.addLayer(LineLayer("syncedGeofencesOutline", "syncedGeofences").apply {
                            setProperties(
                                PropertyFactory.lineColor(Color(0xFF6A0DAD).toArgb()),
                                PropertyFactory.lineWidth(1f),
                            )
                        })

                        val syncedPlacesSource = GeoJsonSource("syncedPlaces")
                        syncedPlacesSource.setGeoJson(syncData.places)
                        style.addSource(syncedPlacesSource)
                        style.addLayer(
                            org.maplibre.android.style.layers.CircleLayer("syncedPlacesCircles", "syncedPlaces").apply {
                            setProperties(
                                PropertyFactory.circleRadius(6f),
                                PropertyFactory.circleColor(Color(0xFFFF6600).toArgb()),
                                PropertyFactory.circleOpacity(0.8f),
                                PropertyFactory.circleStrokeWidth(1.5f),
                                PropertyFactory.circleStrokeColor(Color.White.toArgb()),
                            )
                        })

                        val syncedBeaconsSource = GeoJsonSource("syncedBeacons")
                        syncedBeaconsSource.setGeoJson(syncData.beacons)
                        style.addSource(syncedBeaconsSource)
                        style.addLayer(
                            org.maplibre.android.style.layers.CircleLayer("syncedBeaconsCircles", "syncedBeacons").apply {
                            setProperties(
                                PropertyFactory.circleRadius(5f),
                                PropertyFactory.circleColor(Color.Cyan.toArgb()),
                                PropertyFactory.circleOpacity(0.8f),
                                PropertyFactory.circleStrokeWidth(1.5f),
                                PropertyFactory.circleStrokeColor(Color.White.toArgb()),
                            )
                        })

                        val locationComponent = map.locationComponent
                        // Build LocationComponent activation options
                        val locCompOpts = LocationComponentActivationOptions.builder(context, style)
                            .useDefaultLocationEngine(false)
                            .locationEngine(LocationEngineProxy(locationEngine))
                            .build()
                        locationComponent.activateLocationComponent(locCompOpts)
                        if (ActivityCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) != PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
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

                    // (style.getSource("offlineGeofences") as? GeoJsonSource)?.setGeoJson(offlineData)
                    (style.getSource("syncRegion") as? GeoJsonSource)?.setGeoJson(syncData.region)
                    (style.getSource("syncedGeofences") as? GeoJsonSource)?.setGeoJson(syncData.geofences)
                    (style.getSource("syncedPlaces") as? GeoJsonSource)?.setGeoJson(syncData.places)
                    (style.getSource("syncedBeacons") as? GeoJsonSource)?.setGeoJson(syncData.beacons)

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

        if (mocking) {
            Button({
                client.setMockMode(false).addOnSuccessListener {
                    locationEngine.mockedLocation = null
                    mocking = false
                    println("cleared mock")
                }.addOnFailureListener {
                    println("Unable to stop mocking")
                }
            }) {
                Text("reset mocking")
            }
        }
    }
}
