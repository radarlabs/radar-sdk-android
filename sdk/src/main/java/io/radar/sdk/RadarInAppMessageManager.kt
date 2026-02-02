package io.radar.sdk
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import io.radar.sdk.Radar.RadarLogConversionCallback
import io.radar.sdk.Radar.RadarStatus
import io.radar.sdk.model.RadarEvent
import io.radar.sdk.model.RadarInAppMessage
import org.json.JSONObject

class RadarInAppMessageManager(private val activity: Activity, private val context: Context) {
    private var currentView: View? = null
    private var inAppMessageReceiver: RadarInAppMessageReceiver? = null

    // Time tracking properties
    private var modalShowTime: Long = 0L
    private var currentMessage: RadarInAppMessage? = null

    private fun logConversion(name: String, withDuration: Boolean) {
        val message = currentMessage ?: return

        val metadata = JSONObject()
        metadata.put("campaignId", message.metadata.optString("radar:campaignId"))
        metadata.put("geofenceId", message.metadata.optString("radar:geofenceId"))
        metadata.put("campaignMetadata", message.metadata.optString("radar:campaignMetadata"))

        if (withDuration) {
            metadata.put("displayDuration", System.currentTimeMillis() - modalShowTime)
        }
        val campaign = message.metadata.optString("radar:campaignId")

        Radar.apiClient.sendEvent(name, metadata, campaign, object : RadarApiClient.RadarSendEventApiCallback {
            override fun onComplete(status: RadarStatus, res: JSONObject?, event: RadarEvent?) {
                Radar.logger.i("Conversion name = ${event?.conversionName}: status = $status; event = $event")
            }
        })
    }

    internal fun showInAppMessage(payload: RadarInAppMessage) {
        inAppMessageReceiver?.createInAppMessageView(
            context,
            payload,
            onDismissListener = {
                // Record the time when modal is dismissed
                logConversion("user.dismissed_in_app_message", true)
                inAppMessageReceiver?.onInAppMessageDismissed(payload)
                dismiss()
            },
            onInAppMessageButtonClicked = {
                // Record the time when modal is dismissed via button click
                logConversion("user.clicked_in_app_message", true)
                Log.d("MyInAppMessageReceiver", "called super, activity is $activity")
                if (payload.button?.deepLink != null && payload.button.deepLink != "null" && payload.button.deepLink.isNotBlank()) {
                    payload.button.deepLink.let { deepLink ->
                        try {
                            val uri = deepLink.toUri()
                            Radar.logger.d("Opening URL: $deepLink -> URI: $uri")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            activity.startActivity(intent)
                        } catch (e: Exception) {
                            // Handle invalid URL or no app to handle the intent
                            Radar.logger.e("Error opening URL '$deepLink': ${e.message}")
                        }
                    }
                } else {
                    Radar.logger.d("Button deepLink is null or 'null' string, skipping deepLink opening")
                }
                inAppMessageReceiver?.onInAppMessageButtonClicked(payload)
                dismiss()
            },
            onViewReady = { view ->
                if (currentView != null) {
                    Radar.logger.d("In-app message view already exists, skipping")
                    return@createInAppMessageView
                }
                val rootView = activity.window?.decorView as? ViewGroup
                if (rootView == null) {
                    Radar.logger.e("Activity decorView is null or not a ViewGroup, cannot show in-app message")
                    return@createInAppMessageView
                }
                // The view is now fully initialized and ready to display
                rootView.addView(view)
                currentView = view
                currentMessage = payload
                modalShowTime = System.currentTimeMillis()
                logConversion("user.displayed_in_app_message", false)
            }
        )
    }

    fun dismiss() {
        currentView?.let { modal ->
            (modal.parent as? ViewGroup)?.removeView(modal)
            currentView = null
        }
    }

    internal fun setInAppMessageReceiver(inAppMessageReceiver: RadarInAppMessageReceiver) {
        this.inAppMessageReceiver = inAppMessageReceiver
    }
   
   internal fun showInAppMessages(inAppMessages: Array<RadarInAppMessage>) {
       for (inAppMessage in inAppMessages) {
           if (inAppMessageReceiver != null) {
               inAppMessageReceiver?.onNewInAppMessage(inAppMessage)
           }
       }
   }
}
