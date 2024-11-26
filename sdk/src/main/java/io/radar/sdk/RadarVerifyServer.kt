package io.radar.sdk

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response.Status
import org.json.JSONObject
import java.util.concurrent.CountDownLatch

@RequiresApi(21)
internal class RadarVerifyServer(
    private val context: Context,
    private val logger: RadarLogger,
) : NanoHTTPD(52516) {

    private fun addHeaders(response: Response?) {
        response?.addHeader(
            "Access-Control-Allow-Headers",
            "Content-Type, Accept, Authorization, X-Radar-Device-Type, X-Radar-SDK-Version"
        )
        response?.addHeader(
            "Access-Control-Allow-Origin",
            "*")
        response?.addHeader(
            "Access-Control-Allow-Methods",
            "GET, OPTIONS")
    }

    fun isAdbEnabled(context: Context): Boolean {
        var adb = 0
        try {
            adb = Settings.Secure.getInt(context.contentResolver, Settings.Secure.ADB_ENABLED)
        } catch (e: Settings.SettingNotFoundException) {

        }
        return adb == 1
    }

    override fun serve(session: IHTTPSession): Response {
        val method = session.method
        val uri = session.uri

        if (Method.OPTIONS == method) {
            val response = newFixedLengthResponse(Status.OK, "application/json", null)
            addHeaders(response)
            return response
        } else if (Method.GET == method && "/v1/verify" == uri) {
            val publishableKey = session.headers["authorization"]
            if (publishableKey == null) {
                val response = newFixedLengthResponse(Status.FORBIDDEN, "application/json", null)
                addHeaders(response)
                return response
            }

            val userId = session.parameters.get("Authorization")?.get(0)
            val description = session.parameters.get("Authorization")?.get(0)

            Radar.initialize(context, publishableKey)
            Radar.setUserId(userId)
            Radar.setDescription(description)

            val latch = CountDownLatch(1)
            var response = newFixedLengthResponse(Status.INTERNAL_ERROR, "application/json", null)

            // if (isAdbEnabled(context)) {
            //     val resStr = "{\"meta\":{\"code\":403,\"error\":\"ERROR_FORBIDDEN\"}}"
            //     response = newFixedLengthResponse(Status.FORBIDDEN, "application/json", resStr)
            //     latch.countDown()
            // } else {
                Radar.trackVerified { status, token ->
                    if (status == Radar.RadarStatus.SUCCESS) {
                        token?.toRawJson()?.let {
                            it.putOpt("meta", JSONObject().put("code", 200))
                            val resStr = it.toString()
                            response = newFixedLengthResponse(Status.OK, "application/json", resStr)
                        }
                    } else if (status == Radar.RadarStatus.ERROR_UNAUTHORIZED) {
                        response = newFixedLengthResponse(Status.UNAUTHORIZED, "application/json", null)
                    } else if (status == Radar.RadarStatus.ERROR_FORBIDDEN) {
                        response = newFixedLengthResponse(Status.FORBIDDEN, "application/json", null)
                    } else if (status == Radar.RadarStatus.ERROR_PERMISSIONS) {
                        val resStr = "{\"meta\":{\"code\":400,\"error\":\"ERROR_PERMISSIONS\"}}"
                        response = newFixedLengthResponse(Status.BAD_REQUEST, "application/json", resStr)
                    } else if (status == Radar.RadarStatus.ERROR_LOCATION) {
                        val resStr = "{\"meta\":{\"code\":400,\"error\":\"ERROR_LOCATION\"}}"
                        response = newFixedLengthResponse(Status.BAD_REQUEST, "application/json", resStr)
                    } else if (status == Radar.RadarStatus.ERROR_NETWORK) {
                        val resStr = "{\"meta\":{\"code\":400,\"error\":\"ERROR_NETWORK\"}}"
                        response = newFixedLengthResponse(Status.BAD_REQUEST, "application/json", resStr)
                    }

                    latch.countDown()
                }
            // }

            latch.await()

            addHeaders(response)

            return response
        } else {
            val response = newFixedLengthResponse(Status.NOT_FOUND, "application/json", null)
            addHeaders(response)
            return response
        }
    }

    override fun start() {
        super.start()

        startForegroundService()
    }

    override fun stop() {
        super.stop()

        stopForegroundService()
    }

    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(context, RadarForegroundService::class.java).apply {
                action = "start"
                putExtra("title", "Location checks started")
                putExtra("text", "Please return to the browser to continue")
            }

            context.startForegroundService(intent)
        }
    }

    private fun stopForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val stopIntent = Intent(context, RadarForegroundService::class.java).apply {
                action = "stop"
            }

            context.startService(stopIntent)
        }
    }

}