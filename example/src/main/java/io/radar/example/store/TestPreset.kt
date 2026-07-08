package io.radar.example.store

import io.radar.sdk.RadarTrackingOptions

/**
 * A bundled scenario for functional testing. Applying one writes identity + tracking
 * state through to the SDK in a single tap (see [SettingsStore.apply]). Mirrors the iOS
 * `TestPreset`.
 */
data class TestPreset(
    val id: String,
    val name: String,
    val summary: String,
    val userId: String?,
    val metadata: Map<String, String>,
    val trackingAction: TrackingAction,
) {
    sealed class TrackingAction {
        /** Don't touch the SDK's tracking state. */
        object LeaveUnchanged : TrackingAction()

        /** Call `Radar.startTracking(options)`. */
        data class Start(val options: RadarTrackingOptions) : TrackingAction()

        /** Call `Radar.stopTracking()`. */
        object Stop : TrackingAction()
    }

    companion object {
        /** All built-in presets, in display order. */
        val all: List<TestPreset>
            get() = listOf(default, continuous, responsive, efficient)

        /** Clean slate: clear identity, stop tracking. */
        val default = TestPreset(
            id = "default",
            name = "Default",
            summary = "Clear identity and stop tracking.",
            userId = null,
            metadata = emptyMap(),
            trackingAction = TrackingAction.Stop,
        )

        val continuous = TestPreset(
            id = "continuous",
            name = "Continuous",
            summary = "Updates every ~30s. Highest battery cost.",
            userId = "test-continuous",
            metadata = mapOf("preset" to "continuous"),
            trackingAction = TrackingAction.Start(RadarTrackingOptions.CONTINUOUS),
        )

        val responsive = TestPreset(
            id = "responsive",
            name = "Responsive",
            summary = "Wakes on movement. Moderate battery cost.",
            userId = "test-responsive",
            metadata = mapOf("preset" to "responsive"),
            trackingAction = TrackingAction.Start(RadarTrackingOptions.RESPONSIVE),
        )

        val efficient = TestPreset(
            id = "efficient",
            name = "Efficient",
            summary = "Lowest battery cost.",
            userId = "test-efficient",
            metadata = mapOf("preset" to "efficient"),
            trackingAction = TrackingAction.Start(RadarTrackingOptions.EFFICIENT),
        )
    }
}
