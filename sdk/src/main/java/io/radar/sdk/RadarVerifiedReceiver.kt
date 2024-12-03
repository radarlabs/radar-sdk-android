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
     * Tells the receiver that a verified request failed.
     *
     * @param[context] The context.
     * @param[status] The status.
     */
    abstract fun onVerifiedError(context: Context, status: Radar.RadarStatus)

}