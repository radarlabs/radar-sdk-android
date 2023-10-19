package io.radar.sdk

import android.content.Context

/**
 * A receiver for client-side delivery of verified location tokens. For more information, see [](https://radar.com/documentation/fraud).
 *
 * @see [](https://radar.com/documentation/fraud)
 */
abstract class RadarVerifiedReceiver {

    /**
     * Tells the delegate that the current user's verified location was updated. Receives a JSON Web Token (JWT). Verify the JWT server-side using your secret key.
     *
     * @param[token] The token.
     */
    abstract fun onTokenUpdated(context: Context, token: String)

}