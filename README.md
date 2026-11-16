# MR SHADOW DEVICE INFO

Privacy-conscious native Android device information utility built with Kotlin and Jetpack Compose.

## Build APK

Push changes to `main` or run **Build APK** manually from the GitHub Actions tab. On a successful run, download the `mr-shadow-device-info-debug-apk` artifact. It contains `app-debug.apk`.

The workflow builds the project directly from the repository. No ZIP or Android Studio setup is required.

## App catalog

`app/src/main/assets/apps.json` is intentionally empty until real app details and HTTPS URLs are available.

## Privacy

The app has no internet permission, bot, report backend, or automatic report transmission. Device data is displayed locally. IMEI, serial number, phone/SIM details, location, account identifiers, credentials, and personal files are not collected. Root and bootloader unlock status show `Not Available`. Notifications are a saved preference only.
