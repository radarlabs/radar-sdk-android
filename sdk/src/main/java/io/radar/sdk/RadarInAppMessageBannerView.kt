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

class RadarInAppMessageBannerView(context: Context) : FrameLayout(context) {

    private var onDismissListener: (() -> Unit)? = null

    @SuppressLint("SetTextI18n")
    private val modalContainer = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor("#FFFFFF".toColorInt())
        setPadding(48, 40, 48, 48)
        elevation = 12f
        gravity = Gravity.CENTER_HORIZONTAL
        
        // Create rounded corners
        val shape = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, resources.displayMetrics)
            setColor("#FFFFFF".toColorInt())
        }
        background = shape
    }

    private val titleView = TextView(context).apply {
        setTextColor("#000000".toColorInt())
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        setTypeface(null, android.graphics.Typeface.BOLD)
        text = "Title text"
        gravity = Gravity.CENTER
        setPadding(0, 0, 0, 20)
    }

    private val messageView = TextView(context).apply {
        setTextColor("#666666".toColorInt())
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        text = "Description text that might wrap to the next line"
        gravity = Gravity.CENTER
        setPadding(0, 0, 0, 32)
        setLineSpacing(0f, 1.2f)
    }

    private val actionButton = Button(context).apply {
        text = "Button"
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        setTypeface(null, android.graphics.Typeface.BOLD)
        setPadding(48, 16, 48, 16)
        
        // Create rounded button background
        val buttonShape = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, resources.displayMetrics)
            setColor("#FF6B9D".toColorInt()) // Pink color for the button
        }
        background = buttonShape
    }

    private val dismissButton = TextView(context).apply {
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

    private val headerContainer = FrameLayout(context).apply {
        setPadding(0, 0, 0, 20)
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
    }

    init {
        // Set layout params to be 75% of screen width and centered
        layoutParams = LayoutParams(
            (context.resources.displayMetrics.widthPixels * 0.75).toInt(),
            LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        )

        // Add dismiss button to header (positioned on the right)
        headerContainer.addView(dismissButton, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.END or Gravity.TOP
        })

        // Add views to modal container
        modalContainer.addView(headerContainer)
        modalContainer.addView(titleView)
        modalContainer.addView(messageView)
        modalContainer.addView(actionButton)

        addView(modalContainer)
    }

    fun setTitle(title: String) {
        titleView.text = title
    }

    fun setMessage(text: String) {
        messageView.text = text
    }

    fun setButtonText(text: String) {
        actionButton.text = text
    }

    fun setOnDismissListener(listener: () -> Unit) {
        onDismissListener = listener
    }
}