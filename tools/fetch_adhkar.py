#!/usr/bin/env python3
"""Fetch and validate the licensed Arabic/English morning-evening adhkar dataset.

The upstream release is pinned (not fetched from a moving main branch). Arabic dhikr,
English translations and source references are copied verbatim; no religious text is generated.
"""
from __future__ import annotations

from datetime import datetime, timezone
import hashlib
import json
import re
from pathlib import Path
import time
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

UPSTREAM = "https://raw.githubusercontent.com/Seen-Arabic/Morning-And-Evening-Adhkar-DB/v1.0.2"
SOURCE_REPO = "Seen-Arabic/Morning-And-Evening-Adhkar-DB"
SOURCE_RELEASE = "v1.0.2"
EXPECTED_RECORDS = 34
MAX_BYTES = 512 * 1024
TIMEOUT_SECONDS = 25
USER_AGENT = "Masheqal-Android/0.2 (licensed adhkar content import)"
EXTENDED_SOURCE_COMMIT = "f42f895f914319a844c3e3c2279483cae060ea19"
EXTENDED_SOURCE_BASE = f"https://raw.githubusercontent.com/fitrahive/dua-dhikr/{EXTENDED_SOURCE_COMMIT}"
EXTENDED_MAX_BYTES = 512 * 1024


def fetch(path: str) -> bytes:
    url = f"{UPSTREAM}/{path}"
    last_error: Exception | None = None
    for attempt in range(3):
        try:
            request = Request(url, headers={
                "Accept": "application/json, text/plain;q=0.9",
                "User-Agent": USER_AGENT,
            })
            with urlopen(request, timeout=TIMEOUT_SECONDS) as response:
                if response.status != 200:
                    raise RuntimeError(f"HTTP {response.status} while fetching {url}")
                data = response.read(MAX_BYTES + 1)
                if len(data) > MAX_BYTES:
                    raise RuntimeError(f"{path} exceeds the {MAX_BYTES}-byte safety limit")
                if not data:
                    raise RuntimeError(f"{path} returned an empty response")
                return data
        except (HTTPError, URLError, TimeoutError, OSError, ValueError, RuntimeError) as exc:
            last_error = exc
            if attempt < 2:
                time.sleep(2 ** attempt)
    raise RuntimeError(f"Unable to fetch pinned source file {path}: {last_error}") from last_error


def required_text(obj: dict, key: str, order: int, language: str) -> str:
    value = obj.get(key)
    if not isinstance(value, str) or not value.strip():
        raise RuntimeError(f"{language} record {order} has no valid {key!r}")
    return value




def fetch_pinned_url(url: str, max_bytes: int = EXTENDED_MAX_BYTES) -> bytes:
    """Fetch an immutable, MIT-licensed upstream file with bounded retries and size."""
    last_error: Exception | None = None
    for attempt in range(3):
        try:
            request = Request(url, headers={
                "Accept": "application/json, text/plain;q=0.9",
                "User-Agent": USER_AGENT,
            })
            with urlopen(request, timeout=TIMEOUT_SECONDS) as response:
                if response.status != 200:
                    raise RuntimeError(f"HTTP {response.status} while fetching {url}")
                data = response.read(max_bytes + 1)
                if not data:
                    raise RuntimeError(f"{url} returned an empty response")
                if len(data) > max_bytes:
                    raise RuntimeError(f"{url} exceeds the {max_bytes}-byte safety limit")
                return data
        except (HTTPError, URLError, TimeoutError, OSError, ValueError, RuntimeError) as exc:
            last_error = exc
            if attempt < 2:
                time.sleep(2 ** attempt)
    raise RuntimeError(f"Unable to fetch pinned upstream file {url}: {last_error}") from last_error


def required_external_text(row: dict, key: str, order: int) -> str:
    value = row.get(key)
    if not isinstance(value, str) or not value.strip():
        raise RuntimeError(f"Extended adhkar record {order} is missing {key!r}")
    return value


