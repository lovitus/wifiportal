# WiFi Portal

Small native Android app for applying captive portal probe settings through Shizuku/Sui.

## Scope

- Min SDK 21, target/compile SDK 35.
- No AndroidX UI stack and no native libraries.
- Uses Shizuku UserService, not the deprecated `Shizuku.newProcess`.
- Persists desired settings locally and checks them again whenever the app opens with Shizuku active.
- Requests Shizuku authorization again when permission is missing.

Shizuku/Sui currently require Android 6.0+. The APK keeps minSdk 21 so Android 5 devices can install and retain the same settings model, but in-app privileged apply/check depends on Shizuku/Sui being available.

## Captive portal keys

The app writes and verifies the known captive portal settings used across Android 5 through current NetworkStack builds:

- Android 5/6 legacy: `captive_portal_server`, `captive_portal_detection_enabled`
- Android 6+: `captive_portal_mode`
- Android 7+: `captive_portal_http_url`, `captive_portal_https_url`, `captive_portal_fallback_url`
- NetworkStack / Android 10+: `captive_portal_other_fallback_urls`, `captive_portal_other_http_urls`, `captive_portal_other_https_urls`, `captive_portal_user_agent`, `captive_portal_use_https`, `captive_portal_fallback_probe_specs`
- Optional old Wi-Fi watchdog compatibility keys: `network_avoid_bad_wifi`, `wifi_watchdog_on`, `wifi_watchdog_poor_network_test_enabled`, `wifi_watchdog_background_check_enabled`

For Android 10+ mirrored NetworkStack keys are also written to `device_config connectivity` when available. Older devices simply skip `device_config`.

## Build

```sh
./gradlew testDebugUnitTest assembleDebug
```

Release signing is configured through Gradle properties:

```properties
WIFIPORTAL_STORE_FILE=/absolute/path/wifiportal-release.jks
WIFIPORTAL_STORE_PASSWORD=...
WIFIPORTAL_KEY_ALIAS=wifiportal
WIFIPORTAL_KEY_PASSWORD=...
```

## GitHub Secrets

Generate a release keystore and configure repository secrets:

```sh
scripts/generate-release-keystore.sh
scripts/configure-github-secrets.sh owner/repo release/wifiportal-release.jks
```

Required secrets:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

The workflow builds debug APKs for every push/PR and signed release APKs when the signing secrets are present.
