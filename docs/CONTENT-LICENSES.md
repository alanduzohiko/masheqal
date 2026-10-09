# Content provenance and licensing

## Bundled now

The bundled Arabic Quran text, English translation, Surah metadata, page ranges, and Juz ranges are derived from the TeX Live `quran` package files `qurantext-uthmani.def`, `qurantext-en.translation.def`, and `quran.sty`. Those source files state that the work may be distributed and/or modified under the LaTeX Project Public License (LPPL), version 1.3c or later. The app keeps the upstream provenance visible in `NOTICE.md` and the content manifest.

The bundled Amiri Quran font is from the Debian `fonts-hosny-amiri` package and is distributed under the SIL Open Font License (OFL) 1.1.

## Verified candidate for the Sorani package

A full Sorani Quran translation dataset is available as **Holy Quran Kurdish Sorani Translation Dataset (HQKSTD)**, Mendeley Data V1, DOI `10.17632/byyjd7kmvd.1`. The Mendeley record identifies the package as CC BY 4.0 and states that it contains all 114 Surahs / 6,236 verses with Kurdish and Arabic alignment metadata.

This dataset is **not bundled yet** because the current execution environment cannot fetch the XLSX files, and the release should only activate the exact downloaded file after checksum, schema, verse-count, alignment, attribution, and license metadata are validated. See `CONTENT-PIPELINE.md`.

## Other source-dependent content

Tafsir, Hadith collections, Adhkar/Hisn, Dua, Names of Allah, transliteration/word-analysis datasets, and recitation audio require a dataset/provider with clear redistribution rights. The architecture accepts versioned content packages rather than silently copying data from another application.

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
