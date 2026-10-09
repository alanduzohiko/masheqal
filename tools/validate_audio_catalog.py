#!/usr/bin/env python3
"""Check configured Quran surah-audio editions against the CDN's published manifest."""
from __future__ import annotations

import json
import re
import sys
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "app/src/main/java/com/masheqal/app/ui/screens/QuranScreen.kt"
MANIFEST_URL = "https://cdn.islamic.network/quran/info/by-surah/info.json"
SURAH_COUNT = 114
VALID_BITRATES = {192, 128, 64, 48, 40, 32}


def load_catalog() -> list[tuple[str, int]]:
    source = CATALOG.read_text(encoding="utf-8")
    match = re.search(
        r"object\s+QuranAudioCatalog\s*\{.*?private\s+val\s+editions\s*=\s*mapOf\((.*?)^\s*\)",
        source,
        re.DOTALL | re.MULTILINE,
    )
    if not match:
        raise RuntimeError("Could not find the QuranAudioCatalog editions map")
    entries = [
        (edition, int(bitrate))
        for edition, bitrate in re.findall(r'"(ar\.[A-Za-z0-9]+)"\s+to\s+(\d+)', match.group(1))
    ]
    if not entries:
        raise RuntimeError("QuranAudioCatalog contains no recognized edition/bitrate pairs")
    edition_ids = [edition for edition, _ in entries]
    if len(edition_ids) != len(set(edition_ids)):
        raise RuntimeError("QuranAudioCatalog contains duplicate edition IDs")
    invalid = [(edition, bitrate) for edition, bitrate in entries if bitrate not in VALID_BITRATES]
    if invalid:
        raise RuntimeError(f"Unsupported audio bitrate(s): {invalid}")
    return entries


def find_audio_root(payload: object) -> dict:
    roots = payload if isinstance(payload, list) else [payload]
    for node in roots:
        if (
            isinstance(node, dict)
            and node.get("type") == "directory"
            and "audio-surah" in str(node.get("name", ""))
            and isinstance(node.get("contents"), list)
        ):
            return node
    raise RuntimeError("Upstream manifest lacks the expected audio-surah directory")


def main() -> int:
    catalog = load_catalog()
    request = urllib.request.Request(
        MANIFEST_URL,
        headers={"Accept": "application/json", "User-Agent": "Masheqal-Android-Catalog-Validator/1.0"},
    )
    with urllib.request.urlopen(request, timeout=45) as response:
        if response.status != 200:
            raise RuntimeError(f"Upstream manifest returned HTTP {response.status}")
        payload = json.load(response)

    root = find_audio_root(payload)
    bitrate_nodes = {
        str(node.get("name")): node
        for node in root["contents"]
        if isinstance(node, dict) and node.get("type") == "directory"
    }
    problems: list[str] = []
    verified_files = 0

    for edition, bitrate in catalog:
        bitrate_node = bitrate_nodes.get(str(bitrate))
        edition_node = None
        if bitrate_node is not None:
            edition_node = next(
                (
                    node for node in bitrate_node.get("contents", [])
                    if isinstance(node, dict)
                    and node.get("type") == "directory"
                    and node.get("name") == edition
                ),
                None,
            )
        if edition_node is None:
            problems.append(f"{edition}@{bitrate}: edition/bitrate folder is absent from the manifest")
            continue

        files = {
            node.get("name")
            for node in edition_node.get("contents", [])
            if isinstance(node, dict) and node.get("type") == "file"
        }
        missing = [
            f"{number}.mp3"
            for number in range(1, SURAH_COUNT + 1)
            if f"{number}.mp3" not in files
        ]
        if missing:
            problems.append(
                f"{edition}@{bitrate}: {len(missing)}/{SURAH_COUNT} files missing; "
                f"examples: {', '.join(missing[:6])}"
            )
        else:
            verified_files += SURAH_COUNT
            print(f"VERIFIED {edition}@{bitrate}: all {SURAH_COUNT} surah files are listed")

    if problems:
        print("Quran audio catalogue validation failed:", file=sys.stderr)
        for problem in problems:
            print(f" - {problem}", file=sys.stderr)
        return 1

    print(f"Quran audio catalogue validated: {len(catalog)} editions and {verified_files} files.")
    print("Availability metadata is not a separate license grant.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as error:
        print(f"Quran audio catalogue validation failed: {error}", file=sys.stderr)
        raise SystemExit(1)
