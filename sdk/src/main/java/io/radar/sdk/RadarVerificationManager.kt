package io.radar.sdk

import android.content.Context
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest

internal class RadarVerificationManager(
    private val context: Context,
    private val logger: RadarLogger,
) {

    fun getIntegrityToken(googleCloudProjectNumber: Long?, nonce: String?, block: (integrityToken: String?, integrityException: String?) -> Unit) {
        logger.d("Requesting integrity token")

        if (googleCloudProjectNumber == null) {
            logger.d("Missing Google Cloud project number")

            block(null, null)

            return
        }

        if (nonce == null) {
            logger.d("Missing nonce")

            block(null, null)

            return
        }

        val integrityManager =
            IntegrityManagerFactory.create(context.applicationContext)

        integrityManager.requestIntegrityToken(
            IntegrityTokenRequest.builder()
                .setCloudProjectNumber(googleCloudProjectNumber)
                .setNonce(nonce)
                .build())
            .addOnCompleteListener { response ->
                if (response.isSuccessful) {
                    val integrityToken = response.result.token()

                    logger.d("Successfully requested integrity token | integrityToken = $integrityToken")

                    block(integrityToken, null)
                } else {
                    val integrityException = response.exception?.message

                    logger.d("Error requesting integrity token | integrityException = $integrityException")

                    block(null, integrityException)
                }
            }
    }

}