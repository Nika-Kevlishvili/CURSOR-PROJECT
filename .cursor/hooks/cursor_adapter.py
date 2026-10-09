#!/usr/bin/env python3
"""Cursor entry point for the Asterbit hooks (ADR-0008) — one script, one argument per event.

Cursor's hook payload (seen live, Cursor 3.23.23): a shell command arrives as tool "Shell"
(and on beforeShellExecution as `command` + `cwd`), every edit as "Write" and a deletion as
"Delete", both with tool_input.file_path — and before a Write, Cursor first asks about a
"Read" of the same file, so a denied read also stops the write; a subagent's tool calls carry
`parent_tool_call_id`, which equals the `subagent_id` its subagentStart announced. This
script runs the checks in this folder (guard.py, commit_secrets.py, memory_context.py,
event_log.py) and answers in Cursor's format:

    shell           beforeShellExecution  guard (block / ask) and the staged-secret scan
    tool            preToolUse            secret-file reads and writes; read-only engine agents
    session-start   sessionStart          project memory as additional_context, main-model note
    post-tool       postToolUse           event log; the checkpoint request after a compaction
    tool-failure    postToolUseFailure    event log
    pre-compact     preCompact            compaction log; arms the checkpoint request
    subagent-start  subagentStart         remembers which engine agent a tool call belongs to
    subagent-stop   subagentStop          event log; forgets the agent

`shell` and `tool` fail closed (exit 2) when the adapter itself breaks; every other event
never blocks. ASTERBIT_PROJECT_DIR overrides the project root for the drills.
"""
from __future__ import annotations

import datetime as dt
import json
import os
import re
import sys
from pathlib import Path

PROJECT = Path(os.environ.get("ASTERBIT_PROJECT_DIR") or Path(__file__).resolve().parents[2])
sys.path.insert(0, str(Path(__file__).resolve().parent))
from secret_patterns import is_secret_file  # noqa: E402

GUARD = "ASTERBIT-GUARD"
SECRETS = "ASTERBIT-SECRETS"
MEMORY = "ASTERBIT-MEMORY"
MONITOR = "ASTERBIT-CONTEXT-MONITOR"
EXPECTED_CODER = "claude-sonnet-5-5"           # ADR-0007 option A: the main chat codes on Sonnet 5.5
READ_ONLY_AGENTS = {"auditor", "verifier", "architect", "scout", "advisor"}
NO_SHELL_AGENTS = {"architect", "scout"}       # their roles need no commands at all
FAIL_CLOSED = {"shell", "tool"}
WORK_AROUND = " Explain why to the person you work with and ask — never work around a hook."


def logs() -> Path:
    path = PROJECT / ".cursor/logs"
    path.mkdir(parents=True, exist_ok=True)
    return path


def session_key(session_id: str) -> str:
    return re.sub(r"[^A-Za-z0-9]", "", session_id)[:8] or "unknown"


def handoff_path(session: str) -> str:
    """This session's single handoff file: the existing one, else today's new name."""
    key = session_key(session)
    existing = sorted((PROJECT / "memory/episodic/handoffs").glob(f"*-{key}.md"))
    target = existing[-1] if existing else PROJECT / f"memory/episodic/handoffs/{dt.date.today():%Y-%m-%d}-{key}.md"
    return target.relative_to(PROJECT).as_posix()


def registry(subagent_id: str) -> Path:
    name = re.sub(r"[^A-Za-z0-9_-]", "", subagent_id)[:80] or "unknown"
    return logs() / "cursor-subagents" / f"{name}.json"


def agent_of(event: dict) -> str:
    """'' for the main agent, else the subagent type its subagentStart recorded ('subagent' if unknown)."""
    parent = str(event.get("parent_tool_call_id") or "")
    if not parent:
        return ""
    path = registry(parent)
    try:
        return str(json.loads(path.read_text(encoding="utf-8")).get("type") or "subagent")
    except (OSError, ValueError):
        return "subagent"


