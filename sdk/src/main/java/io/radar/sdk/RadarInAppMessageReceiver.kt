package io.radar.sdk

import io.radar.sdk.model.RadarInAppMessagePayload

/**
 * Interface for handling in-app message lifecycle events.
 * Provides callbacks for different stages of in-app message display and interaction.
 */
interface RadarInAppMessageReceiver {
    

    // more of a lifecycle hook 
    fun beforeInAppMessageDisplayed(payload: RadarInAppMessagePayload) 

    // to show, enqueue or discard (what the state is also trying to do)
    fun shouldDisplayInAppMessage(payload: RadarInAppMessagePayload): RadarInAppMessageOperation
    
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