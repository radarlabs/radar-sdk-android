package io.radar.example.map.overlays

import android.content.Context
import androidx.compose.ui.graphics.vector.ImageVector
import io.radar.example.store.TripDestination
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.geojson.Feature

/**
 * A single producer of map content that can be toggled on/off independently, built on
 * MapLibre's source+layer model.
 *
 * Each source owns a set of [GeoJsonSource][org.maplibre.android.style.sources.GeoJsonSource]s
 * and layers on the map [Style]. The lifecycle:
 * 1. [attach] — add empty sources + layers to a freshly-loaded style (idempotent).
 * 2. [load] — fetch fresh data into an internal cache (async for `Radar.search*`, no-op for
 *    state-driven trip sources).
 * 3. [render] — push the cached data to the style's sources.
 * 4. [setVisible] — toggle layer visibility without refetching.
 *
 * Tappable sources declare [hitLayerIds]; the map queries rendered features on those layers
 * and calls [hitTest] to turn a tapped feature into a [TripDestination].
 */
abstract class MapOverlaySource {
    abstract val id: String
    abstract val label: String
    abstract val icon: ImageVector

    /** Keep rendering while a trip is active (trip mode suppresses all non-whitelisted sources). */
    open val isTripModeWhitelisted: Boolean = false

    /** Whether this source appears in the layer-picker sheet. Trip sources are auto-managed. */
    open val userToggleable: Boolean = true

    /** All layer ids this source manages, toggled together by [setVisible]. */
    abstract val layerIds: List<String>

    /** Layers queried on map tap for [hitTest]. Subset of [layerIds]. */
    open val hitLayerIds: List<String> = emptyList()

    abstract fun attach(style: Style)

    open suspend fun load(context: Context, near: LatLng?) {}

    abstract fun render(style: Style)

    open fun setVisible(style: Style, visible: Boolean) {
        val visibility = if (visible) Property.VISIBLE else Property.NONE
        layerIds.forEach { layerId ->
            style.getLayer(layerId)?.setProperties(PropertyFactory.visibility(visibility))
        }
    }

    open fun hitTest(feature: Feature): TripDestination? = null
}
