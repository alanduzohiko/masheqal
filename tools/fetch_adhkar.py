#!/usr/bin/env python3
"""Fetch and validate the licensed Arabic/English morning-evening adhkar dataset.

The upstream release is pinned (not fetched from a moving main branch). Arabic dhikr,
English translations and source references are copied verbatim; no religious text is generated.
"""
from __future__ import annotations

from datetime import datetime, timezone
import hashlib
import json
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
