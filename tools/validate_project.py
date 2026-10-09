#!/usr/bin/env python3
from pathlib import Path
import json
import hashlib
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app/src/main/res"
CONTENT = ROOT / "app/src/main/assets/content"

# XML syntax / duplicate string names.
xml_files = sorted(RES.rglob("*.xml"))
for file in xml_files:
    ET.parse(file)
for rel in ["values/strings.xml", "values-ar/strings.xml", "values-ckb/strings.xml"]:
    names = [n.attrib["name"] for n in ET.parse(RES / rel).getroot().findall("string")]
    assert len(names) == len(set(names)), f"duplicate string key in {rel}"
base_names = {n.attrib["name"] for n in ET.parse(RES / "values/strings.xml").getroot().findall("string")}
for rel in ["values-ar/strings.xml", "values-ckb/strings.xml"]:
    names = {n.attrib["name"] for n in ET.parse(RES / rel).getroot().findall("string")}
    assert names == base_names, f"resource key mismatch: {rel}"

# Quran structural integrity.
def load(name):
    return json.loads((CONTENT / name).read_text(encoding="utf-8"))

ar, en, surahs = load("quran_ar_uthmani.json"), load("quran_en_translation.json"), load("surahs.json")
assert len(ar) == len(en) == 6236
assert len(surahs) == 114
assert sum(s["ayah_count"] for s in surahs) == 6236
assert [v["id"] for v in ar] == list(range(1, 6237))
assert [(v["id"], v["surah"], v["ayah"]) for v in ar] == [(v["id"], v["surah"], v["ayah"]) for v in en]
# Sorani translation is fetched verbatim from the publisher API before validation.
ckb_path = CONTENT / "quran_ckb_translation.json"
ckb_manifest_path = CONTENT / "quran_ckb_manifest.json"
assert ckb_path.is_file(), "licensed Sorani translation asset was not generated"
assert ckb_manifest_path.is_file(), "Sorani translation provenance manifest is missing"
ckb = json.loads(ckb_path.read_text(encoding="utf-8"))
ckb_manifest = json.loads(ckb_manifest_path.read_text(encoding="utf-8"))
assert len(ckb) == 6236, f"Sorani asset must carry 6236 aligned verse records, got {len(ckb)}"
assert [(v["id"], v["surah"], v["ayah"]) for v in ar] == [(v["id"], v["surah"], v["ayah"]) for v in ckb], "Sorani verse ids do not align with the Arabic source"
missing_ckb = [v for v in ckb if not str(v.get("text", "")).strip()]
assert len(missing_ckb) <= 20, f"Refusing to ship a Sorani translation with {len(missing_ckb)} missing ayahs"
assert all(bool(v.get("missing")) for v in missing_ckb), "Every missing Sorani translation must be explicitly marked"
raw_ckb = ckb_path.read_bytes()
assert hashlib.sha256(raw_ckb).hexdigest() == ckb_manifest.get("contentSha256"), "Sorani asset checksum differs from its provenance manifest"
assert ckb_manifest.get("source") == "QuranEnc.com official API"
assert ckb_manifest.get("version") and ckb_manifest.get("lastUpdate"), "Sorani version/update metadata is mandatory"
assert ckb_manifest.get("attribution") and ckb_manifest.get("publisher") == "QuranEnc.com", "Sorani publisher attribution is mandatory"
assert int(ckb_manifest.get("canonicalAyahCount", 0)) == 6236
assert int(ckb_manifest.get("translatedAyahCount", -1)) == 6236 - len(missing_ckb)

# Licensed morning/evening adhkar integrity and attribution.
adhkar_path = CONTENT / "adhkar_morning_evening.json"
adhkar_manifest_path = CONTENT / "adhkar_morning_evening_manifest.json"
adhkar_license_path = CONTENT / "adhkar_source_LICENSE.txt"
assert adhkar_path.is_file(), "licensed adhkar dataset was not generated"
assert adhkar_manifest_path.is_file(), "adhkar provenance manifest is missing"
assert adhkar_license_path.is_file(), "upstream MIT license must be shipped with the app"
adhkar = load("adhkar_morning_evening.json")
adhkar_manifest = load("adhkar_morning_evening_manifest.json")
assert len(adhkar) == 34, f"Expected 34 pinned adhkar records, got {len(adhkar)}"
assert [row.get("order") for row in adhkar] == list(range(1, 35)), "Adhkar order must be unique and contiguous"
for row in adhkar:
    order = row["order"]
    assert isinstance(row.get("arabic"), str) and row["arabic"].strip(), f"Missing Arabic dhikr at {order}"
    assert isinstance(row.get("translationEn"), str) and row["translationEn"].strip(), f"Missing English meaning at {order}"
    assert isinstance(row.get("sourceAr"), str) and row["sourceAr"].strip(), f"Missing source reference at {order}"
    assert isinstance(row.get("sourceEn"), str) and row["sourceEn"].strip(), f"Missing English source reference at {order}"
    assert isinstance(row.get("repeatCount"), int) and 1 <= row["repeatCount"] <= 1000, f"Invalid repeat count at {order}"
    assert row.get("type") in (0, 1, 2), f"Invalid morning/evening selector at {order}"
