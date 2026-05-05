# Radar SDK for Android — Agent Guide

This file orients AI coding agents (Claude Code, Cursor, Aider, etc.) working on this repo. `CLAUDE.md` is a symlink to this file — edit `AGENTS.md` only.

## Project

Radar's Android SDK for geofencing, location tracking, trips, geocoding, beacons, and verification. Published as a library to Maven Central. Public docs: https://radar.io/documentation.

- Current version: see `version` in [sdk/build.gradle](sdk/build.gradle)
- Migration history: [MIGRATION.md](MIGRATION.md)
- User-facing overview: [README.md](README.md)

## Build, test, lint

All commands run from the repo root:

- `./gradlew build` — full build
- `./gradlew test` — unit tests (Robolectric + JUnit 4)
- `./gradlew lint` — Android lint. Warnings are errors (`lintOptions { warningsAsErrors true }` in [sdk/build.gradle](sdk/build.gradle)) — a lint warning will fail the build.
- `./gradlew :sdk:assembleRelease` — SDK-only release build

The example app requires an API key set in its `MainActivity`. A pre-commit hook validates that real API keys aren't committed in the example.

## Repo layout

- [sdk/](sdk/) — the published library
  - [sdk/src/main/java/io/radar/sdk/](sdk/src/main/java/io/radar/sdk/) — source
  - [sdk/src/test/java/io/radar/sdk/](sdk/src/test/java/io/radar/sdk/) — tests
- [example/](example/) — sample app demonstrating SDK usage
- `sdk-fraud/` — optional fraud-detection module (git submodule, conditionally included by Gradle if present)
- [buildSrc/](buildSrc/) — custom Gradle plugins and CI helpers
- [.github/workflows/radar-release-actions.yml](.github/workflows/radar-release-actions.yml) — release CI

## Architecture

Public API entry point: [sdk/src/main/java/io/radar/sdk/Radar.kt](sdk/src/main/java/io/radar/sdk/Radar.kt) — a Kotlin `object` (singleton) exposing all SDK methods and callback interfaces (`RadarLocationCallback`, `RadarBeaconCallback`, etc.).

Internal structure under [sdk/src/main/java/io/radar/sdk/](sdk/src/main/java/io/radar/sdk/):

- **Managers** — orchestration of subsystems
  - `RadarLocationManager` — background location tracking
  - `RadarSyncManager` — event sync and batching
  - `RadarOfflineEventManager` — offline event queueing with retry-timeout ramping
  - `RadarBeaconManager` — beacon ranging
  - `RadarVerificationManager` — location verification
  - `RadarInAppMessageManager` — in-app messages
- **Networking**
  - `RadarApiClient.kt` — HTTP requests and response parsing
  - `RadarApiHelper.kt` — request helpers
- **Location clients** (pluggable per device)
  - `RadarAbstractLocationClient` — base interface
  - `RadarGoogleLocationClient` — Google Play Services
  - `RadarHuaweiLocationClient` — Huawei HMS
- **State / config**
  - `RadarState`, `RadarSettings` (SharedPreferences-backed)
  - `RadarSdkConfiguration`, `RadarRemoteTrackingOptions`
  - `RadarLogger`, `RadarNotificationHelper`
- **Background entry points**
  - `RadarReceiver`, `RadarLocationReceiver` — broadcast receivers
  - `RadarForegroundService` — continuous foreground tracking
  - `RadarJobScheduler` — `JobScheduler` integration
- **Models** — [sdk/src/main/java/io/radar/sdk/model/](sdk/src/main/java/io/radar/sdk/model/) — data classes (`RadarUser`, `RadarEvent`, `RadarGeofence`, `RadarTrip`, `RadarAddress`, `RadarBeacon`, etc.)

When adding a public API, surface it via `Radar.kt` and keep the implementation in the appropriate manager — `Radar.kt` should stay a thin facade.

## Testing

- Framework: Robolectric 4.x + JUnit 4 (via `androidx.test.ext:junit`)
- Primary test surface: [sdk/src/test/java/io/radar/sdk/RadarTest.kt](sdk/src/test/java/io/radar/sdk/RadarTest.kt)
- Per-manager tests: e.g. `RadarSyncManagerTest`, `RadarOfflineEventManagerTest`
- Test helpers live alongside tests:
  - `matchers/` — custom assertion matchers
  - `model/` — test-data builders
  - `util/` — shared test utilities (`RadarMockLocationProvider`, `RadarApiHelperMock`, `RadarTestUtils`)
- Style: spies + mocks against real Robolectric `Context`. Prefer extending existing helpers over introducing new mocking patterns.

## Code style

- Language: Kotlin (idiomatic / modern). Some legacy Java may exist; new code should be Kotlin.
- `compileSdk` 36, `minSdk` 16, Java 8 target — see [sdk/build.gradle](sdk/build.gradle).
- Lint is strict (warnings = errors). Fix at the source rather than suppressing.

## Release

- Published via the New Maven Central Portal aggregation plugin.
- Release tags trigger [.github/workflows/radar-release-actions.yml](.github/workflows/radar-release-actions.yml), which runs `./gradlew publishAggregationToCentralPortal`.
- Bump the version in [sdk/build.gradle](sdk/build.gradle) and add a [MIGRATION.md](MIGRATION.md) entry for any breaking changes.
