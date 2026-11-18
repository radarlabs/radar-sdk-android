package io.radar.example

import io.radar.example.store.FraudKeyOverrideSetting

/**
 * Stand-in used when the sdk-fraud module isn't included (see build.gradle). Without the fraud
 * module there are no fraud payloads to encrypt.
 */
object FraudKeyOverrideBridge {
    fun set(setting: FraudKeyOverrideSetting) {
        throw IllegalStateException("The sdk-fraud module isn't included in this build")
    }
}
