package io.radar.example.tests.panels

import androidx.compose.runtime.Composable
import io.radar.example.Utils
import io.radar.example.components.ActionButton
import io.radar.example.components.ActionButtonStyle
import io.radar.example.components.TogglePanel
import io.radar.example.store.LocalLogStore
import io.radar.sdk.Radar

@Composable
fun VerifiedPanel() {
    val log = LocalLogStore.current

    TogglePanel("Verified") {
        ActionButton("startTrackingVerified", style = ActionButtonStyle.PRIMARY) {
            Radar.startTrackingVerified(60, false)
            log.writeResult("startTrackingVerified(interval = 60, beacons = false)")
        }
        ActionButton("stopTrackingVerified", style = ActionButtonStyle.DESTRUCTIVE) {
            Radar.stopTrackingVerified()
            log.writeResult("stopTrackingVerified")
        }
        ActionButton("getVerifiedLocationToken") {
            Radar.getVerifiedLocationToken { status, token ->
                log.writeStatus(status, "getVerifiedLocationToken: ${Utils.stringForRadarStatus(status)}", token?.toJson()?.toString(2))
            }
        }
        ActionButton("trackVerified") {
            Radar.trackVerified(false) { status, token ->
                log.writeStatus(status, "trackVerified: ${Utils.stringForRadarStatus(status)}", token?.toJson()?.toString(2))
            }
        }
        ActionButton("isSharing") {
            log.writeResult("isSharing", Radar.isSharing().toString())
        }
        ActionButton("clearSharing") {
            Radar.clearSharing()
            log.writeResult("clearSharing")
        }
        ActionButton("setExpectedJurisdiction (US, CA)") {
            Radar.setExpectedJurisdiction("US", "CA")
            log.writeResult("setExpectedJurisdiction(US, CA)")
        }
    }
}
