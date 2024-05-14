package io.radar.example

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.radar.sdk.Radar
import io.radar.sdk.model.RadarLocationPermissionsStatus

class MainActivity : AppCompatActivity() {

    // private lateinit var requestLocationPermissionLauncher: ActivityResultLauncher<Array<String>>
    // val radarLocationPermissionsManager = RadarLocationPermissionsManager(this)

    // override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    //     super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    //     if (requestCode == permissionsRequestCode) {
    //         updateUI(radarLocationPermissionsManager)
    //     }
    // }

//    private val activityLifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
//        override fun onActivityPaused(activity: Activity) {
//            // for now assume its due to permissions popup, more robust implementation later
//            val titleTextView = findViewById<TextView>(R.id.titleTextView)
//            val descriptionTextView = findViewById<TextView>(R.id.descriptionTextView)
//            val myButton = findViewById<Button>(R.id.myButton)
//            titleTextView.text = "Pending Foreground Permissions"
//            descriptionTextView.text = "Waiting for permissions."
//            myButton.text = "I don't do anything"
//            myButton.setOnClickListener {
//                // Code to request permissions
//            }
//
//        }
//
//        override fun onActivityStarted(activity: Activity) {}
//
//        override fun onActivityDestroyed(activity: Activity) {}
//
//        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
//
//        override fun onActivityStopped(activity: Activity) {}
//
//        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
//
//        override fun onActivityResumed(activity: Activity) {
//             Handler(Looper.getMainLooper()).postDelayed({
//        updateUI(radarLocationPermissionsManager)
//    }, 1000)  // Delay of 1 second
//        }
//    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        
        // var permissionsStatus = radarLocationPermissionsManager.getPermissionsStatus()
        setContentView(R.layout.location_perms)
//
//
//        application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)

        val receiver = MyRadarReceiver { context, status ->
            Log.i("example", "Permissions updated")
            updateUI(status)
        }
        Radar.initialize(this, "prj_test_pk_0000000000000000000000000000000000000000", receiver, Radar.RadarLocationServicesProvider.GOOGLE, true, this as Activity)
        Radar.sdkVersion()?.let { Log.i("version", it) }
        updateUI(Radar.getLocationPermissions())

