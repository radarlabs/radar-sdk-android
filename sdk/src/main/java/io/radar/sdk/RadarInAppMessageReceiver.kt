package io.radar.sdk

import io.radar.sdk.model.RadarInAppMessagePayload

/**
 * Interface for handling in-app message lifecycle events.
 * Provides callbacks for different stages of in-app message display and interaction.
 */
interface RadarInAppMessageReceiver {
    
    /**
     * Called before an in-app message is displayed.
     * Allows the delegate to control whether the message should be shown.
     * 
     * @param payload The payload containing the message data
     * @return RadarInAppMessageOperation indicating how to proceed with the message
     */
    fun beforeInAppMessageDisplayed(payload: RadarInAppMessagePayload): RadarInAppMessageOperation
    
    /**
     * Called when an in-app message is dismissed by the user.
     * Provides an opportunity to perform cleanup or tracking.
     */
    fun onInAppMessageDismissed(payload: RadarInAppMessagePayload)
    
    /**
     * Called when the user clicks the action button in an in-app message.
     * 
     * @param payload The payload containing the message data
     */
    fun onInAppMessageButtonClicked(payload: RadarInAppMessagePayload)
} 