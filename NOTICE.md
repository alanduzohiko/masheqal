# Third-party notices

## Quran data
The bundled Arabic Quran text, English translation, surah metadata, page ranges, and Juz ranges are derived from the TeX Live `quran` package (`qurantext-uthmani.def`, `qurantext-en.translation.def`, `quran.sty`). Those upstream files declare distribution/modification under the LaTeX Project Public License (LPPL), version 1.3c or later. Keep the upstream copyright and license notices when redistributing this material.

Upstream maintainer: Seiied-Mohammad-Javad Razavian.

## Quran font
`app/src/main/res/font/amiri_quran.ttf` is the Amiri Quran font from the Debian `fonts-hosny-amiri` package and is distributed under the SIL Open Font License (OFL) 1.1. Preserve the font's license/notice when redistributing it.

## Source code
Original مەشخەڵ source code in this repository is licensed under Apache-2.0 as declared by `LICENSE`. Third-party data and font licensing above remain separate.


## Time-zone boundary data

The app bundles Timeshape (`net.iakovlev:timeshape:2026b.29`) for offline coordinate-to-time-zone lookup. Its code is MIT-licensed; the bundled time-zone boundary data derives from timezone-boundary-builder / OpenStreetMap and is licensed under ODbL 1.0. See `docs/CONTENT-LICENSES.md`.


## Madinah Mushaf SVG page artwork

- **Source:** [quran-ws/quran-svg](https://github.com/quran-ws/quran-svg), immutable CDN release `v1.1.1`, edition path `hafs-kfqc`, served from `https://cdn.quran.ws/svg/pages/v1.1.1/hafs-kfqc/`.
- **Publisher of the underlying page artwork:** King Fahd Glorious Qur'an Printing Complex (KFGQPC). Its digital-use terms permit use in software, websites, digital publishing and media; the artwork is not owned by this application.
- **Metadata / polygon layer:** the quran-ws/quran-svg repository contribution is CC BY 4.0, with attribution waived for use inside a product as stated by its published licensing notes. The current renderer displays the SVG artwork and fetches the per-page JSON hit regions for ayah tap-to-select. SVG and region metadata are cached separately after successful retrieval. Upstream manifest SHA-256 verification remains pending.
- **Implementation status:** page SVGs are fetched over HTTPS on first view and cached in app-private storage. Only previously visited pages are available offline; the app does not bundle all 604 pages yet. The URL is version-pinned and the markup/size are checked, but manifest SHA-256 verification remains pending.
- **Source license references:** https://quran.ws/docs/reference/quran-svg/ and https://quran.ws/docs/reference/licensing/