        // val verifiedReceiver = object : RadarVerifiedReceiver() {
        //     override fun onTokenUpdated(context: Context, token: String) {
        //         Log.i("example", "Token updated to $token")
        //     }
        // }
        // Radar.setVerifiedReceiver(verifiedReceiver)

//        requestLocationPermissionLauncher = registerForActivityResult(
//            ActivityResultContracts.RequestMultiplePermissions()
//        ) { isGrantedMap ->
//            // Check if the requested permission was granted
//            if (isGrantedMap.all { it.value == true }) {
//                // runDemo()
//            } else {
//                Log.e("example", "Requires all location permissions to be granted")
//            }
//        }

//        val runDemoButton = findViewById<Button>(R.id.runDemoButton)
//        runDemoButton.setOnClickListener {
//            val requiredPermissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                requiredPermissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
//
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//                    requiredPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
//                    requiredPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
//                } else {
//                    requiredPermissions.add(Manifest.permission.BLUETOOTH_ADMIN)
//                }
//            }
//
//            requestLocationPermissionLauncher.launch(requiredPermissions.toTypedArray())
//        }
    }

    private fun updateUI(state: RadarLocationPermissionsStatus){
        // update the UI based on the permissions status
        val titleTextView = findViewById<TextView>(R.id.titleTextView)
        val descriptionTextView = findViewById<TextView>(R.id.descriptionTextView)
        val myButton = findViewById<Button>(R.id.myButton)
         when (state.status) {
        RadarLocationPermissionsStatus.PermissionStatus.NO_PERMISSIONS_GRANTED -> {
            titleTextView.text = "No Permissions"
            descriptionTextView.text = "You have not granted any permissions."
            myButton.text = "Grant Permissions"
            myButton.setOnClickListener {
                Radar.requestLocationPermissions()
            }
        }
        RadarLocationPermissionsStatus.PermissionStatus.FOREGROUND_LOCATION_PENDING -> {
            titleTextView.text = "Pending Foreground Permissions"
            descriptionTextView.text = "Waiting for permissions."
            myButton.text = "I don't do anything"
            myButton.setOnClickListener {
                // Code to request permissions
            }
        }
        RadarLocationPermissionsStatus.PermissionStatus.FOREGROUND_PERMISSIONS_GRANTED -> {
            titleTextView.text = "Foreground Permissions Granted"
            descriptionTextView.text = "You have granted foreground permissions."
            myButton.text = "Check Background Permissions"
            myButton.setOnClickListener {
                Radar.requestLocationPermissions(true)
            }
        }
        RadarLocationPermissionsStatus.PermissionStatus.BACKGROUND_PERMISSIONS_GRANTED -> {
            titleTextView.text = "Background Permissions Granted"
            descriptionTextView.text = "You have granted background permissions."
            myButton.text = "Start Tracking"
            myButton.setOnClickListener {
                // Code to start tracking
            }
        }
             RadarLocationPermissionsStatus.PermissionStatus.FOREGROUND_PERMISSIONS_REJECTED_ONCE -> {
                 titleTextView.text = "Rejected once"
                 descriptionTextView.text = "You have rejected foreground permission"
                 myButton.text = "Grant Permissions"
                 myButton.setOnClickListener {
                     // Code to request permissions
                     Radar.requestLocationPermissions()
                 }
             }
             RadarLocationPermissionsStatus.PermissionStatus.FOREGROUND_PERMISSIONS_REJECTED -> {
                 titleTextView.text = "Rejected Permissions"
                 descriptionTextView.text = "You have rejected foreground permission for good"
                 myButton.text = "I do not do anything"
                 myButton.setOnClickListener {

                 }
             }
        else -> {
            titleTextView.text = "Unknown Permissions Status"
            descriptionTextView.text = "The permissions status is unknown."
            myButton.text = "Check Permissions"
            myButton.setOnClickListener {
                // Code to check permissions
            }
        }
    }

    }

    // fun runDemo() {
    //     Radar.getLocation { status, location, stopped ->
    //         Log.v("example", "Location: status = ${status}; location = $location; stopped = $stopped")
    //     }

    //     Radar.trackOnce { status, location, events, user ->
    //         Log.v("example", "Track once: status = ${status}; location = $location; events = $events; user = $user")
    //     }

    //     val options = RadarTrackingOptions.RESPONSIVE
    //     Radar.startTracking(options)

    //     Radar.getContext { status, location, context ->
    //         Log.v("example", "Context: status = $status; location = $location; context?.geofences = ${context?.geofences}; context?.place = ${context?.place}; context?.country = ${context?.country}")
    //     }

    //     // In the Radar dashboard settings (https://radar.com/dashboard/settings), add this to
    //     // the chain metadata: {"mcdonalds":{"orderActive":"true"}}.
    //     Radar.searchPlaces(
    //         1000,
    //         arrayOf("mcdonalds"),
    //         mapOf("orderActive" to "true"),
    //         null,
    //         null,
    //         10
    //     ) { status, location, places ->
    //         Log.v("example", "Search places: status = $status; location = $location; places = $places")
    //     }

    //     Radar.searchGeofences(
    //         1000,
    //         arrayOf("store"),
    //         null,
    //         10
    //     ) { status, location, geofences ->
    //         Log.v("example", "Search geofences: status = $status; location = $location; geofences = $geofences")
    //     }

    //     Radar.geocode("20 jay street brooklyn") { status, addresses ->
    //         Log.v("example", "Geocode: status = $status; address = ${addresses?.get(0)?.formattedAddress}")
    //     }

    //     Radar.reverseGeocode { status, addresses ->
    //         Log.v("example", "Reverse geocode: status = $status; coordinate = ${addresses?.first()?.formattedAddress}")
    //     }

    //     Radar.ipGeocode { status, address, proxy ->
    //         Log.v("example", "IP geocode: status = $status; country = ${address?.countryCode}; city = ${address?.city}; proxy = $proxy")
    //     }

    //     val origin = Location("example")
    //     origin.latitude = 40.78382
    //     origin.longitude = -73.97536

    //     val destination = Location("example")
    //     destination.latitude = 40.70390
    //     destination.longitude = -73.98670

    //     Radar.autocomplete(
    //         "brooklyn",
    //         origin,
    //         arrayOf("locality"),
    //         10,
    //         "US",
    //         true,
    //     ) { status, addresses ->
    //         Log.v("example", "Autocomplete: status = $status; address = ${addresses?.get(0)?.formattedAddress}")
    //     }

    //     Radar.autocomplete(
    //         "brooklyn",
    //         origin,
    //         arrayOf("locality"),
    //         10,
    //         "US",
    //         mailable = true
    //     ) { status, addresses ->
    //         Log.v("example", "Autocomplete: status = $status; address = ${addresses?.get(0)?.formattedAddress}")
    //     }

    //     Radar.autocomplete(
    //         "brooklyn",
    //         origin,
    //         arrayOf("locality"),
    //         10,
    //         "US"
    //     ) { status, addresses ->
    //         Log.v("example", "Autocomplete: status = $status; address = ${addresses?.get(0)?.formattedAddress}")
    //     }

    //     Radar.autocomplete(
    //         "brooklyn",
    //         origin,
    //         arrayOf("locality"),
    //         10,
    //         "US",
    //         true
    //     ) { status, addresses ->
    //         Log.v("example", "Autocomplete: status = $status; address = ${addresses?.get(0)?.formattedAddress}")
    //         Radar.validateAddress(
    //             addresses?.get(0)
    //         ) { statusValidationRequest, address, verificationStatus->
    //             Log.v("example", "Validate address: status = $statusValidationRequest; address = ${address?.formattedAddress}, verificationStatus = ${Radar.stringForVerificationStatus(verificationStatus)}")
    //         }
    //     }

    //     Radar.getDistance(
    //         origin,
    //         destination,
    //         EnumSet.of(Radar.RadarRouteMode.FOOT, Radar.RadarRouteMode.CAR),
    //         Radar.RadarRouteUnits.IMPERIAL
    //     ) { status, routes ->
    //         Log.v("example", "Distance: status = $status; routes.car.distance.value = ${routes?.car?.distance?.value}; routes.car.distance.text = ${routes?.car?.distance?.text}; routes.car.duration.value = ${routes?.car?.duration?.value}; routes.car.duration.text = ${routes?.car?.duration?.text}")
    //     }

    //     val tripOptions = RadarTripOptions(
    //         "299",
    //         null,
    //         "store",
    //         "123",
    //         Radar.RadarRouteMode.CAR,
    //         approachingThreshold = 0
    //     )
    //     val onTripTrackingOptions = RadarTrackingOptions.CONTINUOUS
    //     Radar.startTrip(tripOptions, onTripTrackingOptions)

    //     var i = 0
    //     Radar.mockTracking(
    //         origin,
    //         destination,
    //         Radar.RadarRouteMode.CAR,
    //         3,
    //         3
    //     ) { status, location, events, user ->
    //         Log.v("example", "Mock track: status = ${status}; location = $location; events = $events; user = $user")

    //         if (i == 2) {
    //             Radar.completeTrip()
    //         }

    //         i++
    //     }

    //     val origin1 = Location("example")
    //     origin1.latitude = 40.78382
    //     origin1.longitude = -73.97536

    //     val origin2 = Location("example")
    //     origin2.latitude = 40.70390
    //     origin2.longitude = -73.98670

    //     val origins = arrayOf(origin1, origin2)

    //     val destination1 = Location("example")
    //     destination1.latitude = 40.64189
    //     destination1.longitude = -73.78779

    //     val destination2 = Location("example")
    //     destination2.latitude = 35.99801
    //     destination2.longitude = -78.94294

    //     val destinations = arrayOf(destination1, destination2)

    //     Radar.getMatrix(
    //         origins,
    //         destinations,
    //         Radar.RadarRouteMode.CAR,
    //         Radar.RadarRouteUnits.IMPERIAL
    //     ) { status, matrix ->
    //         Log.v("example", "Matrix: status = $status; matrix[0][0].duration.text = ${matrix?.routeBetween(0, 0)?.duration?.text}; matrix[0][1].duration.text = ${matrix?.routeBetween(0, 1)?.duration?.text}; matrix[1][0].duration.text = ${matrix?.routeBetween(1, 0)?.duration?.text};  matrix[1][1].duration.text = ${matrix?.routeBetween(1, 1)?.duration?.text}")
    //     }

    //     val conversionMetadata = JSONObject()
    //     conversionMetadata.put("one", "two")

    //     Radar.logConversion(
    //         "app_open_android",
    //         conversionMetadata
    //     ) { status, event ->
    //         Log.v("example", "Conversion name = ${event?.conversionName}: status = $status; event = $event")
    //     }
    // }

}


