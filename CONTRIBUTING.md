# Contributing to the Radar SDK for Android

Thanks for your interest in contributing! 💙 This guide covers how to build, test, and submit changes. For questions about your Radar integration (rather than the SDK source), email [support@radar.com](mailto:support@radar.com).

## Ways to contribute

- **Report a bug or request a feature** — open a [GitHub issue](https://github.com/radarlabs/radar-sdk-android/issues). The issue template asks for a summary, repro steps, and your OS / SDK version.
- **Submit a fix or improvement** — open a pull request (see below).

## Getting started

1. Fork and clone the repo.
2. Build the SDK: `./gradlew build`
3. (Optional) Run the example app: add your **publishable** API key in `example/.../MainActivity.kt`, then build and run the `example` module.

> Set up the API-key pre-commit hook first to avoid leaking a real key from the example app:
> ```
> cp -r hooks .git
> ```

## Build, test, and lint

All commands run from the repo root:

| Command | What it does |
| --- | --- |
| `./gradlew build` | Full build |
| `./gradlew :sdk:testDebugUnitTest` | Unit tests (Robolectric + JUnit 4) |
| `./gradlew :sdk:ktlintCheck` | Kotlin style check |
| `./gradlew :sdk:lintDebug` | Android lint (warnings are treated as errors) |

CI runs Kotlin style checks, Android lint, and the unit test suite (via `./gradlew check build`) on every PR. Run these locally before opening a PR to get a green build faster.

## Code style

- **Kotlin**, idiomatic and modern. New code should be Kotlin; some legacy Java may exist.
- `compileSdk` 36, `minSdk` 16, Java 8 target.
- Lint is strict — fix issues at the source rather than suppressing them.

### Lint & ktlint baselines

Lint and ktlint use baselines (`sdk/lint-baseline.xml`, `sdk/ktlint-baseline.xml`) to suppress pre-existing violations. Please:

- **Only fix violations on lines you changed.** Do **not** run repo-wide formatters such as `./gradlew :sdk:ktlintFormat` — they reformat every file and produce a sprawling, unreviewable diff.
- **If you do clean up a file, remove its entries from the relevant baseline** so the baseline only reflects what's still suppressed. After deleting a file's `<file name="…">…</file>` block, re-run the check; if genuine, non-auto-fixable violations remain, add just those entries back.

## Testing

- Framework: Robolectric 4.x + JUnit 4.
- Primary test surface: `sdk/src/test/java/io/radar/sdk/RadarTest.kt`, plus per-manager tests (e.g. `RadarSyncManagerTest`).
- Test helpers live alongside the tests: `matchers/` (custom assertions), `model/` (test-data builders), `util/` (`RadarMockLocationProvider`, `RadarApiHelperMock`, `RadarTestUtils`).
- Prefer extending existing helpers over introducing new mocking patterns. Add or update tests for any behavior change.

## Public API & breaking changes

- The public API entry point is `Radar.kt` (a Kotlin `object`). When adding a public API, surface it there and keep the implementation in the appropriate manager — `Radar.kt` should stay a thin facade.
- For any **breaking change**, add an entry to `MIGRATION.md` and bump `version` in `sdk/build.gradle`.
- Update the README / public docs when you change the public API.

## Opening a pull request

1. Branch off `master` and push to your fork.
2. Open a PR against `master`. The PR template will prompt you for a summary, type of change, manual test steps, and a checklist — please fill it in.
   - **Radar internal contributors:** link the Linear ticket (e.g. `FENCE-1234`).
   - **External contributors:** delete the internal-only section and link any related GitHub issue (e.g. `Closes #123`).
3. Make sure CI is green (ktlint, Android lint, unit tests).
4. A Radar team member will review your PR.

## License

By contributing, you agree that your contributions are licensed under the repository's [Apache License 2.0](LICENSE). No CLA is required.
