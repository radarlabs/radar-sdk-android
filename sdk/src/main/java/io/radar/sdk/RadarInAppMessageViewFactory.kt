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
     * @param payload The payload containing title, body, and button data
     * @param onDismissListener Optional callback for when the banner is dismissed
     * @param onInAppMessageButtonClicked Optional callback for when the button is clicked
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
        return createInAppMessageView(
            RadarInAppMessagePayload(
                title = RadarInAppMessagePayload.Title("Title text", "#000000"),
                body = RadarInAppMessagePayload.Body("Description text that might wrap to the next line", "#666666"),
                button = RadarInAppMessagePayload.Button("Button", "#FFFFFF", "#FF6B9D")
            )
        )
    }

    /**
     * Creates and returns a new in-app message banner view instance using a payload.
     * 
     * @param payload The payload containing title, body, and button data
     * @param onDismissListener Optional callback for when the banner is dismissed
     * @param onInAppMessageButtonClicked Optional callback for when the button is clicked
     * @return A configured View with the specified content
     */
    override fun createInAppMessageView(
        payload: RadarInAppMessagePayload,
        onDismissListener: (() -> Unit)?,
        onInAppMessageButtonClicked: (() -> Unit)?
    ): View {
        return createInAppMessageView(
            title = payload.title,
            body = payload.body,
            button = payload.button,
            onDismissListener = onDismissListener,
            onInAppMessageButtonClicked = onInAppMessageButtonClicked
        )
    }
    
    private fun createInAppMessageView(
        title: RadarInAppMessagePayload.Title,
        body: RadarInAppMessagePayload.Body,
        button: RadarInAppMessagePayload.Button,
        onDismissListener: (() -> Unit)? = null,
        onInAppMessageButtonClicked: (() -> Unit)? = null
    ): View {
        val bannerView = createBannerContainer()
        
        // Create the view hierarchy
        val modalContainer = createModalContainer()
        val titleView = createTitleView(title)
        val messageView = createMessageView(body)
        val actionButton = createActionButton(button, onInAppMessageButtonClicked)
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
    
    private fun createTitleView(title: RadarInAppMessagePayload.Title): TextView {
        return TextView(context).apply {
            setTextColor(title.color.toColorInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTypeface(null, android.graphics.Typeface.BOLD)
            text = title.text
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }
    }
    
    private fun createMessageView(body: RadarInAppMessagePayload.Body): TextView {
        return TextView(context).apply {
            setTextColor(body.color.toColorInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            text = body.text
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 32)
            setLineSpacing(0f, 1.2f)
        }
    }
    
    private fun createActionButton(button: RadarInAppMessagePayload.Button, onInAppMessageButtonClicked: (() -> Unit)?): Button {
        return Button(context).apply {
            text = button.text
            setTextColor(button.color.toColorInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(48, 16, 48, 16)
            
            // Create rounded button background
            val buttonShape = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, context.resources.displayMetrics)
                setColor(button.backgroundColor.toColorInt())
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