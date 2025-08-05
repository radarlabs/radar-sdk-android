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
    private val apiClient = RadarApiClient(context, Radar.logger)

    private fun showModal(payload: RadarInAppMessage) {
        if (currentView != null) return // prevent duplicates

       loadImage(payload.image?.url) { bitmap ->
            val rootView = activity.window?.decorView as? ViewGroup ?: return@loadImage

            val modal = inAppMessageReceiver?.createInAppMessageView(
                context, 
                payload, 
                onDismissListener = {
                    inAppMessageReceiver?.onInAppMessageDismissed(payload)
                    dismiss()
                },
                onInAppMessageButtonClicked = {
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
                image = bitmap
            )
        
            rootView.addView(modal)
            currentView = modal
       }

       
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

    /**
     * Loads an image from a URL using the existing API pattern.
     * 
     * @param url The URL to load the image from
     * @param callback Callback to handle the result
     */
    internal fun loadImage(url: String?, callback: (android.graphics.Bitmap?) -> Unit) {
        if (url == null) {
            callback(null)
            return
        }

        apiClient.loadImage(url, object : RadarApiHelper.RadarImageApiCallback {
            override fun onComplete(status: Radar.RadarStatus, bitmap: android.graphics.Bitmap?) {
                if (status == Radar.RadarStatus.SUCCESS) {
                    callback(bitmap)
                } else {
                    Radar.logger.e("Error loading image: $status")
                    callback(null)
                }
            }
        })
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