# Content package pipeline

The content layer is intentionally source-driven. The app must never activate religious content solely because a JSON file exists. The current release scope is Arabic and English; Sorani UI and Quran translation are deferred and are not activated by the build.

## Manifest contract

Every external package begins with `manifest.json` containing at least:

```json
{
  "type": "tafsir",
  "version": "1.0.0",
  "title": "Example title",
  "source": "Publisher / collection",
  "license": "Exact license or permission reference"
}
```

Supported package types:

- `tafsir`
- `hadith`
- `adhkar`
- `hisn`
- `dua`
- `names`
- `audio`

## Activation rules

A package is rejected if its type is unknown or if version/source/license metadata are missing. Content update code must validate the full payload, write into a versioned package directory, and only switch the active pointer after successful validation.

## Required religious-data safeguards

Do not invent Quran, Hadith, Tafsir, Tajweed rules, translations, grading or scholarly relationships. For disputes, retain source/view attribution. AI-generated explanations must remain visually and semantically distinct from source content.