//class RadarLocationPermissionsManager(private val context: Context): LifecycleObserver {
//
//    fun requestForegroundPermissions() {
//        RadarLocationPermissionsStatus.saveToPreferences(context, true)
//        // do we get a popup here that takes us out of active?
//        ActivityCompat.requestPermissions(
//            context as Activity,
//            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
//            // change later to global var
//            permissionsRequestCode
//        )
//    }
//
//    fun requestBackgroundPermissions() {
//        // have a side channel thing here? the user leaves the app to go into the settings.
//        ActivityCompat.requestPermissions(
//            context as Activity,
//            arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
//            permissionsRequestCode
//        )
//    }
//
//    fun getPermissionsStatus(): RadarLocationPermissionsStatus {
//        return RadarLocationPermissionsStatus.getFromPreferences(context) ?: RadarLocationPermissionsStatus()
//    }
//
//    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
//    fun handleOnPause() {
//
//    }
//
//    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
//    fun handleOnResume() {
//
//    }
//
//    // intergrate with the radar sdk later
//
//}
//
//class RadarLocationPermissionsStatus() {
//
//    companion object {
//        private const val PREFS_NAME = "RadarLocationPermissionsStatus"
//        private const val STATUS_KEY = "status"
//        private const val DENIED_KEY = "denied"
//
//        // maybe we cna dump this into radarsettings? we are really simply saving a bool. the rest of the object is derived at call time
//        fun getFromPreferences(context: Context): RadarLocationPermissionsStatus? {
//            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
//            val foregroundPopupRequested = prefs.getBoolean(STATUS_KEY, false)
//            val previouslyDeniedForeground = prefs.getBoolean(DENIED_KEY, false)
//            return fromForegroundPopupRequested(context, foregroundPopupRequested, previouslyDeniedForeground)
//        }
//
//        fun saveToPreferences(context: Context, foregroundPopupRequested: Boolean) {
//            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
//            val editor: SharedPreferences.Editor = prefs.edit()
//            editor.putBoolean(STATUS_KEY, foregroundPopupRequested)
//            editor.apply()
//        }
//
//        fun saveDeniedOnce(context: Context, previouslyDeniedForeground: Boolean) {
//            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
//            val editor: SharedPreferences.Editor = prefs.edit()
//            editor.putBoolean(DENIED_KEY, previouslyDeniedForeground)
//            editor.apply()
//        }
//
//        internal const val KEY_STATUS = "status"
//        internal const val KEY_FOREGROUND_POPUP_REQUESTED = "foregroundPopupRequested"
//        internal const val KEY_FOREGROUND_PERMISSIONS_RESULT = "foregroundPermissionResult"
//        internal const val KEY_BACKGROUND_PERMISSIONS_RESULT = "backgroundPermissionResult"
//        internal const val KEY_SHOULD_SHOW_REQUEST_PERMISSION_RATIONALE = "shouldShowRequestPermissionRationale"
//        internal const val KEY_PREVIOUSLY_DENIED_FOREGROUND = "previouslyDeniedForeground"
//
//        private fun fromForegroundPopupRequested(context: Context, foregroundPopupRequested: Boolean, previouslyDeniedForeground: Boolean): RadarLocationPermissionsStatus {
//            val newStatus = RadarLocationPermissionsStatus()
//            newStatus.foregroundPopupRequested = foregroundPopupRequested
//            newStatus.foregroundPermissionResult = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
//            newStatus.backgroundPermissionResult = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
//            newStatus.shouldShowRequestPermissionRationale = ActivityCompat.shouldShowRequestPermissionRationale(context as Activity, Manifest.permission.ACCESS_FINE_LOCATION);
//            // if this is true, we know it is denied once
//            if (newStatus.shouldShowRequestPermissionRationale && !previouslyDeniedForeground) {
//                saveDeniedOnce(context, true)
//                newStatus.previouslyDeniedForeground = true
//            } else {
//                newStatus.previouslyDeniedForeground = previouslyDeniedForeground
//            }
//            // we map the different states to the status to be implemented
//            newStatus.status = mapToStatus(newStatus.foregroundPopupRequested, newStatus.foregroundPermissionResult, newStatus.backgroundPermissionResult, newStatus.shouldShowRequestPermissionRationale, newStatus.previouslyDeniedForeground)
//            // print all the states
//            Log.d("RadarLocationPermissionsStatus", "Foreground Popup Requested: ${newStatus.foregroundPopupRequested}")
//            Log.d("RadarLocationPermissionsStatus", "Foreground Permission Result: ${newStatus.foregroundPermissionResult}")
//            Log.d("RadarLocationPermissionsStatus", "Background Permission Result: ${newStatus.backgroundPermissionResult}")
//            Log.d("RadarLocationPermissionsStatus", "Should Show Request Permission Rationale: ${newStatus.shouldShowRequestPermissionRationale}")
//            Log.d("RadarLocationPermissionsStatus", "Status: ${newStatus.status}")
//            Log.d("RadarLocationPermissionsStatus", "Previously Denied Foreground: ${newStatus.previouslyDeniedForeground}")
//            return newStatus
//        }
//
//
//        // some notes and states we need to map to
//        // start -> f,f,f,f
//        // after requested foreground (pending) -> t,f,f,f have an in-memory flag to denote in pop-up
//        // after granted foreground -> t,t,f,f , triggered by app state
//        // after denied foreground once -> t,f,f,t trigger with app state, handled not much differently than having not granted
//        // after denied foreground -> t,f,f,f (edge, may not need to support)
//        // after requested background -> t,t,t,* (trigger from app state)
//
//        private fun mapToStatus(foregroundPopupRequested: Boolean, foregroundPermissionResult: Boolean, backgroundPermissionResult: Boolean,shouldShowRequestPermissionRationale:Boolean, previouslyDeniedForeground: Boolean): PermissionStatus {
//            if (backgroundPermissionResult) {
//                return PermissionStatus.BACKGROUND_PERMISSIONS_GRANTED
//            }
//            if (foregroundPermissionResult) {
//                return PermissionStatus.FOREGROUND_PERMISSIONS_GRANTED
//            } else {
//                if (shouldShowRequestPermissionRationale) {
//                    return PermissionStatus.FOREGROUND_PERMISSIONS_REJECTED_ONCE
//                } else {
//                    if (foregroundPopupRequested) {
//                        if (previouslyDeniedForeground) {
//                            return PermissionStatus.FOREGROUND_PERMISSIONS_REJECTED
//                        }
//                        // to do: add a check for pop-up, if not it should be unhandled
//                        return PermissionStatus.FOREGROUND_LOCATION_PENDING
//                    } else {
//                        return PermissionStatus.NO_PERMISSIONS_GRANTED
//                    }
//                }
//            }
//            return PermissionStatus.UNKNOWN
//        }
//
//    }
//
//    fun toJson(): JSONObject {
//        val jsonObject = JSONObject()
//        jsonObject.put(KEY_STATUS, status.name)
//        jsonObject.put(KEY_FOREGROUND_POPUP_REQUESTED, foregroundPopupRequested)
//        jsonObject.put(KEY_FOREGROUND_PERMISSIONS_RESULT, foregroundPermissionResult)
//        jsonObject.put(KEY_BACKGROUND_PERMISSIONS_RESULT, backgroundPermissionResult)
//        jsonObject.put(KEY_SHOULD_SHOW_REQUEST_PERMISSION_RATIONALE, shouldShowRequestPermissionRationale)
//        return jsonObject
//    }
//
//    enum class PermissionStatus {
//        NO_PERMISSIONS_GRANTED,
//        FOREGROUND_PERMISSIONS_GRANTED,
//        FOREGROUND_PERMISSIONS_REJECTED_ONCE,
//        FOREGROUND_PERMISSIONS_REJECTED,
//        FOREGROUND_LOCATION_PENDING,
//        BACKGROUND_PERMISSIONS_GRANTED,
//        UNKNOWN
//    }
//
//    var status: PermissionStatus = PermissionStatus.UNKNOWN
//    var foregroundPopupRequested: Boolean = false
//    var foregroundPermissionResult: Boolean = false
//    var backgroundPermissionResult: Boolean = false
//    var shouldShowRequestPermissionRationale: Boolean = false
//    var previouslyDeniedForeground: Boolean = false
//
//}