# Migration guides

## 3.2.x to 3.3.x

- `foregroundService` is no longer available in `RadarTrackingOptions`. This has been replaced by `Radar.setForegroundServiceOptions` instead.

```kotlin
// 3.3.x - enabling foreground service

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
// 3.2.x - enabling foreground service

val trackingOptions: RadarTrackingOptions = RadarTrackingOptions(...)
trackingOptions.foregroundService = RadarTrackingOptionsForegroundService(...)

Radar.startTracking(trackingOptions)
```

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
