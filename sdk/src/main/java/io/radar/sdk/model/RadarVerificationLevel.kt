package io.radar.sdk.model

/**
 * The level of a verified location check.
 */
enum class RadarVerificationLevel(val value: String) {
    /** A check that uses device, network, and IP signals, but not the device location. */
    IP("ip"),

    /** A check that uses the device location. */
    PRECISE("precise");

    internal companion object {
        /** Returns null for a missing, non-string, or unknown value. */
        fun fromJson(value: Any?): RadarVerificationLevel? {
            if (value !is String) {
                return null
            }

            return RadarVerificationLevel.entries.firstOrNull { it.value == value }
        }
    }
}
