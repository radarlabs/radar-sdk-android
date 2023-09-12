package io.radar.sdk

import android.content.Context
import android.util.Log
import com.google.android.play.core.integrity.StandardIntegrityManager
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityToken
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenRequest
import com.google.android.gms.tasks.Task;

internal class RadarVerificationManager(
    private val context: Context,
    private val logger: RadarLogger,
) {


    fun getIntegrityToken(integrityTokenProvider: StandardIntegrityManager.StandardIntegrityTokenProvider, googleCloudProjectNumber: Long?, nonce: String?, block: (integrityToken: String?, integrityException: String?) -> Unit) {
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

        // TODO: construct the request hash
        val requestHash = nonce;
        val startTime = System.currentTimeMillis()
        val integrityTokenResponse: Task<StandardIntegrityToken> = integrityTokenProvider.request(
            StandardIntegrityTokenRequest.builder()
                .setRequestHash(requestHash)
                .build()
        )
        integrityTokenResponse
            .addOnSuccessListener { response ->
                val integrityToken = response.token()

                logger.d("Successfully requested integrity token | integrityToken = $integrityToken")

                val endTime = System.currentTimeMillis()
                val executionTime = endTime - startTime
                Log.v("travis", "Execution time: $executionTime milliseconds to request integrity token")

                block(integrityToken, null)
            }
            .addOnFailureListener { exception ->
                val integrityException = exception?.message

                logger.d("Error requesting integrity token | integrityException = $integrityException")

                block(null, integrityException)
            }
    }

}