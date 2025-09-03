package io.radar.sdk

import android.content.Context
import android.view.View
import io.radar.sdk.model.RadarInAppMessage

/**
 * Interface for handling in-app message lifecycle events.
 * Provides callbacks for different stages of in-app message display and interaction.
 */
interface RadarInAppMessageReceiver {

    fun onNewInAppMessage(inAppMessage: RadarInAppMessage) {
        Radar.showInAppMessage(inAppMessage)
    }
    
    /**
     * Called when an in-app message is dismissed by the user.
     * Provides an opportunity to perform cleanup or tracking.
     */
    fun onInAppMessageDismissed(inAppMessage: RadarInAppMessage) {
        return
    }
    
    /**
     * Called when the user clicks the action button in an in-app message.
     * 
     * @param inAppMessage The payload containing the message data
     */
    fun onInAppMessageButtonClicked(inAppMessage: RadarInAppMessage) {
        return 
        
    }

    fun createInAppMessageView(
        context: Context, 
        inAppMessage: RadarInAppMessage, 
        onDismissListener: (() -> Unit)? = null, 
        onInAppMessageButtonClicked: (() -> Unit)? = null,
        onViewReady: (View) -> Unit
    ) {
        val inAppMessageView = RadarInAppMessageView(context)
        inAppMessageView.initialize(
            inAppMessage, 
            onDismissListener, 
            onInAppMessageButtonClicked,
            onViewReady
        )
    }
}
