# Migration guides

## 1.2.x to 1.3.0

`userId` on `RadarUser` is now nullable and `reidentifyUser()` has been removed. To reidentify a user, simply call `setUserId()` with the new `userId`.

No other code changes are required to upgrade from 1.2.x to 1.3.0.

## 1.1.x to 1.2.0

No code changes are required to upgrade from 1.1.x to 1.2.0.

## 1.0.x to 1.1.0

1.0.x

```java
Radar.init(this);

Radar.startTracking(userId, description);

Radar.trackOnce(userId, description);
```

```xml
<intent-filter>
  <action android:name="com.onradar.sdk.EVENTS_RECEIVED" />
</intent-filter>
```

1.1.0

```java
Radar.initialize(this);

Radar.setUserId(userId);
Radar.setDescription(description);

Radar.startTracking();

Radar.trackOnce(new RadarCallback() {
  @Override
  public void onCallback(RadarStatus status, Location location, RadarEvent[] events, RadarUser user) {
    // do something with status, location, events, user
  }
});
```

```xml
<intent-filter>
  <action android:name="com.onradar.sdk.RECEIVED" />
</intent-filter>
```
