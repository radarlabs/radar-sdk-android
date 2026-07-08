package io.radar.example.map.overlays

import android.content.Context
import android.location.Location
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.ui.graphics.vector.ImageVector
import io.radar.example.store.TripDestination
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

/** Renders places from `Radar.searchPlaces(...)` as teal dots. */
class NearbyPlacesSource : MapOverlaySource() {
    override val id = "nearbyPlaces"
    override val label = "Nearby places"
    override val icon: ImageVector = Icons.Filled.Place

    private val srcId = "src_$id"
    private val circleLayerId = "layer_${id}_circle"

    override val layerIds = listOf(circleLayerId)
    override val hitLayerIds = listOf(circleLayerId)

    private var features: FeatureCollection = FeatureCollection.fromFeatures(emptyArray<Feature>())

    override fun attach(style: Style) {
        if (style.getSource(srcId) == null) style.addSource(GeoJsonSource(srcId, features))
        if (style.getLayer(circleLayerId) == null) {
            style.addLayer(
                CircleLayer(circleLayerId, srcId).withProperties(
                    PropertyFactory.circleColor(OverlayColors.Teal),
                    PropertyFactory.circleRadius(6f),
                    PropertyFactory.circleStrokeColor(0xFFFFFFFF.toInt()),
                    PropertyFactory.circleStrokeWidth(1.5f),
                ),
            )
        }
    }

    override suspend fun load(context: Context, near: LatLng?) {
        val ll = near ?: return
        val location = Location("overlay").apply {
            latitude = ll.latitude
            longitude = ll.longitude
        }
        val places = searchPlacesNear(location, radius = 5000, limit = 100)
        features = FeatureCollection.fromFeatures(
            places.map { place ->
                Feature.fromGeometry(Point.fromLngLat(place.location.longitude, place.location.latitude)).apply {
                    addStringProperty("id", place._id)
                    addStringProperty("name", place.name)
                    addNumberProperty("lat", place.location.latitude)
                    addNumberProperty("lng", place.location.longitude)
                }
            },
        )
    }

    override fun render(style: Style) {
        (style.getSource(srcId) as? GeoJsonSource)?.setGeoJson(features)
    }

    override fun hitTest(feature: Feature): TripDestination? {
        val lat = feature.getNumberProperty("lat")?.toDouble() ?: return null
        val lng = feature.getNumberProperty("lng")?.toDouble() ?: return null
        return TripDestination.Coordinate(
            coordinate = LatLng(lat, lng),
            title = feature.getStringProperty("name") ?: "Place",
        )
    }
}