def classify_daily_occasion(title: str) -> str:
    """Deterministic grouping from the publisher's title; religious text is never inferred."""
    value = title.casefold()
    groups = (
        ("sleep", ("sleep", "dream")),
        ("wake_up", ("waking", "wake up")),
        ("bathroom", ("bathroom", "toilet", "khubuth")),
        ("wudu", ("ablution", "wudu")),
        ("mosque", ("mosque",)),
        ("fasting", ("fast", "iftar", "breaking the fast")),
        ("food", ("eating", "meal", "food", "drink", "bismillah at")),
        ("home", ("house", "home", "outside the house")),
        ("travel", ("travel", "traveler", "travelling", "traveling", "vehicle", "mounting a")),
        ("clothing", ("clothes", "clothing", "wearing")),
        ("weather", ("rain", "wind", "weather")),
        ("protection", ("protection", "forgiveness", "forgiving", "calamity", "debt", "ease in", "laziness", "sadness", "hardship", "distress", "satan")),
        ("general", ("sneez", "character", "parents", "adhan")),
    )
    for category, words in groups:
        if any(word in value for word in words):
            return category
    return "general"


def build_extended_package(base_records: list[dict], output_dir: Path, seen_license_bytes: bytes) -> dict:
    """
    Generate one offline catalogue from two explicitly MIT-licensed datasets.
    Source text, English meanings and references are copied verbatim. Items without a source
    reference in the after-salah file are excluded rather than supplied with a fabricated citation.
    """
    combined: list[dict] = []
    for row in base_records:
        kind = int(row["type"])
        category = "morning_evening" if kind == 0 else ("morning" if kind == 1 else "evening")
        combined.append({
            **row,
            "categoryId": category,
            "titleEn": "",
        })

    after_url = f"{EXTENDED_SOURCE_BASE}/data/dua-dhikr/dhikr-after-salah/en.json"
    daily_url = f"{EXTENDED_SOURCE_BASE}/data/dua-dhikr/daily-dua/en.json"
    license_url = f"{EXTENDED_SOURCE_BASE}/LICENSE"
    raw_after = fetch_pinned_url(after_url)
    raw_daily = fetch_pinned_url(daily_url)
    raw_extended_license = fetch_pinned_url(license_url, 64 * 1024)

    try:
        after_rows = json.loads(raw_after.decode("utf-8"))
        daily_rows = json.loads(raw_daily.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise RuntimeError(f"Pinned daily dua/dhikr JSON could not be parsed: {exc}") from exc

    if not isinstance(after_rows, list) or len(after_rows) != 13:
        raise RuntimeError(f"Expected 13 after-salah records at the pinned source; got {len(after_rows) if isinstance(after_rows, list) else 'non-list'}")
    if not isinstance(daily_rows, list) or len(daily_rows) != 38:
        raise RuntimeError(f"Expected 38 daily-occasion records at the pinned source; got {len(daily_rows) if isinstance(daily_rows, list) else 'non-list'}")

    extended_license_text = raw_extended_license.decode("utf-8")
    if "MIT License" not in extended_license_text or "Copyright (c) 2023 Fitrahive" not in extended_license_text:
        raise RuntimeError("Pinned Fitrahive license is missing or does not match the expected MIT notice")

    skipped_without_source: list[dict] = []
    next_order = len(combined) + 1

    def append_external(row: dict, category_id: str, source_url: str, source_name: str) -> None:
        nonlocal next_order
        if not isinstance(row, dict):
            raise RuntimeError("Every extended adhkar source record must be a JSON object")
        order = next_order
        title = required_external_text(row, "title", order)
        arabic = required_external_text(row, "arabic", order)
        translation = required_external_text(row, "translation", order)
        source = row.get("source")
        if not isinstance(source, str) or not source.strip():
            if source_name.endswith("dhikr-after-salah/en.json"):
                skipped_without_source.append({
                    "title": title,
                    "reason": "The pinned source record contains no source/reference field.",
                    "sourceUrl": source_url,
                })
                return
            raise RuntimeError(f"Daily adhkar record {title!r} has no source/reference")

        notes = row.get("notes")
        notes_text = notes.strip() if isinstance(notes, str) else ""
        count_match = re.search(r"\b(?:read|recite)\s*(\d+)\s*x\b", notes_text, re.IGNORECASE)
        repeat_count = int(count_match.group(1)) if count_match else 1
        if not 1 <= repeat_count <= 1000:
            raise RuntimeError(f"Invalid repetition count in external adhkar {title!r}")

        combined.append({
            "order": next_order,
            "categoryId": category_id,
            "titleEn": title,
            "arabic": arabic,
            "translationEn": translation,
            "transliteration": str(row.get("latin") or ""),
            "repeatCount": repeat_count,
            "repeatDescriptionAr": "",
            "repeatDescriptionEn": notes_text,
            "meritAr": "",
            "meritEn": str(row.get("benefits") or ""),
            "sourceAr": "",
            "sourceEn": source.strip(),
            "type": 3,
            "audioUrl": "",
            "hadithAr": "",
            "hadithEn": "",
            "vocabularyAr": "",
            "vocabularyEn": "",
        })
        next_order += 1

    for row in after_rows:
        append_external(
            row,
            "after_prayer",
            after_url,
            "fitrahive/dua-dhikr/dhikr-after-salah/en.json",
        )
    for row in daily_rows:
        title = required_external_text(row, "title", next_order)
        append_external(
            row,
            classify_daily_occasion(title),
            daily_url,
            "fitrahive/dua-dhikr/daily-dua/en.json",
        )

    if len(combined) != 82:
        raise RuntimeError(
            f"Extended adhkar package must contain 82 attributed rows; got {len(combined)} "
            f"(source-less after-salah records skipped: {len(skipped_without_source)})"
        )
    if [row.get("order") for row in combined] != list(range(1, len(combined) + 1)):
        raise RuntimeError("Combined adhkar item ids must be sequential and unique")
    for row in combined:
        if not str(row.get("arabic", "")).strip() or not str(row.get("translationEn", "")).strip():
            raise RuntimeError(f"Combined adhkar row {row.get('order')} has missing text")
        if not str(row.get("sourceEn", "")).strip():
            raise RuntimeError(f"Combined adhkar row {row.get('order')} has no source/reference")
        if row.get("categoryId") not in {
            "morning_evening", "morning", "evening", "after_prayer", "sleep", "wake_up",
            "bathroom", "food", "mosque", "wudu", "fasting", "home", "travel", "clothing",
            "weather", "protection", "general"
        }:
            raise RuntimeError(f"Unknown category id in combined adhkar row {row.get('order')}")

    content_bytes = (json.dumps(combined, ensure_ascii=False, separators=(",", ":")) + "\n").encode("utf-8")
    content_sha = hashlib.sha256(content_bytes).hexdigest()
    (output_dir / "adhkar_all.json").write_bytes(content_bytes)

    seen_license_text = seen_license_bytes.decode("utf-8")
    if "MIT License" not in seen_license_text or "Copyright (c) 2024 Seen Arabic" not in seen_license_text:
        raise RuntimeError("Seen-Arabic license is missing while building combined adhkar package")
    combined_licenses = (
        "Masheqal bundled adhkar source licenses\n"
        "========================================\n\n"
        "Dataset 1: Seen-Arabic/Morning-And-Evening-Adhkar-DB, release v1.0.2\n"
        "Dataset 2: fitrahive/dua-dhikr, pinned commit " + EXTENDED_SOURCE_COMMIT + "\n\n"
        "Both datasets are used under MIT. Their complete original notices follow.\n\n"
        "----- Seen Arabic LICENSE -----\n" + seen_license_text +
        "\n\n----- Fitrahive LICENSE -----\n" + extended_license_text + "\n"
    ).encode("utf-8")
    (output_dir / "adhkar_all_source_LICENSES.txt").write_bytes(combined_licenses)

    manifest = {
        "source": "Combined licensed adhkar datasets",
        "sourceUrl": "https://github.com/Seen-Arabic/Morning-And-Evening-Adhkar-DB",
        "sourceReleaseUrl": f"https://github.com/fitrahive/dua-dhikr/commit/{EXTENDED_SOURCE_COMMIT}",
        "sourceRef": f"Seen-Arabic/{SOURCE_RELEASE} + fitrahive/dua-dhikr@{EXTENDED_SOURCE_COMMIT}",
        "recordCount": len(combined),
        "contentSha256": content_sha,
        "license": "MIT (both source datasets)",
        "licenseSha256": hashlib.sha256(combined_licenses).hexdigest(),
        "attribution": "Seen Arabic Morning and Evening Adhkar Database + Fitrahive Dua & Dhikr; each item's source/reference is preserved.",
        "languageCoverage": ["Arabic", "English"],
        "soraniTranslationIncluded": False,
        "scholarReviewStatus": "pending",
        "sources": [
            {
                "source": SOURCE_REPO,
                "sourceRef": SOURCE_RELEASE,
                "sourceUrl": "https://github.com/Seen-Arabic/Morning-And-Evening-Adhkar-DB",
                "license": "MIT",
                "copyright": "Copyright (c) 2024 Seen Arabic",
                "recordCount": len(base_records),
                "rawArabicSha256": hashlib.sha256(fetch(f"ar.json")).hexdigest(),
                "rawEnglishSha256": hashlib.sha256(fetch(f"en.json")).hexdigest(),
            },
            {
                "source": "fitrahive/dua-dhikr",
                "sourceRef": EXTENDED_SOURCE_COMMIT,
                "sourceUrl": "https://github.com/fitrahive/dua-dhikr",
                "license": "MIT",
                "copyright": "Copyright (c) 2023 Fitrahive",
                "afterPrayerSourceUrl": after_url,
                "afterPrayerSourceSha256": hashlib.sha256(raw_after).hexdigest(),
                "afterPrayerSourceCount": len(after_rows),
                "afterPrayerIncludedCount": sum(1 for row in after_rows if isinstance(row, dict) and isinstance(row.get("source"), str) and bool(row["source"].strip())),
                "dailySourceUrl": daily_url,
                "dailySourceSha256": hashlib.sha256(raw_daily).hexdigest(),
                "dailySourceCount": len(daily_rows),
                "dailyIncludedCount": len(daily_rows),
                "licenseUrl": license_url,
                "licenseSha256": hashlib.sha256(raw_extended_license).hexdigest(),
            },
        ],
        "skippedRecords": skipped_without_source,
        "fetchedAtUtc": datetime.now(timezone.utc).isoformat(),
    }
    (output_dir / "adhkar_all_manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    print(json.dumps({
        "source": manifest["source"],
        "recordCount": manifest["recordCount"],
        "morningEveningRecords": len(base_records),
        "afterPrayerSourceRecords": len(after_rows),
        "afterPrayerIncluded": manifest["sources"][1]["afterPrayerIncludedCount"],
        "dailyOccasionRecords": len(daily_rows),
        "dailyIncluded": manifest["sources"][1]["dailyIncludedCount"],
        "skippedNoSource": [item["title"] for item in skipped_without_source],
        "license": manifest["license"],
        "scholarReviewStatus": manifest["scholarReviewStatus"],
        "contentSha256": content_sha,
    }, ensure_ascii=False, indent=2))


