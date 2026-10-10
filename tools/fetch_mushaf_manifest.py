#!/usr/bin/env python3
"""Fetch pinned source archives, verify their archive digests, and bundle per-page SHA-256s."""
from __future__ import annotations

import hashlib
import json
import re
import sys
import tempfile
import urllib.request
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSET_PATH = ROOT / "app/src/main/assets/content/mushaf_manifest.json"
VERSION = "v1.1.1"
EDITION = "hafs-kfqc"
PAGE_COUNT = 604
USER_AGENT = "Masheqal-Mushaf-Integrity-Builder/1.0"

ARCHIVES = (
    {
        "kind": "svg",
        "url": "https://github.com/quran-ws/quran-svg/releases/download/v1.1.1/hafs-kfqc-svg.zip",
        "size": 113847685,
        "sha256": "1460746a114a4f797a67ad8aa34e95680b0b025e1c5bad0113a100396764c945",
    },
    {
        "kind": "json",
        "url": "https://github.com/quran-ws/quran-svg/releases/download/v1.1.1/hafs-kfqc-json.zip",
        "size": 492327,
        "sha256": "27dd192cec6b5f919ffd6e7df5216464aa57a671e326fc1a37a9d82dee3f6c13",
    },
)


def fetch_verified_archive(archive: dict, folder: Path) -> Path:
    target = folder / f"hafs-kfqc-{archive['kind']}.zip"
    request = urllib.request.Request(
        archive["url"],
        headers={"Accept": "application/octet-stream", "Accept-Encoding": "identity", "User-Agent": USER_AGENT},
    )
    digest = hashlib.sha256()
    received = 0
    with urllib.request.urlopen(request, timeout=120) as response, target.open("wb") as output:
        if response.status != 200:
            raise RuntimeError(f"Upstream release returned HTTP {response.status}: {archive['url']}")
        while True:
            block = response.read(1024 * 1024)
            if not block:
                break
            received += len(block)
            if received > archive["size"] + 8 * 1024 * 1024:
                raise RuntimeError(
                    f"Upstream archive exceeds the safety cap: kind={archive['kind']}, "
                    f"received={received}, pinned_size={archive['size']}, "
                    f"content_length={response.headers.get('Content-Length')}, url={response.geturl()}"
                )
            digest.update(block)
            output.write(block)

    actual = digest.hexdigest()
    if received != archive["size"] or actual != archive["sha256"]:
        raise RuntimeError(
            f"Upstream {archive['kind']} archive integrity mismatch: "
            f"size={received}, expected_size={archive['size']}, sha256={actual}, "
            f"expected_sha256={archive['sha256']}, url={archive['url']}"
        )
    return target


def resource_entries(archive_path: Path, extension: str) -> dict[str, dict]:
    result: dict[str, dict] = {}
    pattern = re.compile(rf"(^|/)\d{{3}}\.{re.escape(extension)}$")
    with zipfile.ZipFile(archive_path) as archive:
        for info in archive.infolist():
            if info.is_dir() or not pattern.search(info.filename.replace("\\", "/")):
                continue
            basename = info.filename.replace("\\", "/").rsplit("/", 1)[-1]
            page = int(basename[:3])
            if page not in range(1, PAGE_COUNT + 1):
                continue
            raw = archive.read(info)
            if not raw:
                raise RuntimeError(f"Empty source asset in archive: {info.filename}")
            key = f"{EDITION}/{basename}"
            row = {"path": key, "size": len(raw), "sha256": hashlib.sha256(raw).hexdigest()}
            previous = result.get(key)
            if previous is not None and previous != row:
                raise RuntimeError(f"Conflicting duplicate page file: {key}")
            result[key] = row
    expected = {f"{EDITION}/{page:03d}.{extension}" for page in range(1, PAGE_COUNT + 1)}
    missing = sorted(expected - result.keys())
    if missing:
        raise RuntimeError(
            f"Source archive has {len(missing)} missing {extension.upper()} page files "
            f"(examples: {', '.join(missing[:8])})"
        )
    if len(result) != PAGE_COUNT:
        raise RuntimeError(f"Expected {PAGE_COUNT} {extension.upper()} pages, found {len(result)}")
    return result


def main() -> int:
    with tempfile.TemporaryDirectory(prefix="masheqal-mushaf-") as temp:
        folder = Path(temp)
        entries: dict[str, dict] = {}
        for archive in ARCHIVES:
            zip_path = fetch_verified_archive(archive, folder)
            ext = "svg" if archive["kind"] == "svg" else "json"
            part = resource_entries(zip_path, ext)
            entries.update(part)
            print(f"Verified upstream {archive['kind']} archive and {len(part)} per-page SHA-256 values.")

    required_count = PAGE_COUNT * 2
    if len(entries) != required_count:
        raise RuntimeError(f"Expected {required_count} total page assets, found {len(entries)}")
    output = {
        "source": "quran-ws/quran-svg",
        "sourceVersion": VERSION,
        "edition": EDITION,
        "files": [entries[key] for key in sorted(entries)],
    }
    ASSET_PATH.parent.mkdir(parents=True, exist_ok=True)
    ASSET_PATH.write_text(
        json.dumps(output, ensure_ascii=False, separators=(",", ":")) + "\n",
        encoding="utf-8",
    )
    print(f"Wrote bundled runtime manifest: {ASSET_PATH.relative_to(ROOT)} ({len(entries)} entries)")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:
        print(f"Mushaf checksum generation failed: {exc}", file=sys.stderr)
        raise SystemExit(1)
