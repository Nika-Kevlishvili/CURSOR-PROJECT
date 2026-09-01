#!/usr/bin/env python3
"""Mark existing 10k batch JSON files as skipped and rebuild 7000-request files.

Does not delete the original 10k files. Renames them to USED-10k-* so the
Playwright regex (billing-by-scales-batch-dev2-part-NNN-of-MMM.json) ignores them.

Dev2 Postgres was unreachable on 2026-09-01. Identifier skip set is therefore:
- original three SKIP PODs from generate_batch_jsons.py
- identifiers dumped from process_management.billing_by_scales_batch_request (2026-08-22)
- every identifier in original parts 001, 020, 021, 022 (probe / limit-test sources)
"""
from __future__ import annotations

import json
import re
import time
from pathlib import Path

OUT_DIR = Path(__file__).resolve().parent.parent / "500k billing-by-scales"
ROWS_PER_FILE = 7000
USED_PREFIX = "USED-10k-"
ACTIVE_RE = re.compile(r"^billing-by-scales-batch-dev2-part-(\d+)-of-(\d+)\.json$")
USED_RE = re.compile(rf"^{re.escape(USED_PREFIX)}billing-by-scales-batch-dev2-part-(\d+)-of-(\d+)\.json$")
SKIP_SOURCE_PARTS = {1, 20, 21, 22}
ORIGINAL_SKIP = {
    "G2M000000165J9K26TYA0L9PXQ2OXBGDQ",
    "G2M000000204GZ93GQCRC5FYOMC97L4CU",
    "G2M0493944LI0SGN32C8LBKV8KUF4Z86I",
}
ID_DUMPS = [
    Path(
        "/Users/lukachrikishvili/.cursor/projects/Users-lukachrikishvili-Desktop-CURSOR-PROJECT/agent-tools/a9998c5d-1837-40cb-8cbf-19263371c26c.txt"
    ),
    Path(
        "/Users/lukachrikishvili/.cursor/projects/Users-lukachrikishvili-Desktop-CURSOR-PROJECT/agent-tools/b60f6d57-0088-45c1-93a6-75e157e94c16.txt"
    ),
    Path(
        "/Users/lukachrikishvili/.cursor/projects/Users-lukachrikishvili-Desktop-CURSOR-PROJECT/agent-tools/d591af1a-89b5-4a44-bb20-e170dc7875b9.txt"
    ),
    Path(
        "/Users/lukachrikishvili/.cursor/projects/Users-lukachrikishvili-Desktop-CURSOR-PROJECT/agent-tools/dc557d8a-d99a-4abd-9821-9477fcdcb097.txt"
    ),
]


def load_dump_identifiers() -> set[str]:
    found: set[str] = set(ORIGINAL_SKIP)
    for path in ID_DUMPS:
        if not path.exists():
            continue
        rows = json.loads(path.read_text())
        for row in rows:
            if isinstance(row, dict) and row.get("identifier"):
                found.add(str(row["identifier"]))
    return found


def rename_active_10k() -> list[Path]:
    used_files: list[Path] = []
    for path in sorted(OUT_DIR.iterdir()):
        used_match = USED_RE.match(path.name)
        if used_match:
            used_files.append(path)
            continue
        active_match = ACTIVE_RE.match(path.name)
        if not active_match:
            continue
        dest = OUT_DIR / f"{USED_PREFIX}{path.name}"
        if dest.exists():
            raise SystemExit(f"refusing to overwrite {dest}")
        path.rename(dest)
        print(f"MARKED {path.name} -> {dest.name}", flush=True)
        used_files.append(dest)
    used_files.sort(key=lambda p: int(USED_RE.match(p.name).group(1)))
    return used_files


def collect_part_identifiers(path: Path) -> set[str]:
    data = json.loads(path.read_text())
    return {str(row.get("identifier") or "") for row in (data.get("requests") or []) if row.get("identifier")}


def write_chunk(part: int, of_placeholder: str, requests: list[dict]) -> Path:
    dest = OUT_DIR / f"billing-by-scales-batch-dev2-part-{part:03d}-of-{of_placeholder}.json"
    dest.write_text(json.dumps({"requests": requests}, separators=(",", ":")), encoding="utf-8")
    return dest


def main() -> None:
    started = time.time()
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    skip = load_dump_identifiers()
    print(f"skip_from_dumps={len(skip)}", flush=True)

    used_files = rename_active_10k()
    if not used_files:
        raise SystemExit(f"no USED-10k source files in {OUT_DIR}")

    for path in used_files:
        part = int(USED_RE.match(path.name).group(1))
        if part not in SKIP_SOURCE_PARTS:
            continue
        before = len(skip)
        skip |= collect_part_identifiers(path)
        print(f"skip_source_part={part:03d} added={len(skip) - before} skip_total={len(skip)}", flush=True)

    skip_ids_for_file = set(skip)

    chunk: list[dict] = []
    kept = 0
    skipped = 0
    written_paths: list[Path] = []
    of_placeholder = "XXX"

    def flush_chunk() -> None:
        nonlocal chunk
        if not chunk:
            return
        part = len(written_paths) + 1
        dest = write_chunk(part, of_placeholder, chunk)
        print(
            f"WROTE {dest.name} requests={len(chunk)} size_mb={dest.stat().st_size / (1024 * 1024):.1f}",
            flush=True,
        )
        written_paths.append(dest)
        chunk = []

    for path in used_files:
        data = json.loads(path.read_text())
        for row in data.get("requests") or []:
            ident = str(row.get("identifier") or "")
            if not ident or ident in skip:
                skipped += 1
                continue
            skip.add(ident)
            chunk.append(row)
            kept += 1
            if len(chunk) >= ROWS_PER_FILE:
                flush_chunk()
        print(f"READ {path.name} kept={kept} skipped={skipped}", flush=True)
        del data
    flush_chunk()

    file_count = len(written_paths)
    print(f"REBUILD requests={kept} files={file_count} rows_per_file={ROWS_PER_FILE}", flush=True)
    for index, path in enumerate(written_paths, start=1):
        renamed = OUT_DIR / f"billing-by-scales-batch-dev2-part-{index:03d}-of-{file_count:03d}.json"
        if path != renamed:
            path.rename(renamed)
            print(f"RENAME {path.name} -> {renamed.name}", flush=True)

    marker = OUT_DIR / "USED-10k.SKIP.txt"
    marker.write_text(
        "These USED-10k-*.json files are the original 10_000-request payloads.\n"
        "They are kept on purpose and must not be POSTed. Playwright only loads\n"
        "billing-by-scales-batch-dev2-part-NNN-of-MMM.json (7000 requests each).\n"
        f"Rebuilt {file_count} active files with {kept} remaining requests on "
        f"{time.strftime('%Y-%m-%d %H:%M:%S %z')}.\n"
        "Skipped identifiers: original three G2M exclusions, 2026-08-22 batch-table dumps,\n"
        "and every identifier from original parts 001, 020, 021, 022 (already sent in probes).\n",
        encoding="utf-8",
    )
    skip_list = OUT_DIR / "skipped-identifiers.txt"
    skip_list.write_text("\n".join(sorted(skip_ids_for_file)) + "\n", encoding="utf-8")
    print(
        f"DONE kept={kept} skipped_payloads={skipped} skip_ids={len(skip_ids_for_file)} "
        f"files={file_count} elapsed_s={time.time() - started:.1f}",
        flush=True,
    )


if __name__ == "__main__":
    main()
