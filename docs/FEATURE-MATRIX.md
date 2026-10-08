# Feature matrix — implementation status

| Area | Priority | Data / subsystem | Offline | Native | UI | Status |
|---|---|---|---|---|---|---|
| Native Android shell | P0 | Kotlin / Compose / Material3 | Yes | Yes | Global | Implemented |
| Sorani / Arabic / English UI | P0 | Android resources + AppCompat locales | Yes | Yes | Global | Implemented foundation |
| Quran Arabic | P0 | Bundled verified-source asset | Yes | Yes | Quran / Home | Implemented |
| English translation | P0 | Bundled aligned asset | Yes | Yes | Quran | Implemented |
| Surah explorer | P0 | Surah metadata | Yes | Yes | Quran | Implemented |
| Quran search | P0 | In-memory indexed scan foundation | Yes | Yes | Global search | Implemented foundation |
| Reference parser | P0 | Local parser | Yes | Yes | Search | Implemented |
| Page / Juz navigation | P0/P2 | 604 page + 30 Juz ranges | Yes | Yes | Quran Page View | Implemented foundation |
| Reading position | P0 | DataStore | Yes | Yes | Quran / Home | Implemented |
| Prayer times | P0 | Astronomical calculator | Yes | Yes | Prayer / Home | Implemented foundation |
| Qibla | P0/P1 | Location + rotation vector sensor | Yes after location | Yes | Qibla | Implemented |
| Notifications | P1/P3 | AlarmManager + BroadcastReceiver | Yes | Yes | Prayer | Implemented foundation |
| Media session | P0 | Media3 ExoPlayer + MediaSession | Yes for installed audio | Yes | Background audio | Foundation; audio dataset not bundled |
| Offline content manager | P0/P1 | Versioned package metadata | Yes | Yes | Content Center | Implemented foundation |
| Bookmarks | P1 | SQLite | Yes | Yes | Saved | Implemented |
| Notes | P1 | SQLite | Yes | Yes | Notes | Implemented |
| Khatmah | P2 | DataStore | Yes | Yes | Home / Library | Implemented foundation |
| Hijri calendar | P2 | Local tabular calculator | Yes | Yes | Library | Implemented foundation |
| Backup / restore | P3 | JSON + Storage Access Framework | Yes | Yes | Settings | Implemented |
| Share as image | P3 | Bitmap / Canvas / FileProvider | Yes | Yes | Quran / Home | Implemented |
| Widgets | P3 | AppWidgetProvider | Yes | Yes | Launcher | Implemented foundation |
| App shortcuts | P3 | Static shortcuts XML | Yes | Yes | Launcher | Implemented |
| Deep links | P3 | Native intent filters | Yes | Yes | Global | Implemented |
| Tafsir | P1 | Licensed content package required | Yes after install | Yes | Library / Ayah actions | Architecture gate |
| Hadith | P1 | Licensed content package required | Yes after install | Yes | Library / Search | Architecture gate |
| Adhkar / Hisn / Dua | P1 | Licensed content package required | Yes after install | Yes | Adhkar / Library | Architecture gate |
| Sorani Quran translation | P0 | HQKSTD candidate, CC BY 4.0; exact package import/validation still required | Yes after install | Yes | Quran | Architecture gate |
| Word analysis / Tajweed | P2 | Verified dataset required | Yes after install | Yes | Ayah actions | Architecture gate |
| Transliteration | P2 | Verified dataset required | Yes after install | Yes | Quran | Architecture gate |
| Audio downloads / radio | P0/P3 | Licensed provider data | Depends on installed/downloaded content | Yes | Player / Downloads | Architecture gate |
| Semantic search / AI | P4 | Optional service + verified retrieval | No for remote AI | Yes | Search / Study | Future-ready architecture |
| Camera Quran search | P4 | OCR + verified reference matcher | Potentially local | Yes | Future | Future-ready architecture |
| Wear OS / Android Auto | P4 | Shared media/navigation contracts | Depends | Yes | Future | Future-ready architecture |
