package io.radar.example.tests.panels

import android.os.SystemClock
import androidx.compose.runtime.Composable
import io.radar.example.components.ActionButton
import io.radar.example.components.ActionButtonStyle
import io.radar.example.components.TogglePanel
import io.radar.example.store.LocalLogStore
import io.radar.example.store.VerifiedResponse
import io.radar.sdk.Radar
import io.radar.sdk.RadarTrackVerifiedOptions
import io.radar.sdk.RadarTrackingOptions
import io.radar.sdk.model.RadarVerifiedLocationToken

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
                log.writeVerifiedResponse(
                    status,
                    action = "getVerifiedLocationToken",
                    response = token?.let { VerifiedResponse(it) },
                )
            }
        }
        ActionButton("clearVerifiedLocationToken") {
            Radar.clearVerifiedLocationToken()
            log.writeResult("Verified token cache cleared; the next token request will fetch a fresh response.")
        }
        ActionButton("trackVerified") {
            Radar.trackVerified(false) { status, token ->
                log.writeVerifiedResponse(
                    status,
                    action = "trackVerified",
                    response = token?.let { VerifiedResponse(it) },
                )
            }
        }
        ActionButton("trackVerified (options)") {
            // Exercises the options overload trackVerified(options, block).
            val options = RadarTrackVerifiedOptions(reason = "example", transactionId = "example-tx")
            Radar.trackVerified(options) { status, token ->
                log.writeVerifiedResponse(
                    status,
                    action = "trackVerified (options)",
                    response = token?.let { VerifiedResponse(it) },
                )
            }
        }
        ActionButton("trackVerified (beacons)") {
            val start = SystemClock.elapsedRealtime()
            Radar.trackVerified(true) { status, token ->
                val elapsed = SystemClock.elapsedRealtime() - start
                log.writeVerifiedResponse(
                    status,
                    action = "trackVerified (beacons)",
                    response = token?.let { VerifiedResponse(it) },
                    elapsedMilliseconds = elapsed,
                    includeBeacons = true,
                )
            }
        }
        ActionButton("trackVerified (callback, beacons, HIGH)") {
            // Exercises the callback overload trackVerified(beacons, desiredAccuracy, callback).
            Radar.trackVerified(
                true,
                RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy.HIGH,
                object : Radar.RadarTrackVerifiedCallback {
                    override fun onComplete(status: Radar.RadarStatus, token: RadarVerifiedLocationToken?) {
                        log.writeVerifiedResponse(
                            status,
                            action = "trackVerified (callback, beacons, HIGH)",
                            response = token?.let { VerifiedResponse(it) },
                            includeBeacons = true,
                        )
                    }
                }
            )
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
                log.writeVerifiedResponse(
                    status,
                    action = "revealRisk",
                    response = token?.let { VerifiedResponse(it) },
                )
            }
        }
        ActionButton("setExpectedAddress") {
            Radar.setExpectedAddress("111 5th Ave, NY")
        }
    }
}
