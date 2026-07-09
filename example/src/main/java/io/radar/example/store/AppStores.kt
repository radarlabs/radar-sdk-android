package io.radar.example.store

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import io.radar.example.map.overlays.MapOverlayRegistry

/**
 * The app's observable stores, instantiated once in `MainActivity` and injected app-wide
 * via [CompositionLocal]s.
 */
class AppStores(
    val logStore: LogStore,
    val settingsStore: SettingsStore,
    val permissionsStore: PermissionsStore,
    val tripBuilderStore: TripBuilderStore,
    val mapOverlayRegistry: MapOverlayRegistry,
)

val LocalLogStore = staticCompositionLocalOf<LogStore> { error("LogStore not provided") }
val LocalSettingsStore = staticCompositionLocalOf<SettingsStore> { error("SettingsStore not provided") }
val LocalPermissionsStore = staticCompositionLocalOf<PermissionsStore> { error("PermissionsStore not provided") }
val LocalTripBuilderStore = staticCompositionLocalOf<TripBuilderStore> { error("TripBuilderStore not provided") }
val LocalMapOverlayRegistry = staticCompositionLocalOf<MapOverlayRegistry> { error("MapOverlayRegistry not provided") }

@Composable
fun ProvideStores(stores: AppStores, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalLogStore provides stores.logStore,
        LocalSettingsStore provides stores.settingsStore,
        LocalPermissionsStore provides stores.permissionsStore,
        LocalTripBuilderStore provides stores.tripBuilderStore,
        LocalMapOverlayRegistry provides stores.mapOverlayRegistry,
        content = content,
    )
}
