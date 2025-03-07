# Migration guides

## 3.20.x to 3.21.x
- The `Radar.searchPlaces(near, radius, chain, chainMetadata, groups, limit, callback)` is now  `Radar.searchPlaces(near, radius, chain, chainMetadata, groups, countryCodes, limit, callback)`. See [documentation](https://radar.com/documentation/sdk/android#search).

## 3.12.x to 3.13.x
-  The `Radar.trackVerified()` method now returns `token: RadarVerifiedLocationToken`, which includes `user`, `events`, `token,`, `expiresAt`, `expiresIn`, and `passed`. The `Radar.trackVerifiedToken()` method has been removed, since `Radar.trackVerified()` now returns a signed JWT.

```kotlin
// 3.13.x
Radar.trackVerified { (status, token) in
  if (token?.passed == true) {
    // allow access to feature, send token to server for validation
  } else {
    // deny access to feature, show error message
  }
}

// 3.12.x
Radar.trackVerified { status, location, events, user ->
  if (user?.fraud?.passed == true &&
    user.country?.allowed == true &&
    user.state?.allowed == true) {
    // allow access to feature
  } else {
    // deny access to feature, show error message
  }
}

Radar.trackVerifiedToken { status, token ->
  // send token to server for validation
}
```

## 3.11.x to 3.12.x
- The `RadarReceiver` interface has been changed to add a `onLocationPermissionStatusUpdated()` method.

## 3.9.x to 3.10.x
- The `Radar.searchGeofences()` methods have been changed. Use `includeGeometry` to include full geometry of the geofence. Radius is now optional.

## 3.8.x to 3.9.x
- The `Radar.autocomplete(query, near, layers, limit, country, expandUnits, callback)` method is now `Radar.autocomplete(query, near, layers, limit, country, expandUnits, mailable, callback)`.
  - `expandUnits` has been deprecated and will always be true regardless of value passed in.

## 3.6.x to 3.7.x
- Custom events have been renamed to conversions.
  - `Radar.sendEvent(customType, metadata, callback)` is now `Radar.logConversion(name, metadata, callback)`.
  - `Radar.logConversion(name, revenue, metadata, callback)` has been added.
  - `Radar.sendEvent(customType, metadata, location, callback)` has been removed.
  - `RadarSendEventCallback` has been renamed to `RadarLogConversionCallback`.
    - `onComplete(status, location, events, user)` is now `onComplete(status, event)`. `location` and `user` are no longer available, and only the conversion event is returned as `event` instead of a coalesced list of events.
  - On `RadarEvent`, `customType` is now `conversionName`, and `RadarEventType.CUSTOM` is now `RadarEventType.CONVERSION`.

```kotlin
// 3.7.0 - logging conversions
val metadata = JSONObject().put("foo", "bar")

val callback = object : Radar.RadarLogConversionCallback {
      override fun onComplete(status: Radar.RadarStatus, event: RadarEvent?) {
            val conversionName = event?.conversionName // should be "conversion_with_callback"
            val conversionType = event?.type // should be RadarEvent.RadarEventType.CONVERSION
      }
}

Radar.logConversion("conversion_with_callback", metadata, callback)

Radar.logConversion("conversion_with_revenue", 0.2, metadata) { status, event ->
      val revenue = event?.metadata?.get("revenue") // should be 0.2
}
```

```kotlin
// 3.6.x - logging conversions
val metadata = JSONObject().put("foo", "bar")

val callback = object : Radar.RadarSendEventCallback {
      override fun onComplete(status: Radar.RadarStatus, location: Location?, events: Array<RadarEvent>?, user: RadarUser?) {

      }
}

Radar.sendEvent("custom_event_with_callback", metadata, callback)

// sendEvent() with location no longer exists in 3.7.0
Radar.sendEvent("event_with_location", Location(...), metadata) { status, location, events, user ->

}
```

## 3.5.7 to 3.5.8
- If your application depends on classes in `com.google.android.gms.location`, you may need to update your code to reflect the changes made to `play-services-location` in version 20.0.0, as documented [here](https://developers.google.com/android/guides/releases#october_13_2022).  Version 3.5.8 of the Radar SDK updates `play-services-location` to version 21.0.1. Besides the Radar SDK, if your application does not depend on classes in `com.google.android.gms.location` no migrations are necessary.
 
## 3.3.x to 3.4.x

- `foregroundService` is no longer available in `RadarTrackingOptions`. This has been replaced by `Radar.setForegroundServiceOptions` instead.

```kotlin
// 3.4.x - enabling foreground service
// enable or disable the foreground service
val trackingOptions: RadarTrackingOptions = RadarTrackingOptions(...)
trackingOptions.foregroundServiceEnabled = true
// set the foreground service options
val foregroundOptions: RadarTrackingOptionsForegroundService = RadarTrackingOptionsForegroundService(...)
Radar.setForegroundServiceOptions(foregroundOptions)
// start tracking
Radar.startTracking(trackingOptions)
```

```kotlin
// 3.3.x - enabling foreground service
val trackingOptions: RadarTrackingOptions = RadarTrackingOptions(...)
trackingOptions.foregroundService = RadarTrackingOptionsForegroundService(...)
Radar.startTracking(trackingOptions)
```

## 3.2.x to 3.3.x

No changes needed.

## 3.1.x to 3.2.x

- `RadarReceiver` no longer subclasses `BroadcastReceiver`. Instead of registering `RadarReceiver` in your manifest, pass an instance to `Radar.initialize()` in application `onCreate()`.
- `RadarTripCallback` now returns `trip` and `events` on calls to `Radar.startTrip()`, `Radar.updateTrip()`, `Radar.completeTrip()`, and `Radar.cancelTrip()`.
- On `RadarReceiver`, `user` is now optional on `onEventsReceived()`. `user` will be `null` when events are delivered from calls to `Radar.startTrip()`, `Radar.updateTrip()`, `Radar.completeTrip()`, and `Radar.cancelTrip()`.
- `trackingOptions.foregroundService` now starts a foreground service by default when using `RadarTrackingOptions.CONTINUOUS`. If you are already starting a foreground service when using `RadarTrackingOptions.CONTINUOUS`, consider using `trackingOptions.foregroundService` instead.

```kotlin
// 3.2.x

// instead of registering `RadarReceiver` in your manifest, pass `RadarReceiver` to `initialize()`
val receiver = MyRadarReceiver()
Radar.initialize(context, publishableKey, receiver)

// `RadarTripCallback` now returns `trip` and `events`
Radar.startTrip(tripOptions) { status, trip, events ->
  
}

// `user` is now optional
override fun onEventsReceived(context: Context, events: Array<RadarEvent>, user: RadarUser?) {
  
}
```

```kotlin
// 3.1.x

Radar.initialize(context, publishableKey)

Radar.startTrip(options: options) { status in
  
}

override fun onEventsReceived(context: Context, events: Array<RadarEvent>, user: RadarUser) {
  
}
```

## 3.0.x to 3.1.x

- The `Radar.trackOnce(desiredAccuracy, callback)` method is now `Radar.trackOnce(desiredAccuracy, beacons, callback)`. Use `beacons = true` to range beacons.
- The `Radar.stopTrip()` method has been removed. Call `Radar.completeTrip()` or `Radar.cancelTrip()` instead.
- The `ACCESS_BACKGROUND_LOCATION` permission has been removed from the SDK manifest. If using background location, you must manually add the permission to your app manifest.

## 2.1.x to 3.0.x

- This update introduces new tracking options and presets. See the [announcement](https://radar.io/blog/open-source-radar-sdk-v3-custom-tracking-options-public-beta), the [background tracking documentation](https://radar.io/documentation/sdk-v3#android-background), and the [tracking options reference](https://radar.io/documentation/sdk/tracking#android).
  - If you were using `Radar.startTracking()`, you must choose a preset. v2 default behavior was similar to `Radar.startTracking(RadarTrackingOptions.RESPONSIVE)`.
  - If you were using `RadarTrackingOptions.Builder().priority(RadarTrackingPriority.EFFICIENCY)`, use the preset `RadarTrackingOptions.EFFICIENT` instead.
- Support Library dependencies have been migrated to [AndroidX](https://developer.android.com/jetpack/androidx). The `WorkManager` dependency has been removed.
- The `Radar.initialize(context, publishableKey)` method now requires `context` and `publishableKey`.
- The `Radar.updateLocation(location, callback)` method has been renamed to `Radar.trackOnce(location, callback)`.
- The `onClientLocationUpdated()` method is now required in `RadarReceiver`. It tells the receiver that the client's location was updated, but not necessarily synced to the server. To receive only server-synced location updates and user state, use `onLocationUpdated()` instead.
- `adId` collection is now optional. To collect `adId`, call `Radar.setAdIdEnabled(true)`.
- `Radar.setPlacesProvider(placesProvider)` has been removed.

```java
// 3.0.x

Radar.initialize(context, publishableKey);

Radar.trackOnce(location, callback);

Radar.startTracking(RadarTrackingOptions.EFFICIENT);

Radar.setAdIdEnabled(true);
```

```java
// 2.1.x

Radar.initialize(publishableKey);

Radar.updateLocation(location, callback);

RadarTrackingOptions trackingOptions = new RadarTrackingOptions.Builder()
      .priority(RadarTrackingPriority.EFFICIENCY)
      .build();
Radar.startTracking(trackingOptions);
```

## 2.0.x to 2.1.x

- This update introduces `Radar.startTracking(trackingOptions)` to configure advanced tracking options. See https://radar.io/documentation/sdk-v2.
- The `Radar.setTrackingPriority(priority)` method has been removed. Use `RadarTrackingOptions.Builder().priority(priority)` and call `Radar.startTracking(trackingOptions)` instead. See https://radar.io/documentation/sdk-v2.

```java
// 2.1.x

RadarTrackingOptions trackingOptions = new RadarTrackingOptions.Builder()
      .priority(RadarTrackingPriority.EFFICIENCY)
      .build();
Radar.startTracking(trackingOptions);
```

```java
// 2.0.x

Radar.setTrackingPriority(RadarPriority.EFFICIENCY);
Radar.startTracking(trackingOptions);
```

## 1.3.x to 2.0.x

- The package has been renamed from `com.onradar.sdk.*` to `io.radar.sdk.*`.
- The `Radar.initialize(activity)` and `Radar.initialize(activity, publishableKey)` methods have been removed. Call `Radar.initialize(publishableKey)` instead.
- The `RadarReceiver` action has been renamed from `com.onradar.sdk.RECEIVED` to `io.radar.sdk.RECEIVED`. Update the `intent-filter` in your manifest.
- The `RadarCallback` class has moved from `com.onradar.sdk.RadarCallback` to `io.radar.sdk.Radar.RadarCallback`, and the `onCallback()` method has been renamed to `onComplete()`.
- The `Radar.requestPermissions()` and `Radar.checkSelfPermissions()` helper methods have been removed. Call the corresponding methods on `ActivityCompat` and `ContextCompat` instead. https://developer.android.com/training/permissions/requesting
- The `RadarStatus.ERROR_USER_ID` and `RadarStatus.ERROR_PLACES` enum values have been removed. The SDK now handles these cases gracefully.

## 1.2.x to 1.3.x

- On `RadarUser`, `userId` is now nullable.
- The `Radar.reidentifyUser()` method has been removed. To reidentify a user, call `Radar.setUserId()` with the new `userId` instead.
