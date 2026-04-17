package io.radar.example

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.FileObserver
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.location.LocationServices
import io.radar.sdk.Radar
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.engine.LocationEngineProxy
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.layers.CircleLayer
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

private const val TAG = "QaMapView"
private const val FALLBACK_HOST = "https://api.radar-staging.com"

private fun getMapHost(): String = Radar.getHost() ?: FALLBACK_HOST

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
    if (coordinates.isNotEmpty()) coordinates.add(coordinates[0])
    return Polygon.fromLngLats(listOf(coordinates))
}

/**
 * Snapshot of the SDK's persisted sync state
 * (`<filesDir>/RadarSDK/radar_sync_state.json`), decomposed into GeoJSON
 * feature collections the QA map can render directly.
 */
data class SyncRegionData(
    val region: FeatureCollection,
    val geofences: FeatureCollection,
    val places: FeatureCollection,
    val beacons: FeatureCollection,
    val fileExists: Boolean,
    val debug: String,
)

private fun emptySyncRegionData(debug: String) = SyncRegionData(
    region = FeatureCollection.fromFeatures(emptyList()),
    geofences = FeatureCollection.fromFeatures(emptyList()),
    places = FeatureCollection.fromFeatures(emptyList()),
    beacons = FeatureCollection.fromFeatures(emptyList()),
    fileExists = false,
    debug = debug,
)

fun parseSyncRegionData(file: File): SyncRegionData {
    if (!file.exists()) {
        return emptySyncRegionData("missing: ${file.absolutePath}")
    }

    return try {
        val json = JSONObject(file.readText())

        val regionFeatures = mutableListOf<Feature>()
        val centerObj = json.optJSONObject("syncedRegionCenter")
        val radius = json.optDouble("syncedRegionRadius", 0.0)
        if (centerObj != null && radius > 0) {
            val coords = centerObj.optJSONArray("coordinates")
            if (coords != null && coords.length() >= 2) {
                regionFeatures.add(
                    Feature.fromGeometry(
                        createCirclePolygon(coords.getDouble(1), coords.getDouble(0), radius)
                    )
                )
            }
        }

        val geofenceFeatures = mutableListOf<Feature>()
        json.optJSONArray("syncedGeofences")?.let { arr ->
            for (i in 0 until arr.length()) {
                val gf = arr.optJSONObject(i) ?: continue
                val gfType = gf.optString("type", "")

                if (gfType.equals("Polygon", ignoreCase = true) ||
                    gfType.equals("isochrone", ignoreCase = true)
                ) {
                    // Polygon / isochrone geofences: coordinates is an array of rings.
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
                            geofenceFeatures.add(
                                Feature.fromGeometry(Polygon.fromLngLats(listOf(points)))
                            )
                        }
                    }
                } else {
                    // Circle geofences: geometryCenter + geometryRadius.
                    val center = gf.optJSONObject("geometryCenter") ?: continue
                    val gfRadius = gf.optDouble("geometryRadius", 0.0)
                    if (gfRadius <= 0) continue
                    val coords = center.optJSONArray("coordinates") ?: continue
                    if (coords.length() < 2) continue
                    geofenceFeatures.add(
                        Feature.fromGeometry(
                            createCirclePolygon(coords.getDouble(1), coords.getDouble(0), gfRadius)
                        )
                    )
                }
            }
        }

        val placeFeatures = mutableListOf<Feature>()
        json.optJSONArray("syncedPlaces")?.let { arr ->
            for (i in 0 until arr.length()) {
                val place = arr.optJSONObject(i) ?: continue
                val loc = place.optJSONObject("location") ?: continue
                val coords = loc.optJSONArray("coordinates") ?: continue
                if (coords.length() < 2) continue
                placeFeatures.add(
                    Feature.fromGeometry(
                        Point.fromLngLat(coords.getDouble(0), coords.getDouble(1))
                    )
                )
            }
        }

        val beaconFeatures = mutableListOf<Feature>()
        json.optJSONArray("syncedBeacons")?.let { arr ->
            for (i in 0 until arr.length()) {
                val beacon = arr.optJSONObject(i) ?: continue
                val geo = beacon.optJSONObject("geometry") ?: continue
                val coords = geo.optJSONArray("coordinates") ?: continue
                if (coords.length() < 2) continue
                beaconFeatures.add(
                    Feature.fromGeometry(
                        Point.fromLngLat(coords.getDouble(0), coords.getDouble(1))
                    )
                )
            }
        }

        val debug = "region=${if (regionFeatures.isNotEmpty()) "yes" else "no"} " +
            "geofences=${geofenceFeatures.size} " +
            "places=${placeFeatures.size} " +
            "beacons=${beaconFeatures.size}"
        Log.d(TAG, "Parsed sync state: $debug (${file.absolutePath})")

        SyncRegionData(
            region = FeatureCollection.fromFeatures(regionFeatures),
            geofences = FeatureCollection.fromFeatures(geofenceFeatures),
            places = FeatureCollection.fromFeatures(placeFeatures),
            beacons = FeatureCollection.fromFeatures(beaconFeatures),
            fileExists = true,
            debug = debug,
        )
    } catch (e: Exception) {
        Log.w(TAG, "parseSyncRegionData error", e)
        emptySyncRegionData("parse error: ${e.message}")
    }
}

