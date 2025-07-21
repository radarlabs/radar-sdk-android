package io.radar.sdk

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.toColorInt
import io.radar.sdk.model.RadarInAppMessagePayload

/**
 * Interface for creating Radar in-app message views.
 * Provides a contract for creating different types of in-app message views.
 */
interface RadarInAppMessageViewFactoryInterface {
    /**
     * Creates and returns a new in-app message banner view instance using a payload.
     * 
     * @param payload The payload containing title, message, and button text
     * @param onDismissListener Optional callback for when the banner is dismissed
     * @return A configured View with the specified content
     */
    fun createInAppMessageView(
        payload: RadarInAppMessagePayload,
        onDismissListener: (() -> Unit)? = null,
        onInAppMessageButtonClicked: (() -> Unit)? = null
    ): View
}

/**
 * Factory class for creating Radar in-app message views.
 * Provides a centralized way to create different types of in-app message views.
 */
class RadarInAppMessageViewFactory(private val context: Context) : RadarInAppMessageViewFactoryInterface {

    /**
     * Creates and returns a new in-app message banner view instance.
     * 
     * @return A configured View ready for use as a banner
     */
    fun createInAppMessageView(): View {
        return createInAppMessageView("Title text", "Description text that might wrap to the next line", "Button")
    }

    /**
     * Creates and returns a new in-app message banner view instance using a payload.
     * 
     * @param payload The payload containing title, message, and button text
     * @param onDismissListener Optional callback for when the banner is dismissed
     * @return A configured View with the specified content
     */
    override fun createInAppMessageView(
        payload: RadarInAppMessagePayload,
        onDismissListener: (() -> Unit)?,
        onInAppMessageButtonClicked: (() -> Unit)?
    ): View {
        return createInAppMessageView(
            title = payload.title,
            message = payload.message,
            buttonText = payload.buttonText,
            onDismissListener = onDismissListener,
            onInAppMessageButtonClicked = onInAppMessageButtonClicked
        )
    }
    
    private fun createInAppMessageView(
        title: String,
        message: String,
        buttonText: String,
        onDismissListener: (() -> Unit)? = null,
        onInAppMessageButtonClicked: (() -> Unit)? = null
    ): View {
        val bannerView = createBannerContainer()
        
        // Create the view hierarchy
        val modalContainer = createModalContainer()
        val titleView = createTitleView(title)
        val messageView = createMessageView(message)
        val actionButton = createActionButton(buttonText, onInAppMessageButtonClicked)
        val dismissButton = createDismissButton(onDismissListener)
        val headerContainer = createHeaderContainer(dismissButton)
        
        // Assemble the view hierarchy
        headerContainer.addView(dismissButton, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.END or Gravity.TOP
        })
        
        modalContainer.addView(headerContainer)
        modalContainer.addView(titleView)
        modalContainer.addView(messageView)
        modalContainer.addView(actionButton)
        
        bannerView.addView(modalContainer)
        
        return bannerView
    }
    
    private fun createBannerContainer(): FrameLayout {
        return FrameLayout(context).apply {
            // Set layout params to be 75% of screen width and centered
            layoutParams = FrameLayout.LayoutParams(
                (context.resources.displayMetrics.widthPixels * 0.75).toInt(),
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        }
    }
    
    @SuppressLint("SetTextI18n")
    private fun createModalContainer(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor("#FFFFFF".toColorInt())
            setPadding(48, 40, 48, 48)
            //elevation = 12f
            gravity = Gravity.CENTER_HORIZONTAL
            
            // Create rounded corners
            val shape = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, context.resources.displayMetrics)
                setColor("#FFFFFF".toColorInt())
            }
            background = shape
        }
    }
    
    private fun createTitleView(title: String): TextView {
        return TextView(context).apply {
            setTextColor("#000000".toColorInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTypeface(null, android.graphics.Typeface.BOLD)
            text = title
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }
    }
    
    private fun createMessageView(message: String): TextView {
        return TextView(context).apply {
            setTextColor("#666666".toColorInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            text = message
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 32)
            setLineSpacing(0f, 1.2f)
        }
    }
    
    private fun createActionButton(buttonText: String, onInAppMessageButtonClicked: (() -> Unit)?): Button {
        return Button(context).apply {
            text = buttonText
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(48, 16, 48, 16)
            
            // Create rounded button background
            val buttonShape = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, context.resources.displayMetrics)
                setColor("#FF6B9D".toColorInt()) // Pink color for the button
            }
            background = buttonShape
            setOnClickListener { 
                onInAppMessageButtonClicked?.invoke()
            }
        }
    }
    
    private fun createDismissButton(onDismissListener: (() -> Unit)?): TextView {
        return TextView(context).apply {
            setTextColor("#999999".toColorInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            text = "✕"
            gravity = Gravity.END
            
            // Create circular background for dismiss button
            val dismissShape = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor("#F0F0F0".toColorInt())
            }
            background = dismissShape
            
            setPadding(20, 12, 20, 12)
            setOnClickListener { 
                onDismissListener?.invoke()
            }
        }
    }
    
    private fun createHeaderContainer(dismissButton: TextView): FrameLayout {
        return FrameLayout(context).apply {
            setPadding(0, 0, 0, 20)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }
} 