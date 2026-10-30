# Radar SDK Android example app — architecture

This is the sample app demonstrating the Radar Android SDK. It's a Jetpack Compose +
Material3 app organized into a store/service layer, a pluggable map-overlay registry, and
feature-per-folder UI.

## Package organization

`example/src/main/java/io/radar/example/`

```
MainActivity.kt        # Lifecycle, SDK init, store wiring, overlay-source registration
MainView.kt            # 3-tab shell (Map / Debug / Tests); map stays composed underneath
MyInAppMessageReceiver # In-app message callbacks → LogStore
MockableLocationEngine # MapLibre location engine with a mock override (long-press mocking)
Utils.kt               # Radar status/event/source string formatters (reused by the console)

store/                 # Observable stores (one per concern) + value types
  LogStore.kt          # The single RadarReceiver + unified console source-of-truth
  ConsoleEntry.kt      # One console row (+ Kind enum)
  SettingsStore.kt     # SDK identity/tracking snapshot + persisted environment and per-environment keys
  RadarEnvironment.kt  # Production / Staging / Local: the only place API hosts are defined
  KeyCheck.kt          # Probes /v1/config on each environment to see where a key is valid
  PermissionsStore.kt  # Runtime-permission status snapshot
  TripBuilderStore.kt  # Map-driven trip selection + active-trip mirror + visualization
  TestPreset.kt        # Bundled tracking presets
  TripDestination.kt / TripEventMarker.kt  # Cross-cutting value types
  AppStores.kt         # CompositionLocals + ProvideStores (app-wide store injection)

components/            # Reusable primitives: ActionButton, TogglePanel, ControlRow, FieldEditor, EnvironmentBadge
console/               # Debug tab: ConsoleView, ConsoleEntryRow, ConsoleKindUi (icon/tint)
map/                   # Map tab: MapScreen + trip-builder UI + MockLocationController
  overlays/            # Pluggable map sources: MapOverlaySource + MapOverlayRegistry + sources
tests/                 # Tests tab: TestsView, RecentActivityCard
  panels/              # 6 collapsible panels of ActionButtons
  settings/            # Settings sheet sections (behind the Tests-tab gear)
theme/                 # Radar-branded Material3 theme
```

## Conventions

- **One store per concern.** Don't merge stores. Cross-store coordination lives in
  `MainActivity` (registration) or in store `bind(...)` methods (e.g. `TripBuilderStore.bind`).
- **One RadarReceiver.** `LogStore` is the *only* receiver — passed to `Radar.initialize`.
  Everything else reads `LogStore.entries` (UI) or subscribes via `LogStore.onEvents` /
  `onLocation` (non-UI, e.g. the trip builder). Don't call `Radar.setReceiver` elsewhere.
- **All console output flows through `LogStore`.** Don't `Log.v` from UI — use
  `logStore.write*`. `ActionButton` auto-logs its own tap, so the Tests tab is
  self-documenting; API completion handlers log their result via `logStore.writeStatus(...)`.
- **Map overlays are plugins.** Adding a layer = one new `MapOverlaySource` subclass +
  one `register(...)` line in `MainActivity`. Trip-related sources are `isTripModeWhitelisted`
  (force-render during a trip) and `userToggleable = false` (hidden from the layer picker).
- **Trip lifecycle lives in `TripBuilderStore`.** New trip features hang off that store
  rather than mirroring `Radar.getTrip()` elsewhere.

## Platform notes

- **Map:** MapLibre with Radar vector tiles; overlays use GeoJSON sources + layers +
  `queryRenderedFeatures` hit-testing.
- **3 tabs:** Map / Debug / Tests.
- **Synced data** comes from `filesDir/RadarSDK/offlineData.json` (the internal
  `RadarSyncManager` isn't visible to the example module).
- **Tracking-options breakdown** uses the public `Radar.getTrackingOptions()` JSON
  (the server-driven `RadarSdkConfiguration` is `internal`).
- **Trip progress** surfaces through the custom foreground-service notification configured in
  `MainActivity`.

## Setup

Pick the environment and its publishable key in the Tests-tab settings gear (Environment →
Apply & restart). Don't hardcode hosts or edit the SDK's default hosts: `RadarEnvironment` writes
the SDK's host overrides at launch, and the environment badge plus the key check show what the
app is actually talking to.

Default keys can also be set in `MainActivity` (`DEFAULT_PUBLISHABLE_KEY` for production,
`DEFAULT_STAGING_PUBLISHABLE_KEY` for staging). The committed values are the `prj_live_pk_...`
placeholder: the `radar-keys` git filter (`hooks/clean-filter`) strips real keys on commit and
saves them to `.radar-env`.
