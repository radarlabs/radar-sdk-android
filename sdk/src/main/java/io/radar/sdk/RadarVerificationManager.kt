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
            val integrityException = "Missing Google Cloud project number"

            logger.d(integrityException)

            block(null, integrityException)

            return
        }

        if (nonce == null) {
            val integrityException = "Missing nonce"

            logger.d(integrityException)

            block(null, integrityException)

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