package io.radar.sdk

import android.content.Context
import android.location.Location
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.android.play.core.integrity.StandardIntegrityManager
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityToken
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenRequest
import com.google.android.gms.tasks.Task;
import com.google.android.play.core.integrity.IntegrityManagerFactory
import io.radar.sdk.RadarUtils.hashSHA256

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal class RadarVerificationManager(
    private val context: Context,
    private val logger: RadarLogger,
) {

    private lateinit var standardIntegrityTokenProvider: StandardIntegrityManager.StandardIntegrityTokenProvider

    fun getRequestHash(location: Location, nonce: String): String {
        val stringBuffer = StringBuilder()
        // build a string of installId, latitude, longitude, mocked, nonce, sharing
        stringBuffer.append(RadarSettings.getInstallId(this.context))
        stringBuffer.append(location.latitude)
        stringBuffer.append(location.longitude)
        stringBuffer.append(location.isFromMockProvider)
        stringBuffer.append(nonce)
        stringBuffer.append(RadarUtils.isScreenSharing(this.context))
        return hashSHA256(stringBuffer.toString());
    }

    internal fun warmupStandardIntegrityTokenProvider(googlePlayProjectNumber: Long, requestHash: String?,  block: (integrityToken: String?, integrityException: String?) -> Unit) {

        // Create an instance of a standard integrity manager
        val standardIntegrityManager = IntegrityManagerFactory.createStandard(this.context)
        standardIntegrityManager.prepareIntegrityToken(
            StandardIntegrityManager.PrepareIntegrityTokenRequest.builder()
                .setCloudProjectNumber(googlePlayProjectNumber)
                .build()
        )
            .addOnSuccessListener { tokenProvider ->
                this.standardIntegrityTokenProvider = tokenProvider
                Radar.logger.d("successful warm up of the integrity token provider")
                this.fetchTokenFromGoogle(requestHash, block)

            }
            .addOnFailureListener { exception ->
                val warmupException = exception?.message
                Radar.logger.e("Error warming up integrity token provider | warmupException = $warmupException", Radar.RadarLogType.SDK_ERROR, exception)
                block(null, warmupException)
            }

    }

    fun getIntegrityToken(googlePlayProjectNumber: Long?, requestHash: String?, block: (integrityToken: String?, integrityException: String?) -> Unit) {
        if (requestHash == null) {
            val integrityException = "Missing request hash"

            logger.d(integrityException)

            block(null, integrityException)

            return
        }

        if (googlePlayProjectNumber == null) {
            val integrityException = "Google Play project number is null"

            logger.d("Error warming up integrity token provider: Google Play project number is null");

            block(null, integrityException)

            return;
        }

        if (!this::standardIntegrityTokenProvider.isInitialized) {
            this.warmupStandardIntegrityTokenProvider(googlePlayProjectNumber, requestHash, block)

            return
        }
        this.fetchTokenFromGoogle(requestHash, block)
    }

    internal fun fetchTokenFromGoogle(requestHash: String?, block: (integrityToken: String?, integrityException: String?) -> Unit) {
        logger.d("Requesting integrity token")

        val integrityTokenResponse: Task<StandardIntegrityToken> = this.standardIntegrityTokenProvider.request(
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