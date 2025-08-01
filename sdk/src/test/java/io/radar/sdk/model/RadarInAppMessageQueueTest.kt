//package io.radar.sdk.model
//
//import android.content.Context
//import android.content.SharedPreferences
//import android.os.Build
//import androidx.test.ext.junit.runners.AndroidJUnit4
//import io.radar.sdk.RadarState
//import io.radar.sdk.RadarTestUtils
//import junit.framework.Assert.assertNull
//import org.json.JSONObject
//import org.junit.Assert.*
//import org.junit.Before
//import org.junit.Test
//import org.junit.runner.RunWith
//import org.robolectric.annotation.Config
//import org.robolectric.RuntimeEnvironment
//
//@RunWith(AndroidJUnit4::class)
//@Config(sdk = [Build.VERSION_CODES.P])
//class RadarInAppMessageQueueTest {
//
//    private lateinit var context: Context
//    private lateinit var sharedPreferences: SharedPreferences
//
//    @Before
//    fun setUp() {
//        context = RuntimeEnvironment.getApplication()
//        sharedPreferences = context.getSharedPreferences("RadarSDK", Context.MODE_PRIVATE)
//        // Clear any existing data before each test
//        sharedPreferences.edit().clear().apply()
//    }
//
//    // RadarInAppMessagePayload JSON Serialization Tests
//
//    @Test
//    fun testPayloadToJson() {
//        val payload = RadarInAppMessagePayload(
//            title = "Welcome Message",
//            message = "Thanks for using our app!",
//            buttonText = "Get Started"
//        )
//
//        val jsonString = payload.toJson()
//        val jsonObject = JSONObject(jsonString)
//
//        assertEquals("Welcome Message", jsonObject.getString("title"))
//        assertEquals("Thanks for using our app!", jsonObject.getString("message"))
//        assertEquals("Get Started", jsonObject.getString("buttonText"))
//    }
//
//    @Test
//    fun testPayloadFromJson() {
//        val jsonString = """
//            {
//                "title": "Test Title",
//                "message": "Test Message",
//                "buttonText": "Test Button"
//            }
//        """.trimIndent()
//
//        val payload = RadarInAppMessagePayload.fromJson(jsonString)
//
//        assertNotNull(payload)
//        assertEquals("Test Title", payload?.title)
//        assertEquals("Test Message", payload?.message)
//        assertEquals("Test Button", payload?.buttonText)
//    }
//
//    @Test
//    fun testPayloadFromJsonWithMissingFields() {
//        val jsonString = """
//            {
//                "title": "Test Title",
//                "message": "Test Message"
//            }
//        """.trimIndent()
//
//        val payload = RadarInAppMessagePayload.fromJson(jsonString)
//
//        assertNull(payload)
//    }
//
//    @Test
//    fun testPayloadFromJsonWithInvalidJson() {
//        val invalidJson = "{ invalid json }"
//
//        val payload = RadarInAppMessagePayload.fromJson(invalidJson)
//
//        assertNull(payload)
//    }
//
//    @Test
//    fun testPayloadRoundTrip() {
//        val originalPayload = RadarInAppMessagePayload(
//            title = "Original Title",
//            message = "Original Message",
//            buttonText = "Original Button"
//        )
//
//        val jsonString = originalPayload.toJson()
//        val deserializedPayload = RadarInAppMessagePayload.fromJson(jsonString)
//
//        assertNotNull(deserializedPayload)
//        assertEquals(originalPayload.title, deserializedPayload?.title)
//        assertEquals(originalPayload.message, deserializedPayload?.message)
//        assertEquals(originalPayload.buttonText, deserializedPayload?.buttonText)
//    }
//
//    // RadarState Queue Tests
//
//    @Test
//    fun testEnqueueInAppMessage() {
//        val payload = RadarInAppMessagePayload(
//            title = "Test Title",
//            message = "Test Message",
//            buttonText = "Test Button"
//        )
//
//        RadarState.enqueueInAppMessage(context, payload)
//
//        val queue = RadarState.getInAppMessageQueue(context)
//        assertEquals(1, queue.size)
//        assertEquals(payload.title, queue[0].title)
//        assertEquals(payload.message, queue[0].message)
//        assertEquals(payload.buttonText, queue[0].buttonText)
//    }
//
//    @Test
//    fun testEnqueueMultipleMessages() {
//        val payload1 = RadarInAppMessagePayload("Title 1", "Message 1", "Button 1")
//        val payload2 = RadarInAppMessagePayload("Title 2", "Message 2", "Button 2")
//        val payload3 = RadarInAppMessagePayload("Title 3", "Message 3", "Button 3")
//
//        RadarState.enqueueInAppMessage(context, payload1)
//        RadarState.enqueueInAppMessage(context, payload2)
//        RadarState.enqueueInAppMessage(context, payload3)
//
//        val queue = RadarState.getInAppMessageQueue(context)
//        assertEquals(3, queue.size)
//        assertEquals("Title 1", queue[0].title)
//        assertEquals("Title 2", queue[1].title)
//        assertEquals("Title 3", queue[2].title)
//    }
//
//    @Test
//    fun testDequeueInAppMessage() {
//        val payload1 = RadarInAppMessagePayload("Title 1", "Message 1", "Button 1")
//        val payload2 = RadarInAppMessagePayload("Title 2", "Message 2", "Button 2")
//
//        RadarState.enqueueInAppMessage(context, payload1)
//        RadarState.enqueueInAppMessage(context, payload2)
//
//        val dequeuedPayload = RadarState.dequeueInAppMessage(context)
//
//        assertNotNull(dequeuedPayload)
//        assertEquals("Title 1", dequeuedPayload?.title)
//
//        val remainingQueue = RadarState.getInAppMessageQueue(context)
//        assertEquals(1, remainingQueue.size)
//        assertEquals("Title 2", remainingQueue[0].title)
//    }
//
//    @Test
//    fun testDequeueFromEmptyQueue() {
//        val dequeuedPayload = RadarState.dequeueInAppMessage(context)
//
//        assertNull(dequeuedPayload)
//    }
//
//    @Test
//    fun testDequeueAllMessages() {
//        val payload1 = RadarInAppMessagePayload("Title 1", "Message 1", "Button 1")
//        val payload2 = RadarInAppMessagePayload("Title 2", "Message 2", "Button 2")
//
//        RadarState.enqueueInAppMessage(context, payload1)
//        RadarState.enqueueInAppMessage(context, payload2)
//
//        val firstDequeued = RadarState.dequeueInAppMessage(context)
//        val secondDequeued = RadarState.dequeueInAppMessage(context)
//        val thirdDequeued = RadarState.dequeueInAppMessage(context)
//
//        assertNotNull(firstDequeued)
//        assertNotNull(secondDequeued)
//        assertNull(thirdDequeued)
//
//        assertEquals("Title 1", firstDequeued?.title)
//        assertEquals("Title 2", secondDequeued?.title)
//
//        assertTrue(RadarState.getInAppMessageQueue(context).isEmpty())
//    }
//
//    @Test
//    fun testClearInAppMessageQueue() {
//        val payload1 = RadarInAppMessagePayload("Title 1", "Message 1", "Button 1")
//        val payload2 = RadarInAppMessagePayload("Title 2", "Message 2", "Button 2")
//
//        RadarState.enqueueInAppMessage(context, payload1)
//        RadarState.enqueueInAppMessage(context, payload2)
//
//        assertEquals(2, RadarState.getInAppMessageQueueSize(context))
//
//        RadarState.clearInAppMessageQueue(context)
//
//        assertEquals(0, RadarState.getInAppMessageQueueSize(context))
//        assertTrue(RadarState.getInAppMessageQueue(context).isEmpty())
//    }
//
//    @Test
//    fun testGetInAppMessageQueueSize() {
//        assertEquals(0, RadarState.getInAppMessageQueueSize(context))
//
//        val payload1 = RadarInAppMessagePayload("Title 1", "Message 1", "Button 1")
//        RadarState.enqueueInAppMessage(context, payload1)
//        assertEquals(1, RadarState.getInAppMessageQueueSize(context))
//
//        val payload2 = RadarInAppMessagePayload("Title 2", "Message 2", "Button 2")
//        RadarState.enqueueInAppMessage(context, payload2)
//        assertEquals(2, RadarState.getInAppMessageQueueSize(context))
//
//        RadarState.dequeueInAppMessage(context)
//        assertEquals(1, RadarState.getInAppMessageQueueSize(context))
//    }
//
//    @Test
//    fun testQueuePersistence() {
//        val payload = RadarInAppMessagePayload("Persistent Title", "Persistent Message", "Persistent Button")
//
//        RadarState.enqueueInAppMessage(context, payload)
//
//        // Verify the queue is stored in SharedPreferences
//        val queueJson = sharedPreferences.getString("in_app_message_queue", null)
//        assertNotNull(queueJson)
//
//        // Verify the JSON format is correct
//        val jsonArray = org.json.JSONArray(queueJson)
//        assertEquals(1, jsonArray.length())
//
//        val payloadJson = jsonArray.getString(0)
//        val parsedPayload = RadarInAppMessagePayload.fromJson(payloadJson)
//
//        assertNotNull(parsedPayload)
//        assertEquals(payload.title, parsedPayload?.title)
//    }
//
//    @Test
//    fun testQueueWithSpecialCharacters() {
//        val payload = RadarInAppMessagePayload(
//            title = "Title with \"quotes\" and 'apostrophes'",
//            message = "Message with\nnewlines\tand\ttabs",
//            buttonText = "Button with émojis 🎉"
//        )
//
//        RadarState.enqueueInAppMessage(context, payload)
//        val dequeuedPayload = RadarState.dequeueInAppMessage(context)
//
//        assertNotNull(dequeuedPayload)
//        assertEquals(payload.title, dequeuedPayload?.title)
//        assertEquals(payload.message, dequeuedPayload?.message)
//        assertEquals(payload.buttonText, dequeuedPayload?.buttonText)
//    }
//
//    @Test
//    fun testQueueWithEmptyStrings() {
//        val payload = RadarInAppMessagePayload("", "", "")
//
//        RadarState.enqueueInAppMessage(context, payload)
//        val dequeuedPayload = RadarState.dequeueInAppMessage(context)
//
//        assertNotNull(dequeuedPayload)
//        assertEquals("", dequeuedPayload?.title)
//        assertEquals("", dequeuedPayload?.message)
//        assertEquals("", dequeuedPayload?.buttonText)
//    }
//
//    @Test
//    fun testQueueWithVeryLongStrings() {
//        val longTitle = "A".repeat(1000)
//        val longMessage = "B".repeat(2000)
//        val longButtonText = "C".repeat(500)
//
//        val payload = RadarInAppMessagePayload(longTitle, longMessage, longButtonText)
//
//        RadarState.enqueueInAppMessage(context, payload)
//        val dequeuedPayload = RadarState.dequeueInAppMessage(context)
//
//        assertNotNull(dequeuedPayload)
//        assertEquals(longTitle, dequeuedPayload?.title)
//        assertEquals(longMessage, dequeuedPayload?.message)
//        assertEquals(longButtonText, dequeuedPayload?.buttonText)
//    }
//
//    @Test
//    fun testQueueOrdering() {
//        val payloads = listOf(
//            RadarInAppMessagePayload("First", "Message 1", "Button 1"),
//            RadarInAppMessagePayload("Second", "Message 2", "Button 2"),
//            RadarInAppMessagePayload("Third", "Message 3", "Button 3")
//        )
//
//        // Enqueue all payloads
//        payloads.forEach { RadarState.enqueueInAppMessage(context, it) }
//
//        // Dequeue and verify order
//        payloads.forEachIndexed { index, expectedPayload ->
//            val dequeuedPayload = RadarState.dequeueInAppMessage(context)
//            assertNotNull(dequeuedPayload)
//            assertEquals(expectedPayload.title, dequeuedPayload?.title)
//            assertEquals(expectedPayload.message, dequeuedPayload?.message)
//            assertEquals(expectedPayload.buttonText, dequeuedPayload?.buttonText)
//        }
//
//        // Verify queue is empty
//        assertNull(RadarState.dequeueInAppMessage(context))
//    }
//}