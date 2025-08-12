package io.radar.example

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import org.json.JSONObject

class MyActivityLifecycleCallbacks : Application.ActivityLifecycleCallbacks {
    override fun onActivityResumed(activity: Activity) {
        // Your custom logic. E.g. tracking, analytics, session checks, etc.
        Log.d("MyLifecycle", "onActivityResumed, Intent metadata: " + activity.intent.getStringExtra("radar_campaign_metadata"))

        val campaignMetadata = try {
            activity.intent.getStringExtra("radar_campaign_metadata")?.let { JSONObject(it) }
        } catch (e: Exception) {
            null
        }
    }
    // Other lifecycle methods (optional)
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivityDestroyed(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
}