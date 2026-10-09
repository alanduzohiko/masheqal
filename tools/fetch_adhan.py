#!/usr/bin/env python3
"""Fetch and validate two CC0 adhan recordings from Wikimedia Commons."""
from __future__ import annotations

import hashlib
import html
import json
from pathlib import Path
import urllib.parse
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
API = "https://commons.wikimedia.org/w/api.php"
MAX_BYTES = 2 * 1024 * 1024
USER_AGENT = "MasheqalAndroidBuild/1.0 (https://github.com/alanduzohiko/masheqal)"

VOICES = (
    {
        "id": "beautiful_adhan",
        "title": "File:Beautiful_adhan.ogg",
        "file_page": "https://commons.wikimedia.org/wiki/File:Beautiful_adhan.ogg",
        "target": ROOT / "app/src/main/res/raw/adhan.ogg",
        "manifest": ROOT / "app/src/main/assets/content/adhan_audio_manifest.json",
        "usage": "Default offline prayer-call recording",
    },
    {
        "id": "community_adhan",
        "title": "File:Muslim_calling_to_prayer.ogg",
        "file_page": "https://commons.wikimedia.org/wiki/File:Muslim_calling_to_prayer.ogg",
        "target": ROOT / "app/src/main/res/raw/adhan_community.ogg",
        "manifest": ROOT / "app/src/main/assets/content/adhan_community_manifest.json",
        "usage": "Selectable alternate offline prayer-call recording",
    },
)


def request_json(url: str) -> dict:
    req = urllib.request.Request(
        url,
        headers={"User-Agent": USER_AGENT, "Accept": "application/json"},
    )
    with urllib.request.urlopen(req, timeout=30) as response:
        return json.loads(response.read().decode("utf-8"))


def fetch_voice(voice: dict) -> dict:
    params = urllib.parse.urlencode({
        "action": "query",
        "titles": voice["title"],
        "prop": "imageinfo",
        "iiprop": "url|sha1|size|mime|extmetadata",
        "format": "json",
        "formatversion": "2",
    })
    payload = request_json(API + "?" + params)
    pages = payload.get("query", {}).get("pages", [])
    if not pages or "imageinfo" not in pages[0]:
        raise RuntimeError(f"Wikimedia Commons did not return source metadata for {voice['title']}")
    info = pages[0]["imageinfo"][0]
    metadata = info.get("extmetadata", {})
    license_name = html.unescape(metadata.get("LicenseShortName", {}).get("value", "")).strip()
    license_url = html.unescape(metadata.get("LicenseUrl", {}).get("value", "")).strip()
    author = html.unescape(metadata.get("Artist", {}).get("value", "")).strip()

    if license_name not in {"CC0", "CC0 1.0", "CC0 1.0 Universal"}:
        raise RuntimeError(f"Refusing to bundle {voice['title']} under unapproved license: {license_name!r}")
    if "creativecommons.org/publicdomain/zero/1.0" not in license_url:
        raise RuntimeError(f"Unexpected CC0 license URL for {voice['title']}: {license_url!r}")

    expected_size = int(info.get("size", 0))
    if expected_size <= 0 or expected_size > MAX_BYTES:
        raise RuntimeError(f"{voice['title']} size is missing or exceeds the 2 MiB safety limit")
    if not str(info.get("mime", "")).startswith("audio/"):
        raise RuntimeError(f"Unexpected non-audio MIME type for {voice['title']}: {info.get('mime')!r}")
    source_url = info.get("url", "")
    if not source_url.startswith("https://upload.wikimedia.org/wikipedia/commons/"):
        raise RuntimeError("Unexpected Wikimedia media host; refusing to download")

    req = urllib.request.Request(source_url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(req, timeout=45) as response:
        audio = response.read(MAX_BYTES + 1)
    if len(audio) != expected_size or len(audio) > MAX_BYTES:
        raise RuntimeError(
            f"{voice['title']} size mismatch: expected {expected_size}, received {len(audio)}"
        )

    sha1 = hashlib.sha1(audio).hexdigest()
    if sha1.lower() != str(info.get("sha1", "")).lower():
        raise RuntimeError(f"Downloaded {voice['title']} failed the upstream SHA-1 integrity check")
    sha256 = hashlib.sha256(audio).hexdigest()
    voice["target"].parent.mkdir(parents=True, exist_ok=True)
    voice["manifest"].parent.mkdir(parents=True, exist_ok=True)
    voice["target"].write_bytes(audio)

    manifest = {
        "id": voice["id"],
        "source": "Wikimedia Commons",
        "sourceFile": voice["title"],
        "sourceUrl": voice["file_page"],
        "mediaUrl": source_url,
        "author": author,
        "license": "CC0 1.0 Universal",
        "licenseUrl": license_url,
        "mediaType": info.get("mime", ""),
        "byteCount": len(audio),
        "sha1": sha1,
        "sha256": sha256,
        "usage": voice["usage"],
    }
    voice["manifest"].write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    return {
        "id": voice["id"],
        "source": voice["file_page"],
        "author": author,
        "license": manifest["license"],
        "bytes": len(audio),
        "sha256": sha256,
        "asset": str(voice["target"].relative_to(ROOT)),
    }


def main() -> None:
    results = [fetch_voice(voice) for voice in VOICES]
    print(json.dumps({"verifiedAdhanVoices": results}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
