package io.radar.example.tests.panels

import android.os.SystemClock
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
        ActionButton("trackVerified (beacons)") {
            val start = SystemClock.elapsedRealtime()
            Radar.trackVerified(true) { status, token ->
                val elapsed = SystemClock.elapsedRealtime() - start
                val beacons = token?.user?.beacons.orEmpty()
                val beaconDesc = beacons.joinToString("\n") { "${it.description ?: it._id ?: "beacon"} (${it.uuid} ${it.major}/${it.minor})" }
                log.writeStatus(
                    status,
                    "trackVerified (beacons): ${Utils.stringForRadarStatus(status)} in $elapsed ms, ${beacons.size} beacons",
                    beaconDesc
                )
            }
        }
        ActionButton("startRangingBeacons", style = ActionButtonStyle.PRIMARY) {
            Radar.startRangingBeacons()
            log.writeResult("startRangingBeacons")
        }
        ActionButton("stopRangingBeacons", style = ActionButtonStyle.DESTRUCTIVE) {
            Radar.stopRangingBeacons()
            log.writeResult("stopRangingBeacons")
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
        ActionButton("revealRisk") {
            Radar.revealRisk { status, token ->
                log.writeStatus(status, token?.toJson().toString())
            }
        }
        ActionButton("setExpectedAddress") {
            Radar.setExpectedAddress("111 5th Ave, NY")
        }
    }
}
