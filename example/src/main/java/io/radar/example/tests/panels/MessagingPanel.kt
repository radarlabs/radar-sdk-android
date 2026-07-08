package io.radar.example.tests.panels

import androidx.compose.runtime.Composable
import io.radar.example.Utils
import io.radar.example.components.ActionButton
import io.radar.example.components.ActionButtonStyle
import io.radar.example.components.TogglePanel
import io.radar.example.store.LocalLogStore
import io.radar.sdk.Radar
import io.radar.sdk.model.RadarInAppMessage
import org.json.JSONObject

private const val DEMO_IN_APP_MESSAGE = """
{
  "title": { "text": "Welcome to Radar", "color": "#2A1688" },
  "body": { "text": "This is a demo in-app message from the example app.", "color": "#1A1A1A" },
  "button": { "text": "Got it", "color": "#FFFFFF", "backgroundColor": "#2A1688", "deepLink": "radar://home" },
  "image": { "name": "radar", "url": "https://radar.com/img/radar-logo.png" },
  "metadata": { "source": "example" }
}
"""

@Composable
fun MessagingPanel() {
    val log = LocalLogStore.current

    TogglePanel("Messaging & Conversions") {
        ActionButton("showInAppMessage", style = ActionButtonStyle.PRIMARY) {
            val message = RadarInAppMessage.fromJson(DEMO_IN_APP_MESSAGE)
            if (message != null) {
                Radar.showInAppMessage(message)
                log.writeResult("showInAppMessage", message.title.text)
            } else {
                log.writeError("showInAppMessage: failed to parse demo payload")
            }
        }
        ActionButton("logConversion") {
            val metadata = JSONObject().apply { put("one", "two") }
            Radar.logConversion("app_open_android", metadata) { status, event ->
                log.writeStatus(status, "logConversion: ${Utils.stringForRadarStatus(status)}", "name = ${event?.conversionName}")
            }
        }
    }
}
