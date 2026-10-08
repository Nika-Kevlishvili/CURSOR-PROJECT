#!/usr/bin/env python3
"""Memory hygiene: what to archive, consolidate or revisit (ADR-0004, ADR-0006).

Advisory — run by /sdlc-wrap and /sdlc-status, not in CI: an old handoff is a
chore, not a broken engine. It never moves or deletes anything; it lists actions.

    python3 sdlc/checks/memory_hygiene.py [repo-root] [--today YYYY-MM-DD]

Exit codes: 0 = nothing due, 3 = actions due (listed), 2 = inconclusive.
"""
from __future__ import annotations

import argparse
import datetime as dt
import re
import subprocess
import sys
from pathlib import Path

HANDOFF_MAX_DAYS = 7     # a live handoff older than this was never wrapped
CONSOLIDATE_EVERY = 10   # ADR-0004: consolidate every 10 session summaries
QMD_TRIGGER = 150        # ADR-0004: 150+ archived files → propose the QMD ADR
DATE = re.compile(r"\b(\d{4}-\d{2}-\d{2})")
DEBT = re.compile("asterbit" + r"-debt:([^\n]*)")  # split so this line is not a marker itself


def field(text: str, name: str) -> str:
    match = re.search(rf"^{name}:\s*(.*)$", text, re.M)
    return match.group(1).split("  #")[0].strip() if match else ""


def stale_handoffs(root: Path, today: dt.date) -> list[str]:
    actions = []
    for path in sorted((root / "memory/episodic/handoffs").glob("*.md")):
        stamp = DATE.search(field(path.read_text(encoding="utf-8"), "updated") or path.name)
        if not stamp:
            actions.append(f"handoff with no date in 'updated:' or its name — check by hand: {path.relative_to(root)}")
        elif (today - dt.date.fromisoformat(stamp.group(1))).days > HANDOFF_MAX_DAYS:
            actions.append(f"archive (/sdlc-wrap step 7): {path.relative_to(root)} — last updated {stamp.group(1)}")
    return actions


def consolidation(root: Path) -> list[str]:
    live = list((root / "memory/episodic/sessions").glob("*.md"))
    archived = [p for p in (root / "memory/archive").rglob("*") if p.is_file() and p.name != ".gitkeep"]
    actions = []
    if len(live) >= CONSOLIDATE_EVERY:
        actions.append(f"consolidate: {len(live)} live session summaries → monthly digest in memory/semantic/digests/, originals to memory/archive/sessions/ (ADR-0004)")
    if len(archived) >= QMD_TRIGGER:
        actions.append(f"QMD trigger reached: {len(archived)} archived files (ADR-0004) — propose the search ADR to the owner")
    return actions


def adr_reviews(root: Path, today: dt.date) -> list[str]:
    actions = []
    for path in sorted((root / "docs/decisions").glob("[0-9][0-9][0-9][0-9]-*.md")):
        text = path.read_text(encoding="utf-8")
        due = DATE.match(field(text, "review_by"))
        if due and field(text, "status") in ("proposed", "accepted") and dt.date.fromisoformat(due.group(1)) < today:
            actions.append(f"ADR review due: {path.name} (review_by {due.group(1)})")
    return actions


def debt_markers(root: Path, files: list[str]) -> list[str]:
    actions = []
    for rel in (f for f in files if not f.endswith(".md") and (root / f).is_file()):
        try:
            lines = (root / rel).read_text(encoding="utf-8").splitlines()
        except UnicodeDecodeError:
            continue
        for number, line in enumerate(lines, start=1):
            marker = DEBT.search(line)
            if marker and "revisit:" not in marker.group(1):
                actions.append(f"debt marker without a revisit trigger: {rel}:{number}")
    return actions


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description="Memory hygiene report")
    parser.add_argument("root", nargs="?", type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument("--today", type=dt.date.fromisoformat, default=dt.date.today())
    args = parser.parse_args(argv)
    root = args.root.resolve()
    try:
        out = subprocess.run(["git", "-C", str(root), "ls-files", "--cached", "--others", "--exclude-standard"],
                             capture_output=True, text=True, check=True).stdout
        actions = (stale_handoffs(root, args.today) + consolidation(root) + adr_reviews(root, args.today)
                   + debt_markers(root, out.splitlines()))
    except (OSError, ValueError, subprocess.CalledProcessError) as error:
        print(f"MEMORY-HYGIENE: INCONCLUSIVE — {type(error).__name__}: {error}")
        return 2
    print(f"MEMORY-HYGIENE: {root} on {args.today}; actions due: {len(actions)}")
    for action in actions:
        print(f"  - {action}")
    return 3 if actions else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
