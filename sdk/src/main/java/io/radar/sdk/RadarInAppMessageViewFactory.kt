package io.radar.sdk

import android.content.Context
import android.view.View

/**
 * Factory class for creating Radar in-app message views.
 * Provides a centralized way to create different types of in-app message views.
 */
class RadarInAppMessageViewFactory(private val context: Context) {

    /**
     * Creates and returns a new RadarInAppMessageBannerView instance.
     * 
     * @return A configured RadarInAppMessageBannerView ready for use
     */
    fun createInAppMessageView(): View {
        return RadarInAppMessageBannerView(context)
    }

    /**
     * Creates and returns a new RadarInAppMessageBannerView instance with custom configuration.
     * 
     * @param title The title text for the banner
     * @param message The message text for the banner
     * @param buttonText The text for the action button
     * @param onDismissListener Optional callback for when the banner is dismissed
     * @return A configured RadarInAppMessageBannerView with the specified content
     */
    fun createInAppMessageView(
        title: String,
        message: String,
        buttonText: String,
        onDismissListener: (() -> Unit)? = null
    ): View {
        return RadarInAppMessageBannerView(context).apply {
            setTitle(title)
            setMessage(message)
            setButtonText(buttonText)
            onDismissListener?.let { setOnDismissListener(it) }
        }
    }
} 