assert adhkar_manifest.get("source") == "Seen-Arabic/Morning-And-Evening-Adhkar-DB"
assert adhkar_manifest.get("sourceRef") == "v1.0.2", "Adhkar upstream version must remain pinned"
assert adhkar_manifest.get("license") == "MIT"
assert adhkar_manifest.get("licenseCopyright") == "Copyright (c) 2024 Seen Arabic"
assert adhkar_manifest.get("recordCount") == 34
assert adhkar_manifest.get("soraniTranslationIncluded") is False, "Do not mislabel English meanings as Sorani"
assert hashlib.sha256(adhkar_path.read_bytes()).hexdigest() == adhkar_manifest.get("contentSha256")
assert hashlib.sha256(adhkar_license_path.read_bytes()).hexdigest() == adhkar_manifest.get("licenseSha256")
assert "MIT License" in adhkar_license_path.read_text(encoding="utf-8")
assert "Copyright (c) 2024 Seen Arabic" in adhkar_license_path.read_text(encoding="utf-8")

# Extended daily adhkar / Dua collection from two MIT-licensed sources.
all_adhkar_path = CONTENT / "adhkar_all.json"
all_manifest_path = CONTENT / "adhkar_all_manifest.json"
all_licenses_path = CONTENT / "adhkar_all_source_LICENSES.txt"
assert all_adhkar_path.is_file(), "combined daily adhkar content was not generated"
assert all_manifest_path.is_file(), "combined daily adhkar manifest is missing"
assert all_licenses_path.is_file(), "full source licenses must be distributed with combined content"
all_adhkar = load("adhkar_all.json")
all_manifest = load("adhkar_all_manifest.json")
all_licenses = all_licenses_path.read_text(encoding="utf-8")
assert len(all_adhkar) == 82, f"Expected 82 source-attributed adhkar/dua records, got {len(all_adhkar)}"
assert [row.get("order") for row in all_adhkar] == list(range(1, 83)), "Combined adhkar ids must be unique and contiguous"
allowed_adhkar_categories = {
    "morning_evening", "morning", "evening", "after_prayer", "sleep", "wake_up",
    "bathroom", "food", "mosque", "wudu", "fasting", "home", "travel", "clothing",
    "weather", "protection", "general"
}
for row in all_adhkar:
    order = row.get("order")
    assert row.get("categoryId") in allowed_adhkar_categories, f"Invalid adhkar category at {order}"
    assert isinstance(row.get("arabic"), str) and row["arabic"].strip(), f"Missing Arabic dhikr at {order}"
    assert isinstance(row.get("translationEn"), str) and row["translationEn"].strip(), f"Missing English meaning at {order}"
    assert isinstance(row.get("sourceEn"), str) and row["sourceEn"].strip(), f"Missing reference at adhkar order {order}"
    assert isinstance(row.get("repeatCount"), int) and 1 <= row["repeatCount"] <= 1000, f"Invalid repeat count at {order}"
    assert row.get("type") in (0, 1, 2, 3), f"Invalid type at adhkar order {order}"
assert all_manifest.get("source") == "Combined licensed adhkar datasets"
assert all_manifest.get("license") == "MIT (both source datasets)"
assert all_manifest.get("recordCount") == len(all_adhkar)
assert all_manifest.get("languageCoverage") == ["Arabic", "English"]
assert all_manifest.get("soraniTranslationIncluded") is False, "Do not mislabel English meanings as Sorani"
assert all_manifest.get("scholarReviewStatus") == "pending", "Independent religious review status must remain explicit"
assert hashlib.sha256(all_adhkar_path.read_bytes()).hexdigest() == all_manifest.get("contentSha256")
assert hashlib.sha256(all_licenses_path.read_bytes()).hexdigest() == all_manifest.get("licenseSha256")
assert "Copyright (c) 2024 Seen Arabic" in all_licenses
assert "Copyright (c) 2023 Fitrahive" in all_licenses
assert all_manifest.get("sourceRef", "").endswith("f42f895f914319a844c3e3c2279483cae060ea19")
source_rows = all_manifest.get("sources", [])
assert len(source_rows) == 2
assert source_rows[0].get("source") == "Seen-Arabic/Morning-And-Evening-Adhkar-DB"
assert source_rows[0].get("license") == "MIT"
assert source_rows[1].get("source") == "fitrahive/dua-dhikr"
assert source_rows[1].get("license") == "MIT"
assert source_rows[1].get("sourceRef") == "f42f895f914319a844c3e3c2279483cae060ea19"
skipped_no_reference = {row.get("title") for row in all_manifest.get("skippedRecords", [])}
assert skipped_no_reference == {"Tasbih", "Tahmid", "Takbir"}, "Do not silently add source-less adhkar"

