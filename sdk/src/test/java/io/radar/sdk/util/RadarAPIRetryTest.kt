package io.radar.sdk.util

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.radar.sdk.Radar
import io.radar.sdk.RadarAPIRetryWrapper
import io.radar.sdk.RadarApiHelper
import io.radar.sdk.RadarApiHelperMock
import io.radar.sdk.RadarTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class RadarAPIRetryTest {

     companion object {
        private val apiHelperMock = RadarApiHelperMock()
        private val apiRetryWrapper = RadarAPIRetryWrapper(apiHelperMock)
        private val context: Context = ApplicationProvider.getApplicationContext()

    }

    @Before
    fun setUp() {
        apiHelperMock.retryCounter = 0
    }


    @Test
    fun testSuccessOnFirstTry() {
        val latch = CountDownLatch(1)
        val paramJson = JSONObject()
        paramJson.put("retry", 1)
        var callbackStatus: Radar.RadarStatus? = null
        var successOn: Int? = null
        val testCallback = object : RadarApiHelper.RadarApiCallback {
            override fun onComplete(status: Radar.RadarStatus, res: JSONObject?) {
                callbackStatus = status
                successOn = res?.getInt("successOn")
                latch.countDown()
            }
        }
        apiRetryWrapper.requestWithRetry(context, "GET", "http://testPath", null, paramJson, false, testCallback)

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(RadarTest.LATCH_TIMEOUT, TimeUnit.SECONDS)
        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)
        assertEquals(successOn,1)
    }
    
    @Test
    fun testSuccessOnThirdTry() {
        val latch = CountDownLatch(1)
        val paramJson = JSONObject()
        paramJson.put("retry", 3)
        var callbackStatus: Radar.RadarStatus? = null
        var successOn: Int? = null
        val testCallback = object : RadarApiHelper.RadarApiCallback {
            override fun onComplete(status: Radar.RadarStatus, res: JSONObject?) {
                callbackStatus = status
                successOn = res?.getInt("successOn")
                latch.countDown()
            }
        }
        apiRetryWrapper.requestWithRetry(context, "GET", "http://testPath", null, paramJson, false, testCallback)

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(RadarTest.LATCH_TIMEOUT, TimeUnit.SECONDS)
        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)
        assertEquals(successOn,3)
        
    }

    @Test
    fun testSuccessOnLastTry() {
        val latch = CountDownLatch(1)
        val paramJson = JSONObject()
        paramJson.put("retry", 5)
        var callbackStatus: Radar.RadarStatus? = null
        var successOn: Int? = null
        //create a mock callback
        val testCallback = object : RadarApiHelper.RadarApiCallback {
            override fun onComplete(status: Radar.RadarStatus, res: JSONObject?) {
                callbackStatus = status
                successOn = res?.getInt("successOn")
                latch.countDown()
            }
        }
        apiRetryWrapper.requestWithRetry(context, "GET", "http://testPath", null, paramJson, false, testCallback)

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(RadarTest.LATCH_TIMEOUT, TimeUnit.SECONDS)
        assertEquals(Radar.RadarStatus.SUCCESS, callbackStatus)
        assertEquals(successOn,5)
        
    }

    @Test
    fun testFailure() {
        val latch = CountDownLatch(1)
        val paramJson = JSONObject()
        paramJson.put("retry", 6)
        var callbackStatus: Radar.RadarStatus? = null
        var successOn: Int? = null
        //create a mock callback
        val testCallback = object : RadarApiHelper.RadarApiCallback {
            override fun onComplete(status: Radar.RadarStatus, res: JSONObject?) {
                callbackStatus = status
                successOn = res?.getInt("successOn")
                latch.countDown()
            }
        }
        apiRetryWrapper.requestWithRetry(context, "GET", "http://testPath", null, paramJson, false, testCallback)

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        latch.await(RadarTest.LATCH_TIMEOUT, TimeUnit.SECONDS)
        assertEquals(Radar.RadarStatus.ERROR_UNKNOWN, callbackStatus)
        assertNull(successOn)
        
    }


}