@Composable
fun MapView(disabled: Boolean) {
    val context = LocalContext.current
    val client = LocationServices.getFusedLocationProviderClient(context)
    val syncFile = remember { File(File(context.filesDir, "RadarSDK"), "radar_sync_state.json") }

    var syncData by remember { mutableStateOf(parseSyncRegionData(syncFile)) }

    // FileObserver gives us immediate updates when the SDK rewrites the sync state.
    DisposableEffect(syncFile) {
        val observer = object : FileObserver(
            syncFile.absolutePath,
            (CLOSE_WRITE or MODIFY or CREATE)
        ) {
            override fun onEvent(event: Int, path: String?) {
                syncData = parseSyncRegionData(syncFile)
            }
        }
        observer.startWatching()
        onDispose { observer.stopWatching() }
    }

    val locationEngine = MockableLocationEngine.get(context)
    var mocking by remember { mutableStateOf(false) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    // Proxy the composable's lifecycle to the MapLibre MapView. Without this the
    // map surface never fully activates and style layers can silently fail to render.
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
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box {
        AndroidView(
            modifier = Modifier.graphicsLayer { alpha = if (disabled) 0f else 1f },
            factory = { ctx ->
                val styleName = "radar-default-v1"
                val styleURL = "${getMapHost()}/maps/styles/$styleName?publishableKey=$PUBLISHABLE_KEY"

                MapLibre.getInstance(ctx)
                val mapView = MapView(ctx)
                mapView.onCreate(null)
                mapView.onStart()
                mapView.onResume()
                mapViewRef = mapView

                mapView.getMapAsync { map ->
                    map.uiSettings.isLogoEnabled = false
                    map.uiSettings.isTiltGesturesEnabled = false
                    map.uiSettings.attributionGravity = Gravity.END + Gravity.BOTTOM
                    map.uiSettings.setAttributionMargins(0, 0, 24, 24)

                    map.addOnMapLongClickListener { point ->
                        if (ActivityCompat.checkSelfPermission(
                                ctx,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                                ctx,
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
                            mocking = true
                        }
                        true
                    }

                    map.setStyle(styleURL) { style ->
                        // Sync region — dashed blue outline, light fill. Drawn first so
                        // geofences/places/beacons render above it.
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

                        // Synced geofences — purple fill + outline.
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

                        // Synced places — orange dots.
                        val syncedPlacesSource = GeoJsonSource("syncedPlaces")
                        syncedPlacesSource.setGeoJson(syncData.places)
                        style.addSource(syncedPlacesSource)
                        style.addLayer(CircleLayer("syncedPlacesCircles", "syncedPlaces").apply {
                            setProperties(
                                PropertyFactory.circleRadius(6f),
                                PropertyFactory.circleColor(Color(0xFFFF6600).toArgb()),
                                PropertyFactory.circleOpacity(0.8f),
                                PropertyFactory.circleStrokeWidth(1.5f),
                                PropertyFactory.circleStrokeColor(Color.White.toArgb()),
                            )
                        })

                        // Synced beacons — cyan dots.
                        val syncedBeaconsSource = GeoJsonSource("syncedBeacons")
                        syncedBeaconsSource.setGeoJson(syncData.beacons)
                        style.addSource(syncedBeaconsSource)
                        style.addLayer(CircleLayer("syncedBeaconsCircles", "syncedBeacons").apply {
                            setProperties(
                                PropertyFactory.circleRadius(5f),
                                PropertyFactory.circleColor(Color.Cyan.toArgb()),
                                PropertyFactory.circleOpacity(0.8f),
                                PropertyFactory.circleStrokeWidth(1.5f),
                                PropertyFactory.circleStrokeColor(Color.White.toArgb()),
                            )
                        })

                        val locationComponent = map.locationComponent
                        val locCompOpts = LocationComponentActivationOptions.builder(ctx, style)
                            .useDefaultLocationEngine(false)
                            .locationEngine(LocationEngineProxy(locationEngine))
                            .build()
                        locationComponent.activateLocationComponent(locCompOpts)

                        val hasLocationPermission = ActivityCompat.checkSelfPermission(
                            ctx, Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED ||
                            ActivityCompat.checkSelfPermission(
                                ctx, Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                        if (!hasLocationPermission) return@setStyle
                        locationComponent.isLocationComponentEnabled = true

                        val location = locationComponent.lastKnownLocation ?: return@setStyle
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
                    (style.getSource("syncRegion") as? GeoJsonSource)?.setGeoJson(syncData.region)
                    (style.getSource("syncedGeofences") as? GeoJsonSource)?.setGeoJson(syncData.geofences)
                    (style.getSource("syncedPlaces") as? GeoJsonSource)?.setGeoJson(syncData.places)
                    (style.getSource("syncedBeacons") as? GeoJsonSource)?.setGeoJson(syncData.beacons)

                    map.uiSettings.isScrollGesturesEnabled = !disabled
                    map.uiSettings.isZoomGesturesEnabled = !disabled
                    map.uiSettings.isRotateGesturesEnabled = !disabled
                }
            },
        )

        // QA overlay and reset-mocking button only paint on the Map tab so they
        // don't bleed through onto the Logs / Tests tabs when the map is hidden
        // (the AndroidView gets alpha=0 above, but sibling Composables do not).
        if (!disabled) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (syncData.fileExists) "sync state: ${syncData.debug}"
                           else "sync state unavailable",
                    color = Color.White,
                )
                Text(
                    text = syncFile.absolutePath,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }

            if (mocking) {
                Button(
                    onClick = {
                        client.setMockMode(false).addOnSuccessListener {
                            locationEngine.mockedLocation = null
                            mocking = false
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                ) {
                    Text("reset mocking")
                }
            }
        }
    }
}
