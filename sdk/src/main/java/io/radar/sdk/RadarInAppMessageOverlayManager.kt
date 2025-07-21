package io.radar.sdk
import android.app.Activity
import android.view.View
import android.view.ViewGroup
import io.radar.sdk.model.RadarInAppMessagePayload

class RadarInAppMessageOverlayManager {
    private var currentView: View? = null
    private var inAppMessageReceiver: RadarInAppMessageReceiver? = null

    fun showModal(activity: Activity, payload: RadarInAppMessagePayload) {
        if (currentView != null) return // prevent duplicates

        val rootView = activity.window?.decorView as? ViewGroup ?: return

        val factory = Radar.inAppMessageViewFactory
        val modal = factory.createInAppMessageView(
            payload = payload,
            onDismissListener = {
                inAppMessageReceiver?.onInAppMessageDismissed(payload)
                dismiss() 
            },
            onInAppMessageButtonClicked = {
                inAppMessageReceiver?.onInAppMessageButtonClicked(payload)
            }
        )
        if (inAppMessageReceiver != null) {
            val result = inAppMessageReceiver?.beforeInAppMessageDisplayed(payload)
            if (result == RadarInAppMessageOperation.DISCARD) {
                return
            }
            if (result == RadarInAppMessageOperation.ENQUEUE) {
                // TODO: enqueue the message
                return
            }
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

    fun setInAppMessageReceiver(inAppMessageReceiver: RadarInAppMessageReceiver?) {
        this.inAppMessageReceiver = inAppMessageReceiver
    }
}