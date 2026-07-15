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

Debug builds of the example app can point the SDK at a locally hosted server (e.g. for testing `trackVerified` against a self-signed HTTPS dev server):

1. Set `LOCAL_DEV_HOST` in `MainActivity.kt` to your machine's LAN IP and port, e.g. `"https://192.168.X.X"` (use your LAN IP, not `localhost`). This overrides both the regular host and the verified host.
2. Run a **debug** build. Debug builds automatically trust self-signed certificates and allow the local host — no extra flags or config needed. Release builds are unaffected: none of the certificate-bypass code is compiled into the published SDK.

Leave `LOCAL_DEV_HOST` blank to use Radar's production hosts.

## Contributing

Interested in contributing? See [CONTRIBUTING.md](CONTRIBUTING.md) for how to build, test, and submit changes.

## Support

Have questions? We're here to help! Email us at [support@radar.com](mailto:support@radar.com).
