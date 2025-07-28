package io.radar.sdk
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import io.radar.sdk.model.RadarInAppMessagePayload

class RadarInAppMessageOverlayManager(private val activity: Activity, private val context: Context) {
    private var currentView: View? = null
    private var inAppMessageReceiver: RadarInAppMessageReceiver? = null

    private fun showModal(payload: RadarInAppMessagePayload) {
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
                // call link if it exists
                payload.button.url?.let { url ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    activity.startActivity(intent)
                }
                dismiss()
            }
        )
       
        // Add to root view
        rootView.addView(modal)
        currentView = modal
    }

    private fun dismiss() {
        currentView?.let { modal ->
            (modal.parent as? ViewGroup)?.removeView(modal)
            currentView = null
        }
    }

    internal fun setInAppMessageReceiver(inAppMessageReceiver: RadarInAppMessageReceiver?) {
        this.inAppMessageReceiver = inAppMessageReceiver
    }

   internal fun enqueueInAppMessage(payload: RadarInAppMessagePayload) {
        if (inAppMessageReceiver != null) {
            val result = inAppMessageReceiver?.shouldDisplayInAppMessage(payload)
            if (result == RadarInAppMessageOperation.DISCARD) {
                return
            }
//            if (result == RadarInAppMessageOperation.ENQUEUE) {
//                RadarState.enqueueInAppMessage(activity, payload)
//                return
//            }
        }
        showModal(payload)
   }

//   internal fun dequeueInAppMessage() {
//        val payload = RadarState.dequeueInAppMessage(activity)
//        if (payload != null) {
//            showModal(payload)
//        }
//   }
}