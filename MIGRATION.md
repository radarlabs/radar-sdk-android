# Migration guides

## 1.0.x to 1.1.0

1.0.x

```
Radar.init(this);

Radar.startTracking(userId, description);

Radar.trackOnce(userId, description);
```

```
<intent-filter>
  <action android:name="com.onradar.sdk.EVENTS_RECEIVED" />
</intent-filter>
```

1.1.0

```
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

```
<intent-filter>
  <action android:name="com.onradar.sdk.RECEIVED" />
</intent-filter>
```