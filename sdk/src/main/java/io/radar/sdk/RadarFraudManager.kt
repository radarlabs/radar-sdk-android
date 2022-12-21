package io.radar.sdk

import android.content.Context
import android.util.Log
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest

internal class RadarFraudManager(
    private val context: Context,
    private val verificationSettings: RadarVerificationSettings?,
    private val logger: RadarLogger,
) {

    fun getIntegrityToken(block: (integrityToken: String?) -> Unit) {
        val nonce = verificationSettings?.nonce

        val integrityManager =
            IntegrityManagerFactory.create(context.applicationContext)

        if (verificationSettings == null || nonce == null) {
            // TODO: handle missing settings or nonce
            logger.d("getIntegrityToken - missing nonce or verificiationSettings, has call to /config returned yet?")
            return;
        }

        logger.d("Requesting integrity token with nonce $nonce")

        integrityManager.requestIntegrityToken(
            IntegrityTokenRequest.builder()
                .setCloudProjectNumber(verificationSettings.projectNumber)
                .setNonce(nonce)
                .build())
            .addOnCompleteListener { response ->
                if (response.isSuccessful) {
                    val integrityToken = response.result.token()

                    logger.d("Successfully requested integrity token | integrityToken = $integrityToken")

                    verificationSettings.integrityToken = integrityToken
                    RadarSettings.setVerificationSettings(context, verificationSettings);

                    block(integrityToken)
                } else {
                    logger.d("Error requesting integrity token | exception = ${response.exception?.message}")

                    block(null)
                }
            }
    }

}