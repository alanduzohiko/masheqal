# مەشخەڵ — Native Android Foundation

**Identity:** `com.masheqal.app`  
**Minimum Android:** 11 / API 30  
**Compile/Target:** API 36  
**UI:** Kotlin + Jetpack Compose + Material 3  
**Data:** SQLite user store + DataStore preferences + versioned content assets  
**Media:** AndroidX Media3 / MediaSession  
**Background work:** WorkManager / AlarmManager where appropriate

## What is genuinely implemented in this foundation

- Native Android app shell; no WebView/PWA/browser wrapper.
- Original مەشخەڵ visual identity, launcher icon, theme tokens and RTL support.
- Sorani Kurdish (`ckb`), Arabic (`ar`) and English (`en`) UI resources.
- Bundled Quran Arabic in Uthmani-compatible text form, 114 surahs / 6236 ayahs.
- Bundled English translation aligned 1:1 to the same 6236 ayahs.
- Quran search with Arabic normalization, diacritic-insensitive matching and reference parsing (`2:255`, `2 255`, `2-255`).
- Surah explorer and exact-ayah reader with saved reading position.
- Page view with 604 page ranges and 30 Juz ranges derived from the bundled Quran package metadata.
- Bookmark and private-note persistence in a dedicated SQLite database.
- Khatmah page progress with local persistence.
- Prayer calculation engine with multiple common methods, madhhab selection and a high-latitude rule parameter.
- Native sensor-based Qibla bearing calculation.
- Native prayer notification scheduling with reboot/time/timezone resilience hooks.
- Native Media3 playback service foundation with MediaSession integration.
- Native app widget, launcher shortcuts and `masheqal://` deep links.
- Android Sharesheet text sharing and local high-resolution Quran share-card generation.
- Local JSON backup/restore for personal state.
- Content Center that refuses to activate source-dependent religious datasets without source/license metadata.
- GitHub Actions cloud build workflow that can build an installable debug APK without a local computer or Android Studio.

## Deliberate content gates

The project does **not** ship unverified or unclear-license religious datasets merely to make a screen look complete. Sorani Quran translation, Tafsir, Hadith, Adhkar/Hisn/Dua, and recitation audio are represented by a source-package pipeline instead of fabricated content. See `docs/CONTENT-LICENSES.md` and `docs/CONTENT-PIPELINE.md`.

## Build note

The current execution environment contains Java 21 but no Android SDK and no Gradle installation. It also cannot fetch external build dependencies from the internet. Therefore no APK/AAB is claimed as built locally in this delivery. The repository contains a cloud CI workflow that provisions Android SDK + Gradle and uploads the resulting debug APK and unsigned release artifacts.

## Mobile-only delivery path

The user can use a connected GitHub account to host this project and run the workflow from the GitHub web UI. No Android Studio, desktop SDK, or local terminal is required for the cloud build path.

## Release signing

No private signing key is included. The debug APK is installable for testing. Release APK/AAB outputs are intentionally unsigned until the owner's private keystore is supplied through a secure CI secret. Never put a keystore or credentials into source control.


## Product V2 work (in progress)

- RTL-aware Compose navigation transitions.
- Localized Gregorian date in Home.
- The Quran page reader now has a true Madinah Mushaf SVG mode, an alternate text/English reading mode, and compressed page caching for pages already visited.
- Mushaf artwork uses the pinned `quran-ws/quran-svg` release `v1.1.1`. The first view needs internet; each successfully viewed page is then stored for offline access. Full 604-page offline download, ayah hit-testing/highlighting, and page-manifest SHA-256 enforcement are not yet complete.
- See `docs/IMPLEMENTATION-CHECKLIST.md` for the full 177-section acceptance tracker. This branch is not yet a publish-ready release.
