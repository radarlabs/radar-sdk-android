package io.radar.example.map.overlays

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.ui.graphics.vector.ImageVector
import io.radar.example.store.TripDestination
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection

/**
 * Renders the SDK's synced offline geofences from `filesDir/RadarSDK/offlineData.json` as
 * purple tappable shapes. (The internal `RadarSyncManager` isn't accessible from the example
 * module, so this reads the offline data file directly.)
 */
class SyncedGeofencesSource : MapOverlaySource() {
    override val id = "syncedGeofences"
    override val label = "Synced geofences (offline)"
    override val icon: ImageVector = Icons.Filled.CloudDone

    private val srcId = "src_$id"
    private val fillLayerId = "layer_${id}_fill"
    private val lineLayerId = "layer_${id}_line"

    override val layerIds = listOf(fillLayerId, lineLayerId)
    override val hitLayerIds = listOf(fillLayerId)

    private var features: FeatureCollection = FeatureCollection.fromFeatures(emptyArray<Feature>())

    override fun attach(style: Style) {
        if (style.getSource(srcId) == null) style.addSource(GeoJsonSource(srcId, features))
        if (style.getLayer(fillLayerId) == null) {
            style.addLayer(
                FillLayer(fillLayerId, srcId).withProperties(
                    PropertyFactory.fillColor(OverlayColors.Purple),
                    PropertyFactory.fillOpacity(0.12f),
                ),
            )
        }
        if (style.getLayer(lineLayerId) == null) {
            style.addLayer(
                LineLayer(lineLayerId, srcId).withProperties(
                    PropertyFactory.lineColor(OverlayColors.Purple),
                    PropertyFactory.lineWidth(1.5f),
                ),
            )
        }
    }

    override suspend fun load(context: Context, near: LatLng?) {
        val geofences = readOfflineGeofences(context)
        features = FeatureCollection.fromFeatures(
            geofences.map { g ->
                Feature.fromGeometry(circlePolygon(g.latitude, g.longitude, g.radius)).apply {
                    addStringProperty("id", g.id)
                    g.tag?.let { addStringProperty("tag", it) }
                    g.externalId?.let { addStringProperty("externalId", it) }
                    addStringProperty("description", g.description)
                    addNumberProperty("centerLat", g.latitude)
                    addNumberProperty("centerLng", g.longitude)
                }
            },
        )
    }

    override fun render(style: Style) {
        (style.getSource(srcId) as? GeoJsonSource)?.setGeoJson(features)
    }

    override fun hitTest(feature: Feature): TripDestination? {
        val gid = feature.getStringProperty("id") ?: return null
        val lat = feature.getNumberProperty("centerLat")?.toDouble() ?: return null
        val lng = feature.getNumberProperty("centerLng")?.toDouble() ?: return null
        return TripDestination.Geofence(
            id = gid,
            tag = feature.getStringProperty("tag"),
            externalId = feature.getStringProperty("externalId"),
            title = feature.getStringProperty("description") ?: "Geofence",
            coordinate = LatLng(lat, lng),
        )
    }
}
