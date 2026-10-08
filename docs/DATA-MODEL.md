# Data model

## Local user state

SQLite database `masheqal_user.db` stores user-generated data separately from content assets.

Current tables:

- `bookmarks`: type, reference, title, timestamps.
- `notes`: reference, body, tags, timestamps.

`SettingsRepository` and `PersonalRepository` use Android DataStore for preferences and lightweight progress state, including language/theme, prayer method/madhhab, screen-awake preference, reading position and Khatmah progress.

## Content state

Bundled/core content is immutable app data under `assets/content`. External content packages live under the app's private `files/content-packages` directory and are activated only after manifest validation.

## Non-destructive rules

Cache/content downloads may be deleted independently. Migrations must be additive and preserve user-generated records. Backup restore validates the schema before applying data and does not silently replace the existing local database.
