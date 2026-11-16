package io.radar.example

import io.radar.example.store.FraudKeyOverrideSetting
import io.radar.sdk.fraud.internal.FraudKeyOverride
import io.radar.sdk.fraud.internal.RadarInternalApi

/**
 * Sets the fraud SDK's internal key override. Compiled only when the sdk-fraud module is included
 * (see build.gradle); `src/withoutFraud` has the stand-in.
 */
object FraudKeyOverrideBridge {
    /** Sets [setting] as the fraud key override. Throws IllegalArgumentException if the key or version is invalid. */
    @OptIn(RadarInternalApi::class)
    fun set(setting: FraudKeyOverrideSetting) {
        FraudKeyOverride.set(setting.publicKeyBase64, setting.keyVersion)
    }
}
