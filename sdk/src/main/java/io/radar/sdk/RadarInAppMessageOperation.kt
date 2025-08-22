package io.radar.sdk

/**
 * Enum representing the possible operations for handling in-app messages.
 * Used by RadarInAppMessageDelegate to control message display behavior.
 */
enum class RadarInAppMessageOperation {
    /**
     * Display the in-app message normally.
     */
    DISPLAY,

    /**
     * Discard the in-app message completely.
     */
    DISCARD
} 