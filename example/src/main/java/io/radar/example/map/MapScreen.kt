package io.radar.example.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.FileObserver
import android.view.Gravity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import io.radar.example.MockableLocationEngine
import io.radar.example.map.overlays.MapOverlayRegistry
import io.radar.example.store.LocalMapOverlayRegistry
import io.radar.example.store.LocalSettingsStore
import io.radar.example.store.LocalTripBuilderStore
import io.radar.example.store.TripBuilderStore
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.engine.LocationEngineProxy
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import java.io.File

private const val HOST = "https://api.radar.io"

/**
 * The Map tab: a full-screen MapLibre map with the overlay registry, an interactive trip
 * builder, a layer picker, and long-press mock-location. Rewrite of the original `MapView`,
 * reusing [MockableLocationEngine] and the offline-data circle helpers.
 *
 * [active] toggles gestures + visibility — the map stays composed under the other tabs
 * (alpha 0, gestures off) so it doesn't reload when switching tabs.
 */
@Composable
fun MapScreen(active: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val registry = LocalMapOverlayRegistry.current
    val tripStore = LocalTripBuilderStore.current
    val settings = LocalSettingsStore.current
    val scope = rememberCoroutineScope()

    val mockController = remember { MockLocationController(context) }
    val styleRef = remember { mutableStateOf<Style?>(null) }
    val mapRef = remember { mutableStateOf<MapLibreMap?>(null) }
    var showPicker by remember { mutableStateOf(false) }

    val publishableKey = settings.resolvedPublishableKey

    // Re-apply source visibility + data whenever a render is requested and the style is ready.
    LaunchedEffect(registry.renderVersion, styleRef.value) {
        val style = styleRef.value ?: return@LaunchedEffect
        registry.sources.forEach { source ->
            val enabled = registry.isActuallyEnabled(source)
            source.setVisible(style, enabled)
            if (enabled) source.render(style)
        }
    }

    // Re-fetch data for enabled sources when the enabled set or trip mode changes.
    LaunchedEffect(registry.enabledIds, registry.isInTripMode, styleRef.value) {
        if (styleRef.value != null) registry.refreshAll(context)
    }

    // Refresh synced-geofence overlay when the SDK writes offline data.
    DisposableEffect(Unit) {
        val file = File(File(context.filesDir, "RadarSDK"), "offlineData.json")
        val observer = object : FileObserver(file.absolutePath, CLOSE_WRITE or MODIFY or CREATE) {
            override fun onEvent(event: Int, path: String?) {
                scope.launch { registry.refreshAll(context) }
            }
        }.apply { startWatching() }
        onDispose { observer.stopWatching() }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = if (active) 1f else 0f },
            factory = { ctx ->
                MapLibre.getInstance(ctx)
                val mapView = MapView(ctx)
                mapView.getMapAsync { map ->
                    mapRef.value = map
                    map.uiSettings.isLogoEnabled = false
                    map.uiSettings.isTiltGesturesEnabled = false
                    map.uiSettings.attributionGravity = Gravity.END or Gravity.BOTTOM
                    map.uiSettings.setAttributionMargins(0, 0, 24, 24)

                    map.addOnMapClickListener { point ->
                        handleMapClick(map, styleRef.value, registry, tripStore, point)
                    }
                    map.addOnMapLongClickListener { point ->
                        if (!hasLocationPermission(ctx)) return@addOnMapLongClickListener false
                        mockController.setMock(point) { loc ->
                            registry.lastLocation = LatLng(loc.latitude, loc.longitude)
                            scope.launch { registry.refreshAll(context) }
                        }
                        true
                    }

                    val styleUrl = "$HOST/maps/styles/radar-default-v1?publishableKey=$publishableKey"
                    map.setStyle(styleUrl) { style ->
                        styleRef.value = style
                        registry.sources.forEach { it.attach(style) }
                        setupLocationComponent(ctx, map, style)

                        val savedCamera = restoreCamera(context)
                        val target = when {
                            savedCamera != null -> {
                                map.cameraPosition = savedCamera
                                savedCamera.target
                            }
                            else -> {
                                val last = map.locationComponent.lastKnownLocation
                                if (last != null) {
                                    val ll = LatLng(last.latitude, last.longitude)
                                    map.cameraPosition = CameraPosition.Builder().target(ll).zoom(13.0).build()
                                    ll
                                } else {
                                    null
                                }
                            }
                        }
                        target?.let { registry.lastLocation = it }
                        map.addOnCameraIdleListener { saveCamera(context, map.cameraPosition) }
                        scope.launch { registry.refreshAll(context) }
                    }
                }
                mapView
            },
            update = { mapView ->
                mapView.getMapAsync { map ->
                    map.uiSettings.isScrollGesturesEnabled = active
                    map.uiSettings.isZoomGesturesEnabled = active
                    map.uiSettings.isRotateGesturesEnabled = active
                }
            },
        )

        if (active) {
            // Floating controls (bottom-right)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SmallFloatingActionButton(onClick = { scope.launch { registry.refreshAll(context) } }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh overlays")
                }
                SmallFloatingActionButton(onClick = { showPicker = true }) {
                    Icon(Icons.Filled.Layers, contentDescription = "Map layers")
                }
            }

            // Reset mocking (top-start)
            if (mockController.mocking) {
                TextButton(
                    onClick = { mockController.clear() },
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                ) {
                    Text("Reset mock location")
                }
            }

            // Trip builder / active trip (bottom, above the tab bar)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            ) {
                when {
                    tripStore.pendingHit != null -> PendingHitCard(tripStore)
                    tripStore.hasActiveTrip -> ActiveTripBar(tripStore)
                    tripStore.selectedDestinations.isNotEmpty() -> BuilderTray(tripStore)
                }
            }
        }
    }

    if (showPicker) {
        OverlayPickerSheet(registry, onDismiss = { showPicker = false })
    }
}

