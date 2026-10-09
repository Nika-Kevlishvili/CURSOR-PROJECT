#!/usr/bin/env python3
"""Review-loop ledger for the build loop (ADR-0006).

The loop, per milestone and per [high-risk] task:
    review 1 → fix 1 → review 2 → fix 2 → review 3 (final) → the owner.
Rounds are counted from an append-only ledger, never from the model's memory.
LOW findings are logged but never keep the loop open. If HIGH+MEDIUM do not
fall between two reviews, the loop stops early and goes to the owner.
Reviews 2 and 3 must cover exactly the fix: --base is the previous review's
--head and --head is the fix's commit. Every SHA must be a real commit in --repo
(default: this repository), a review's base must be an ancestor of its head, and
a fix must build on the reviewed head. Loop ids: M1, M1-T4, gate-spec, ENG-…;
after the owner decides an escalated loop, continue under a new id (M1-r2).

Usage:
    review_rounds.py review <loop> --base SHA --head SHA --high N --medium N --low N
    review_rounds.py fix <loop> --head SHA
    review_rounds.py status <loop>
Options: --ledger PATH (default: tasks/review-log.jsonl in this repo),
         --repo PATH (where the commits live; default: this repo).

Exit codes. status: 0 = closed clean, 1 = escalate to the owner, 2 = inconclusive
(ledger unreadable or malformed, or a usage error), 3 = continue (the next step is printed).
review / fix: 0 = recorded, 1 = step not allowed now (nothing written),
2 = inconclusive or usage error (nothing written).
"""
from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path

MAX_FIXES = 2
REPO = Path(__file__).resolve().parents[2]
DEFAULT_LEDGER = REPO / "tasks/review-log.jsonl"
LOOP_ID = re.compile(r"^[A-Za-z0-9._-]{1,40}$")
SHA = re.compile(r"^[0-9a-f]{7,40}$")
COUNTS = ("high", "medium", "low")


def checked(entry: object) -> dict:
    """A ledger line must be a known step; review counts must be whole numbers ≥ 0."""
    if not isinstance(entry, dict) or entry.get("step") not in ("review", "fix") or not isinstance(entry.get("loop"), str):
        raise ValueError(f"malformed ledger entry: {str(entry)[:80]}")
    if entry["step"] == "review" and not all(type(entry.get(k)) is int and entry[k] >= 0 for k in COUNTS):
        raise ValueError(f"review entry needs whole-number counts ≥ 0: {str(entry)[:80]}")
    return entry


def count(text: str) -> int:
    value = int(text)
    if value < 0:
        raise argparse.ArgumentTypeError("a finding count must be 0 or more")
    return value


def same(a: str, b: str) -> bool:
    return bool(a) and bool(b) and (a.startswith(b) or b.startswith(a))


def load(ledger: Path, loop: str) -> list[dict]:
    if not ledger.exists():
        return []
    entries = [checked(json.loads(line)) for line in ledger.read_text(encoding="utf-8").splitlines() if line.strip()]
    return [e for e in entries if e.get("loop") == loop]


def history_problem(repo: Path, older: str, newer: str) -> str:
    """Why `older..newer` is not a real range of commits in `repo`, or "" when it is."""
    def git(*args: str) -> int:
        return subprocess.run(["git", "-C", str(repo), *args], capture_output=True, text=True, timeout=15).returncode
    if git("rev-parse", "--git-dir") != 0:
        raise RuntimeError(f"{repo} is not a git repository, so the commits cannot be checked")
    for sha in (older, newer):
        if git("cat-file", "-e", f"{sha}^{{commit}}") != 0:
            return f"commit {sha} does not exist in {repo}"
    if git("merge-base", "--is-ancestor", older, newer) != 0:
        return f"{older} is not an ancestor of {newer} — the range is not one line of history"
    return ""


def blocking(entry: dict) -> int:
    return int(entry["high"]) + int(entry["medium"])


