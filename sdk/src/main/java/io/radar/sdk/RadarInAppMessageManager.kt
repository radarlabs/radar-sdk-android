package io.radar.sdk
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import io.radar.sdk.model.RadarInAppMessage

class RadarInAppMessageManager(private val activity: Activity, private val context: Context) {
    private var currentView: View? = null
    private var inAppMessageReceiver: RadarInAppMessageReceiver? = null

    private fun showModal(payload: RadarInAppMessage) {
        if (currentView != null) return // prevent duplicates

        val rootView = activity.window?.decorView as? ViewGroup ?: return

        val modal = inAppMessageReceiver?.createInAppMessageView(context, payload, onDismissListener = {
                inAppMessageReceiver?.onInAppMessageDismissed(payload)
                dismiss()
            },
            onInAppMessageButtonClicked = {
                inAppMessageReceiver?.onInAppMessageButtonClicked(payload)
                payload.button.url?.let { url ->
                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                    activity.startActivity(intent)
                }
                dismiss()
            })
       
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


   internal fun showInAppMessages(inAppMessages: Array<RadarInAppMessage>) {
       for (inAppMessage in inAppMessages) {
           if (inAppMessageReceiver != null) {
               val result = inAppMessageReceiver?.onNewInAppMessage(inAppMessage)
               if (result == RadarInAppMessageOperation.DISCARD) {
                   return
               }
           }
           showModal(inAppMessage)
       }

   }

}