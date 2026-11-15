# JoyMart LLP Android — Pro UX Update

## What this package is
This is the complete Android Studio project consolidated from the Android source/project files previously supplied in this conversation, with an app-wide UX and error-handling pass. Existing modules and screens are retained; the update focuses on shared theme, authentication flow, network feedback, and loading states.

## Included existing modules
- Login and dynamic-hierarchy registration with sponsor verification
- Role-based dashboard/navigation
- Profile
- Genealogy (latest compact/card-only zoom screen from the supplied project)
- Wallet, withdraw, transfer and transaction screens
- Products, categories and packages
- Existing ERP WebView
- Retrofit API, token storage and DTOs

## UX / reliability changes in this release
- Startup shows the animated JoyMart splash while saved session is checked.
- If a valid token is present, app opens the signed-in workspace after bootstrap.
- If there is no valid saved session, app opens Login.
- HTTP 401/403 during session restore clears the stale token and returns to Login.
- Login failure/network failure resets the Login form fields (login and password).
- Registration failure keeps the entered form data so the user can retry.
- Registration success and authentication errors use a consistent modern dialog.
- Sponsor verification shows a readable network error and Retry action.
- Shared typography, color scheme, component shapes and loading overlay are polished.
- DNS, timeout and connection exceptions are converted to understandable messages.
- API timeouts are bounded; verbose HTTP logging is disabled in release builds.

## Server configuration
The existing API base URL is configured in:
`app/build.gradle.kts`

Current value:
`https://joymartllp.com/api/native/v1/`

If the app shows `Unable to resolve host`, this is a DNS/domain reachability problem, not a form-field validation error. Verify that the domain resolves publicly, the SSL certificate is valid, and the API URL is correct. No Laravel/server file is changed by this Android package.

## Open in Android Studio
1. Extract this ZIP.
2. Open the extracted `JoyMartLLP_Android_Full_Project` folder in Android Studio.
3. Use JDK 17.
4. Install Android SDK Platform 35.
5. Check `API_BASE_URL` in `app/build.gradle.kts`.
6. Click **Sync Project with Gradle Files**.
7. Run **Build > Rebuild Project**, then run on an emulator/device.

The included `gradlew` script downloads Gradle 8.9 on first use if it is not already installed. Internet is required for that first Gradle download and dependency resolution.

## Important test status
- ZIP integrity was checked.
- Kotlin parser syntax check was run on the complete source tree; no parser-level syntax errors were reported.
- A full Android Gradle build could not be completed in this environment because `services.gradle.org` could not be resolved. Please run Gradle Sync/Rebuild in Android Studio and share the complete Build Output if it reports any remaining compiler errors.
- Live API/domain reachability and account login were not tested here.
