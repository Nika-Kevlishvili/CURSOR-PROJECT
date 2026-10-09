#!/usr/bin/env python3
"""Lesson graduation: which corrections should become a rule or a check (ADR-0006, P4).

Reads the table in tasks/lessons.md. Two kinds of proposal, never applied
automatically — the owner approves every one:
- promote: lessons about the same thing on 2+ different dates whose rule is not
  yet in AGENTS.md ("corrected twice → the rule goes into AGENTS.md");
- build a check: a lesson classified `mechanical` with no machine check guarding it
  (a mechanical failure should become a check, not prose).
Similar lessons are grouped by word overlap after paths, numbers and short words
are stripped (ideas from GSD's graduation workflow and AIWorkHub's skill miner).

    python3 sdlc/checks/lessons_graduate.py [path-to-lessons.md]

Exit codes: 0 = nothing to propose, 3 = proposals listed, 2 = inconclusive.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

SIMILAR = 0.34
STOP = {"that", "this", "with", "from", "were", "when", "then", "than", "into", "only", "every", "must",
        "should", "before", "after", "about", "their", "there", "which", "what", "keep", "file", "files"}
NO_CHECK = re.compile(r"^\s*(?:$|—|-|\?|none\b|no\b|n/?a\b|todo\b|tbd\b)|\bno (?:general )?check\b|\bnot yet\b", re.I)


def words(text: str) -> set[str]:
    text = re.sub(r"`[^`]*`|\S*[/\\.]\S*|\d+", " ", text.lower())
    return {w for w in re.findall(r"[a-z]{4,}", text) if w not in STOP}


def parse(path: Path) -> list[dict[str, str]]:
    rows = [line for line in path.read_text(encoding="utf-8").splitlines() if line.startswith("|")]
    cells = [[c.strip() for c in row.strip("|").split("|")] for row in rows]
    header_at = next(i for i, c in enumerate(cells) if c and c[0].lower() == "date")
    header = [h.lower() for h in cells[header_at]]
    body = [c for c in cells[header_at + 1:] if not set("".join(c)) <= set("-: ")]
    return [dict(zip(header, c)) for c in body]


def column(row: dict[str, str], prefix: str) -> str:
    return next((v for k, v in row.items() if k.startswith(prefix)), "")


def clusters(lessons: list[dict[str, str]]) -> list[list[dict[str, str]]]:
    groups: list[tuple[set[str], list[dict[str, str]]]] = []
    for lesson in lessons:
        terms = words(column(lesson, "what went wrong") + " " + column(lesson, "rule"))
        for leader, members in groups:
            if terms and len(terms & leader) / len(terms | leader) >= SIMILAR:
                members.append(lesson)
                break
        else:
            groups.append((terms, [lesson]))
    return [members for _, members in groups]


def proposals(lessons: list[dict[str, str]]) -> list[str]:
    found = []
    for group in clusters(lessons):
        dates = sorted({column(l, "date") for l in group})
        promoted = any(column(l, "in agents").lower().startswith("yes") for l in group)
        if len(dates) >= 2 and not promoted:
            found.append(f"promote to AGENTS.md (repeated on {', '.join(dates)}): {column(group[-1], 'rule')}")
    for lesson in lessons:
        if column(lesson, "kind").lower() == "mechanical" and NO_CHECK.search(column(lesson, "check")):
            found.append(f"build a check ({column(lesson, 'date')}, mechanical, no check yet): {column(lesson, 'rule')}")
    return found


def main(argv: list[str]) -> int:
    path = Path(argv[0]) if argv else Path(__file__).resolve().parents[2] / "tasks/lessons.md"
    try:
        lessons = parse(path)
    except (OSError, StopIteration) as error:
        print(f"LESSONS: INCONCLUSIVE — cannot read the lessons table in {path} ({type(error).__name__})")
        return 2
    found = proposals(lessons)
    print(f"LESSONS: {len(lessons)} lessons in {path.name}; proposals for the owner: {len(found)}")
    for item in found:
        print(f"  - {item}")
    return 3 if found else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
