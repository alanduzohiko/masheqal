#!/usr/bin/env python3
"""Validate the pinned Quran SVG CDN manifest contains integrity metadata for all Hafs pages."""
from __future__ import annotations

import json
import re
import sys
import urllib.request
from pathlib import Path

MANIFEST_URL = "https://cdn.quran.ws/svg/pages/v1.1.1/manifest.json"
EDITION = "hafs-kfqc"
PAGE_COUNT = 604
SHA256 = re.compile(r"^[0-9a-fA-F]{64}$")
PAGE_FILE = re.compile(r"^\d{3}\.(?:svg|json)$")


def canonical_path(raw: str) -> str | None:
    normalized = raw.replace("\\", "/").strip("/")
    marker = f"{EDITION}/"
    index = normalized.rfind(marker)
    suffix = normalized[index + len(marker):] if index >= 0 else normalized
    if PAGE_FILE.fullmatch(suffix):
        return f"{EDITION}/{suffix}"
    return None


def collect(value: object, parent_path: str = "") -> dict[str, tuple[int, str]]:
    entries: dict[str, tuple[int, str]] = {}

    def join(parent: str, child: str) -> str:
        left, right = parent.strip("/"), child.replace("\\", "/").strip("/")
        if not left:
            return right
        if not right:
            return left
        if right.startswith(left + "/"):
            return right
        return left + "/" + right

    def visit(node: object, parent: str) -> None:
        if isinstance(node, list):
            for item in node:
                visit(item, parent)
            return
        if not isinstance(node, dict):
            return

        explicit = next(
            (str(node[key]).strip() for key in ("path", "file", "relativePath")
             if isinstance(node.get(key), str) and str(node[key]).strip()),
            None,
        )
        name = str(node.get("name", "")).strip()
        current = explicit.replace("\\", "/").strip("/") if explicit else (join(parent, name) if name else parent)

        sha = next(
            (str(node[key]).strip().lower() for key in ("sha256", "sha256sum", "sha-256")
             if isinstance(node.get(key), str) and SHA256.fullmatch(str(node[key]).strip())),
            None,
        )
        size_value = next(
            (node[key] for key in ("size", "bytes", "byteSize", "fileSize")
             if str(node.get(key, "")).isdigit()),
            None,
        )
        if sha and size_value is not None:
            path = canonical_path(current) or canonical_path(parent)
            size = int(size_value)
            if path and size > 0:
                previous = entries.get(path)
                candidate = (size, sha)
                if previous is not None and previous != candidate:
                    raise ValueError(f"Manifest has conflicting integrity entries for {path}")
                entries[path] = candidate

        excluded = {"path", "file", "relativePath", "name", "size", "bytes", "byteSize", "fileSize",
                    "sha256", "sha256sum", "sha-256"}
        for key, child in node.items():
            if key not in excluded:
                visit(child, join(current, str(key)))

    visit(value, parent_path)
    return entries


def main() -> int:
    request = urllib.request.Request(
        MANIFEST_URL,
        headers={"Accept": "application/json", "User-Agent": "Masheqal-Mushaf-Manifest-Validator/1.0"},
    )
    with urllib.request.urlopen(request, timeout=45) as response:
        if response.status != 200:
            raise RuntimeError(f"Manifest returned HTTP {response.status}")
        payload = json.load(response)

    entries = collect(payload)
    required = [
        f"{EDITION}/{page:03d}.{extension}"
        for page in range(1, PAGE_COUNT + 1)
        for extension in ("svg", "json")
    ]
    missing = [path for path in required if path not in entries]
    if missing:
        examples = ", ".join(missing[:10])
        raise RuntimeError(
            f"Manifest exposes integrity metadata for {len(entries)} page assets; "
            f"{len(missing)} required entries are missing (examples: {examples})"
        )
    invalid = [path for path in required if entries[path][0] <= 0 or not SHA256.fullmatch(entries[path][1])]
    if invalid:
        raise RuntimeError(f"Manifest contains invalid size/SHA-256 values (examples: {', '.join(invalid[:10])})")

    print(
        f"Verified pinned Quran SVG manifest metadata for {PAGE_COUNT} pages: "
        f"{PAGE_COUNT} SVG and {PAGE_COUNT} ayah JSON assets."
    )
    print("This validates published digests and sizes, not artistic/religious review or phone-render QA.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:
        print(f"Quran SVG manifest validation failed: {exc}", file=sys.stderr)
        raise SystemExit(1)
