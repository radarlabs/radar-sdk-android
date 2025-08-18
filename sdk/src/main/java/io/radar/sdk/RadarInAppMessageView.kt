package io.radar.sdk

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import io.radar.sdk.model.RadarInAppMessage

/**
 * Custom view for displaying Radar in-app messages.
 * Creates a modal-style banner with title, body, action button, and dismiss functionality.
 * Includes a screen-wide overlay to blur out the background.
 */
class RadarInAppMessageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val OVERLAY_BACKGROUND_COLOR = "#80000000" // Semi-transparent black overlay
        private const val MODAL_BACKGROUND_COLOR = "#FFFFFF" // White modal background
        private const val DISMISS_BUTTON_TEXT_COLOR = "#FFFFFF" // White text for dismiss button
        private const val DISMISS_BUTTON_BACKGROUND_COLOR = "#5A6872" // Gray background for dismiss button
    }

    private var onDismissListener: (() -> Unit)? = null
    private var onInAppMessageButtonClicked: (() -> Unit)? = null

    init {
        // Set layout params to match parent (full screen)
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )
    }

    /**
     * Initializes the view with the provided inAppMessage and callbacks.
     * 
     * @param inAppMessage The inAppMessage containing title, body, and button data
     * @param image Optional bitmap image to display above the body text
     * @param onDismissListener Optional callback for when the banner is dismissed
     * @param onInAppMessageButtonClicked Optional callback for when the button is clicked
     */
    fun initialize(
        inAppMessage: RadarInAppMessage,
        onDismissListener: (() -> Unit)? = null,
        onInAppMessageButtonClicked: (() -> Unit)? = null
    ) {
        this.onDismissListener = onDismissListener
        this.onInAppMessageButtonClicked = onInAppMessageButtonClicked
        Radar.loadImage(inAppMessage.image?.url) { image ->
            createInAppMessageView(inAppMessage, image)
        }
    }

    private fun createInAppMessageView(inAppMessage: RadarInAppMessage, image: Bitmap? = null) {
        // Clear any existing views
        removeAllViews()
        
        // Create the overlay background
        val overlayBackground = createOverlayBackground()
        
        // Create the modal container with conditional padding
        val modalContainer = createModalContainer(image != null)
        
        // Add image if provided (at the top)
        if (image != null) {
            val imageContainer = createImageContainer(image)
            modalContainer.addView(imageContainer)
        } else {
            val headerContainer = createHeaderContainer()
            
            headerContainer.addView(createDismissButton(), LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END or Gravity.TOP
            })
            modalContainer.addView(headerContainer)
        }
        
        val titleView = createTitleView(inAppMessage.title)
        modalContainer.addView(titleView)
        
        val messageView = createMessageView(inAppMessage.body)
        modalContainer.addView(messageView)
        if (inAppMessage.button != null) {
            val actionButton = createActionButton(inAppMessage.button)
            modalContainer.addView(actionButton)
        }

        // Add overlay and modal to the main container
        addView(overlayBackground)
        addView(modalContainer)
    }
    
    private fun createOverlayBackground(): View {
        return View(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(OVERLAY_BACKGROUND_COLOR.toColorInt()) // Semi-transparent black overlay
            
            // Add click listener to dismiss when overlay is tapped
            setOnClickListener {
                onDismissListener?.invoke()
            }
        }
    }
    
    private fun createModalContainer(hasImage: Boolean = false): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(MODAL_BACKGROUND_COLOR.toColorInt())
            
            // Adjust padding based on whether image is present
            if (hasImage) {
                setPadding(0, 0, 0, 48) // No top or horizontal padding when image is present
            } else {
                setPadding(48, 40, 48, 48) // Full padding when no image
            }
            
            gravity = Gravity.CENTER_HORIZONTAL
            
            // Set layout params to center the modal
            layoutParams = LayoutParams(
                (context.resources.displayMetrics.widthPixels * 0.75).toInt(),
                LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
            
            // Create rounded corners
            val shape = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, context.resources.displayMetrics)
                setColor(MODAL_BACKGROUND_COLOR.toColorInt())
            }
            background = shape
        }
    }
    
    private fun createTitleView(title: RadarInAppMessage.Title): TextView {
        return TextView(context).apply {
            setTextColor(title.color.toColorInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTypeface(null, android.graphics.Typeface.BOLD)
            text = title.text
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
            
            // Add horizontal margins to compensate for removed container padding
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(48, 0, 48, 0)
            }
        }
    }
    
    private fun createMessageView(body: RadarInAppMessage.Body): TextView {
        return TextView(context).apply {
            setTextColor(body.color.toColorInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            text = body.text
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 32)
            setLineSpacing(0f, 1.2f)
            
            // Add horizontal margins to compensate for removed container padding
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(48, 0, 48, 0)
            }
        }
    }
    
    private fun createActionButton(button: RadarInAppMessage.Button): Button {
        return Button(context).apply {
            Radar.logger.i(button.text)
            text = button.text
            // required to override capitalization of string
            transformationMethod = null
            setTextColor(button.color.toColorInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(48, 16, 48, 16)

            // Create rounded button background
            val buttonShape = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12f, context.resources.displayMetrics)
                setColor(button.backgroundColor.toColorInt())
            }
            background = buttonShape
            
            // Add horizontal margins to compensate for removed container padding
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(48, 0, 48, 0)
            }
            
            setOnClickListener { 
                onInAppMessageButtonClicked?.invoke()
            }
        }
    }

    private fun createDismissButton(): TextView {
        return TextView(context).apply {
            
         val closeIcon = ContextCompat.getDrawable(context, R.drawable.close)?.apply {
            setTint(android.graphics.Color.WHITE)
        }
        setCompoundDrawablesWithIntrinsicBounds(
            closeIcon, // Replace with your icon name
            null, null, null
        )

        gravity = Gravity.CENTER

            // Create circular background for dismiss button
            val dismissShape = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(DISMISS_BUTTON_BACKGROUND_COLOR.toColorInt())
            }
            background = dismissShape

            setPadding(12, 6, 12, 6)

            setOnClickListener { 
                onDismissListener?.invoke()
            }
        }
    }

    private fun createHeaderContainer(): FrameLayout {
        return FrameLayout(context).apply {
            setPadding(0, 0, 0, 20)
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
        }
    }
    
    private fun createImageContainer(image: Bitmap): FrameLayout {
        return FrameLayout(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                (context.resources.displayMetrics.widthPixels * 0.4).toInt() // 40% of screen width height
            ).apply {
                setMargins(0, 0, 0, 24) // Only bottom margin for spacing
            }
            
            // Add the image
            val imageView = ImageView(context).apply {
                setImageBitmap(image)
                scaleType = ImageView.ScaleType.CENTER_CROP
                layoutParams = LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT
                )
                
                // Create rounded corners for the image
                val shape = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadii = floatArrayOf(
                        42f, 42f,  // top-left
                        42f, 42f,  // top-right
                        0f, 0f,    // bottom-right
                        0f, 0f     // bottom-left
                    )
                }
                background = shape
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    clipToOutline = true
                }
            }
            addView(imageView)
            
            // Add dismiss button as overlay
            val dismissButton = createDismissButton()
            addView(dismissButton, LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END or Gravity.TOP
                setMargins(0, 16, 16, 0) // Top and right margins for positioning
            })
        }
    }
} 