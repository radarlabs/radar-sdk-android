# Migration guides

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

// 2.1.x
Radar.initialize(publishableKey);

Radar.updateLocation(location, callback);

RadarTrackingOptions trackingOptions = new RadarTrackingOptions.Builder()
      .priority(RadarTrackingPriority.EFFICIENCY)
      .build();
Radar.startTracking(trackingOptions);
```

## 2.0.x to 2.1.x

- This update introduces `Radar.startTracking(trackingOptions)` to configure advanced tracking options. See https://radar.io/documentation/sdk/android-background.
- The `Radar.setTrackingPriority(priority)` method has been removed. Use `RadarTrackingOptions.Builder().priority(priority)` and call `Radar.startTracking(trackingOptions)` instead. See https://radar.io/documentation/sdk/android-background.

```java
// 2.1.x
RadarTrackingOptions trackingOptions = new RadarTrackingOptions.Builder()
      .priority(RadarTrackingPriority.EFFICIENCY)
      .build();
Radar.startTracking(trackingOptions);

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

- `userId` on `RadarUser` is now nullable.
- The `Radar.reidentifyUser()` method has been removed. To reidentify a user, call `Radar.setUserId()` with the new `userId` instead.
