#!/usr/bin/env python3
"""Fetch the Commons Adhan.ogg only after checking its public-domain license metadata."""
from __future__ import annotations

import hashlib
import html
import json
from pathlib import Path
import urllib.parse
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
TARGET = ROOT / "app/src/main/res/raw/adhan.ogg"
MANIFEST = ROOT / "app/src/main/assets/content/adhan_audio_manifest.json"
FILE_PAGE = "https://commons.wikimedia.org/wiki/File:Adhan.ogg"
API = "https://commons.wikimedia.org/w/api.php"
MAX_BYTES = 2 * 1024 * 1024
USER_AGENT = "MasheqalAndroidBuild/1.0 (https://github.com/alanduzohiko/masheqal)"


def request_json(url: str) -> dict:
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT, "Accept": "application/json"})
    with urllib.request.urlopen(req, timeout=30) as response:
        return json.loads(response.read().decode("utf-8"))


def main() -> None:
    params = urllib.parse.urlencode({
        "action": "query",
        "titles": "File:Adhan.ogg",
        "prop": "imageinfo",
        "iiprop": "url|sha1|size|mime|extmetadata",
        "format": "json",
        "formatversion": "2",
    })
    payload = request_json(API + "?" + params)
    pages = payload.get("query", {}).get("pages", [])
    if not pages or "imageinfo" not in pages[0]:
        raise RuntimeError("Wikimedia Commons did not return source metadata for File:Adhan.ogg")
    info = pages[0]["imageinfo"][0]
    metadata = info.get("extmetadata", {})
    license_name = html.unescape(metadata.get("LicenseShortName", {}).get("value", "")).strip()
    license_url = html.unescape(metadata.get("LicenseUrl", {}).get("value", "")).strip()
    author = html.unescape(metadata.get("Artist", {}).get("value", "")).strip()
    if license_name not in {"CC0", "CC0 1.0", "CC0 1.0 Universal"}:
        raise RuntimeError(f"Refusing to bundle adhan audio under unapproved license: {license_name!r}")
    if "creativecommons.org/publicdomain/zero/1.0" not in license_url:
        raise RuntimeError(f"Unexpected CC0 license URL: {license_url!r}")
    if int(info.get("size", 0)) <= 0 or int(info["size"]) > MAX_BYTES:
        raise RuntimeError("The adhan audio size is missing or exceeds the 2 MiB safety limit")
    source_url = info.get("url", "")
    if not source_url.startswith("https://upload.wikimedia.org/wikipedia/commons/"):
        raise RuntimeError("Unexpected Wikimedia media host; refusing to download")
    req = urllib.request.Request(source_url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(req, timeout=45) as response:
        audio = response.read(MAX_BYTES + 1)
    if len(audio) != int(info["size"]) or len(audio) > MAX_BYTES:
        raise RuntimeError(f"Adhan audio size mismatch: expected {info['size']}, received {len(audio)}")
    sha1 = hashlib.sha1(audio).hexdigest()
    if sha1.lower() != str(info.get("sha1", "")).lower():
        raise RuntimeError("Downloaded adhan audio failed the upstream SHA-1 integrity check")
    sha256 = hashlib.sha256(audio).hexdigest()
    TARGET.parent.mkdir(parents=True, exist_ok=True)
    MANIFEST.parent.mkdir(parents=True, exist_ok=True)
    TARGET.write_bytes(audio)
    manifest = {
        "source": "Wikimedia Commons",
        "sourceFile": "File:Adhan.ogg",
        "sourceUrl": FILE_PAGE,
        "mediaUrl": source_url,
        "author": author,
        "license": "CC0 1.0 Universal",
        "licenseUrl": license_url,
        "mediaType": info.get("mime", ""),
        "byteCount": len(audio),
        "sha1": sha1,
        "sha256": sha256,
        "usage": "Bundled offline prayer-call playback",
    }
    MANIFEST.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({
        "source": FILE_PAGE,
        "license": manifest["license"],
        "bytes": len(audio),
        "sha256": sha256,
        "asset": str(TARGET.relative_to(ROOT)),
    }, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
