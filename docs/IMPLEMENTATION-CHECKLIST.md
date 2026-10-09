# مەشخەڵ — Product V2 implementation & acceptance tracker

**Status at creation:** No section is considered accepted merely because a screen, route, or button exists. “Not verified” means an evidence-backed review is still required; it does not assert that the whole item is absent.

## Hard acceptance rules

- No fake religious text, citation, translation, audio synchronization, prayer time or source license.
- “Implemented” and “tested” are separate states. A passing compile is not functional acceptance.
- P0 is not complete until Kurdish Sorani is real, search/Quran navigation work, prayer results are compared against trusted test vectors, a usable audio path is verified, core flows work offline, and phone UI is visually reviewed.
- Every source-dependent dataset needs source, version, license/permission, attribution and integrity checks before activation.
- Record actual commands, workflow URL, commit SHA, test results, artifact checksum, and device checks in the delivery report.
- Preserve the phone-only workflow: cloud CI must build the APK; never require the user to use a desktop or edit source code.

## Known gaps identified in the initial audit

1. The shipped Qur'an assets include Arabic and English, but not the Sorani translation dataset.
2. The content center treats Sorani, Tafsir, Hadith, Adhkar/Hisn, Dua, Names and audio as external/gated content; that is not acceptable as the final core experience.
3. The current reader is primarily verse cards/text, not validated 604-page canonical Mushaf rendering.
4. The existing prayer calculator is custom and has not yet passed reference-based accuracy tests.
5. Media3/MediaSession groundwork exists, but a complete reciter selection, playback, background, lock-screen and verified audio-source flow has not been acceptance-tested.
6. Navigation previously had no authored route transition system; an RTL-aware, reduced-distance fade/slide transition was added on this branch. This alone does not satisfy overall visual QA.
7. A full native app requires real-device/emulator interaction tests and three-language UI checks, not only static source checks and CI compilation.

## Current branch implementation notes

- **Sorani Quran text:** the CI build fetches the QuranEnc publisher-hosted translation, verifies canonical alignment across 6,236 verse rows, records source/version metadata and a checksum, then bundles the generated asset in the APK. Runtime availability is offline after installation; this does not mean every verse is necessarily translated, and gaps remain explicitly marked.
- **Adhkar and daily Dua:** the CI build combines 82 records from two pinned MIT datasets: 34 morning/evening items, 10 after-salah items with a source citation, and 38 daily-occasion supplications. The app supports search/category filters and locally persisted daily counters; independent scholarly review and Sorani meanings remain pending.
- **99 Names of Allah:** CI bundles 99 ordered records under the attributed Apache-2.0 dataset provenance and ships the complete license notice. The app supports Arabic/transliteration/English search, learned/not-learned filtering and local learning progress. English-meaning scholarly review is explicitly pending; references missing from the dataset are not invented.
- **Mushaf:** page SVGs use the version-pinned quran-ws/quran-svg release and are cached after a successful view. The current app does not ship the complete 604-page offline cache, ayah hit-testing/highlighting, or upstream page-hash checks.
- **Prayer times and Qibla:** the app requests a fresh GPS/network fix, rejects stale/invalid locations and surfaces estimated accuracy. The Qibla page has an animated compass, true-north correction, bearing and great-circle distance. City-by-city prayer reference comparison and on-device sensor/time-zone/location verification remain part of acceptance.
- **Audio:** the Media3 player exposes eight Quran reciters, selected reciter persistence and a per-surah DownloadManager path. Two CC0-verified adhan recordings can be selected and previewed separately from Quran reciters, and prayer alarms use the saved adhan voice. Provider behavior and actual playback/download/offline use on a physical device still need verification.
- **First-run and home experience:** there is an animated branded intro, a four-step setup flow, and Home shortcuts to Dua, Names of Allah, adhkar and Quran audio. This is not a substitute for visual QA on several phone sizes or accessibility/RTL review.
- **Still not accepted:** Tafsir, Hadith, a complete independently licensed Hisn al-Muslim collection, Sorani meanings for adhkar/dua/names, full Mushaf offline mode, and complete in-device UI accessibility/RTL review.

## State definitions

- **Not verified** — no durable evidence recorded.
- **In progress** — implementation is actively being changed.
- **Implemented** — code and source/content are present.
- **Tested** — the relevant acceptance tests passed and evidence is recorded.
- **Blocked** — exact external dependency, license, permission or environment blocker is documented.