def agent_type(raw: object) -> str:
    return str(raw or "").split(":")[-1].strip().lower()


def deny(text: str) -> dict:
    return {"permission": "deny", "user_message": text, "agent_message": text + WORK_AROUND}


def blocked(reason: str) -> dict:
    return deny(f"{GUARD}: blocked — {reason}.")


def shell(event: dict) -> dict:
    import commit_secrets
    import guard
    command = str(event.get("command") or "")
    cwd = str(event.get("cwd") or PROJECT)
    finding = guard.bash_problem(command, cwd)
    if finding and finding[0] == "block":
        return blocked(finding[1])
    try:
        code, message = commit_secrets.check(command, cwd)
    except Exception as error:  # noqa: BLE001 — a scan that cannot run is no evidence, never a pass
        return deny(f"{SECRETS}: blocked — the secret scan could not run ({type(error).__name__}: {error}).")
    if code == 2:
        return deny(message)
    if finding:
        text = f"{GUARD}: ask first — {finding[1]}."
        return {"permission": "ask", "user_message": text, "agent_message": text}
    return {"permission": "allow"}


def tool(event: dict) -> dict:
    import guard
    name = str(event.get("tool_name") or "")
    tool_input = event.get("tool_input") if isinstance(event.get("tool_input"), dict) else {}
    path = str(tool_input.get("file_path") or tool_input.get("path") or "")
    agent = agent_of(event)
    if agent in NO_SHELL_AGENTS and name == "Shell":
        return blocked(f"the {agent} agent has no shell — its role is read-only, without commands")
    if agent in READ_ONLY_AGENTS:
        finding = guard.read_only_problem(name, {"command": str(tool_input.get("command") or ""), "file_path": path})
        if finding:
            return blocked(f"{finding[1]} ({agent})")
    if name in ("Write", "Delete", "Read", "Grep") and path and is_secret_file(path):
        return blocked(f"{os.path.basename(path)} is a secret file — the agent neither reads nor changes it; "
                       "a person edits it by hand")
    return {"permission": "allow"}


def session_start(event: dict) -> dict:
    import memory_context
    model = str(event.get("model_id") or event.get("model") or "unknown")
    note = (f"ASTERBIT-RUNTIME: Cursor {event.get('cursor_version', '?')}, main model {model}; "
            f"ADR-0007 expects {EXPECTED_CODER} (effort high) for the coder.")
    if not model.startswith(EXPECTED_CODER):
        note += " The main model differs — say so in your first reply; the person you work with picks it in the model picker."
    return {"additional_context": note + "\n" + memory_context.build_context(PROJECT)}


def checkpoint_flag(session: str) -> Path:
    return logs() / f"cursor-checkpoint-{session_key(session)}.json"


def log_call(event: dict, outcome: str) -> None:
    import event_log
    record = {"session": session_key(str(event.get("conversation_id") or "")), "event": "tool_call",
              "agent": agent_of(event) or "main", "tool": str(event.get("tool_name") or ""),
              "summary": event_log.summarise(event.get("tool_input")), "outcome": outcome,
              "host": "cursor", "model": str(event.get("model") or "")}
    if outcome == "failed":
        record["failure"] = str(event.get("failure_type") or "")
    event_log.append(PROJECT, record)


def post_tool(event: dict) -> dict:
    log_call(event, "ok")
    session = str(event.get("conversation_id") or "")
    if event.get("parent_tool_call_id"):
        return {}
    try:
        checkpoint_flag(session).unlink()   # parallel tool calls: only the one that removes the flag asks
    except FileNotFoundError:
        return {}
    return {"additional_context": (
        f"{MONITOR}: Cursor has just compacted this conversation. Before anything else, write a checkpoint into this "
        f"session's handoff `{handoff_path(session)}` (one file per session; create it if missing), in the order of "
        "AGENTS.md 'Checkpoints' and following the owner's principles in memory/README.md: where we are, what is done, "
        "the exact next step, decisions with reasons, verbatim facts. Say plainly what the summary may have lost.")}


