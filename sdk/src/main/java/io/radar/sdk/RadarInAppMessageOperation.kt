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
     * Enqueue the in-app message to be displayed later.
     */
    ENQUEUE,
    
    /**
     * Discard the in-app message completely.
     */
    DISCARD
} 