def next_step(entries: list[dict]) -> tuple[str, int]:
    reviews = [e for e in entries if e["step"] == "review"]
    fixes = [e for e in entries if e["step"] == "fix"]
    if not entries or entries[-1]["step"] == "fix":
        number = len(reviews) + 1
        return f"review {number}" + (" (final)" if number == MAX_FIXES + 1 else ""), 3
    if blocking(reviews[-1]) == 0:
        return "done: no HIGH or MEDIUM finding left", 0
    if len(reviews) >= 2 and blocking(reviews[-1]) >= blocking(reviews[-2]):
        return "escalate to the owner: HIGH+MEDIUM did not fall between reviews", 1
    if len(fixes) >= MAX_FIXES:
        return "escalate to the owner: findings remain after the final review", 1
    return f"fix {len(fixes) + 1}", 3


def record(args: argparse.Namespace, entries: list[dict]) -> int:
    expected, _ = next_step(entries)
    if not expected.startswith(args.command):
        print(f"REVIEW-LOOP: '{args.command}' is not allowed now for {args.loop} — next step is: {expected}")
        return 1
    if not SHA.match(args.head) or (args.command == "review" and not SHA.match(args.base)):
        print("REVIEW-LOOP: --base/--head must be git commit SHAs (7–40 hex characters)")
        return 1
    if args.command == "review" and same(args.base, args.head):
        print("REVIEW-LOOP: empty diff range (base == head) — record BASE before the work, then review base..head")
        return 1
    reviews = [e for e in entries if e["step"] == "review"]
    if args.command == "fix" and same(args.head, reviews[-1]["head"]):
        print("REVIEW-LOOP: the fix has no new commit (--head equals the reviewed head) — commit the fix first")
        return 1
    if args.command == "review" and reviews and not (same(args.base, reviews[-1]["head"]) and same(args.head, entries[-1]["head"])):
        print(f"REVIEW-LOOP: review {len(reviews) + 1} must cover exactly the fix: "
              f"--base {reviews[-1]['head']} --head {entries[-1]['head']}")
        return 1
    older = args.base if args.command == "review" else reviews[-1]["head"]
    problem = history_problem(args.repo, older, args.head)
    if problem:
        print(f"REVIEW-LOOP: {problem}")
        return 1
    entry ={"ts": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"), "loop": args.loop,
             "step": args.command, "round": sum(e["step"] == args.command for e in entries) + 1, "head": args.head}
    if args.command == "review":
        entry.update(base=args.base, high=args.high, medium=args.medium, low=args.low)
    args.ledger.parent.mkdir(parents=True, exist_ok=True)
    with args.ledger.open("a", encoding="utf-8") as handle:
        handle.write(json.dumps(entry) + "\n")
    print(f"REVIEW-LOOP: recorded {args.command} {entry['round']} for {args.loop}; next: {next_step(entries + [entry])[0]}")
    return 0


def parse(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Review-loop ledger (ADR-0006)")
    parser.add_argument("command", choices=("review", "fix", "status"))
    parser.add_argument("loop", help="milestone or task id, e.g. M1 or M1-T4")
    parser.add_argument("--ledger", type=Path, default=DEFAULT_LEDGER)
    parser.add_argument("--repo", type=Path, default=REPO)
    parser.add_argument("--base", default="")
    parser.add_argument("--head", default="")
    for level in COUNTS:
        parser.add_argument(f"--{level}", type=count, default=None)
    args = parser.parse_args(argv)
    if not LOOP_ID.match(args.loop):
        parser.error("loop id: 1–40 characters from A-Z a-z 0-9 . _ -")
    if args.command == "review" and None in (args.high, args.medium, args.low):
        parser.error("review needs --high, --medium and --low (counts from the auditor's report)")
    return args


def main(argv: list[str]) -> int:
    args = parse(argv)
    try:
        entries = load(args.ledger, args.loop)
        if args.command != "status":
            return record(args, entries)
        step, code = next_step(entries)
    except Exception as error:  # any failure proves nothing: inconclusive, never escalate or refuse by accident
        print(f"REVIEW-LOOP: INCONCLUSIVE — ledger {args.ledger} or repository {args.repo} unreadable or malformed "
              f"({type(error).__name__}: {error})")
        return 2
    print(f"REVIEW-LOOP: {args.loop} — {len(entries)} entries in {args.ledger.name}; next: {step}")
    return code


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
