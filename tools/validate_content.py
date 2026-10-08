#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "app/src/main/assets/content"

def load(name):
    with (ROOT / name).open(encoding="utf-8") as f:
        return json.load(f)

ar = load("quran_ar_uthmani.json")
en = load("quran_en_translation.json")
surahs = load("surahs.json")
pages = load("quran_page_ranges.json")
juzs = load("quran_juz_ranges.json")
manifest = load("content-manifest.json")

assert len(ar) == len(en) == 6236
assert len(surahs) == 114
assert sum(s["ayah_count"] for s in surahs) == 6236
assert [v["id"] for v in ar] == list(range(1, 6237))
assert [(a["id"], a["surah"], a["ayah"]) for a in ar] == [(e["id"], e["surah"], e["ayah"]) for e in en]

for ranges, expected, key in [(pages, 604, "page"), (juzs, 30, "juz")]:
    assert len(ranges) == expected
    assert [r[key] for r in ranges] == list(range(1, expected + 1))
    cursor = 1
    for r in ranges:
        assert r["first_global_ayah"] == cursor
        assert r["last_global_ayah"] >= r["first_global_ayah"]
        cursor = r["last_global_ayah"] + 1
    assert cursor == 6237

assert manifest["quran"]["arabic"]["verses"] == 6236
assert manifest["quran"]["english"]["verses"] == 6236
print("Content validation passed: 114 surahs, 6236 ayahs, 604 pages, 30 juz, aligned English translation.")
