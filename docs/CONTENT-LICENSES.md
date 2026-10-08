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
