#!/usr/bin/env python3
"""Discover/download the verbatim Sorani translation from the publisher's public API.

QuranEnc permits verbatim re-publication with source, publisher, version and transcript
information, and requires updates to the latest issued version. No religious text is generated.
The source dataset is fetched at build time and is not committed into this repository.
"""
from __future__ import annotations

import argparse
from datetime import datetime, timezone
import hashlib
import json
import time
from pathlib import Path
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen

API = "https://quranenc.com/api/v1"
MAX_RESPONSE_BYTES = 4 * 1024 * 1024
TIMEOUT_SECONDS = 25
LICENSE = "QuranEnc.com terms: verbatim republication with publisher/source, version and transcript attribution; retain exact translation content and update to latest issued version."
ATTRIBUTION = "Translation: Burhan Muhammad-Amin (Tafsiri Asan) — via QuranEnc.com"


def get_json(url: str) -> Any:
    error: Exception | None = None
    for attempt in range(3):
        try:
            request = Request(url, headers={
                "Accept": "application/json",
                "User-Agent": "Masheqal-Android/0.2 (Sorani Quran source import)",
            })
            with urlopen(request, timeout=TIMEOUT_SECONDS) as response:
                if not 200 <= response.status < 300:
                    raise RuntimeError(f"HTTP {response.status} from QuranEnc API")
                chunks = bytearray()
                while True:
                    chunk = response.read(32 * 1024)
                    if not chunk:
                        break
                    chunks.extend(chunk)
                    if len(chunks) > MAX_RESPONSE_BYTES:
                        raise RuntimeError("QuranEnc response exceeds the 4 MiB safety limit")
            return json.loads(chunks.decode("utf-8"))
        except (HTTPError, URLError, TimeoutError, OSError, ValueError, RuntimeError) as exc:
            error = exc
            if attempt < 2:
                time.sleep(2 ** attempt)
    raise RuntimeError(f"QuranEnc API request failed for {url}: {error}") from error


def as_list(value: Any) -> list[dict[str, Any]]:
    if isinstance(value, list):
        return [item for item in value if isinstance(item, dict)]
    if isinstance(value, dict):
        for key in ("translations", "results", "result", "data"):
            child = value.get(key)
            if isinstance(child, list):
                return [item for item in child if isinstance(item, dict)]
    return []


def discover_translation() -> tuple[dict[str, Any] | None, list[dict[str, Any]]]:
    url_candidates = [
        f"{API}/translations/list/ku/?localization=en",
        f"{API}/translations/list/ckb/?localization=en",
        f"{API}/translations/list/?{urlencode({'localization': 'en'})}",
    ]
    all_entries: dict[str, dict[str, Any]] = {}
    errors: list[str] = []
    for url in url_candidates:
        try:
            payload = as_list(get_json(url))
            if not payload:
                errors.append(f"{url}: no translation records")
                continue
            for entry in payload:
                key = str(entry.get("key", "")).strip()
                if key:
                    all_entries[key] = entry
        except Exception as exc:
            errors.append(f"{url}: {exc}")

    entries = list(all_entries.values())
    candidates = []
    for entry in entries:
        key = str(entry.get("key", "")).lower()
        title = str(entry.get("title", "")).lower()
        description = str(entry.get("description", "")).lower()
        language = str(entry.get("language_iso_code", "")).lower()
        signal = " ".join((key, title, description))
        if (
            "sorani" in signal
            or ("kurdish" in signal and language in {"ku", "ckb"})
            or ("burhan" in signal and language in {"ku", "ckb"})
            or language == "ckb"
        ):
            candidates.append(entry)

    # Prefer the known, publisher-attributed Tafsiri Asan edition only when the API advertises it.
    preferred = [
        entry for entry in candidates
        if "sorani" in " ".join(
            str(entry.get(k, "")).lower() for k in ("key", "title", "description")
        )
        and (
            "burhan" in " ".join(
                str(entry.get(k, "")).lower() for k in ("key", "title", "description")
            )
            or "tafsiri asan" in " ".join(
                str(entry.get(k, "")).lower() for k in ("key", "title", "description")
            )
        )
    ]
    if len(preferred) == 1:
        return preferred[0], candidates
    if len(candidates) == 1:
        return candidates[0], candidates
    return None, candidates or [{"diagnostic": err} for err in errors]


def compact_entry(entry: dict[str, Any]) -> dict[str, Any]:
    return {
        "key": entry.get("key"),
        "language_iso_code": entry.get("language_iso_code"),
        "title": entry.get("title"),
        "description": entry.get("description"),
        "version": entry.get("version"),
        "last_update": entry.get("last_update"),
    }


def inspect_source() -> int:
    chosen, candidates = discover_translation()
    print(json.dumps({
        "provider": "QuranEnc.com official public API",
        "endpoint": f"{API}/translations/list/",
        "matches": [compact_entry(entry) for entry in candidates],
        "unique_candidate_selected": compact_entry(chosen) if chosen else None,
    }, ensure_ascii=False, indent=2))
    return 0 if chosen else 2