private fun handleMapClick(
    map: MapLibreMap,
    style: Style?,
    registry: MapOverlayRegistry,
    tripStore: TripBuilderStore,
    point: LatLng,
): Boolean {
    if (style == null) return false
    val screen = map.projection.toScreenLocation(point)
    for (source in registry.sources) {
        if (!registry.isActuallyEnabled(source) || source.hitLayerIds.isEmpty()) continue
        val features = map.queryRenderedFeatures(screen, *source.hitLayerIds.toTypedArray())
        val destination = features.firstNotNullOfOrNull { source.hitTest(it) }
        if (destination != null) {
            tripStore.proposeHit(destination)
            return true
        }
    }
    return false
}

@SuppressLint("MissingPermission")
private fun setupLocationComponent(context: Context, map: MapLibreMap, style: Style) {
    val locationComponent = map.locationComponent
    val options = LocationComponentActivationOptions.builder(context, style)
        .useDefaultLocationEngine(false)
        .locationEngine(LocationEngineProxy(MockableLocationEngine.get(context)))
        .build()
    locationComponent.activateLocationComponent(options)
    if (hasLocationPermission(context)) {
        locationComponent.isLocationComponentEnabled = true
    }
}

private fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

private const val MAP_PREFS = "radar_example_map"

private fun saveCamera(context: Context, position: CameraPosition) {
    val target = position.target ?: return
    context.getSharedPreferences(MAP_PREFS, Context.MODE_PRIVATE).edit {
        putFloat("lat", target.latitude.toFloat())
        putFloat("lng", target.longitude.toFloat())
        putFloat("zoom", position.zoom.toFloat())
        putBoolean("has", true)
    }
}

private fun restoreCamera(context: Context): CameraPosition? {
    val prefs = context.getSharedPreferences(MAP_PREFS, Context.MODE_PRIVATE)
    if (!prefs.getBoolean("has", false)) return null
    val lat = prefs.getFloat("lat", 0f).toDouble()
    val lng = prefs.getFloat("lng", 0f).toDouble()
    val zoom = prefs.getFloat("zoom", 13f).toDouble()
    return CameraPosition.Builder().target(LatLng(lat, lng)).zoom(zoom).build()
}
