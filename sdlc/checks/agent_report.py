#!/usr/bin/env python3
"""Agent report: what the agent did, what failed, which models ran (ADR-0006 P5, ADR-0008).

Reads the event log that .cursor/hooks/cursor_adapter.py writes (.cursor/logs/events-*.jsonl):
every tool call, and every subagent with the model it ran on. Cursor's own transcripts hold
no token counts, so day-to-day cost is read from Cursor's usage page. A transcript file passed
with --transcript is still counted when it has usage; a file with none is inconclusive, never
"context now 0". An unreadable log line is skipped and counted, not hidden and not fatal.

    python3 sdlc/checks/agent_report.py [--date YYYY-MM-DD] [--session KEY] [--logs DIR] [--transcript PATH]

Exit codes: 0 = report printed · 2 = inconclusive (no events found — an empty log is
not evidence that nothing happened — or a transcript was given but holds no token usage).
"""
from __future__ import annotations

import argparse
import collections
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CONTEXT_FIELDS = ("input_tokens", "cache_read_input_tokens", "cache_creation_input_tokens")


def context_size(usage: dict) -> int:
    """Tokens in the main model's prompt. On an advisor turn the top-level figure sums every round,
    so the real context is the last type == "message" round (engine assessment A4)."""
    iterations = usage.get("iterations")
    if isinstance(iterations, list):
        rounds = [item for item in iterations if isinstance(item, dict) and item.get("type") == "message"]
        if rounds:
            usage = rounds[-1]
    return sum(int(usage.get(field) or 0) for field in CONTEXT_FIELDS)


def load_events(logs: Path, date: str | None, session: str | None) -> tuple[list[dict], int]:
    """(events, number of unreadable lines skipped)."""
    events, skipped = [], 0
    for path in sorted(logs.glob(f"events-{date or '*'}.jsonl")):
        for line in path.read_text(encoding="utf-8").splitlines():
            if not line.strip():
                continue
            try:
                record = json.loads(line)
            except json.JSONDecodeError:
                skipped += 1
                continue
            if not isinstance(record, dict):
                skipped += 1
            elif not session or record.get("session") == session:
                events.append(record)
    return events, skipped


def token_totals(transcript: Path) -> tuple[int, int] | None:
    """(last context size, total output tokens), or None when the transcript has no usage.

    One API response is written as several lines with the same message id and usage, so
    output is counted once per id (the last line wins)."""
    last, outputs = None, {}
    for number, line in enumerate(transcript.read_text(encoding="utf-8", errors="ignore").splitlines()):
        try:
            message = json.loads(line).get("message") or {}
            usage = message.get("usage")
        except (json.JSONDecodeError, AttributeError):
            continue
        if isinstance(usage, dict) and usage:
            last = context_size(usage)
            outputs[message.get("id") or f"line {number}"] = int(usage.get("output_tokens") or 0)
    return None if last is None else (last, sum(outputs.values()))


def report(events: list[dict]) -> None:
    calls = [e for e in events if e.get("event", "tool_call") == "tool_call"]
    starts = [e for e in events if e.get("event") == "subagent_start"]
    by_tool = collections.Counter(e.get("tool", "?") for e in calls)
    by_agent = collections.Counter(e.get("agent", "?") for e in calls)
    failed = [e for e in calls if e.get("outcome") == "failed"]
    touched = sorted({e["summary"] for e in calls if e.get("tool") in ("Write", "Delete")
                      and e.get("outcome") == "ok" and e.get("summary")})
    print(f"AGENT-REPORT: {len(calls)} tool calls in {len({e.get('session') for e in calls})} session(s), {len(failed)} failed")
    print("  by tool:  " + ", ".join(f"{tool} {n}" for tool, n in by_tool.most_common()))
    print("  by agent: " + ", ".join(f"{agent} {n}" for agent, n in by_agent.most_common()))
    if starts:
        runs = collections.Counter(f"{e.get('agent', '?')} on {e.get('model') or 'unknown model'}" for e in starts)
        print("  subagents: " + ", ".join(f"{run} ×{n}" for run, n in runs.most_common()))
    for event in failed[:10]:
        print(f"  failed: {event.get('ts')} {event.get('tool')} — {event.get('summary')}")
    print(f"  files changed ({len(touched)}): " + ", ".join(touched[:15]) + (" …" if len(touched) > 15 else ""))


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description="Summarise the agent event log")
    parser.add_argument("--logs", type=Path, default=ROOT / ".cursor/logs")
    parser.add_argument("--date")
    parser.add_argument("--session")
    parser.add_argument("--transcript", type=Path)
    args = parser.parse_args(argv)
    try:
        events, skipped = load_events(args.logs, args.date, args.session)
        tokens = token_totals(args.transcript) if args.transcript else None
    except (OSError, ValueError) as error:
        print(f"AGENT-REPORT: INCONCLUSIVE — {type(error).__name__}: {error}")
        return 2
    if not any(e.get("event", "tool_call") == "tool_call" for e in events):
        print(f"AGENT-REPORT: INCONCLUSIVE — no tool calls in {args.logs} for the given filter"
              + (f" ({skipped} unreadable line(s) skipped)" if skipped else ""))
        return 2
    report(events)
    if skipped:
        print(f"  skipped {skipped} unreadable log line(s)")
    if args.transcript and tokens is None:
        print(f"  tokens: INCONCLUSIVE — no token usage in {args.transcript}")
        return 2
    if tokens:
        print(f"  tokens: context now {tokens[0]:,}; output so far {tokens[1]:,}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