def export_translation(output_dir: Path) -> None:
    chosen, candidates = discover_translation()
    if not chosen:
        raise RuntimeError(
            "Could not uniquely identify a published Sorani/Kurdish translation; candidates: "
            + json.dumps([compact_entry(entry) for entry in candidates], ensure_ascii=False)
        )

    key = str(chosen.get("key", "")).strip()
    version = str(chosen.get("version", "")).strip()
    last_update = str(chosen.get("last_update", "")).strip()
    if not key or not version or not last_update:
        raise RuntimeError(
            "QuranEnc translation metadata must include key, version and last_update before release: "
            + json.dumps(compact_entry(chosen), ensure_ascii=False)
        )

    surah_file = output_dir / "surahs.json"
    if not surah_file.is_file():
        raise RuntimeError(f"Canonical surah metadata is missing: {surah_file}")
    surahs = json.loads(surah_file.read_text(encoding="utf-8"))
    if len(surahs) != 114:
        raise RuntimeError(f"Expected 114 canonical surahs, found {len(surahs)}")

    translations: dict[tuple[int, int], dict[str, Any]] = {}
    expected_total = 0
    for surah in surahs:
        surah_number = int(surah["number"])
        expected_count = int(surah["ayah_count"])
        expected_total += expected_count
        url = f"{API}/translation/sura/{key}/{surah_number}"
        payload = get_json(url)
        rows = as_list(payload)
        if not rows:
            raise RuntimeError(f"Surah {surah_number}: API returned no verse rows for {key}")
        for row in rows:
            try:
                row_surah = int(row.get("sura", row.get("surah")))
                ayah = int(row.get("aya", row.get("ayah")))
            except (TypeError, ValueError):
                raise RuntimeError(f"Surah {surah_number}: verse row has no numeric reference")
            if row_surah != surah_number or ayah < 1 or ayah > expected_count:
                raise RuntimeError(f"Unexpected verse reference {row_surah}:{ayah} in surah {surah_number}")
            ref = (row_surah, ayah)
            if ref in translations:
                raise RuntimeError(f"Duplicate translation row {row_surah}:{ayah}")
            text = row.get("translation")
            if not isinstance(text, str):
                raise RuntimeError(f"Translation text missing or not a string at {row_surah}:{ayah}")
            # Preserve the publisher's raw translation string exactly; do not trim or normalize it.
            translations[ref] = {
                "surah": row_surah,
                "ayah": ayah,
                "text": text,
                "footnotes": row.get("footnotes", ""),
            }
        time.sleep(0.05)

    expected_refs = {
        (int(s["number"]), a)
        for s in surahs for a in range(1, int(s["ayah_count"]) + 1)
    }
    unexpected = sorted(set(translations) - expected_refs)
    if unexpected:
        raise RuntimeError(f"Source contains unexpected ayah references: {unexpected[:10]}")

    records = []
    missing = []
    global_id = 0
    for surah in surahs:
        s = int(surah["number"])
        for ayah in range(1, int(surah["ayah_count"]) + 1):
            global_id += 1
            row = translations.get((s, ayah))
            text = row["text"] if row else ""
            if not text:
                missing.append(f"{s}:{ayah}")
            records.append({
                "id": global_id,
                "surah": s,
                "ayah": ayah,
                "text": text,
                "footnotes": row["footnotes"] if row else "",
                "missing": row is None or not bool(text),
            })
    if global_id != 6236 or len(records) != 6236:
        raise RuntimeError(f"Canonical output must contain 6,236 verse records, got {len(records)}")
    if len(missing) > 20:
        raise RuntimeError(
            f"Refusing to activate translation with {len(missing)} missing/empty ayahs; "
            "no missing verse will be fabricated."
        )

    output_dir.mkdir(parents=True, exist_ok=True)
    content_path = output_dir / "quran_ckb_translation.json"
    content_bytes = (json.dumps(records, ensure_ascii=False, separators=(",", ":")) + "\n").encode("utf-8")
    content_path.write_bytes(content_bytes)
    manifest = {
        "source": "QuranEnc.com official API",
        "endpoint": f"{API}/translation/sura/{key}/{{surah}}",
        "publisher": "QuranEnc.com",
        "translator": str(chosen.get("title") or "Sorani translator as listed by QuranEnc.com"),
        "language": "ckb",
        "translationKey": key,
        "version": version,
        "lastUpdate": last_update,
        "fetchedAtUtc": datetime.now(timezone.utc).isoformat(),
        "attribution": ATTRIBUTION,
        "licenseTerms": LICENSE,
        "canonicalAyahCount": 6236,
        "translatedAyahCount": sum(1 for row in records if row["text"]),
        "missingAyahs": missing,
        "contentSha256": hashlib.sha256(content_bytes).hexdigest(),
    }
    manifest_path = output_dir / "quran_ckb_manifest.json"
    manifest_path.write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    print(json.dumps({
        "source": manifest["source"],
        "translationKey": key,
        "version": version,
        "lastUpdate": last_update,
        "canonicalAyahCount": 6236,
        "translatedAyahCount": manifest["translatedAyahCount"],
        "missingCount": len(missing),
        "sha256": manifest["contentSha256"],
        "manifest": str(manifest_path),
    }, ensure_ascii=False, indent=2))


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--inspect-only", action="store_true")
    parser.add_argument(
        "--output-dir",
        default="app/src/main/assets/content",
        help="Content assets directory to receive the verified Sorani JSON",
    )
    args = parser.parse_args()
    if args.inspect_only:
        raise SystemExit(inspect_source())
    export_translation(Path(args.output_dir))


if __name__ == "__main__":
    main()
