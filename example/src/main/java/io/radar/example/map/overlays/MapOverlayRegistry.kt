package io.radar.example.map.overlays

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import org.maplibre.android.geometry.LatLng

/**
 * Central registry of map overlay sources. `MapScreen` observes [renderVersion] to know when
 * to re-apply sources to the style. Enabled-state persists across launches.
 *
 * During an active trip ([isInTripMode] = true), only [MapOverlaySource.isTripModeWhitelisted]
 * sources render — the map focuses on the trip.
 */
class MapOverlayRegistry(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    val sources = mutableStateListOf<MapOverlaySource>()

    var enabledIds by mutableStateOf(prefs.getStringSet(KEY_ENABLED, null) ?: DEFAULT_ENABLED)
        private set

    var isInTripMode by mutableStateOf(false)
        private set

    /** Bumped whenever the map should re-apply visibility/data to the style. */
    var renderVersion by mutableIntStateOf(0)
        private set

    var lastLocation: LatLng? = null

    fun register(source: MapOverlaySource) {
        if (sources.none { it.id == source.id }) sources.add(source)
    }

    fun isEnabled(source: MapOverlaySource): Boolean = enabledIds.contains(source.id)

    fun isActuallyEnabled(source: MapOverlaySource): Boolean =
        if (isInTripMode) source.isTripModeWhitelisted else enabledIds.contains(source.id)

    fun toggle(source: MapOverlaySource) {
        enabledIds = if (enabledIds.contains(source.id)) {
            enabledIds - source.id
        } else {
            enabledIds + source.id
        }
        prefs.edit { putStringSet(KEY_ENABLED, enabledIds) }
        requestRender()
    }

    fun setTripMode(active: Boolean) {
        if (isInTripMode != active) {
            isInTripMode = active
            requestRender()
        }
    }

    fun requestRender() {
        renderVersion++
    }

    /** Re-fetch data for all currently-active sources, then request a render. */
    suspend fun refreshAll(context: Context) {
        for (source in sources) {
            if (isActuallyEnabled(source)) {
                source.load(context, lastLocation)
            }
        }
        requestRender()
    }

    companion object {
        private const val PREFS = "radar_example_overlays"
        private const val KEY_ENABLED = "enabledSourceIds"

        // Enabled by default: synced geofences (offline data). Others opt-in via the picker.
        private val DEFAULT_ENABLED = setOf("syncedGeofences")
    }
}