## Master traceability checklist

- [ ] **1. ABSOLUTE PRODUCT PRINCIPLE** — P3/P4 · 1 · Foundation · **Not verified**
- [ ] **2. START FROM ZERO** — P3/P4 · 1 · Foundation · **Not verified**
- [ ] **3. TRUE NATIVE ANDROID** — P0 · 1 · Foundation · **Not verified**
- [ ] **4. PRODUCT NAME AND BRAND** — P3/P4 · 1 · Foundation · **Not verified**
- [ ] **5. VISUAL DIRECTION** — P3/P4 · 1 · Foundation · **Not verified**
- [ ] **6. TYPOGRAPHY** — P3/P4 · 1 · Foundation · **Not verified**
- [ ] **7. FIRST-CLASS SORANI KURDISH** — P0 · 1 · Foundation · **Not verified**
- [ ] **8. MULTI-LANGUAGE ARCHITECTURE** — P0 · 1 · Foundation · **Not verified**
- [ ] **9. RESPONSIVE DESIGN** — P3/P4 · 1 · Foundation · **Not verified**
- [ ] **10. INFORMATION ARCHITECTURE** — P3/P4 · 1 · Foundation · **Not verified**
- [ ] **11. HOME — PERSONALIZED ISLAMIC DASHBOARD** — P0 · 2 · Home/Quran · **Not verified**
- [ ] **12. HOME CUSTOMIZATION** — P3/P4 · 2 · Home/Quran · **Not verified**
- [ ] **13. QURAN — THE CENTRAL EXPERIENCE** — P0 · 2 · Home/Quran · **Not verified**
- [ ] **14. SURAH EXPLORER** — P0 · 2 · Home/Quran · **Not verified**
- [ ] **15. QURAN NAVIGATION SYSTEM** — P0 · 2 · Home/Quran · **Not verified**
- [ ] **16. MUSHAF MODE** — P0 · 2 · Home/Quran · **Not verified**
- [ ] **17. MULTIPLE QURAN MODES** — P0 · 2 · Home/Quran · **Not verified**
- [ ] **18. AYAH COMMAND CENTER** — P0 · 2 · Home/Quran · **Not verified**
- [ ] **19. TRANSLATION ENGINE** — P0 · 2 · Home/Quran · **Not verified**
- [ ] **20. TAFSIR PLATFORM** — P1 · 3 · Translation/Tafsir/search · **Not verified**
- [ ] **21. TAFSIR SEARCH** — P1 · 3 · Translation/Tafsir/search · **Not verified**
- [ ] **22. QURAN WORD-BY-WORD** — P1 · 3 · Translation/Tafsir/search · **Not verified**
- [ ] **23. TAJWEED** — P1 · 3 · Translation/Tafsir/search · **Not verified**
- [ ] **24. TRANSLITERATION** — P2 · 3 · Translation/Tafsir/search · **Not verified**
- [ ] **25. QURAN AUDIO ENGINE** — P0 · 3 · Study/search · **Not verified**
- [ ] **26. RECITATION SYNCHRONIZATION** — P3/P4 · 3 · Study/search · **Not verified**
- [ ] **27. AUDIO LEARNING** — P3/P4 · 3 · Study/search · **Not verified**
- [ ] **28. AUDIO PLAYLISTS** — P3/P4 · 3 · Study/search · **Not verified**
- [ ] **29. OFFLINE AUDIO DOWNLOADS** — P3/P4 · 3 · Study/search · **Not verified**
- [ ] **30. LIVE QURAN RADIO** — P3/P4 · 3 · Study/search · **Not verified**
- [ ] **31. GLOBAL SEARCH** — P0 · 3 · Study/search · **Not verified**
- [ ] **32. VOICE SEARCH** — P3/P4 · 3 · Study/search · **Not verified**
- [ ] **33. SEMANTIC DISCOVERY** — P3/P4 · 3 · Study/search · **Not verified**
- [ ] **34. HADITH LIBRARY** — P1 · 6 · Worship content · **Not verified**
- [ ] **35. HADITH FILTERS** — P3/P4 · 6 · Worship content · **Not verified**
- [ ] **36. RELATED HADITH** — P3/P4 · 6 · Worship content · **Not verified**
- [ ] **37. ADHKAR** — P1 · 6 · Worship content · **Not verified**
- [ ] **38. HISN AL-MUSLIM** — P3/P4 · 6 · Worship content · **Not verified**
- [ ] **39. DUA PLATFORM** — P1 · 6 · Worship content · **Not verified**
- [ ] **40. TASBIH** — P1 · 6 · Worship content · **Not verified**
- [ ] **41. 99 NAMES OF ALLAH** — P1 · 6 · Worship content · **Not verified**
- [ ] **42. PRAYER TIMES ENGINE** — P0 · 5/7 · Prayer & plans · **Not verified**
- [ ] **43. PRAYER NOTIFICATIONS** — P3/P4 · 5/7 · Prayer & plans · **Not verified**
- [ ] **44. QIBLA** — P1 · 5/7 · Prayer & plans · **Not verified**
- [ ] **45. ISLAMIC CALENDAR** — P2 · 5/7 · Prayer & plans · **Not verified**
- [ ] **46. RAMADAN MODE** — P2 · 5/7 · Prayer & plans · **Not verified**
- [ ] **47. FASTING** — P3/P4 · 5/7 · Prayer & plans · **Not verified**
- [ ] **48. KHATMAH** — P2 · 5/7 · Prayer & plans · **Not verified**
- [ ] **49. READING GOALS** — P2 · 5/7 · Prayer & plans · **Not verified**
- [ ] **50. MEMORIZATION / HIFZ MODE** — P2 · 7/8 · Personal study · **Not verified**
- [ ] **51. STUDY MODE** — P2 · 7/8 · Personal study · **Not verified**
- [ ] **52. PERSONAL NOTES** — P2 · 7/8 · Personal study · **Not verified**
- [ ] **53. HIGHLIGHTS** — P2 · 7/8 · Personal study · **Not verified**
- [ ] **54. UNIFIED SAVED SYSTEM** — P1 · 7/8 · Personal study · **Not verified**
- [ ] **55. HISTORY** — P3/P4 · 7/8 · Personal study · **Not verified**
- [ ] **56. SHARE AS IMAGE** — P3/P4 · 7/8 · Personal study · **Not verified**
- [ ] **57. NATIVE SHARING** — P3/P4 · 7/8 · Personal study · **Not verified**
- [ ] **58. DAILY SHARE CARD** — P3/P4 · 7/8 · Personal study · **Not verified**
- [ ] **59. SMART HOME EXPERIENCE** — P3/P4 · 7/8 · Personal study · **Not verified**
- [ ] **60. APP SEARCH EVERYWHERE** — P3/P4 · 7/8 · Personal study · **Not verified**
- [ ] **61. OFFLINE-FIRST ARCHITECTURE** — P0 · 1/3 · Data integrity · **Not verified**
- [ ] **62. OFFLINE CONTENT MANAGER** — P3/P4 · 1/3 · Data integrity · **Not verified**
- [ ] **63. DATABASE ARCHITECTURE** — P3/P4 · 1/3 · Data integrity · **Not verified**
- [ ] **64. CONTENT VERSIONING** — P3/P4 · 1/3 · Data integrity · **Not verified**
- [ ] **65. DATA INTEGRITY** — P3/P4 · 1/3 · Data integrity · **Not verified**
- [ ] **66. CONTENT SOURCES** — P3/P4 · 1/3 · Data integrity · **Not verified**
- [ ] **67. LICENSING** — P3/P4 · 1/3 · Data integrity · **Not verified**
- [ ] **68. ADVANCED AI ASSISTANT** — P3/P4 · 9 · Optional intelligence · **Not verified**
- [ ] **69. AI QURAN STUDY** — P3/P4 · 9 · Optional intelligence · **Not verified**
- [ ] **70. AI SEARCH ASSISTANT** — P3/P4 · 9 · Optional intelligence · **Not verified**
- [ ] **71. SEMANTIC CONTENT DISCOVERY** — P3/P4 · 9 · Optional intelligence · **Not verified**
- [ ] **72. VOICE COMMANDS** — P3/P4 · 9 · Optional intelligence · **Not verified**
- [ ] **73. OPTIONAL CAMERA QURAN SEARCH** — P3/P4 · 9 · Optional intelligence · **Not verified**
- [ ] **74. QURAN MEMORIZATION ASSISTANT** — P3/P4 · 9 · Optional intelligence · **Not verified**
- [ ] **75. PERSONALIZED READING ASSISTANT** — P3/P4 · 9 · Optional intelligence · **Not verified**
- [ ] **76. DAILY WORSHIP PLAN** — P3/P4 · 9 · Optional intelligence · **Not verified**
- [ ] **77. WORSHIP STATISTICS** — P3/P4 · 9 · Optional intelligence · **Not verified**
- [ ] **78. SMART REMINDERS** — P3/P4 · 9 · Optional intelligence · **Not verified**
- [ ] **79. NATIVE ANDROID NOTIFICATIONS** — P3/P4 · 4/5 · Audio/notifications · **Not verified**
- [ ] **80. REBOOT RESILIENCE** — P3/P4 · 4/5 · Audio/notifications · **Not verified**
- [ ] **81. TIMEZONE RESILIENCE** — P3/P4 · 4/5 · Audio/notifications · **Not verified**
- [ ] **82. LOCATION RESILIENCE** — P3/P4 · 4/5 · Audio/notifications · **Not verified**
- [ ] **83. QIBLA SENSOR FALLBACKS** — P3/P4 · 4/5 · Audio/notifications · **Not verified**
- [ ] **84. WIDGET ECOSYSTEM** — P3/P4 · 4/5 · Audio/notifications · **Not verified**
- [ ] **85. LAUNCHER SHORTCUTS** — P3/P4 · 4/5 · Audio/notifications · **Not verified**
- [ ] **86. LOCK SCREEN AUDIO** — P3/P4 · 4/5 · Audio/notifications · **Not verified**
- [ ] **87. BACKGROUND AUDIO** — P3/P4 · 4/5 · Audio/notifications · **Not verified**
- [ ] **88. AUDIO RESUME** — P3/P4 · 4/5 · Audio/notifications · **Not verified**
- [ ] **89. CUSTOM AUDIO QUEUE** — P3/P4 · 4/5 · Audio/notifications · **Not verified**
- [ ] **90. FRIDAY MODE** — P3/P4 · 8 · Personalization/backup · **Not verified**
- [ ] **91. RAMADAN EXPERIENCE** — P3/P4 · 8 · Personalization/backup · **Not verified**
- [ ] **92. BEGINNER / ADVANCED EXPERIENCE** — P3/P4 · 8 · Personalization/backup · **Not verified**
- [ ] **93. FOCUS MODE** — P3/P4 · 8 · Personalization/backup · **Not verified**
- [ ] **94. NIGHT READING** — P3/P4 · 8 · Personalization/backup · **Not verified**
- [ ] **95. PERSONALIZATION** — P3/P4 · 8 · Personalization/backup · **Not verified**
- [ ] **96. PRIVACY** — P3/P4 · 8 · Personalization/backup · **Not verified**
- [ ] **97. PRIVACY CENTER** — P3/P4 · 8 · Personalization/backup · **Not verified**
- [ ] **98. BACKUP** — P3/P4 · 8 · Personalization/backup · **Not verified**
- [ ] **99. FUTURE CLOUD SYNC** — P3/P4 · 8 · Personalization/backup · **Not verified**
- [ ] **100. ACCESSIBILITY** — P3/P4 · 10 · Accessibility & polish · **Not verified**
- [ ] **101. REDUCED MOTION** — P3/P4 · 10 · Accessibility & polish · **Not verified**
- [ ] **102. PERFORMANCE** — P3/P4 · 10 · Accessibility & polish · **Not verified**
- [ ] **103. LOW-MEMORY DEVICES** — P3/P4 · 10 · Accessibility & polish · **Not verified**
- [ ] **104. ERROR HANDLING** — P3/P4 · 10 · Accessibility & polish · **Not verified**
- [ ] **105. STATE RESTORATION** — P3/P4 · 10 · Accessibility & polish · **Not verified**
- [ ] **106. SYSTEM UI** — P3/P4 · 10 · Accessibility & polish · **Not verified**
- [ ] **107. DESIGN COMPONENT LIBRARY** — P3/P4 · 10 · Accessibility & polish · **Not verified**
- [ ] **108. ICON SYSTEM** — P3/P4 · 10 · Accessibility & polish · **Not verified**
- [ ] **109. MICRO-INTERACTIONS** — P3/P4 · 10 · Accessibility & polish · **Not verified**
- [ ] **110. ONBOARDING** — P3/P4 · 1 · Architecture · **Not verified**
- [ ] **111. HOME EMPTY STATES** — P3/P4 · 1 · Architecture · **Not verified**
- [ ] **112. OFFLINE EMPTY STATES** — P3/P4 · 1 · Architecture · **Not verified**
- [ ] **113. SEARCH HISTORY** — P3/P4 · 1 · Architecture · **Not verified**
- [ ] **114. SMART SUGGESTIONS** — P3/P4 · 1 · Architecture · **Not verified**
- [ ] **115. DEEP LINKS** — P0 · 1 · Architecture · **Not verified**
- [ ] **116. APP SHORTCUT INTEGRATION** — P3/P4 · 1 · Architecture · **Not verified**
- [ ] **117. ANDROID SHARE TARGET** — P3/P4 · 1 · Architecture · **Not verified**
- [ ] **118. SMART CONTENT CROSS-LINKING** — P3/P4 · 1 · Architecture · **Not verified**
- [ ] **119. CONTENT GRAPH** — P3/P4 · 1 · Architecture · **Not verified**
- [ ] **120. MODULAR ARCHITECTURE** — P0 · 1 · Architecture · **Not verified**
- [ ] **121. DOMAIN LOGIC** — P0 · 1 · Architecture · **Not verified**
- [ ] **122. TESTING — QURAN** — P3/P4 · 10 · Verification · **Not verified**
- [ ] **123. TESTING — PRAYER** — P3/P4 · 10 · Verification · **Not verified**
- [ ] **124. TESTING — QIBLA** — P3/P4 · 10 · Verification · **Not verified**
- [ ] **125. TESTING — AUDIO** — P3/P4 · 10 · Verification · **Not verified**
- [ ] **126. TESTING — SEARCH** — P3/P4 · 10 · Verification · **Not verified**
- [ ] **127. TESTING — NOTIFICATIONS** — P3/P4 · 10 · Verification · **Not verified**
- [ ] **128. TESTING — RTL** — P3/P4 · 10 · Verification · **Not verified**
- [ ] **129. TESTING — ACCESSIBILITY** — P3/P4 · 10 · Verification · **Not verified**
- [ ] **130. EXTREME EDGE CASES** — P3/P4 · 10 · Verification · **Not verified**
- [ ] **131. VISUAL QA** — P3/P4 · 10 · Verification · **Not verified**
- [ ] **132. NO FAKE FEATURES** — P3/P4 · 10 · Verification · **Not verified**
- [ ] **133. NO UNNECESSARY COMPLEXITY ON HOME** — P3/P4 · 10 · Verification · **Not verified**
- [ ] **134. RELIGIOUS UX** — P3/P4 · 10 · Product trust · **Not verified**
- [ ] **135. ADS** — P3/P4 · 10 · Product trust · **Not verified**
- [ ] **136. CONTENT SAFETY / TRUST** — P3/P4 · 10 · Product trust · **Not verified**
- [ ] **137. AI CONTENT UI** — P3/P4 · 10 · Product trust · **Not verified**
- [ ] **138. MODERN UX FEATURES** — P3/P4 · 10 · Product trust · **Not verified**
- [ ] **139. OPTIONAL WEAR OS** — P3/P4 · 10 · Product trust · **Not verified**
- [ ] **140. OPTIONAL ANDROID AUTO** — P3/P4 · 10 · Product trust · **Not verified**
- [ ] **141. FUTURE MULTI-DEVICE** — P3/P4 · 10 · Product trust · **Not verified**
- [ ] **142. INTERNATIONALIZATION** — P3/P4 · 5/10 · UX quality · **Not verified**
- [ ] **143. CONTENT UPDATE SYSTEM** — P3/P4 · 5/10 · UX quality · **Not verified**
- [ ] **144. STORAGE MANAGEMENT** — P3/P4 · 5/10 · UX quality · **Not verified**
- [ ] **145. PERFORMANCE BUDGET** — P3/P4 · 5/10 · UX quality · **Not verified**
- [ ] **146. BATTERY** — P3/P4 · 5/10 · UX quality · **Not verified**
- [ ] **147. PERMISSION UX** — P3/P4 · 5/10 · UX quality · **Not verified**
- [ ] **148. ONBOARDING PERSONALIZATION** — P3/P4 · 5/10 · UX quality · **Not verified**
- [ ] **149. DEFAULT EXPERIENCE** — P3/P4 · 5/10 · UX quality · **Not verified**
- [ ] **150. POWER USER EXPERIENCE** — P3/P4 · 5/10 · UX quality · **Not verified**
- [ ] **151. DAILY EXPERIENCE** — P3/P4 · 5/10 · UX quality · **Not verified**
- [ ] **152. TRAVEL EXPERIENCE** — P3/P4 · 5/10 · UX quality · **Not verified**
- [ ] **153. BEDTIME EXPERIENCE** — P3/P4 · 5/10 · UX quality · **Not verified**
- [ ] **154. MORNING EXPERIENCE** — P3/P4 · 5/10 · UX quality · **Not verified**
- [ ] **155. PRAYER MODE** — P3/P4 · 5/10 · UX quality · **Not verified**
- [ ] **156. KURDISH CULTURAL QUALITY** — P3/P4 · 5/10 · UX quality · **Not verified**
- [ ] **157. VISUAL HIERARCHY** — P3/P4 · 5/10 · UX quality · **Not verified**
- [ ] **158. INFORMATION DENSITY** — P3/P4 · 5/10 · UX quality · **Not verified**
- [ ] **159. SEARCH RESULT DEEP LINKING** — P3/P4 · 5/10 · UX quality · **Not verified**
- [ ] **160. CONTENT SELECTION** — P3/P4 · 1/10 · Data/security · **Not verified**
- [ ] **161. USER CONTROL** — P3/P4 · 1/10 · Data/security · **Not verified**
- [ ] **162. CORE DATA ENTITIES** — P3/P4 · 1/10 · Data/security · **Not verified**
- [ ] **163. DATA RELATIONSHIPS** — P3/P4 · 1/10 · Data/security · **Not verified**
- [ ] **164. LOCAL FIRST USER DATA** — P3/P4 · 1/10 · Data/security · **Not verified**
- [ ] **165. BACKUP FORMAT** — P3/P4 · 1/10 · Data/security · **Not verified**
- [ ] **166. FAIL-SAFE MIGRATION** — P3/P4 · 1/10 · Data/security · **Not verified**
- [ ] **167. SECURITY** — P3/P4 · 1/10 · Data/security · **Not verified**
- [ ] **168. LOGGING** — P3/P4 · 1/10 · Data/security · **Not verified**
- [ ] **169. CRASH RESILIENCE** — P3/P4 · 1/10 · Data/security · **Not verified**
- [ ] **170. FINAL PRODUCT QUALITY BAR** — P3/P4 · 10 · Release · **Not verified**
- [ ] **171. IMPLEMENTATION ORDER** — P3/P4 · 10 · Release · **Not verified**
- [ ] **172. PRIORITY** — P3/P4 · 10 · Release · **Not verified**
- [ ] **173. FINAL PRE-PUBLISH CHECKLIST** — P3/P4 · 10 · Release · **Not verified**
- [ ] **174. ABSOLUTE NO-CHEATING RULE** — P3/P4 · 10 · Release · **Not verified**
- [ ] **175. FINAL CREATIVE DIRECTIVE** — P3/P4 · 10 · Release · **Not verified**
- [ ] **176. FINAL DEFINITION OF SUCCESS** — P3/P4 · 10 · Release · **Not verified**
- [ ] **177. FINAL COMMAND** — P3/P4 · 10 · Release · **Not verified**

## Required release evidence (not optional)

- [ ] Debug APK is produced by the current commit's CI, non-empty, downloaded and inspected.
- [ ] APK package name, min SDK, version, checksum and debug/release signing state are reported.
- [ ] Release APK/AAB build state is reported separately from installable debug APK.
- [ ] Unit tests include Quran dataset structure/alignment, prayer calculation test vectors, reference parsing, persistence and migrations.
- [ ] Compose/UI tests cover every main tab, nested Quran routes, content errors, settings/language switching, permission-denied cases and key tap actions.
- [ ] Screenshot/visual review at phone size for Sorani, Arabic and English, light/dark mode, font scaling, and RTL directional motion.
- [ ] The installable artifact is only described as install-tested if it was actually installed on a device/emulator. Do not claim tests on the user's Realme X2 Pro without direct evidence.

## Change log

- **Foundation V2 start:** isolated branch `product-v2/foundation-rebuild`.
- **Motion:** added subtle fade/slide transitions to Navigation Compose; horizontal direction reverses with RTL layout direction. This is an implemented code change but remains pending CI compile and visual review.
