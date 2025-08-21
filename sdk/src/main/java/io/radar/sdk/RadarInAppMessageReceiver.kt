package io.radar.sdk
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import androidx.core.net.toUri

import io.radar.sdk.model.RadarInAppMessage

/**
 * Interface for handling in-app message lifecycle events.
 * Provides callbacks for different stages of in-app message display and interaction.
 */
interface RadarInAppMessageReceiver {

    val activity: Activity?

    // to show, enqueue or discard (what the state is also trying to do)
    fun onNewInAppMessage(inAppMessage: RadarInAppMessage): RadarInAppMessageOperation {
        return RadarInAppMessageOperation.DISPLAY
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
        val activity = activity
        if (inAppMessage.button?.deepLink != null && inAppMessage.button.deepLink != "null" && inAppMessage.button.deepLink.isNotBlank() && activity != null) {
            inAppMessage.button.deepLink.let { deepLink ->
                try {
                    val uri = deepLink.toUri()
                    Radar.logger.d("Opening URL: $deepLink -> URI: $uri")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    activity.startActivity(intent)
                } catch (e: Exception) {
                    // Handle invalid URL or no app to handle the intent
                    Radar.logger.e("Error opening URL '$deepLink': ${e.message}")
                }
            }
        } else {
            Radar.logger.d("Button URL is null or 'null' string, skipping URL opening")
        }
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