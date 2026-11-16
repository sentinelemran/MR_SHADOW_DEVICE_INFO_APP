# Device Info

Privacy-conscious Android device information utility.

The app reports device values provided by Android. Some fields (notably baseband, security patch, and hardware properties) may be unavailable or intentionally hidden by a device vendor or Android build. Camera details are queried as camera metadata only; the app does not open the camera or capture images.

The app's debug APK produced by GitHub Actions is installable for testing and is signed automatically with the standard debug key. For distributing a release build, use a private release keystore and build a signed release APK/AAB. Never commit the keystore or passwords to this repository.

The Apps page links to APK files from the public `sentinelemran/Apps` repository. The app does not silently install those downloads; Android manages the user-initiated download/install process.
