package io.radar.sdk

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlendModeColorFilter
import android.graphics.ColorFilter
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.marginTop
import androidx.core.view.setMargins
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
        private const val DISMISS_BUTTON_BACKGROUND_COLOR = "#808080" // Gray background for dismiss button
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
     * @param onDismissListener Optional callback for when the banner is dismissed
     * @param onInAppMessageButtonClicked Optional callback for when the button is clicked
     * @param onViewReady Callback called when the view is fully initialized and ready to display
     */
    fun initialize(
        inAppMessage: RadarInAppMessage,
        onDismissListener: (() -> Unit)? = null,
        onInAppMessageButtonClicked: (() -> Unit)? = null,
        onViewReady: (View) -> Unit
    ) {
        this.onDismissListener = onDismissListener
        this.onInAppMessageButtonClicked = onInAppMessageButtonClicked
        Radar.loadImage(inAppMessage.image?.url) { image ->
            println("Image is: $image")
            createInAppMessageView(inAppMessage, image)
            // Call the callback when the view is fully initialized
            onViewReady(this)
        }
    }

    /**
     * convert a value in dp to px
     */
    private fun <T : Number> dp(v: T): Float {
        return v.toFloat() * context.resources.displayMetrics.density // density, multiply to convert from dp to px
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
                setPadding(0, 0, 0, 40) // No top or horizontal padding when image is present
            } else {
                setPadding(40, 40, 40, 40) // Full padding when no image
            }
            
            gravity = Gravity.CENTER_HORIZONTAL
            
            // Set layout params to center the modal
            layoutParams = LayoutParams(
                dp(350).toInt(),
                LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )


            // Create rounded corners
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // if we are SDK >= 21, we can use clipping to make it look better
                outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, dp(20))
                    }
                }
                clipToOutline = true
            } else {
                // otherwise just set the background to have a radius
                val shape = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, context.resources.displayMetrics)
                    setColor(MODAL_BACKGROUND_COLOR.toColorInt())
                }
                background = shape
            }

            setOnClickListener {
                // empty click listener so the click is handled instead of propagating to the background
            }
        }
    }
    
    private fun createTitleView(title: RadarInAppMessage.Title): TextView {
        return TextView(context).apply {
            setTextColor(title.color.toColorInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 34f)
            setTypeface(null, android.graphics.Typeface.BOLD)
            text = title.text
            gravity = Gravity.CENTER

            // Add horizontal margins to compensate for removed container padding
            layoutParams = LayoutParams(
                dp(310).toInt(),
                LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(48, 0, 48, 0)
                setPadding(0, 0, 0, 15)
            }
        }
    }
    
    private fun createMessageView(body: RadarInAppMessage.Body): TextView {
        return TextView(context).apply {
            setTextColor(body.color.toColorInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            text = body.text
            gravity = Gravity.CENTER
            setLineSpacing(0f, 1.2f)
            
            // Add horizontal margins to compensate for removed container padding
            layoutParams = LayoutParams(
                dp(310).toInt(),
                LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(48, 0, 48, 0)
                setPadding(0, 0, 0, 50)
            }
        }
    }
    
    private fun createActionButton(button: RadarInAppMessage.Button): Button {
        return Button(context).apply {
            text = button.text
            // required to override capitalization of string
            transformationMethod = null
            setTextColor(button.color.toColorInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            setTypeface(null, android.graphics.Typeface.BOLD)

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
            val drawable = ContextCompat.getDrawable(context, R.drawable.close)?.apply {
                setColorFilter(
                    android.graphics.Color.WHITE,
                    android.graphics.PorterDuff.Mode.SRC_IN
                )
            }
            setCompoundDrawablesWithIntrinsicBounds(
                drawable,
                null, null, null
            )

            gravity = Gravity.CENTER

            // Create circular background for dismiss button
            val dismissShape = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(DISMISS_BUTTON_BACKGROUND_COLOR.toColorInt())
                alpha = (0.5f * 255).toInt()
            }
            background = dismissShape

            setPadding(18, 12, 18, 12)

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
                dp(200).toInt()
            ).apply {
                setMargins(0, 0, 0, 24) // Only bottom margin for spacing
            }
            
            // Add the image
            val imageView = ImageView(context).apply {
                setImageBitmap(image)
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            addView(imageView)
            
            // Add dismiss button as overlay
            val dismissButton = createDismissButton().apply {
                layoutParams = LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.END or Gravity.TOP
                    setMargins(dp(12).toInt())
                }
            }
            addView(dismissButton)
        }
    }
} 