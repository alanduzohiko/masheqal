#!/usr/bin/env python3
"""Inspect the licensed HQKSTD source workbook without committing it to the app repository.

Source: Mendeley Data, DOI 10.17632/byyjd7kmvd.1, Version 1, CC BY 4.0.
This first mode intentionally prints sheet names, row counts and column headers only.
The actual asset generator will be added after the workbook schema is verified.
"""
from __future__ import annotations

import argparse
import json
import os
import tempfile
import time
import urllib.request
import zipfile
from pathlib import Path

DATASET_URL = (
    "https://api.data.mendeley.com/datasets/byyjd7kmvd/zip/file_downloaded?version=1"
)
MAX_DOWNLOAD_BYTES = 40 * 1024 * 1024


def download_dataset(destination: Path) -> None:
    request = urllib.request.Request(
        DATASET_URL,
        headers={"User-Agent": "Masheqal-Android/0.1 (licensed research dataset import)"},
    )
    error: Exception | None = None
    for attempt in range(3):
        try:
            with urllib.request.urlopen(request, timeout=45) as response:
                if response.status < 200 or response.status >= 300:
                    raise RuntimeError(f"Dataset endpoint returned HTTP {response.status}")
                length = response.headers.get("Content-Length")
                if length and int(length) > MAX_DOWNLOAD_BYTES:
                    raise RuntimeError("Dataset archive exceeds the allowed 40 MiB size")
                total = 0
                with destination.open("wb") as output:
                    while chunk := response.read(64 * 1024):
                        total += len(chunk)
                        if total > MAX_DOWNLOAD_BYTES:
                            raise RuntimeError("Dataset archive exceeded the 40 MiB size limit")
                        output.write(chunk)
            if destination.stat().st_size == 0:
                raise RuntimeError("Dataset download was empty")
            return
        except Exception as exc:  # retry transient Mendeley/CDN failures only a few times
            error = exc
            time.sleep(2 ** attempt)
    raise RuntimeError(f"Could not retrieve the licensed Mendeley dataset: {error}") from error


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--inspect-only", action="store_true")
    parser.add_argument("--output-dir", default="build/downloads/hqkstd")
    args = parser.parse_args()

    try:
        import openpyxl
    except ImportError as exc:
        raise SystemExit("openpyxl is required; install the pinned CI dependency first") from exc

    target = Path(args.output_dir)
    target.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="masheqal-hqkstd-") as tmp:
        archive = Path(tmp) / "hqkstd.zip"
        download_dataset(archive)
        workbook_files: list[Path] = []
        with zipfile.ZipFile(archive) as zf:
            for info in zf.infolist():
                if info.is_dir():
                    continue
                name = Path(info.filename).name
                suffix = Path(name).suffix.lower()
                if suffix not in {".xlsx", ".xlsm"}:
                    continue
                # Use a fixed safe filename to prevent archive path traversal.
                output = Path(tmp) / name
                with zf.open(info) as source, output.open("wb") as dest:
                    dest.write(source.read(MAX_DOWNLOAD_BYTES + 1))
                if output.stat().st_size > MAX_DOWNLOAD_BYTES:
                    raise RuntimeError("Workbook is unexpectedly large")
                workbook_files.append(output)

        if not workbook_files:
            raise RuntimeError("Mendeley archive contained no XLSX workbook")

        report = []
        for file in workbook_files:
            wb = openpyxl.load_workbook(file, read_only=True, data_only=True)
            sheets = []
            for ws in wb.worksheets:
                rows = ws.iter_rows(min_row=1, max_row=3, values_only=True)
                sample = list(rows)
                headers = [str(v).strip() if v is not None else "" for v in (sample[0] if sample else ())]
                nonempty_headers = [h for h in headers if h]
                sheets.append({
                    "sheet": ws.title,
                    "rows": ws.max_row,
                    "columns": ws.max_column,
                    "headers": nonempty_headers,
                    "sample_row_widths": [len(row) for row in sample],
                    "nonempty_header_count": len(nonempty_headers),
                })
            wb.close()
            report.append({
                "workbook": file.name,
                "bytes": file.stat().st_size,
                "sheets": sheets,
            })
        print(json.dumps({
            "source": "HQKSTD Mendeley Data V1",
            "doi": "10.17632/byyjd7kmvd.1",
            "license": "CC BY 4.0",
            "inspection_mode": bool(args.inspect_only),
            "workbooks": report,
        }, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
