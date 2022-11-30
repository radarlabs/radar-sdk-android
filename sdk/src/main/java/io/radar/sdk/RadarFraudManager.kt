package io.radar.sdk

import android.content.Context
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest

internal class RadarFraudManager(
    private val context: Context,
    private val logger: RadarLogger,
) {

    fun getIntegrityToken(block: (integrityToken: String?) -> Unit) {
        val nonce = "ZmRzYWxrZnNhZmtsYXNka2Zsc2FkZmtsYXNsZGtma2xzZGY="

        val integrityManager =
            IntegrityManagerFactory.create(context.applicationContext)

        logger.d("Requesting integrity token")

        integrityManager.requestIntegrityToken(
            IntegrityTokenRequest.builder()
                .setCloudProjectNumber(690986093081)
                .setNonce(nonce)
                .build())
            .addOnCompleteListener { response ->
                if (response.isSuccessful) {
                    val integrityToken = response.result.token()

                    logger.d("Successfully requested integrity token | integrityToken = $integrityToken")

                    block(integrityToken)
                } else {
                    logger.d("Error requesting integrity token | exception = ${response.exception?.message}")

                    block(null)
                }
            }
    }

}