def tool_failure(event: dict) -> dict:
    log_call(event, "failed")
    return {}


def pre_compact(event: dict) -> dict:
    session = str(event.get("conversation_id") or "")
    percent = event.get("context_usage_percent")
    record = {"ts": f"{dt.datetime.now(dt.timezone.utc):%Y-%m-%dT%H:%M:%SZ}", "session": session_key(session),
              "outcome": "cursor: pre-compact", "host": "cursor", "trigger": event.get("trigger"), "percent": percent,
              "tokens": event.get("context_tokens"), "window": event.get("context_window_size"),
              "first": event.get("is_first_compaction"), "keys": sorted(event)}
    with (logs() / "compactions.jsonl").open("a", encoding="utf-8") as handle:
        handle.write(json.dumps(record) + "\n")
    if not event.get("parent_tool_call_id"):
        checkpoint_flag(session).write_text(json.dumps({"percent": percent}), encoding="utf-8")
    return {"user_message": f"{MEMORY}: Cursor is compacting the context ({percent}% used); right after it the agent "
                            "is asked to write this session's checkpoint into memory/episodic/handoffs/."}


def subagent_start(event: dict) -> dict:
    import event_log
    subagent = str(event.get("subagent_id") or event.get("tool_call_id") or "")
    kind = agent_type(event.get("subagent_type"))
    model = str(event.get("subagent_model") or "")
    if subagent:
        path = registry(subagent)
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps({"type": kind, "model": model}), encoding="utf-8")
    event_log.append(PROJECT, {"session": session_key(str(event.get("conversation_id") or "")), "event": "subagent_start",
                               "agent": kind or "subagent", "tool": "Task", "summary": event_log.summarise(
                                   {"description": str(event.get("task") or "")}), "outcome": "ok", "host": "cursor",
                               "model": model})
    return {"permission": "allow"}


def subagent_stop(event: dict) -> dict:
    import event_log
    subagent = str(event.get("subagent_id") or "")
    event_log.append(PROJECT, {"session": session_key(str(event.get("conversation_id") or "")), "event": "subagent_stop",
                               "agent": agent_type(event.get("subagent_type")) or "subagent", "tool": "Task",
                               "summary": f"{event.get('status', '?')} in {event.get('duration_ms', '?')} ms",
                               "outcome": "ok" if event.get("status") == "completed" else "failed", "host": "cursor",
                               "model": str(event.get("model") or "")})
    if subagent:
        registry(subagent).unlink(missing_ok=True)
    return {}


HANDLERS = {"shell": shell, "tool": tool, "session-start": session_start, "post-tool": post_tool,
            "tool-failure": tool_failure, "pre-compact": pre_compact, "subagent-start": subagent_start,
            "subagent-stop": subagent_stop}


def main() -> int:
    handler = sys.argv[1] if len(sys.argv) > 1 else ""
    try:
        event = json.load(sys.stdin)
        if not isinstance(event, dict):
            raise ValueError("the hook input is not a JSON object")
        print(json.dumps(HANDLERS[handler](event), ensure_ascii=False))
        return 0
    except Exception as error:  # noqa: BLE001 — a broken guard must not let things through
        if handler in FAIL_CLOSED or handler not in HANDLERS:
            print(f"{GUARD}: blocked — the Cursor adapter failed on '{handler}' ({type(error).__name__}: {error}).",
                  file=sys.stderr)
            return 2
        if handler == "subagent-start":
            print(json.dumps({"permission": "allow"}))
        print(f"ASTERBIT-CURSOR: '{handler}' hook failed ({type(error).__name__}: {error}).", file=sys.stderr)
        return 0 if handler == "subagent-start" else 1


if __name__ == "__main__":
    sys.exit(main())
