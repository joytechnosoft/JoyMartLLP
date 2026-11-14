# JoyMart LLP Android — Full Project

This is a consolidated Android Studio project reconstructed from the supplied JoyMart source archives. It keeps the latest supplied `main.zip` Kotlin source, includes the compact Genealogy screen patch, and adds the previously supplied Wallet/Transactions Compose screens. Existing website/Laravel files are not included or modified.

## Toolchain baseline
- Android Gradle Plugin: 8.7.3
- Kotlin: 2.0.21 (Compose compiler plugin)
- Gradle: 8.9
- JDK: 17
- compileSdk/targetSdk: 35; minSdk: 24

These are the compatible versions from the earlier project configuration, not a claim that they are the newest releases available today.

## Open and build
1. Extract the ZIP.
2. Open the extracted `JoyMartLLP_Android_Full_Project` folder in Android Studio.
3. Install Android SDK Platform 35 and JDK 17 if prompted.
4. Check `app/build.gradle.kts` and confirm `API_BASE_URL` is your production Native API URL ending in `/api/native/v1/`.
5. Sync Gradle, then Build > Make Project.

`gradlew` and `gradlew.bat` bootstrap Gradle 8.9 from the official Gradle distribution if it is not already cached; network access is required for the first run.

## Included app source
- Login / registration and authenticated member registration from latest supplied `main.zip`
- Dashboard, profile, catalog, package, product and ERP WebView screens
- Genealogy screen replaced with the supplied compact-card Web-style update
- Wallet and Transactions Compose screens added and Wallet navigation wired
- Retrofit API client with bearer-token storage and raw GET/POST calls

## Important limits
- The user-supplied source archives did not include the original Gradle wrapper JAR, so the included launcher scripts download Gradle 8.9 directly instead of using the standard wrapper JAR.
- This environment does not have Android SDK/Gradle installed and cannot run a real Android build. The archive structure and ZIP integrity are checked, but compile/runtime validation must be done in Android Studio.
- The API URL and backend response compatibility must be checked against the deployed Laravel Native API. The Laravel server is not changed by this package.
