# Build and verification status

Date: 2026-10-08

## Local execution environment

- Java: 21.0.12 available.
- Kotlin compiler: 1.9.0 available for standalone/pure Kotlin checks.
- Gradle: not installed.
- Android SDK / `aapt2` / `adb`: not available.
- External network access from the build container: unavailable.

Therefore no Android APK/AAB build is claimed from this environment. No ZIP has been renamed to an APK, and no artifact is presented as compiled when it was not.

## Project toolchain

- Android Gradle Plugin: 9.4.0
- Gradle: 9.6.1 (cloud CI)
- Kotlin: 2.4.10
- Compose BOM: 2026.09.00
- Media3: 1.11.1
- minSdk: 30 (Android 11)
- target/compileSdk: 36

The target SDK is 36 because Google Play's current requirement for new apps and updates after August 31, 2026 is Android 15 / API 36 or higher. The minimum remains API 30 so the target phone's Android 11 is supported.

## Verification performed here

- 14 Android XML resources parsed successfully (after final resource additions this project has 14 resource XML files).
- 118 UI string keys are synchronized across default, Arabic, and Sorani resources.
- Quran: 114 Surahs, 6,236 ayahs.
- Arabic/English ayah IDs, Surah IDs, and ayah numbers are aligned 1:1.
- Quran page ranges: 604 contiguous ranges covering global ayahs 1–6,236.
- Juz ranges: 30 contiguous ranges covering global ayahs 1–6,236.
- Project content validator: passed.
- Kotlin delimiter/source sanity: passed.
- Pure Kotlin domain matrix: passed for 7 prayer methods × 2 Asr madhhabs, plus Qibla normalization and Hijri structural checks.

## Not verifiable here

- Full Android/Compose compilation.
- Android lint.
- Instrumentation/UI tests.
- Playback against a real recitation endpoint.
- Alarm delivery on a real Android 11 device.
- Sensor behavior on the Realme X2 Pro.
- Final APK installation.

Those require the Android SDK/build environment and, for hardware validation, an actual device.

## Cloud build path

`.github/workflows/android-build.yml` provisions JDK 17, Android SDK API 36/build-tools 36.0.0 and Gradle 9.6.1, then runs content validation, unit tests, lint, debug APK build, and unsigned release APK/AAB packaging. It does not contain a private signing key.
