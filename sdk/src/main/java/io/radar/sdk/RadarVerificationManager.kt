package io.radar.sdk

import android.content.Context
import android.location.Location
import android.os.Build
import android.util.Log
import com.google.android.play.core.integrity.StandardIntegrityManager
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityToken
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenRequest
import com.google.android.gms.tasks.Task;
import io.radar.sdk.RadarUtils.hashSHA256

internal class RadarVerificationManager(
    private val context: Context,
    private val logger: RadarLogger,
) {

    fun getRequestHash(location: Location, nonce: String): String {
        val stringBuffer = StringBuilder()
        // build a string of installId, latitude, longitude, mocked, nonce, sharing
        stringBuffer.append(RadarSettings.getInstallId(this.context))
        stringBuffer.append(location.latitude)
        stringBuffer.append(location.longitude)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //Note(travis): throw an error if SDK isn't JELLY_BEAN_MR2?
            //by now we can be sure that the SDK is high enough, but IDE generates error
            stringBuffer.append(location.isFromMockProvider)
        }
        stringBuffer.append(nonce)
        stringBuffer.append(RadarUtils.isScreenSharing(this.context))
        return hashSHA256(stringBuffer.toString());
    }

    fun getIntegrityToken(integrityTokenProvider: StandardIntegrityManager.StandardIntegrityTokenProvider, requestHash: String?, block: (integrityToken: String?, integrityException: String?) -> Unit) {
        logger.d("Requesting integrity token")

        if (requestHash == null) {
            val integrityException = "Missing request hash"

            logger.d(integrityException)

            block(null, integrityException)

            return
        }
        
        val integrityTokenResponse: Task<StandardIntegrityToken> = integrityTokenProvider.request(
            StandardIntegrityTokenRequest.builder()
                .setRequestHash(requestHash)
                .build()
        )
        integrityTokenResponse
            .addOnSuccessListener { response ->
                val integrityToken = response.token()

                logger.d("Successfully requested integrity token | integrityToken = $integrityToken")

                block(integrityToken, null)
            }
            .addOnFailureListener { exception ->
                val integrityException = exception?.message

                logger.d("Error requesting integrity token | integrityException = $integrityException")

                block(null, integrityException)
            }
    }

}