package io.radar.example

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.location_perms)


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
                myButton.visibility = View.VISIBLE
                titleTextView.text = "No Permissions"
                descriptionTextView.text = "You have not granted any locations permissions. We need your location for this demo"
                myButton.text = "Grant foreground Permissions"
                myButton.setOnClickListener {
                    Radar.requestForegroundLocationPermissions()
                }
            }
            RadarLocationPermissionsStatus.PermissionStatus.FOREGROUND_LOCATION_PENDING -> {
                myButton.visibility = View.GONE
                titleTextView.text = "Pending Foreground Permissions"
                descriptionTextView.text = "Waiting for permissions."

            }
            RadarLocationPermissionsStatus.PermissionStatus.FOREGROUND_PERMISSIONS_GRANTED -> {
                myButton.visibility = View.VISIBLE 
                titleTextView.text = "Foreground Permissions Granted"
                descriptionTextView.text = "You have granted foreground permissions. Please also grant background permissions."
                myButton.text = "Grant Background Permissions"
                myButton.setOnClickListener {
                    Radar.requestBackgroundLocationPermissions()
                }
            }
            RadarLocationPermissionsStatus.PermissionStatus.APPROXIMATE_PERMISSIONS_GRANTED -> {
                myButton.visibility = View.VISIBLE 
                titleTextView.text = "Foreground approximate Permissions Granted"
                descriptionTextView.text = "You have granted approximate foreground permissions. That not enough for this demo. Please also grant background permissions."
                myButton.text = "Check Background Permissions"
                myButton.setOnClickListener {
                    Radar.requestBackgroundLocationPermissions()
                }
            }

            RadarLocationPermissionsStatus.PermissionStatus.BACKGROUND_PERMISSIONS_GRANTED -> {
                myButton.visibility = View.GONE 
                titleTextView.text = "Background Permissions Granted"
                descriptionTextView.text = "You have granted background permissions. We have all the permissions we need, thanks!"
            }
            RadarLocationPermissionsStatus.PermissionStatus.FOREGROUND_PERMISSIONS_REJECTED_ONCE -> {
                myButton.visibility = View.VISIBLE 
                titleTextView.text = "Rejected once"
                descriptionTextView.text = "You have rejected foreground permission, for now. But we would still like to ask again."
                myButton.text = "Grant Permissions"
                myButton.setOnClickListener {
                    Radar.requestForegroundLocationPermissions()
                }
            }
            RadarLocationPermissionsStatus.PermissionStatus.FOREGROUND_PERMISSIONS_REJECTED -> {
                myButton.visibility = View.VISIBLE 
                titleTextView.text = "Rejected foreground Permissions"
                descriptionTextView.text = "You have rejected foreground permission for good, please activate it in the settings."
                myButton.text = "Go to settings to change it"
                myButton.setOnClickListener {
//                    Radar.requestForegroundLocationPermissions()
                    Radar.updateLocationPermissionsStatusOnActivityResume()
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                }
            }
            RadarLocationPermissionsStatus.PermissionStatus.BACKGROUND_PERMISSIONS_REJECTED -> {
                myButton.visibility = View.VISIBLE
                titleTextView.text = "Rejected background Permissions"
                descriptionTextView.text = "You have rejected background permission for good, please activate it in the settings."
                myButton.text = "Go to settings to change it"
                myButton.setOnClickListener {
//                    Radar.requestForegroundLocationPermissions()
                    Radar.updateLocationPermissionsStatusOnActivityResume()
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                }
            }
            else -> {
                myButton.visibility = View.VISIBLE 
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
