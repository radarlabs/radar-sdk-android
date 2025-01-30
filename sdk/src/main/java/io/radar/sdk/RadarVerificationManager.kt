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

    var started = false

    private lateinit var standardIntegrityTokenProvider: StandardIntegrityManager.StandardIntegrityTokenProvider
    private var lastWarmUpTimestampSeconds = 0L
    private val handler = Handler(this.context.mainLooper)
    private val connectivityManager = this.context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var startedInterval = 0
    private var startedBeacons = false
    private var runnable: Runnable? = null
    private var lastToken: RadarVerifiedLocationToken? = null
    private var lastTokenElapsedRealtime: Long = 0L
    private var lastTokenBeacons: Boolean = false
    private var lastIPs: String? = null
    private var expectedCountryCode: String? = null
    private var expectedStateCode: String? = null

    internal companion object {
        private const val WARM_UP_WINDOW_SECONDS = 3600 * 12 // 12 hours
    }

    private fun isIntegrityApiIncluded(): Boolean {
        return try {
            Class.forName("com.google.android.play.core.integrity.StandardIntegrityManager")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    fun trackVerified(beacons: Boolean = false, desiredAccuracy: RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy = RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy.MEDIUM, callback: Radar.RadarTrackVerifiedCallback? = null) {
        val verificationManager = this
        val lastTokenBeacons = beacons

        val usage = "trackVerified"
        Radar.apiClient.getConfig(usage, true, object : RadarApiClient.RadarGetConfigApiCallback {
            override fun onComplete(status: Radar.RadarStatus, config: RadarConfig?) {
                if (status != Radar.RadarStatus.SUCCESS || config == null) {
                    Radar.handler.post {
                        if (status != Radar.RadarStatus.SUCCESS) {
                            Radar.sendError(status)
                        }

                        callback?.onComplete(status)
                    }

                    return
                }

                val googlePlayProjectNumber = config.googlePlayProjectNumber

                Radar.locationManager.getLocation(
                    desiredAccuracy,
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
                                    if (status != Radar.RadarStatus.SUCCESS) {
                                        Radar.sendError(status)
                                    }

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
                                                    if (status != Radar.RadarStatus.SUCCESS) {
                                                        Radar.sendError(status)
                                                    }

                                                    callback?.onComplete(status, token)
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

    private fun callTrackVerified() {
        val verificationManager = this

        if (!verificationManager.started) {
            return
        }

        verificationManager.trackVerified(this.startedBeacons, RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy.HIGH, object : Radar.RadarTrackVerifiedCallback {
            override fun onComplete(
                status: Radar.RadarStatus,
                token: RadarVerifiedLocationToken?
            ) {
                verificationManager.scheduleNextIntervalWithLastToken()
            }
        })
    }

    fun scheduleNextIntervalWithLastToken() {
        val verificationManager = this

        var minInterval: Int = verificationManager.startedInterval

        this.lastToken?.let {
            val lastTokenElapsed = (SystemClock.elapsedRealtime() - this.lastTokenElapsedRealtime).toInt() / 1000

            // if expiresIn is shorter than interval, override interval
            // re-request early to maximize the likelihood that a cached token is available
            minInterval = minOf(it.expiresIn - lastTokenElapsed, verificationManager.startedInterval)

            verificationManager.logger.d("Calculated next interval | minInterval = $minInterval; expiresIn = ${it.expiresIn}; lastTokenElapsed = $lastTokenElapsed; startedInterval = ${verificationManager.startedInterval}")
        }

        var interval = minInterval - 10

        // min interval is 10 seconds
        if (interval < 10) {
            interval = 10
        }

        if (runnable == null) {
            runnable = Runnable {
                verificationManager.logger.d("Token request interval fired")

                callTrackVerified()
            }
        }

        runnable?.let {
            handler.removeCallbacks(it)

            if (!verificationManager.started) {
                return
            }

            verificationManager.logger.d("Requesting token again in $interval seconds")

            handler.postDelayed(it, interval * 1000L)
        }
    }

    fun startTrackingVerified(interval: Int, beacons: Boolean) {
        val verificationManager = this

        verificationManager.stopTrackingVerified()

        verificationManager.started = true
        verificationManager.startedInterval = interval
        verificationManager.startedBeacons = beacons

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
                callTrackVerified()
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

        if (startedInterval < 20) {
            Radar.locationManager.locationClient.requestLocationUpdates(RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy.HIGH, 0, 0, RadarLocationReceiver.getVerifiedLocationPendingIntent(context))
        }

        if (this.isLastTokenValid()) {
            this.scheduleNextIntervalWithLastToken()
        } else {
            callTrackVerified()
        }
    }

    fun stopTrackingVerified() {
        this.started = false

        try {
            if (startedInterval < 20) {
                Radar.locationManager.locationClient.removeLocationUpdates(RadarLocationReceiver.getVerifiedLocationPendingIntent(context))
            }

            networkCallback?.let {
                connectivityManager.unregisterNetworkCallback(it)
            }

            runnable?.let {
                handler.removeCallbacks(it)
            }
        } catch (e: Exception) {
            Radar.logger.e("Error unregistering callbacks", Radar.RadarLogType.SDK_EXCEPTION, e)
        }
    }

    fun getVerifiedLocationToken(beacons: Boolean, desiredAccuracy: RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy, callback: Radar.RadarTrackVerifiedCallback? = null) {
        if (this.isLastTokenValid()) {
            Radar.flushLogs()

            callback?.onComplete(Radar.RadarStatus.SUCCESS, this.lastToken)

            return
        }

        this.trackVerified(beacons, desiredAccuracy, callback)
    }

    fun clearVerifiedLocationToken() {
        this.lastToken = null
    }

    fun isLastTokenValid(): Boolean {
        val lastToken = this.lastToken ?: return false

        val lastTokenElapsed = (SystemClock.elapsedRealtime() - this.lastTokenElapsedRealtime) / 1000
        val lastDistanceToStateBorder = lastToken.user.state?.distanceToBorder ?: -1.0

        val lastTokenValid = lastTokenElapsed < lastToken.expiresIn && lastToken.passed && lastDistanceToStateBorder > 1609

        if (lastTokenValid) {
            Radar.logger.d("Last token valid | lastToken.expiresIn = ${lastToken.expiresIn}; lastTokenElapsed = $lastTokenElapsed; lastToken.passed = ${lastToken.passed}; lastDistanceToStateBorder = $lastDistanceToStateBorder")
        } else {
            Radar.logger.d("Last token invalid | lastToken.expiresIn = ${lastToken.expiresIn}; lastTokenElapsed = $lastTokenElapsed; lastToken.passed = ${lastToken.passed}; lastDistanceToStateBorder = $lastDistanceToStateBorder")
        }

        return lastTokenValid
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
        stringBuffer.append(false)
        return hashSHA256(stringBuffer.toString())
    }

    private fun warmUpProviderAndFetchTokenFromGoogle(googlePlayProjectNumber: Long, requestHash: String?, block: (integrityToken: String?, integrityException: String?) -> Unit) {
        if (!isIntegrityApiIncluded()) {
            val integrityException = "Integrity API not included"

            logger.w(integrityException)

            block(null, integrityException)

            return
        }

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
        if (!isIntegrityApiIncluded()) {
            val integrityException = "Integrity API not included"

            logger.w(integrityException)

            block(null, integrityException)

            return
        }

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
        if (!isIntegrityApiIncluded()) {
            val integrityException = "Integrity API not included"

            logger.w(integrityException)

            block(null, integrityException)

            return
        }

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