package io.radar.sdk

import android.content.Context
import android.location.Location
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import androidx.annotation.RequiresApi
import com.google.android.play.core.integrity.StandardIntegrityManager
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityToken
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenRequest
import com.google.android.gms.tasks.Task
import com.google.android.play.core.integrity.IntegrityManagerFactory
import io.radar.sdk.RadarUtils.hashSHA256
import io.radar.sdk.model.RadarBeacon
import io.radar.sdk.model.RadarConfig
import io.radar.sdk.model.RadarEvent
import io.radar.sdk.model.RadarGeofence
import io.radar.sdk.model.RadarUser
import org.json.JSONObject

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal class RadarVerificationManager(
    private val context: Context,
    private val logger: RadarLogger,
) {

    private lateinit var standardIntegrityTokenProvider: StandardIntegrityManager.StandardIntegrityTokenProvider
    private var lastWarmUpTimestampSeconds = 0L
    private val handler = Handler(this.context.mainLooper)
    private val connectivityManager = this.context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    internal companion object {
        private const val WARM_UP_WINDOW_SECONDS = 3600 * 12 // 12 hours
    }

    fun trackVerified(beacons: Boolean = false, callback: Radar.RadarTrackCallback? = null) {
        val verificationManager = this

        val googlePlayProjectNumber = RadarSettings.getGooglePlayProjectNumber(this.context)

        Radar.locationManager.getLocation(RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy.HIGH, Radar.RadarLocationSource.FOREGROUND_LOCATION, object :
            Radar.RadarLocationCallback {
            override fun onComplete(status: Radar.RadarStatus, location: Location?, stopped: Boolean) {
                if (status != Radar.RadarStatus.SUCCESS || location == null) {
                    Radar.handler.post {
                        callback?.onComplete(status)
                    }

                    return
                }

                val requestHash = verificationManager.getRequestHash(location)

                verificationManager.getIntegrityToken(googlePlayProjectNumber, requestHash) { integrityToken, integrityException ->
                    val callTrackApi = { beacons: Array<RadarBeacon>? ->
                        Radar.apiClient.track(location, RadarState.getStopped(verificationManager.context), RadarActivityLifecycleCallbacks.foreground, Radar.RadarLocationSource.FOREGROUND_LOCATION, false, beacons, true, integrityToken, integrityException, false, callback = object : RadarApiClient.RadarTrackApiCallback {
                            override fun onComplete(
                                status: Radar.RadarStatus,
                                res: JSONObject?,
                                events: Array<RadarEvent>?,
                                user: RadarUser?,
                                nearbyGeofences: Array<RadarGeofence>?,
                                config: RadarConfig?,
                                token: String?
                            ) {
                                if (status == Radar.RadarStatus.SUCCESS) {
                                    Radar.locationManager.updateTrackingFromMeta(config?.meta)
                                }
                                Radar.handler.post {
                                    callback?.onComplete(status, location, events, user)
                                }
                            }
                        })
                    }

                    if (beacons && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Radar.apiClient.searchBeacons(location, 1000, 10, object : RadarApiClient.RadarSearchBeaconsApiCallback {
                            override fun onComplete(status: Radar.RadarStatus, res: JSONObject?, beacons: Array<RadarBeacon>?, uuids: Array<String>?, uids: Array<String>?) {
                                if (!uuids.isNullOrEmpty() || !uids.isNullOrEmpty()) {
                                    Radar.beaconManager.startMonitoringBeaconUUIDs(uuids, uids)

                                    Radar.beaconManager.rangeBeaconUUIDs(uuids, uids, false, object : Radar.RadarBeaconCallback {
                                        override fun onComplete(status: Radar.RadarStatus, beacons: Array<RadarBeacon>?) {
                                            if (status != Radar.RadarStatus.SUCCESS || beacons == null) {
                                                callTrackApi(null)

                                                return
                                            }

                                            callTrackApi(beacons)
                                        }
                                    })
                                } else if (beacons != null) {
                                    Radar.beaconManager.startMonitoringBeacons(beacons)

                                    Radar.beaconManager.rangeBeacons(beacons, false, object : Radar.RadarBeaconCallback {
                                        override fun onComplete(status: Radar.RadarStatus, beacons: Array<RadarBeacon>?) {
                                            if (status != Radar.RadarStatus.SUCCESS || beacons == null) {
                                                callTrackApi(null)

                                                return
                                            }

                                            callTrackApi(beacons)
                                        }
                                    })
                                } else {
                                    callTrackApi(null)
                                }
                            }
                        }, false)
                    } else {
                        callTrackApi(null)
                    }
                }
            }
        })
    }

    fun trackVerifiedToken(beacons: Boolean = false, callback: Radar.RadarTrackTokenCallback? = null) {
        val verificationManager = this

        val googlePlayProjectNumber = RadarSettings.getGooglePlayProjectNumber(this.context)

        Radar.locationManager.getLocation(RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy.HIGH, Radar.RadarLocationSource.FOREGROUND_LOCATION, object :
            Radar.RadarLocationCallback {
            override fun onComplete(status: Radar.RadarStatus, location: Location?, stopped: Boolean) {
                if (status != Radar.RadarStatus.SUCCESS || location == null) {
                    Radar.handler.post {
                        callback?.onComplete(status)
                    }

                    return
                }

                val requestHash = verificationManager.getRequestHash(location)

                verificationManager.getIntegrityToken(googlePlayProjectNumber, requestHash) { integrityToken, integrityException ->
                    val callTrackApi = { beacons: Array<RadarBeacon>? ->
                        Radar.apiClient.track(location, RadarState.getStopped(verificationManager.context), RadarActivityLifecycleCallbacks.foreground, Radar.RadarLocationSource.FOREGROUND_LOCATION, false, beacons, true, integrityToken, integrityException, true, object : RadarApiClient.RadarTrackApiCallback {
                            override fun onComplete(
                                status: Radar.RadarStatus,
                                res: JSONObject?,
                                events: Array<RadarEvent>?,
                                user: RadarUser?,
                                nearbyGeofences: Array<RadarGeofence>?,
                                config: RadarConfig?,
                                token: String?
                            ) {
                                if (status == Radar.RadarStatus.SUCCESS) {
                                    Radar.locationManager.updateTrackingFromMeta(config?.meta)
                                }
                                Radar.handler.post {
                                    callback?.onComplete(status, token)
                                }
                            }
                        })
                    }

                    if (beacons && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Radar.apiClient.searchBeacons(location, 1000, 10, object : RadarApiClient.RadarSearchBeaconsApiCallback {
                            override fun onComplete(status: Radar.RadarStatus, res: JSONObject?, beacons: Array<RadarBeacon>?, uuids: Array<String>?, uids: Array<String>?) {
                                if (!uuids.isNullOrEmpty() || !uids.isNullOrEmpty()) {
                                    Radar.beaconManager.startMonitoringBeaconUUIDs(uuids, uids)

                                    Radar.beaconManager.rangeBeaconUUIDs(uuids, uids, false, object : Radar.RadarBeaconCallback {
                                        override fun onComplete(status: Radar.RadarStatus, beacons: Array<RadarBeacon>?) {
                                            if (status != Radar.RadarStatus.SUCCESS || beacons == null) {
                                                callTrackApi(null)

                                                return
                                            }

                                            callTrackApi(beacons)
                                        }
                                    })
                                } else if (beacons != null) {
                                    Radar.beaconManager.startMonitoringBeacons(beacons)

                                    Radar.beaconManager.rangeBeacons(beacons, false, object : Radar.RadarBeaconCallback {
                                        override fun onComplete(status: Radar.RadarStatus, beacons: Array<RadarBeacon>?) {
                                            if (status != Radar.RadarStatus.SUCCESS || beacons == null) {
                                                callTrackApi(null)

                                                return
                                            }

                                            callTrackApi(beacons)
                                        }
                                    })
                                } else {
                                    callTrackApi(null)
                                }
                            }
                        }, false)
                    } else {
                        callTrackApi(null)
                    }
                }
            }
        })
    }

    fun startTrackingVerified(token: Boolean, interval: Int, beacons: Boolean) {
        val verificationManager = this

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_BLUETOOTH)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            .build()

        connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)

                verificationManager.logger.d("Network connected")

                if (token) {
                    verificationManager.trackVerifiedToken(beacons)
                } else {
                    verificationManager.trackVerified(beacons)
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)

                verificationManager.logger.d("Network lost")

                if (token) {
                    verificationManager.trackVerifiedToken(beacons)
                } else {
                    verificationManager.trackVerified(beacons)
                }
            }
        })

        handler.post(object : Runnable {
            override fun run() {
                if (token) {
                    verificationManager.trackVerifiedToken(beacons)
                } else {
                    verificationManager.trackVerified(beacons)
                }

                handler.postDelayed(this, interval * 1000L)
            }
        })
    }

    fun getRequestHash(location: Location): String {
        val stringBuffer = StringBuilder()
        stringBuffer.append(RadarSettings.getInstallId(this.context))
        stringBuffer.append(location.latitude)
        stringBuffer.append(location.longitude)
        stringBuffer.append(location.isFromMockProvider)
        stringBuffer.append(RadarUtils.isScreenSharing(this.context))
        return hashSHA256(stringBuffer.toString())
    }

    internal fun warmupProviderAndFetchTokenFromGoogle(googlePlayProjectNumber: Long, requestHash: String?,  block: (integrityToken: String?, integrityException: String?) -> Unit) {

        val standardIntegrityManager = IntegrityManagerFactory.createStandard(this.context)
        standardIntegrityManager.prepareIntegrityToken(
            StandardIntegrityManager.PrepareIntegrityTokenRequest.builder()
                .setCloudProjectNumber(googlePlayProjectNumber)
                .build()
        )
            .addOnSuccessListener { tokenProvider ->
                this.standardIntegrityTokenProvider = tokenProvider
                Radar.logger.d("successful warm up of the integrity token provider")
                this.lastWarmUpTimestampSeconds = System.currentTimeMillis() / 1000
                this.fetchTokenFromGoogle(requestHash, block)

            }
            .addOnFailureListener { exception ->
                val warmupException = exception.message
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

            logger.d("Error warming up integrity token provider: Google Play project number is null")

            block(null, integrityException)

            return
        }

        val nowSeconds = System.currentTimeMillis() / 1000
        val warmUpProvider = !this::standardIntegrityTokenProvider.isInitialized
                || this.lastWarmUpTimestampSeconds == 0L
                || (nowSeconds - this.lastWarmUpTimestampSeconds) > WARM_UP_WINDOW_SECONDS
        if (warmUpProvider) {
            this.warmupProviderAndFetchTokenFromGoogle(googlePlayProjectNumber, requestHash, block)

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
                val integrityException = exception.message

                logger.d("Error requesting integrity token | integrityException = $integrityException")
                
                block(null, integrityException)
            }
    }

}