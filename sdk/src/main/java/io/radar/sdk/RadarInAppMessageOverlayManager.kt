package io.radar.sdk
import android.app.Activity
import android.view.ViewGroup

class RadarInAppMessageOverlayManager {
    private var currentView: RadarInAppMessageBannerView? = null

    fun showModal(activity: Activity, title: String, message: String) {
        if (currentView != null) return // prevent duplicates

        val rootView = activity.window?.decorView as? ViewGroup ?: return

        val modal = RadarInAppMessageBannerView(activity).apply {
            setTitle(title)
            setMessage(message)
            setOnDismissListener { dismiss() }
        }

        // Add to root view
        rootView.addView(modal)
        currentView = modal
    }

    fun dismiss() {
        currentView?.let { modal ->
            (modal.parent as? ViewGroup)?.removeView(modal)
            currentView = null
        }
    }
}