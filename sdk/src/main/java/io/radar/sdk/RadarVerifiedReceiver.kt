package io.radar.sdk

import android.content.Context
import io.radar.sdk.model.RadarVerifiedLocationToken

/**
 * A receiver for client-side delivery of verified location tokens. For more information, see [](https://radar.com/documentation/fraud).
 *
 * @see [](https://radar.com/documentation/fraud)
 */
abstract class RadarVerifiedReceiver {

    /**
     * Tells the receiver that the current user's verified location was updated. Verify the token server-side using your secret key.
     *
     * @param[token] The token.
     */
    abstract fun onTokenUpdated(context: Context, token: RadarVerifiedLocationToken)

    /**
     * Tells the receiver that the device's IP address changed while IP change monitoring is active.
     *
     * @param[context] The context.
     */
    open fun onIpChanged(context: Context) {}

    /**
     * Tells the receiver that the device's screen sharing state changed.
     *
     * @param[context] The context.
     * @param[sharing] The current screen sharing state.
     */
    open fun onSharingChanged(context: Context, sharing: Boolean) {}

}