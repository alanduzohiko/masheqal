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
    # Strip literals before comments so URLs such as https:// do not look like comments.
    s = re.sub(r'"(?:\\.|[^"\\])*"', '""', s)
    s = re.sub(r"'(?:\\.|[^'\\])*'", "''", s)
    s = re.sub(r"//.*", "", s)
    s = re.sub(r"/\*.*?\*/", "", s, flags=re.S)
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
