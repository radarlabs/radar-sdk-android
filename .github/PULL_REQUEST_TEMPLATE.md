<!--
Thanks for contributing to the Radar Android SDK! 💙

External contributors: a Radar team member will review your PR. By contributing you
agree your changes are licensed under this repo's Apache 2.0 license. No CLA needed.

Before opening, please run the same checks CI runs:
  ./gradlew :sdk:ktlintCheck
  ./gradlew :sdk:lintDebug
  ./gradlew test
-->

## Summary

<!-- What does this PR do, and why? Keep it focused. -->

## Linear ticket (Radar internal)

<!-- Internal contributors: link the Linear ticket, e.g. FENCE-1234. External contributors can delete this section. -->

## Related issue

<!-- Link any related GitHub issue, e.g. Closes #123. -->

## Type of change

- [ ] Bug fix (non-breaking)
- [ ] New feature (non-breaking)
- [ ] Breaking change (alters existing public API behavior)
- [ ] Documentation / internal only (no SDK behavior change)

## Manual test steps

<!--
How did you verify this? Give steps a reviewer can follow when possible — ideally
via the example app in `example/`. Include device/OS/emulator and any setup.
-->

1.
2.
3.

## Checklist

- [ ] Added or updated unit tests (`./gradlew test`)
- [ ] `./gradlew :sdk:ktlintCheck` and `./gradlew :sdk:lintDebug` pass locally
- [ ] For breaking changes: added a `MIGRATION.md` entry and bumped `version` in `sdk/build.gradle`
- [ ] Updated README / public docs if the public API changed
- [ ] No real API keys committed in the example app
- [ ] Only fixed lint/ktlint violations on lines I changed; removed any now-stale baseline entries

## Screenshots / recordings (optional)

<!-- For example-app or other user-visible changes. -->
