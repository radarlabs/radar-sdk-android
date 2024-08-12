package io.radar.sdk

import android.content.Context
import android.location.Location
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.SystemClock
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
import io.radar.sdk.model.RadarVerifiedLocationToken
import org.json.JSONObject
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Enumeration

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal class RadarVerificationManager(
    private val context: Context,
    private val logger: RadarLogger,
) {

    private lateinit var standardIntegrityTokenProvider: StandardIntegrityManager.StandardIntegrityTokenProvider
    private var lastWarmUpTimestampSeconds = 0L
    private val handler = Handler(this.context.mainLooper)
    private val connectivityManager = this.context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var started = false
    private var scheduled = false
    private var lastToken: RadarVerifiedLocationToken? = null
    private var lastTokenElapsedRealtime: Long = 0L
    private var lastTokenBeacons: Boolean = false
    private var lastIPs: String? = null
    private var expectedCountryCode: String? = null
    private var expectedStateCode: String? = null

    internal companion object {
        private const val WARM_UP_WINDOW_SECONDS = 3600 * 12 // 12 hours
    }

    fun trackVerified(beacons: Boolean = false, callback: Radar.RadarTrackVerifiedCallback? = null) {
        val verificationManager = this
        val lastTokenBeacons = beacons

        val usage = "trackVerified"
        Radar.apiClient.getConfig(usage, true, object : RadarApiClient.RadarGetConfigApiCallback {
            override fun onComplete(status: Radar.RadarStatus, config: RadarConfig?) {
                if (status != Radar.RadarStatus.SUCCESS || config == null) {
                    Radar.handler.post {
                        callback?.onComplete(status)
                    }

                    return
                }

                val googlePlayProjectNumber = config.googlePlayProjectNumber

                Radar.locationManager.getLocation(
                    RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy.MEDIUM,
                    Radar.RadarLocationSource.FOREGROUND_LOCATION,
                    object :
                        Radar.RadarLocationCallback {
                        override fun onComplete(
                            status: Radar.RadarStatus,
                            location: Location?,
                            stopped: Boolean
                        ) {
                            if (status != Radar.RadarStatus.SUCCESS || location == null) {
                                Radar.handler.post {
                                    callback?.onComplete(status)
                                }

                                return
                            }

                            val requestHash = verificationManager.getRequestHash(location)

                            verificationManager.getIntegrityToken(
                                googlePlayProjectNumber,
                                requestHash
                            ) { integrityToken, integrityException ->
                                val callTrackApi = { beacons: Array<RadarBeacon>? ->
                                    Radar.apiClient.track(
                                        location,
                                        RadarState.getStopped(verificationManager.context),
                                        RadarActivityLifecycleCallbacks.foreground,
                                        Radar.RadarLocationSource.FOREGROUND_LOCATION,
                                        false,
                                        beacons,
                                        true,
                                        integrityToken,
                                        integrityException,
                                        false,
                                        verificationManager.expectedCountryCode,
                                        verificationManager.expectedStateCode,
                                        callback = object : RadarApiClient.RadarTrackApiCallback {
                                            override fun onComplete(
                                                status: Radar.RadarStatus,
                                                res: JSONObject?,
                                                events: Array<RadarEvent>?,
                                                user: RadarUser?,
                                                nearbyGeofences: Array<RadarGeofence>?,
                                                config: RadarConfig?,
                                                token: RadarVerifiedLocationToken?
                                            ) {
                                                if (status == Radar.RadarStatus.SUCCESS) {
                                                    Radar.locationManager.updateTrackingFromMeta(
                                                        config?.meta
                                                    )
                                                }
                                                if (token != null) {
                                                    verificationManager.lastToken = token
                                                    verificationManager.lastTokenElapsedRealtime = SystemClock.elapsedRealtime()
                                                    verificationManager.lastTokenBeacons = lastTokenBeacons
                                                }
                                                Radar.handler.post {
                                                    callback?.onComplete(
                                                        status,
                                                        token
                                                    )
                                                }
                                            }
                                        })
                                }

                                if (beacons && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    Radar.apiClient.searchBeacons(
                                        location,
                                        1000,
                                        10,
                                        object : RadarApiClient.RadarSearchBeaconsApiCallback {
                                            override fun onComplete(
                                                status: Radar.RadarStatus,
                                                res: JSONObject?,
                                                beacons: Array<RadarBeacon>?,
                                                uuids: Array<String>?,
                                                uids: Array<String>?
                                            ) {
                                                if (!uuids.isNullOrEmpty() || !uids.isNullOrEmpty()) {
                                                    Radar.beaconManager.startMonitoringBeaconUUIDs(
                                                        uuids,
                                                        uids
                                                    )

                                                    Radar.beaconManager.rangeBeaconUUIDs(
                                                        uuids,
                                                        uids,
                                                        false,
                                                        object : Radar.RadarBeaconCallback {
                                                            override fun onComplete(
                                                                status: Radar.RadarStatus,
                                                                beacons: Array<RadarBeacon>?
                                                            ) {
                                                                if (status != Radar.RadarStatus.SUCCESS || beacons == null) {
                                                                    callTrackApi(null)

                                                                    return
                                                                }

                                                                callTrackApi(beacons)
                                                            }
                                                        })
                                                } else if (beacons != null) {
                                                    Radar.beaconManager.startMonitoringBeacons(
                                                        beacons
                                                    )

                                                    Radar.beaconManager.rangeBeacons(
                                                        beacons,
                                                        false,
                                                        object : Radar.RadarBeaconCallback {
                                                            override fun onComplete(
                                                                status: Radar.RadarStatus,
                                                                beacons: Array<RadarBeacon>?
                                                            ) {
                                                                if (status != Radar.RadarStatus.SUCCESS || beacons == null) {
                                                                    callTrackApi(null)

                                                                    return
                                                                }

                                                                callTrackApi(beacons)
                                                            }
                                                        })
                                                } else {
                                                    callTrackApi(arrayOf())
                                                }
                                            }
                                        },
                                        false
                                    )
                                } else {
                                    callTrackApi(null)
                                }
                            }
                        }
                    })
            }
        })
    }

    fun startTrackingVerified(interval: Int, beacons: Boolean) {
        val verificationManager = this

        verificationManager.started = true
        verificationManager.scheduled = false

        val trackVerified = { ->
            verificationManager.trackVerified(beacons, object : Radar.RadarTrackVerifiedCallback {
                override fun onComplete(
                    status: Radar.RadarStatus,
                    token: RadarVerifiedLocationToken?
                ) {
                    var expiresIn = 0
                    var minInterval: Int = interval

                    token?.let {
                        expiresIn = it.expiresIn

                        // if expiresIn is shorter than interval, override interval
                        minInterval = minOf(it.expiresIn, interval)
                    }

                    // re-request early to maximize the likelihood that a cached token is available
                    if (minInterval > 20) {
                        minInterval -= 10
                    }

                    // min interval is 10 seconds
                    if (minInterval < 10) {
                        minInterval = 10;
                    }

                    if (verificationManager.scheduled) {
                        verificationManager.logger.d("Token request already scheduled")

                        return
                    }

                    verificationManager.logger.d("Requesting token again in $minInterval seconds | minInterval = $minInterval; expiresIn = $expiresIn; interval = $interval")

                    verificationManager.scheduled = true

                    handler.postDelayed(object : Runnable {
                        override fun run() {
                            if (verificationManager.started) {
                                verificationManager.logger.d("Token request interval fired")

                                trackVerified()

                                verificationManager.scheduled = false
                            }
                        }
                    }, minInterval * 1000L)
                }
            })
        }

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_BLUETOOTH)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            .build()

        val handleNetworkChange = {
            val ips = verificationManager.getIPs()
            var changed = false

            if (verificationManager.lastIPs == null) {
                verificationManager.logger.d("First time getting IPs")
                changed = false
            } else if (ips == "error") {
                verificationManager.logger.d("Error getting IPs")
                changed = true
            } else if (ips != verificationManager.lastIPs) {
                verificationManager.logger.d("IPs changed | ips = $ips; lastIPs = ${verificationManager.lastIPs}")
                changed = true
            } else {
                verificationManager.logger.d("IPs unchanged")
            }
            verificationManager.lastIPs = ips

            if (changed) {
                trackVerified()
            }
        }

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                verificationManager.logger.d("Network connected")
                handleNetworkChange()
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                verificationManager.logger.d("Network lost")
                handleNetworkChange()
            }
        }

        networkCallback?.let {
            connectivityManager.registerNetworkCallback(request, it)
        }

        trackVerified()
    }

    fun stopTrackingVerified() {
        this.started = false

        networkCallback?.let {
            connectivityManager.unregisterNetworkCallback(it)
        }
    }

    fun getVerifiedLocationToken(callback: Radar.RadarTrackVerifiedCallback? = null) {
        val lastTokenElapsed = (SystemClock.elapsedRealtime() - this.lastTokenElapsedRealtime) / 1000

        if (this.lastToken != null) {
            this.lastToken?.let {
                if (lastTokenElapsed < it.expiresIn) {
                    Radar.logger.d("Last token valid | lastToken.expiresIn = ${it.expiresIn}; lastTokenElapsed = $lastTokenElapsed")

                    Radar.flushLogs()

                    callback?.onComplete(Radar.RadarStatus.SUCCESS, it)

                    return
                }

                Radar.logger.d("Last token invalid | lastToken.expiresIn = ${it.expiresIn}; lastTokenElapsed = $lastTokenElapsed")
            }
        } else {
            Radar.logger.d("No last token")
        }

        this.trackVerified(this.lastTokenBeacons, callback)
    }

    fun setExpectedJurisdiction(countryCode: String?, stateCode: String?) {
        this.expectedCountryCode = countryCode
        this.expectedStateCode = stateCode
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

    private fun warmUpProviderAndFetchTokenFromGoogle(googlePlayProjectNumber: Long, requestHash: String?, block: (integrityToken: String?, integrityException: String?) -> Unit) {
        val standardIntegrityManager = IntegrityManagerFactory.createStandard(this.context)
        standardIntegrityManager.prepareIntegrityToken(
            StandardIntegrityManager.PrepareIntegrityTokenRequest.builder()
                .setCloudProjectNumber(googlePlayProjectNumber)
                .build()
        )
            .addOnSuccessListener { tokenProvider ->
                this.standardIntegrityTokenProvider = tokenProvider
                Radar.logger.d("Successfully warmed up integrity token provider")
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
            this.warmUpProviderAndFetchTokenFromGoogle(googlePlayProjectNumber, requestHash, block)

            return
        }
        this.fetchTokenFromGoogle(requestHash, block)
    }

    private fun fetchTokenFromGoogle(requestHash: String?, block: (integrityToken: String?, integrityException: String?) -> Unit) {
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

    fun getIPs(): String {
        val ips = mutableListOf<String>()

        try {
            val interfaces: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface: NetworkInterface = interfaces.nextElement()
                val addresses: Enumeration<InetAddress> = networkInterface.inetAddresses

                while (addresses.hasMoreElements()) {
                    val address: InetAddress = addresses.nextElement()
                    address.hostAddress?.let { ips.add(it) }
                }
            }
        } catch (e: Exception) {
            logger.d("Error getting IPs | e = ${e.localizedMessage}")

            return "error"
        }

        if (ips.size > 0) {
            return ips.joinToString(",")
        }

        return "error"
    }

}