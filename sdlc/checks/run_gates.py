#!/usr/bin/env python3
"""Product gate runner (ADR-0006, P5): every gate is PASS, FAIL or INCONCLUSIVE — never a guess.

Gates live in docs/gates.json, chosen with the owner in /sdlc-evaluate (design mode):

    [{"id": "G1-unit", "layer": "G1", "command": ["npm", "test"],
      "count_pattern": "(\\d+) passed", "min_count": 1, "timeout": 600}]

`command` is a list (run without a shell). With `count_pattern`, a gate is green only
if the first group is a number ≥ `min_count` (default 1): "0 tests ran" is not a pass.

    python3 sdlc/checks/run_gates.py [--gates PATH] [--layer G1]

Exit codes: 0 = every gate PASS · 1 = a gate FAILED · 2 = inconclusive (a gate could not
run or proved nothing, or no gates are defined) and nothing failed.
"""
from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def load(path: Path) -> list[dict]:
    gates = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(gates, list):
        raise ValueError("docs/gates.json must be a list of gates")
    for gate in gates:
        command = gate.get("command")
        if not isinstance(gate.get("id"), str) or not isinstance(command, list) or not command \
                or not all(isinstance(part, str) for part in command):
            raise ValueError(f"gate needs a string 'id' and a non-empty list 'command': {gate}")
    return gates


def gate_settings(gate: dict) -> tuple[int, int, re.Pattern[str] | None]:
    """(timeout, min_count, count pattern) — a bad value raises, and the gate is then INCONCLUSIVE, not FAIL."""
    pattern = re.compile(gate["count_pattern"]) if gate.get("count_pattern") else None
    if pattern is not None and pattern.groups < 1:
        raise ValueError("count_pattern needs a group around the number, e.g. (\\d+) passed")
    return int(gate.get("timeout", 600)), int(gate.get("min_count", 1)), pattern


def run_gate(gate: dict) -> tuple[str, str]:
    try:
        timeout, minimum, pattern = gate_settings(gate)
    except (re.error, ValueError, TypeError) as error:
        return "INCONCLUSIVE", f"bad gate config — {error}"
    try:
        proc = subprocess.run(gate["command"], cwd=ROOT, capture_output=True, text=True, timeout=timeout)
    except FileNotFoundError:
        return "INCONCLUSIVE", f"command not found: {gate['command'][0]}"
    except subprocess.TimeoutExpired:
        return "INCONCLUSIVE", f"timed out after {timeout} s"
    except OSError as error:
        return "INCONCLUSIVE", f"command could not run — {error}"
    if proc.returncode != 0:
        return "FAIL", f"exit {proc.returncode}"
    if pattern is None:
        return "PASS", "exit 0"
    match = pattern.search(proc.stdout + proc.stderr)
    found = (match.group(1) or "") if match else ""
    counted = int(found) if found.isdigit() else None
    if counted is None:
        return "INCONCLUSIVE", "exit 0 but the count pattern was not found — nothing proved"
    if counted < minimum:
        return "INCONCLUSIVE", f"exit 0 but only {counted} counted (minimum {minimum})"
    return "PASS", f"exit 0, {counted} counted"


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description="Run the product gates")
    parser.add_argument("--gates", type=Path, default=ROOT / "docs/gates.json")
    parser.add_argument("--layer", help="run only gates of this layer, e.g. G0")
    args = parser.parse_args(argv)
    try:
        gates = [g for g in load(args.gates) if not args.layer or g.get("layer") == args.layer]
    except (OSError, ValueError) as error:
        print(f"GATES: INCONCLUSIVE — {type(error).__name__}: {error}")
        return 2
    if not gates:
        print(f"GATES: INCONCLUSIVE — no gates defined in {args.gates}" + (f" for layer {args.layer}" if args.layer else ""))
        return 2
    verdicts = []
    for gate in gates:
        verdict, detail = run_gate(gate)
        verdicts.append(verdict)
        print(f"{verdict:12} {gate['id']:20} {detail}")
    summary = {v: verdicts.count(v) for v in ("PASS", "FAIL", "INCONCLUSIVE")}
    print(f"GATES: {len(gates)} run — " + ", ".join(f"{k} {n}" for k, n in summary.items()))
    return 1 if summary["FAIL"] else 2 if summary["INCONCLUSIVE"] else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
