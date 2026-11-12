# Security Policy

## Supported versions

Only the latest release receives fixes. Nimbus is distributed as an APK through
[GitHub Releases](https://github.com/JastRedPanda/nimbus-android/releases) — there
is no store build, no beta channel and no backporting to older versions.

| Version | Supported |
| ------- | --------- |
| latest release | yes |
| anything older | no |

## Reporting a vulnerability

Use GitHub's private reporting form: **Security → Report a vulnerability** in the
repository. The report stays visible only to the maintainer. If the form is
unavailable, contact [@JastRedPanda](https://github.com/JastRedPanda) directly
and ask for a private channel before sharing details.

Please include the app version (`versionName` from the release tag), Android
version, device model and the steps needed to reproduce.

Do not open a public issue for a vulnerability.

## Scope

The app has no backend, no accounts and no analytics. It talks to three public
Open-Meteo endpoints over HTTPS and stores everything locally. The interesting
attack surface is therefore small, but these still count:

- the APK and its update path (signing, `debuggable` flag, exported components)
- the home screen widget provider and its update broadcasts
- the foreground service and the exact-alarm scheduling
- the WebView-free city search: input handling and stored data
- anything that leaks the user's saved cities or location outside the device

Build and release issues that could produce a wrongly signed or tampered APK are
in scope too.

## Signing key

Releases are signed with one permanent certificate. Its SHA-256 fingerprint is
published in the README. If a downloaded APK does not match it, do not install
it — report it instead.
