# Content provenance and licensing

## Included in the CI-built APK

The bundled Arabic Quran text, English translation, Surah metadata, page ranges, and Juz ranges are derived from the TeX Live `quran` package files `qurantext-uthmani.def`, `qurantext-en.translation.def`, and `quran.sty`. Those source files state that the work may be distributed and/or modified under the LaTeX Project Public License (LPPL), version 1.3c or later. The app keeps the upstream provenance visible in `NOTICE.md` and the content manifest.

The bundled Amiri Quran font is from the Debian `fonts-hosny-amiri` package and is distributed under the SIL Open Font License (OFL) 1.1.

## Alternate Sorani dataset not used by the current build

**Holy Quran Kurdish Sorani Translation Dataset (HQKSTD)** is an alternative dataset listed on Mendeley Data V1, DOI `10.17632/byyjd7kmvd.1`, with CC BY 4.0 metadata and Kurdish/Arabic alignment. It is not the source used by the current application build. The active pipeline uses the QuranEnc publisher API described below and validates the returned verse alignment, attribution, version, and checksum before bundling it.

## 99 Names of Allah

- **Packaged data:** `UmmahLibrary/ummah-library/packages/data/datasets/asma.json`, pinned at commit `094dc91e11316a0eb5150f84dd106f3ea1be60e5`.
- **Underlying source and license:** `my-prayers/muslim-data-flutter`, pinned attribution/license source at commit `6df43672a5306ff6e19b1e29dacfe88cf6385ccb`; **Apache-2.0**. The complete Apache license text travels with the bundled asset.
- **Coverage:** 99 numbered records with Arabic names, transliteration, English meanings, and the source's references array. Missing references are left missing and are not fabricated. The English meaning wording has **not** received independent scholarly review yet; this status is disclosed in the app. Sorani meanings are not part of the dataset.
- **Integrity:** CI checks the fixed source revision, ordered and unique 1–99 sequence, non-empty Arabic/transliteration/meaning, license, source/manifest attribution, and SHA-256 checksums.

## Adhkar and daily supplications

The CI import bundles 82 rows into an offline catalogue from two pinned MIT-licensed sources:
- `Seen-Arabic/Morning-And-Evening-Adhkar-DB`, release `v1.0.2` (34 items, Arabic and English).
- `fitrahive/dua-dhikr`, pinned commit `f42f895f914319a844c3e3c2279483cae060ea19` (10 after-salah items with source references and 38 daily-occasion supplications).

The build verifies source JSON, references, repetition counts, the full MIT notices, category ids, record count, and SHA-256 checksums. Three items from the upstream after-salah list ("Tasbih", "Tahmid", and "Takbir") are excluded because the pinned data file supplies no source/reference field for them; no citation is invented. The bundled data includes Arabic text and English meanings, not Sorani translations. Independent scholar review of vocalisation, translation wording, and grading remains pending, so these sources are not represented as having completed scholarly review. The broader Hisn al-Muslim corpus, Tafsir, Hadith collections and specialist Quran study datasets still need separately reviewed sources and rights.

## Quran recitation audio

The player streams Surah MP3 files from the Al Quran Cloud / Islamic Network CDN (`cdn.islamic.network`) through Media3 and exposes eight reciter choices. The code supports per-surah downloads using Android `DownloadManager` and prefers a completed, non-empty local file on subsequent playback. Actual streaming, interruption/retry, offline playback, and storage behavior still require physical-device QA; these are not yet accepted as verified end-to-end behavior. Before a public release, review current provider terms, service availability, and edition URLs.

For example, an open Hadith project (`open-hadith-data`) currently states that its structured data is CC0 but that its English translations are sourced from sunnah.com and have separate redistribution terms. Therefore the app must treat the Arabic/source fields and translations as separately licensed inputs rather than assuming the whole package is freely redistributable.

## Rule

No religious content is inserted merely to make a screen appear complete. Every activated dataset must carry source, author/attribution where applicable, version, license, and validation metadata.


## Pinned native Mushaf page renderer

- **Source:** `quran-ws/quran-svg`, immutable CDN release `v1.1.1`, edition `hafs-kfqc` (Madinah Mushaf / Hafs).
- **Page artwork terms:** King Fahd Glorious Qur'an Printing Complex terms permit digital publishing, software, media and website use. The terms remain those of the publisher; the app does not claim ownership of the page artwork.
- **Ayah polygon/metadata layer:** CC BY 4.0 for the repository's contribution; attribution inside a product is waived per its `NOTICE.md`. The first implementation renders page SVG only and does not yet use ayah polygons for tap/highlight.
- **Source references:** https://github.com/quran-ws/quran-svg and https://quran.ws/docs/reference/quran-svg/
- **Delivery behavior:** SVG pages are fetched over HTTPS on first view and cached compressed in app-private storage. Therefore each visited page works offline afterward; this is not yet a complete 604-page offline bundle and must not be described as such.
- **Renderer:** AndroidSVG (`com.caverock:androidsvg-aar:1.4`), Apache-2.0.
- **Integrity caveat:** release path is version-pinned and SVG markup/size are validated, but page SHA-256 validation against the upstream manifest is still pending.


## Sorani Quran translation (publisher-hosted API)

- **Source:** QuranEnc.com official translation API, `/api/v1/translations/list/` and `/api/v1/translation/sura/{translation_key}/{surah_number}`.
- **Reuse terms:** QuranEnc permits downloaded translations to be republished under seven conditions: preserve content verbatim; credit publisher and QuranEnc.com; state source version; retain transcript information; notify QuranEnc of notes; update to the latest version issued; and avoid inappropriate advertisements. The app records the API-reported version/update, preserves raw translation strings, and records gaps instead of inventing text.
- **In-app attribution:** generated from the selected translation title and API-reported version; QuranEnc.com is always shown.
- **Integrity:** each row maps to canonical 1–6,236 ids and surah/ayah coordinates. A build-time validator checks row count, publisher, version, update value and SHA-256 of the generated text asset.
- **Delivery:** the publisher API is queried by cloud CI; generated content is bundled in the APK, so the translation remains available offline after installation.


## Offline adhan audio

The app provides a separate adhan-recording selector (distinct from the Quran reciter picker) in first-run setup and Settings. Users can preview and choose between:

- **Beautiful Adhan:** [Wikimedia Commons — File:Beautiful_adhan.ogg](https://commons.wikimedia.org/wiki/File:Beautiful_adhan.ogg), author Adam-synagda.
- **Community Adhan:** [Wikimedia Commons — File:Muslim_calling_to_prayer.ogg](https://commons.wikimedia.org/wiki/File:Muslim_calling_to_prayer.ogg), author Aishatu98.

Both recordings are released under CC0 1.0 Universal. `tools/fetch_adhan.py` verifies the live Commons license metadata, expected media host, MIME type, file size, upstream SHA-1, and the generated local SHA-256 on each CI build. It records individual manifests and fails closed if any condition changes. The selected recording is saved locally and is used for adhan previews and prayer reminders; sunrise alerts do not invoke the adhan.
