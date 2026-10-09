#!/usr/bin/env python3
"""Fetch the pinned 99 Names dataset and its Apache-2.0 source license."""
from __future__ import annotations

from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import time
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[1]
DATA_COMMIT = "094dc91e11316a0eb5150f84dd106f3ea1be60e5"
LICENSE_COMMIT = "6df43672a5306ff6e19b1e29dacfe88cf6385ccb"
DATA_URL = (
    "https://raw.githubusercontent.com/UmmahLibrary/ummah-library/"
    + DATA_COMMIT + "/packages/data/datasets/asma.json"
)
LICENSE_URL = (
    "https://raw.githubusercontent.com/my-prayers/muslim-data-flutter/"
    + LICENSE_COMMIT + "/LICENSE"
)
ATTRIBUTION = (
    "The 99 Names are from the Quran and Sunnah. Arabic, transliteration, and "
    "English meanings are from my-prayers/muslim-data-flutter (Apache-2.0), "
    "as packaged and attributed by UmmahLibrary/ummah-library. The English "
    "meaning wording has not yet received independent scholarly review. "
    "No Quran/hadith reference has been inferred where the source provides none."
)
MAX_DATA_BYTES = 256 * 1024
MAX_LICENSE_BYTES = 64 * 1024
USER_AGENT = "Masheqal-Android/0.2 (pinned licensed Names of Allah data import)"


def fetch(url: str, limit: int) -> bytes:
    last_error: Exception | None = None
    for attempt in range(3):
        try:
            request = Request(url, headers={
                "Accept": "application/json, text/plain;q=0.9",
                "User-Agent": USER_AGENT,
            })
            with urlopen(request, timeout=30) as response:
                if response.status != 200:
                    raise RuntimeError(f"HTTP {response.status} while fetching {url}")
                data = response.read(limit + 1)
                if not data:
                    raise RuntimeError(f"Empty response from {url}")
                if len(data) > limit:
                    raise RuntimeError(f"Response from {url} exceeds {limit} bytes")
                return data
        except (HTTPError, URLError, TimeoutError, OSError, ValueError, RuntimeError) as exc:
            last_error = exc
            if attempt < 2:
                time.sleep(2 ** attempt)
    raise RuntimeError(f"Unable to fetch pinned source {url}: {last_error}") from last_error


def main() -> None:
    raw = fetch(DATA_URL, MAX_DATA_BYTES)
    license_bytes = fetch(LICENSE_URL, MAX_LICENSE_BYTES)
    try:
        dataset = json.loads(raw.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise RuntimeError(f"Names dataset is not valid UTF-8 JSON: {exc}") from exc

    if not isinstance(dataset, dict) or not isinstance(dataset.get("names"), list):
        raise RuntimeError("Names dataset must be an object with a names array")
    names = dataset["names"]
    if len(names) != 99:
        raise RuntimeError(f"Expected 99 Names of Allah, got {len(names)}")

    numbers: list[int] = []
    for row in names:
        if not isinstance(row, dict):
            raise RuntimeError("Every Names of Allah record must be an object")
        number = row.get("number")
        if not isinstance(number, int) or isinstance(number, bool):
            raise RuntimeError(f"Invalid name number: {number!r}")
        numbers.append(number)
        for field in ("arabic", "transliteration", "meaning"):
            if not isinstance(row.get(field), str) or not row[field].strip():
                raise RuntimeError(f"Name {number} has no valid {field}")
        references = row.get("references")
        if not isinstance(references, list):
            raise RuntimeError(f"Name {number} must carry a references array, even when empty")

    if numbers != list(range(1, 100)):
        raise RuntimeError("Names must be unique and ordered from 1 to 99")
    source_text = str(dataset.get("source", ""))
    if "my-prayers/muslim-data" not in source_text or "Apache-2.0" not in source_text:
        raise RuntimeError("Dataset provenance does not identify the Apache-2.0 source")

    license_text = license_bytes.decode("utf-8")
    if "Apache License" not in license_text or "Version 2.0" not in license_text:
        raise RuntimeError("The upstream Apache-2.0 license text is missing or unexpected")

    output = ROOT / "app/src/main/assets/content"
    output.mkdir(parents=True, exist_ok=True)
    data_path = output / "names_of_allah.json"
    license_path = output / "names_of_allah_APACHE-2.0.txt"
    manifest_path = output / "names_of_allah_manifest.json"
    # Preserve the upstream generated JSON byte-for-byte.
    data_path.write_bytes(raw)
    license_path.write_bytes(license_bytes)

    manifest = {
        "source": "UmmahLibrary/ummah-library packages/data/datasets/asma.json",
        "sourceUrl": DATA_URL,
        "sourceCommit": DATA_COMMIT,
        "underlyingDataSource": "my-prayers/muslim-data-flutter",
        "underlyingDataSourceUrl": "https://github.com/my-prayers/muslim-data-flutter",
        "underlyingDataCommit": LICENSE_COMMIT,
        "license": "Apache-2.0",
        "licenseUrl": LICENSE_URL,
        "recordCount": len(names),
        "dataSha256": hashlib.sha256(raw).hexdigest(),
        "licenseSha256": hashlib.sha256(license_bytes).hexdigest(),
        "attribution": ATTRIBUTION,
        "languageCoverage": ["Arabic", "transliteration (Latin)", "English meaning"],
        "soraniMeaningIncluded": False,
        "englishMeaningScholarReviewStatus": "pending",
        "recordsWithReferences": sum(1 for row in names if row["references"]),
        "recordsWithoutReferences": sum(1 for row in names if not row["references"]),
        "fetchedAtUtc": datetime.now(timezone.utc).isoformat(),
    }
    manifest_path.write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    print(json.dumps({
        "source": manifest["source"],
        "recordCount": manifest["recordCount"],
        "license": manifest["license"],
        "referencesPresent": manifest["recordsWithReferences"],
        "referencesNotProvidedBySource": manifest["recordsWithoutReferences"],
        "soraniMeaningIncluded": manifest["soraniMeaningIncluded"],
        "englishMeaningScholarReviewStatus": manifest["englishMeaningScholarReviewStatus"],
        "dataSha256": manifest["dataSha256"],
        "licenseSha256": manifest["licenseSha256"],
    }, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