# Adhan audio must be fetched from Commons only after the publisher's CC0 metadata is checked.
adhan_path = RES / "raw" / "adhan.ogg"
adhan_manifest_path = CONTENT / "adhan_audio_manifest.json"
assert adhan_path.is_file(), "CC0 adhan audio asset was not generated"
assert adhan_manifest_path.is_file(), "Adhan source/license manifest is missing"
adhan_manifest = json.loads(adhan_manifest_path.read_text(encoding="utf-8"))
adhan_bytes = adhan_path.read_bytes()
assert 0 < len(adhan_bytes) <= 2 * 1024 * 1024, "Adhan audio asset is empty or too large"
assert hashlib.sha256(adhan_bytes).hexdigest() == adhan_manifest.get("sha256"), "Adhan asset checksum differs from its manifest"
assert hashlib.sha1(adhan_bytes).hexdigest() == adhan_manifest.get("sha1"), "Adhan asset SHA-1 differs from upstream metadata"
assert adhan_manifest.get("source") == "Wikimedia Commons"
assert adhan_manifest.get("sourceFile") == "File:Beautiful_adhan.ogg"
assert adhan_manifest.get("license") == "CC0 1.0 Universal"
assert "publicdomain/zero/1.0" in adhan_manifest.get("licenseUrl", "")
assert adhan_manifest.get("author") and adhan_manifest.get("byteCount") == len(adhan_bytes)

# The selectable alternate Adhan recording has its own license and checksum manifest.
community_adhan_path = RES / "raw" / "adhan_community.ogg"
community_adhan_manifest_path = CONTENT / "adhan_community_manifest.json"
assert community_adhan_path.is_file(), "alternate CC0 adhan recording was not generated"
assert community_adhan_manifest_path.is_file(), "alternate adhan provenance manifest is missing"
community_adhan_manifest = json.loads(community_adhan_manifest_path.read_text(encoding="utf-8"))
community_adhan_bytes = community_adhan_path.read_bytes()
assert 0 < len(community_adhan_bytes) <= 2 * 1024 * 1024, "Alternate adhan audio is empty or too large"
assert hashlib.sha256(community_adhan_bytes).hexdigest() == community_adhan_manifest.get("sha256"), "Alternate adhan SHA-256 mismatch"
assert hashlib.sha1(community_adhan_bytes).hexdigest() == community_adhan_manifest.get("sha1"), "Alternate adhan SHA-1 mismatch"
assert community_adhan_manifest.get("source") == "Wikimedia Commons"
assert community_adhan_manifest.get("sourceFile") == "File:Muslim_calling_to_prayer.ogg"
assert community_adhan_manifest.get("license") == "CC0 1.0 Universal"
assert "publicdomain/zero/1.0" in community_adhan_manifest.get("licenseUrl", "")
assert community_adhan_manifest.get("author") and community_adhan_manifest.get("byteCount") == len(community_adhan_bytes)

for name, expected, key in [("quran_page_ranges.json", 604, "page"), ("quran_juz_ranges.json", 30, "juz")]:
    ranges = load(name)
    assert len(ranges) == expected
    cursor = 1
    for i, r in enumerate(ranges, 1):
        assert r[key] == i
        assert r["first_global_ayah"] == cursor
        assert r["last_global_ayah"] >= cursor
        cursor = r["last_global_ayah"] + 1
    assert cursor == 6237

# Guard against visible replacement placeholders and broken Quran page routes.
quran_screen = (ROOT / "app/src/main/java/com/masheqal/app/ui/screens/QuranScreen.kt").read_text(encoding="utf-8")
assert "§" not in quran_screen, "Unresolved placeholder marker in QuranScreen.kt"
assert 'nav.navigate("quran/page/$page")' in quran_screen, "Page picker must navigate to the selected Mushaf page"
assert '"${stringResource(R.string.juz)} $juz"' in quran_screen, "Juz picker must render the selected juz number"

# Kotlin delimiter sanity (comments and common string literals removed).
def cleaned(s: str) -> str:
    # Remove quoted strings first so URLs such as https:// do not look like comments.
    s = re.sub(r'"(?:\\.|[^"\\])*"', '""', s)
    s = re.sub(r"//.*", "", s)
    s = re.sub(r"/\*.*?\*/", "", s, flags=re.S)
    # Remove char literals after comments so apostrophes in KDoc are left untouched.
    s = re.sub(r"'(?:\\.|[^'\\])*'", "''", s)
    return s
for file in sorted((ROOT / "app/src/main/java").rglob("*.kt")):
    stack = []
    pairs = {")": "(", "]": "[", "}": "{"}
    for ch in cleaned(file.read_text(encoding="utf-8")):
        if ch in "([{": stack.append(ch)
        elif ch in ")]}":
            assert stack and stack[-1] == pairs[ch], f"delimiter mismatch in {file}"
            stack.pop()
    assert not stack, f"unclosed delimiter in {file}: {stack}"

print(f"Project validation passed: {len(xml_files)} XML resources, {len(base_names)} synchronized UI strings, 6236 aligned Arabic/English/Sorani records, {len(missing_ckb)} explicit Sorani gaps, 604 pages, 30 juz, Kotlin delimiter sanity OK.")
