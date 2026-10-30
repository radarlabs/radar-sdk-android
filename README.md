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

To run the example app, clone this repository, build the app, and set your publishable API key in the Tests tab under Settings → Environment. You can also set default keys in `MainActivity.kt` (`DEFAULT_PUBLISHABLE_KEY` for production, `DEFAULT_STAGING_PUBLISHABLE_KEY` for staging).

Setup Radar key check pre-commit hook to prevent accidental key leak when working with the Example app.
```
git config filter.radar-keys.clean hooks/clean-filter
git config filter.radar-keys.smudge hooks/smudge-filter
```

### Switching environments (production, staging, local server)

In the example app, open the Tests tab, tap the settings gear, and pick an environment under **Environment**: Production, Staging, or Local. Each environment keeps its own publishable key. **Apply & restart** saves the choice and relaunches the app.

- The strip across the top of the app shows the active environment, the key, and which environments the key is valid on. On launch the app checks the key against both production and staging, and the strip turns red if the key isn't valid on the selected environment.
- Errors that usually mean a wrong host or key (network, unauthorized, publishable key) include the environment, host, key, and key-check result in the Debug console.
- **Local** points both the regular and verified host at your machine's `http://` URL, e.g. `http://192.168.X.X:8081` (use your LAN IP, not `localhost`; use `10.0.2.2` on the emulator). The example permits cleartext (HTTP) traffic for this, so no extra config is needed. The production verified hosts keep strict certificate pinning.

> **Note:** the example enables cleartext (HTTP) traffic (`cleartextTrafficPermitted="true"` in `network_security_config.xml`) purely to support local testing. This is a convenience for the sample app only — do not enable cleartext in a real app, as it weakens your app's network security. Production apps should use HTTPS and leave cleartext disabled (the Android default).

## Contributing

Interested in contributing? See [CONTRIBUTING.md](CONTRIBUTING.md) for how to build, test, and submit changes.

## Support

Have questions? We're here to help! Email us at [support@radar.com](mailto:support@radar.com).
