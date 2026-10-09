#!/usr/bin/env python3
"""Drill every Asterbit gate: clean controls must pass, seeded defects must be caught.

Covers the hooks Cursor runs (.cursor/hooks.json → .cursor/hooks/cursor_adapter.py, drilled in
drill_cursor.py with Cursor's real payload shape, ADR-0008), the structure check and the other
check scripts (drill_checks.py). A gate that never blocks and a gate that cannot block print the
same nothing, so every case is tied to the gate's OWN marker or output, never to a substring a
crash could also produce.

Exit codes: 0 = every gate ARMED, 1 = a gate is DEAD (missed a seeded defect)
or DEFECTIVE (blocked a clean control), 2 = inconclusive (a drill could not run).
Drill the drill: `ASTERBIT_DRILL_STUB=1 python3 sdlc/checks/drill_hooks.py`
replaces the hook script with a do-nothing stub; the result must then be DEAD (exit 1).

Risky-looking strings are assembled at run time so that guards on the machine
running this file do not mistake the drill itself for an attack.
"""
from __future__ import annotations

import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

from drill_checks import drill_engine_checks, tracked_copy  # check-script drills (ADR-0006); sibling module
from drill_cursor import drill_cursor  # hook drills in Cursor's payload shape (ADR-0008); sibling module

ROOT = Path(__file__).resolve().parents[2]
HOOKS = ROOT / ".cursor/hooks"
STUB = os.environ.get("ASTERBIT_DRILL_STUB") == "1"


class Drill:
    def __init__(self) -> None:
        self.results: list[tuple[str, str, str]] = []

    def add(self, gate: str, verdict: str, case: str) -> None:
        self.results.append((gate, verdict, case))

    def expect_bool(self, gate: str, case: str, ok: bool) -> None:
        """For gates that write files or context instead of blocking: ok must come from the gate's own output."""
        self.add(gate, "WORKS" if ok else "MISSED", case)

    def expect_clean(self, gate: str, case: str, ok: bool) -> None:
        """A clean control that fails is a defect in the gate (it blocks good work), not a missed seed."""
        self.add(gate, "PASSED" if ok else "FALSE-BLOCK", case)


def drill_structure_check(drill: Drill, scratch: Path) -> None:
    check = [sys.executable, str(ROOT / "sdlc/checks/check_structure.py")]
    clean = subprocess.run(check + [str(ROOT)], capture_output=True, text=True, timeout=120)
    drill.expect_clean("check_structure", "clean control: this repo", clean.returncode == 0 and "PASS" in clean.stdout)
    copy = tracked_copy(scratch / "structure")
    (copy / "CONTRIBUTING.md").unlink()
    (copy / "docs/unlisted-note.md").write_text("see [note](nowhere-drill.md)\n", encoding="utf-8")
    (copy / "memory/archive/handoffs").mkdir(parents=True, exist_ok=True)
    (copy / "memory/archive/handoffs/2026-01-01-drill.v1.md").write_text("summary quoting [a link](missing-example.md)\n", encoding="utf-8")
    seeded = subprocess.run(check + [str(copy)], capture_output=True, text=True, timeout=120)
    expected = ("required file missing: CONTRIBUTING.md", "does not list docs/unlisted-note.md", "broken link -> nowhere-drill.md")
    drill.expect_bool("check_structure", "3 planted defects all reported; a link inside archived memory is not checked",
                      seeded.returncode == 1 and all(e in seeded.stdout for e in expected) and "missing-example.md" not in seeded.stdout)


def report(drill: Drill) -> int:
    for gate, verdict, case in drill.results:
        print(f"{verdict:12} {gate:18} {case}")
    verdicts = {v for _, v, _ in drill.results}
    print(f"\ncases: {len(drill.results)} · hooks dir: {HOOKS}{' · STUB MODE' if STUB else ''}")
    if verdicts & {"MISSED", "FALSE-BLOCK"}:
        print("RESULT: " + ("DEAD (a seeded defect got through) " if "MISSED" in verdicts else "")
              + ("DEFECTIVE (a clean control was blocked)" if "FALSE-BLOCK" in verdicts else ""))
        return 1
    if "INCONCLUSIVE" in verdicts or not drill.results:
        print("RESULT: INCONCLUSIVE")
        return 2
    print("RESULT: ARMED" + (" (some cases SKIPPED — see above)" if "SKIPPED" in verdicts else ""))
    return 0


def main() -> int:
    if shutil.which("git") is None:
        print("RESULT: INCONCLUSIVE — git is not installed")
        return 2
    drill = Drill()
    with tempfile.TemporaryDirectory(prefix="asterbit-drill-") as tmp:
        drill_cursor(drill, Path(tmp))
        drill_structure_check(drill, Path(tmp))
        drill_engine_checks(drill, Path(tmp))
    return report(drill)


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as error:  # a crashed drill proved nothing: inconclusive, not DEAD
        print(f"RESULT: INCONCLUSIVE — the drill itself crashed ({type(error).__name__}: {error})")
        sys.exit(2)
