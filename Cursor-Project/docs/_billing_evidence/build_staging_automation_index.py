# -*- coding: utf-8 -*-
"""Index asserting Playwright tests from EnergoTS working tree (staging checkout)."""
from __future__ import annotations

import json
import re
from pathlib import Path

ENERGOTS = Path(__file__).resolve().parents[2] / "EnergoTS"
OUT = Path(__file__).resolve().parent / "automation_index_staging.json"

PREP = re.compile(r"(?i)(data\s*preparation|clean\s*up|run\s*billings|^helper)")
EXCLUDE_PATH = re.compile(r"(?i)goodsOrder|serviceOrder|goods.?order|service.?order")
TEST_LINE = re.compile(r"""(?:test|it)(?:\.(?:only|skip))?\(\s*(['"`])(.+?)\1""")
ID_RE = re.compile(r"(REG|PDT|PHN)-(\d+)", re.I)
TC_RE = re.compile(r"TC-BE-\d+", re.I)


def main() -> None:
    # Prefer staging layout; also accept tests/billing if present
    roots = [
        ENERGOTS / "src" / "tests" / "billing",
        ENERGOTS / "tests" / "billing",
    ]
    specs: list[Path] = []
    for root in roots:
        if not root.is_dir():
            continue
        specs.extend(sorted(root.rglob("*.spec.ts")))

    specs = [p for p in specs if not EXCLUDE_PATH.search(str(p).replace("\\", "/"))]
    # de-dupe by relative path preference: src/tests first
    seen = set()
    uniq: list[Path] = []
    for p in specs:
        rel = p.relative_to(ENERGOTS).as_posix()
        if rel in seen:
            continue
        seen.add(rel)
        uniq.append(p)
    specs = uniq

    rows: list[dict] = []
    for p in specs:
        rel = p.relative_to(ENERGOTS).as_posix()
        text = p.read_text(encoding="utf-8", errors="replace")
        for i, line in enumerate(text.splitlines(), 1):
            m = TEST_LINE.search(line)
            if not m:
                continue
            title = m.group(2)
            if PREP.search(title):
                continue
            norm = [f"{a.upper()}-{b}" for a, b in ID_RE.findall(title)]
            tc = TC_RE.search(title)
            all_ids = list(dict.fromkeys(norm + ([tc.group(0).upper()] if tc else [])))
            aid = norm[0] if norm else (tc.group(0).upper() if tc else p.stem)
            rows.append(
                {
                    "AutomationId": aid,
                    "AllIds": ",".join(all_ids),
                    "AutomationName": title[:500],
                    "AutomationFile": rel,
                    "Line": i,
                    "SourceBranch": "staging (working tree)",
                }
            )

    OUT.write_text(json.dumps(rows, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"EnergoTS={ENERGOTS}")
    print(f"specs={len(specs)} asserting={len(rows)} unique_ids={len({r['AutomationId'] for r in rows})}")
    for p in sorted({r["AutomationFile"] for r in rows}):
        print(" ", p)


if __name__ == "__main__":
    main()
