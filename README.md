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
- City-name reverse lookup and manual city selection using Android Geocoder; selected cities are stored locally.
- A small offline fallback catalog for five major Iraqi cities when Android geocoding is unavailable; these use approximate city-centre coordinates, not GPS precision.
- Offline IANA time-zone lookup from city/device coordinates, with an explicit device-zone fallback notice.
- Original مەشخەڵ visual identity, launcher icon, theme tokens and RTL support.
- Arabic (`ar`) and English (`en`) UI resources; Sorani is deferred from the current release scope.
- Bundled Quran Arabic in Uthmani-compatible text form, 114 surahs / 6236 ayahs.
- Bundled English translation aligned 1:1 to the same 6236 ayahs.
- Quran search with Arabic normalization, diacritic-insensitive matching and reference parsing (`2:255`, `2 255`, `2-255`).
- Global search spans Quran verses/references, saved bookmarks and notes, offline adhkar/supplications, and the 99 Names of Allah, with shared Arabic normalization.
- CI validates configured Quran audio edition/bitrate pairs against the upstream surah manifest, including all 114 filename entries per edition. This verifies published availability metadata only, not an individual reciter copyright grant.
- Surah explorer and exact-ayah reader with saved reading position.
- Page view with 604 page ranges and 30 Juz ranges derived from the bundled Quran package metadata.
- Official Madinah Mushaf SVG page reader with tap-to-select ayah regions, visual selected-ayah highlighting, bookmark/copy/share/English-meaning actions, and an optional all-604-page offline download with progress and cancellation.
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

The project does **not** ship unverified or unclear-license religious datasets merely to make a screen look complete. Sorani UI and Quran translation are deliberately deferred and excluded from the active build. Tafsir, Hadith, the broader Hisn al-Muslim corpus, and specialist study datasets remain gated until source rights and content are validated. See `docs/CONTENT-LICENSES.md` and `docs/CONTENT-PIPELINE.md`.

## Build note

The current execution environment contains Java 21 but no Android SDK and no Gradle installation. It also cannot fetch external build dependencies from the internet. Therefore no APK/AAB is claimed as built locally in this delivery. The repository contains a cloud CI workflow that provisions Android SDK + Gradle and uploads the resulting debug APK and unsigned release artifacts.

## Mobile-only delivery path

The user can use a connected GitHub account to host this project and run the workflow from the GitHub web UI. No Android Studio, desktop SDK, or local terminal is required for the cloud build path.

## Release signing

No private signing key is included. The debug APK is installable for testing. Release APK/AAB outputs are intentionally unsigned until the owner's private keystore is supplied through a secure CI secret. Never put a keystore or credentials into source control.


## Product V2 work — current verified state

Historical CI baseline: run [#244](https://github.com/alanduzohiko/masheqal/actions/runs/37996956735) passed all pipeline steps for commit `c7ceccaecb7f09a0fc189799bcb22ba051716f21`. Later commits add global-search coverage and correct the Quran reciter catalogue; check the latest branch workflow before treating the current head as green. Build/CI evidence is not physical-device acceptance.

**Current phase:** Phase 1 — Foundation and P0 acceptance. This branch is not yet ready for Phase 2 until current-head CI and real-device core-flow acceptance both pass.

- Quran page navigation resolves the legacy surah/ayah routes to the relevant Mushaf page. The reader uses pinned Madinah Mushaf SVG artwork and supports parsing the two ayah-region JSON formats used by the source.
- Tapping a mapped ayah region opens an action sheet; the selected ayah is highlighted. Available actions include bookmark, copy, share, and explicitly displayed English meaning. Missing ayah-region metadata can be retried.
- The app now has an optional downloader for all 604 Mushaf pages plus their ayah-region metadata, with progress and cancellation. Successfully cached pages are retained if a download is interrupted. This feature still needs real-device network, storage, cancellation, and visual-alignment QA.
- The full-surah picker uses twelve IDs listed in the upstream by-surah manifest and CI checks every 114-file set. Verse playback is deliberately pinned to Alafasy until other IDs are verified against the separate by-ayah catalogue. These checks prove listing metadata, not phone playback or independent copyright ownership.
- A native prayer engine adapter and reference-vector/unit-test coverage exist. Independent city-by-city prayer verification, current-location/geocoder/time-zone edge cases, sensor checks, and scheduled-notification behavior still need device-level acceptance.
- The codebase includes Arabic/English resources, an animated intro, onboarding, local bookmarks/notes and reading progress, Qibla, adhkar/dua/Names content paths, tasbih, khatmah, backup/restore and other native foundations. Their complete screen-by-screen visual, language, persistence, and accessibility acceptance has not been demonstrated.

### Important limits — not a finished release

- Upstream Mushaf manifest SHA-256 integrity validation has not been completed.
- Full 604-page offline download has been implemented in code but has not been confirmed end-to-end on a physical device.
- Physical-device installation and UI review were not performed by this workflow. Do not treat CI success as a phone test.
- Tafsir and Hadith libraries, a complete independently licensed Hisn al-Muslim corpus, Sorani UI/content, and several advanced Quran study functions remain deferred or gated.
- Audio provider playback/download reliability, background/lock-screen behavior, adhan alarms across reboot/time-zone changes, and notification permissions need real-device acceptance.
- Detailed Arabic/English screen coverage, visual QA across phone sizes, accessibility/font scaling, and all-settings persistence still require verification.
- Release artifacts produced by CI are unsigned; store distribution requires the owner's signing key configured securely, never committed to the repository.

See `docs/IMPLEMENTATION-CHECKLIST.md` for the acceptance tracker and remaining evidence requirements.