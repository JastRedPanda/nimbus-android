# Contributing to Nimbus Weather

Thanks for wanting to help. This is a small project with one maintainer, so the
rules are simple and the scope is deliberately narrow.

## Before you write code

Open an issue first. Describe the problem, not the solution you have in mind —
it saves you from building something that does not fit. Bug reports and feature
requests both go through the issue templates.

Small fixes (typos, obvious crashes, broken layout on a specific device) can go
straight to a pull request.

## Building

Requirements: JDK 17, Android SDK with API 35, and a device or emulator running
Android 8.0 (API 26) or newer.

```bash
./gradlew assembleDebug   # debug APK
./gradlew test            # unit tests
```

Install on a connected device:

```bash
./gradlew installDebug
```

## What the project expects from a change

- **Strings go to resources.** Every user-facing string lives in
  `app/src/main/res/values/strings.xml` and must be translated in all four
  locales: `values-ru`, `values-uk`, `values-cs`. A missing translation falls
  back to English and looks broken next to translated neighbours.
- **Units and formats are resources too.** `m/s`, `hPa`, `°C` / `°F` are
  localized per language, not hardcoded.
- **Commits follow [Conventional Commits](https://www.conventionalcommits.org/).**
  Release notes are generated from them: `feat:`, `fix:`, `perf:`, `refactor:`,
  `docs:`, `test:`, `ci:`, `chore:`. A `!` after the type marks a breaking
  change.
- **No new dependencies without a reason.** The app ships without analytics,
  trackers or crash reporting, and it should stay that way.
- **Tests for logic, not for pixels.** Unit tests use JUnit, coroutines-test,
  MockK and Turbine. Instrumented tests in `app/src/androidTest` are for things
  that only exist on a device (frame metrics, screenshots).

## Pull requests

- One topic per pull request. If it touches unrelated things, split it.
- Describe what changed and why. For UI changes, attach a screenshot.
- CI runs `assembleDebug test` on every push. A red build gets no review.
- The maintainer may push commits to your branch instead of writing a full
  review — that is normal here.

## Translations

Interface strings are in the four locale folders listed above. If you are a
native speaker of Ukrainian, Czech or Russian and something reads awkwardly,
a pull request that fixes the wording is genuinely welcome — those translations
were written by one person.

## Releases

Tags are pushed by the maintainer only:

```bash
git tag v1.51
git push origin v1.51
```

The `Release` workflow builds the APK with the project keystore and publishes
the release. Never change the signing key: releases are installed over the
previous version, and a different certificate breaks that.
