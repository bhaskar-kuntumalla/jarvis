# Jarvis (Milestone 1 skeleton)

Package: `com.bhaskar.jarvis` · minSdk 26 · compileSdk/targetSdk 36 (Android 16)

## Status

Phase C complete: the `core` module contains the command model (`open`, `tap`, `type`),
the parser, the validator, and the step result types, with unit tests.
The app still shows a placeholder screen. No device control exists yet (Phase D).

## Layout

- `app/`: Android app (Compose, `MainActivity`, manifest with no permissions)
- `core/`: pure Kotlin/JVM module for command parsing and validation (no Android imports)
- `.github/workflows/android.yml`: CI that runs core tests and builds a debug APK

## Build without a local machine (GitHub Actions)

1. Create a new GitHub repository and upload the contents of this folder.
2. Open the **Actions** tab. The `android-m1` workflow runs on push.
3. When it finishes, download the `jarvis-m1-debug-apk` artifact.
4. Copy the APK to your phone and install it. Allow installs from your file manager or browser when prompted.

## Build locally (when you have Android Studio)

Open the folder in Android Studio, let Gradle sync, then run `./gradlew :core:test :app:assembleDebug`.

## Not yet verified

This skeleton was generated without running Gradle, because the authoring environment has no Android SDK and cannot reach Google's Maven repository. Version numbers (AGP 8.9.1, Kotlin 2.0.21, Gradle 8.11.1, Compose BOM 2025.01.00) were chosen as a consistent set and must be confirmed by the first CI run. The Gradle wrapper JAR is not included; CI uses the Gradle setup action instead.
