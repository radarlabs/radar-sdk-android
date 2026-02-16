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
import kotlin.jvm.functions.Function1

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal class RadarVerificationManager(
    private val context: Context,
    private val logger: RadarLogger,
) {

    var started = false

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

    fun trackVerified(
        beacons: Boolean = false,
        desiredAccuracy: RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy = RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy.MEDIUM,
        reason: String? = null,
        transactionId: String? = null,
        callback: Radar.RadarTrackVerifiedCallback? = null
    ) {
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

                            verificationManager.getFraudPayload(location, googlePlayProjectNumber) { result ->
                                val fraudPayload = result?.get("payload") as? String
                                // -- payload encryption --
                                // val keyVersionNumber = result?.get("keyVersion") as? Int
                                // val fraudKeyVersion = keyVersionNumber
                                
                                if (result?.containsKey("error") == true || fraudPayload == null) {
                                    val error = result?.get("error") as? String ?: "Unknown error"
                                    logger.e("Error getting fraud payload: $error", Radar.RadarLogType.SDK_ERROR)
                                    callback?.onComplete(Radar.RadarStatus.ERROR_PLUGIN)
                                    return@getFraudPayload
                                }
                                val callTrackApi = { beacons: Array<RadarBeacon>? ->
                                    Radar.apiClient.track(
                                        location,
                                        RadarState.getStopped(verificationManager.context),
                                        RadarActivityLifecycleCallbacks.foreground,
                                        Radar.RadarLocationSource.FOREGROUND_LOCATION,
                                        false,
                                        beacons,
                                        true,
                                        false,
                                        verificationManager.expectedCountryCode,
                                        verificationManager.expectedStateCode,
                                        reason ?: "manual",
                                        transactionId,
                                        fraudPayload,
                                        // -- payload encryption --
                                        // fraudKeyVersion,
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

    private fun callTrackVerified(reason: String?) {
        val verificationManager = this

        if (!verificationManager.started) {
            return
        }

        verificationManager.trackVerified(this.startedBeacons, RadarTrackingOptions.RadarTrackingOptionsDesiredAccuracy.HIGH, reason, null, object : Radar.RadarTrackVerifiedCallback {
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

                callTrackVerified("interval")
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
                callTrackVerified("ip_change")
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
            callTrackVerified("start")
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

        this.trackVerified(beacons, desiredAccuracy, "last_token_invalid", null, callback)
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

    private fun getFraudPayload(location: Location, googlePlayProjectNumber: Long?, callback: (Map<String, Any?>?) -> Unit) {
        try {
            val fraudClass = Class.forName("io.radar.sdk.fraud.RadarSDKFraud")
            val sharedInstanceMethod = fraudClass.getMethod("sharedInstance")
            val fraudInstance = sharedInstanceMethod.invoke(null)
            
            // Create adapter callback that matches getFraudPayload's Function1 signature
            val getFraudPayloadCallback = object : Function1<Map<String, Any?>?, Unit> {
                override fun invoke(result: Map<String, Any?>?): Unit {
                    callback(result)
                }
            }
            
            // Create options map
            val options = mutableMapOf<String, Any?>(
                "context" to context,
                "location" to location
            )
            
            // Add integrity-related parameters if available
            if (googlePlayProjectNumber != null) {
                options["googlePlayProjectNumber"] = googlePlayProjectNumber
            }
            
            val getFraudPayloadMethod = fraudClass.getMethod("getFraudPayload", 
                java.util.Map::class.java,
                Function1::class.java)
            
            getFraudPayloadMethod.invoke(fraudInstance, options, getFraudPayloadCallback)
        } catch (e: ClassNotFoundException) {
            logger.d("Skipping fraud checks: RadarSDKFraud submodule not available")
            callback(null)
        } catch (e: Exception) {
            logger.e("Error calling fraud detection", Radar.RadarLogType.SDK_EXCEPTION, e)
            callback(mapOf("error" to (e.message ?: "Unknown error")))
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