def main() -> None:
    raw_ar = fetch("ar.json")
    raw_en = fetch("en.json")
    raw_license = fetch("LICENSE")

    try:
        arabic_rows = json.loads(raw_ar.decode("utf-8"))
        english_rows = json.loads(raw_en.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise RuntimeError(f"Upstream JSON could not be parsed: {exc}") from exc

    if not isinstance(arabic_rows, list) or not isinstance(english_rows, list):
        raise RuntimeError("Upstream data must consist of JSON arrays")
    if len(arabic_rows) != EXPECTED_RECORDS or len(english_rows) != EXPECTED_RECORDS:
        raise RuntimeError(
            f"Pinned release must contain {EXPECTED_RECORDS} Arabic and English records; "
            f"found {len(arabic_rows)} and {len(english_rows)}"
        )

    license_text = raw_license.decode("utf-8")
    if "MIT License" not in license_text or "Copyright (c) 2024 Seen Arabic" not in license_text:
        raise RuntimeError("Upstream license is missing or does not match the expected MIT notice")

    records: list[dict] = []
    seen: set[int] = set()
    for ar, en in zip(arabic_rows, english_rows, strict=True):
        if not isinstance(ar, dict) or not isinstance(en, dict):
            raise RuntimeError("Every upstream record must be a JSON object")
        order = ar.get("order")
        if not isinstance(order, int) or isinstance(order, bool) or order not in range(1, EXPECTED_RECORDS + 1):
            raise RuntimeError(f"Invalid Arabic record order: {order!r}")
        if order in seen or en.get("order") != order:
            raise RuntimeError(f"Duplicate or misaligned order in record {order}")
        seen.add(order)
        arabic = required_text(ar, "content", order, "Arabic")
        english_arabic = required_text(en, "content", order, "English")
        if arabic != english_arabic:
            raise RuntimeError(f"Arabic text differs between language datasets at order {order}")
        translation = required_text(en, "translation", order, "English")
        source_ar = required_text(ar, "source", order, "Arabic")
        source_en = required_text(en, "source", order, "English")

        count = ar.get("count")
        if not isinstance(count, int) or isinstance(count, bool) or not 1 <= count <= 1000:
            raise RuntimeError(f"Invalid repeat count at order {order}: {count!r}")
        if en.get("count") != count:
            raise RuntimeError(f"Repeat count differs between language datasets at order {order}")
        kind = ar.get("type")
        if kind not in (0, 1, 2) or en.get("type") != kind:
            raise RuntimeError(f"Invalid or inconsistent morning/evening type at order {order}")

        records.append({
            "order": order,
            "arabic": arabic,
            "translationEn": translation,
            "transliteration": str(en.get("transliteration") or ""),
            "repeatCount": count,
            "repeatDescriptionAr": str(ar.get("count_description") or ""),
            "repeatDescriptionEn": str(en.get("count_description") or ""),
            "meritAr": str(ar.get("fadl") or ""),
            "meritEn": str(en.get("fadl") or ""),
            "sourceAr": source_ar,
            "sourceEn": source_en,
            "type": kind,
            "audioUrl": str(ar.get("audio") or ""),
            "hadithAr": str(ar.get("hadith_text") or ""),
            "hadithEn": str(en.get("hadith_text") or ""),
            "vocabularyAr": str(ar.get("explanation_of_hadith_vocabulary") or ""),
            "vocabularyEn": str(en.get("explanation_of_hadith_vocabulary") or ""),
        })

    records.sort(key=lambda item: item["order"])
    if [row["order"] for row in records] != list(range(1, EXPECTED_RECORDS + 1)):
        raise RuntimeError("Canonical order sequence must be unique and contiguous")

    content_bytes = (json.dumps(records, ensure_ascii=False, separators=(",", ":")) + "\n").encode("utf-8")
    content_sha = hashlib.sha256(content_bytes).hexdigest()
    license_sha = hashlib.sha256(raw_license).hexdigest()
    output_dir = Path("app/src/main/assets/content")
    output_dir.mkdir(parents=True, exist_ok=True)
    (output_dir / "adhkar_morning_evening.json").write_bytes(content_bytes)
    (output_dir / "adhkar_source_LICENSE.txt").write_bytes(raw_license)

    manifest = {
        "source": SOURCE_REPO,
        "sourceUrl": "https://github.com/Seen-Arabic/Morning-And-Evening-Adhkar-DB",
        "sourceReleaseUrl": f"https://github.com/{SOURCE_REPO}/releases/tag/{SOURCE_RELEASE}",
        "sourceRef": SOURCE_RELEASE,
        "recordCount": len(records),
        "arabicSourceSha256": hashlib.sha256(raw_ar).hexdigest(),
        "englishSourceSha256": hashlib.sha256(raw_en).hexdigest(),
        "contentSha256": content_sha,
        "license": "MIT",
        "licenseCopyright": "Copyright (c) 2024 Seen Arabic",
        "licenseSha256": license_sha,
        "attribution": "Morning and Evening Adhkar Database — Seen Arabic; source references are preserved on each record.",
        "languageCoverage": ["Arabic", "English"],
        "soraniTranslationIncluded": False,
        "fetchedAtUtc": datetime.now(timezone.utc).isoformat(),
    }
    (output_dir / "adhkar_morning_evening_manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )

    build_extended_package(records, output_dir, raw_license)

    print(json.dumps({
        "source": SOURCE_REPO,
        "release": SOURCE_RELEASE,
        "recordCount": len(records),
        "license": "MIT",
        "soraniTranslationIncluded": False,
        "contentSha256": content_sha,
        "licenseSha256": license_sha,
    }, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
