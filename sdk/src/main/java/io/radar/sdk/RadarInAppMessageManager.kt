package io.radar.sdk
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import io.radar.sdk.Radar.RadarLogConversionCallback
import io.radar.sdk.model.RadarEvent
import io.radar.sdk.model.RadarInAppMessage
import org.json.JSONObject

class RadarInAppMessageManager(private val activity: Activity, private val context: Context) {
    private var currentView: View? = null
    private var inAppMessageReceiver: RadarInAppMessageReceiver? = null

    // Time tracking properties
    private var modalShowTime: Long = 0L
    private var modalDismissTime: Long = 0L

    private fun showModal(payload: RadarInAppMessage) {
        if (currentView != null) return // prevent duplicates

        if (activity == null) {
            Radar.logger.e("Activity is null, cannot show in-app message")
            return
        }

        val rootView = activity.window?.decorView as? ViewGroup ?: return

        // Record the time when modal is shown
        modalShowTime = System.currentTimeMillis()

        val modal = inAppMessageReceiver?.createInAppMessageView(
            context,
            payload,
            onDismissListener = {
                // Record the time when modal is dismissed
                modalDismissTime = System.currentTimeMillis()

                Radar.sendLogConversionRequest("in_app_message_dismissed", makeConversionMetadata(payload), callback = object : RadarLogConversionCallback {
                    override fun onComplete(status: Radar.RadarStatus, event: RadarEvent?) {
                        Radar.logger.i("Conversion name = ${event?.conversionName}: status = $status; event = $event")
                    }
                })

                inAppMessageReceiver?.onInAppMessageDismissed(payload)
                dismiss()
            },
            onInAppMessageButtonClicked = {
                // Record the time when modal is dismissed via button click
                modalDismissTime = System.currentTimeMillis()
                Radar.sendLogConversionRequest("in_app_message_clicked", makeConversionMetadata(payload), callback = object : RadarLogConversionCallback {
                    override fun onComplete(status: Radar.RadarStatus, event: RadarEvent?) {
                        Radar.logger.i("Conversion name = ${event?.conversionName}: status = $status; event = $event")
                    }
                })
                
                inAppMessageReceiver?.onInAppMessageButtonClicked(payload)
                if (payload.button?.url != null && payload.button.url != "null") {
                    payload.button.url.let { url ->
                        if (url.isBlank()) {
                            Radar.logger.d("Button URL is blank, skipping URL opening")
                        } else {
                            try {
                                val uri = url.toUri()
                                Radar.logger.d("Opening URL: $url -> URI: $uri")
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                activity.startActivity(intent)
                            } catch (e: Exception) {
                                // Handle invalid URL or no app to handle the intent
                                Radar.logger.e("Error opening URL '$url': ${e.message}")
                            }
                        }
                    }
                } else {
                    Radar.logger.d("Button URL is null or 'null' string, skipping URL opening")
                }

                dismiss()
            },
        )
        
        rootView.addView(modal)
        currentView = modal
    }

    private fun dismiss() {
        currentView?.let { modal ->
            (modal.parent as? ViewGroup)?.removeView(modal)
            currentView = null
        }
    }

    private fun makeConversionMetadata(payload: RadarInAppMessage): JSONObject {
        val metadata = JSONObject()
        val payloadMetadata = payload.metadata
         if (payloadMetadata != null) {
            val payloadMetadataJson = JSONObject(payloadMetadata.toString())
            metadata.put("campaignId", payloadMetadataJson.optString("radar:campaignId"))
            metadata.put("geofenceId", payloadMetadataJson.optString("radar:geofenceId"))
            metadata.put("campaignMetadata", payloadMetadataJson.optString("radar:campaignMetadata"))
        }
        metadata.put("display_duration", System.currentTimeMillis() - modalShowTime)

        return metadata
    }


    internal fun setInAppMessageReceiver(inAppMessageReceiver: RadarInAppMessageReceiver?) {
        this.inAppMessageReceiver = inAppMessageReceiver
    }

   internal fun showInAppMessages(inAppMessages: Array<RadarInAppMessage>) {
       for (inAppMessage in inAppMessages) {
           if (inAppMessageReceiver != null) {
               val result = inAppMessageReceiver?.onNewInAppMessage(inAppMessage)
               if (result == RadarInAppMessageOperation.DISCARD) {
                   continue
               }
           }
           showModal(inAppMessage)
       }

   }

}