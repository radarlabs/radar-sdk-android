![Radar](https://raw.githubusercontent.com/radarlabs/radar-sdk-android/master/logo.png?v=3)

[![Maven Central](https://shields.io/maven-central/v/io.radar/sdk)](https://search.maven.org/artifact/io.radar/sdk)
[![CircleCI](https://circleci.com/gh/radarlabs/radar-sdk-android/tree/master.svg?style=shield)](https://app.circleci.com/pipelines/github/radarlabs/radar-sdk-android?branch=master)

[Radar](https://radar.io) is the leading geofencing and location tracking platform.

The Radar SDK abstracts away cross-platform differences between location services, allowing you to add geofencing, location tracking, trip tracking, geocoding, and search to your apps with just a few lines of code.

## Documentation

See the full documentation [here](https://radar.io/documentation).

You can also see a detailed SDK reference [here](https://radarlabs.github.io/radar-sdk-android/).

## Migrating

See migration guides in `MIGRATION.md`.

## Examples

See an example app in `example/`.

To run the example app, clone this repository, add your publishable API key in `MainActivity.kt`, and build the app.

Setup Radar public key check pre-commit hook with `cp -r hooks .git` to prevent accidental key leak when working with the Example app.

### Testing against a local server

The example app can point the SDK at a locally hosted HTTP server (e.g. for testing `trackVerified` against a local dev server):

1. Set `TARGET_HOST` in `MainActivity.kt` to your machine's `http://` URL, e.g. `"http://192.168.X.X:8081"` (use your LAN IP, not `localhost`; use `10.0.2.2` on the emulator). This overrides both the regular host and the verified host.
2. Run the app. The example permits cleartext (HTTP) traffic to the local host — no extra flags or config needed. The production verified hosts keep strict certificate pinning.

Leave `TARGET_HOST` blank to use Radar's production hosts.

> **Note:** the example enables cleartext (HTTP) traffic (`cleartextTrafficPermitted="true"` in `network_security_config.xml`) purely to support local testing. This is a convenience for the sample app only — do not enable cleartext in a real app, as it weakens your app's network security. Production apps should use HTTPS and leave cleartext disabled (the Android default).

## Contributing

Interested in contributing? See [CONTRIBUTING.md](CONTRIBUTING.md) for how to build, test, and submit changes.

## Support

Have questions? We're here to help! Email us at [support@radar.com](mailto:support@radar.com).
