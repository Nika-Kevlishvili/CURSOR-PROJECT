"""The agent's event log (ADR-0006 P5, ADR-0008): one JSON line per tool call and per subagent.

.cursor/hooks/cursor_adapter.py appends to .cursor/logs/events-<date>.jsonl (not in git).
Each line: time, session, event, agent, tool, a short summary of the input (command, file
path, pattern…), the outcome, the host and the model. Tool input is untrusted data: known
provider tokens and generic credentials (Bearer / Authorization values, password=… style
assignments, user:password@ in URLs) are redacted, invisible characters stripped and the
summary cut to 200 characters. Other secret shapes can still get through, so a secret never
belongs in a command. Known limit: calls a guard blocks never reach postToolUse, so they are
not here.
"""
from __future__ import annotations

import datetime as dt
import json
import os
import re
import sys
from pathlib import Path

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from secret_patterns import redact  # noqa: E402
from untrusted import strip_invisible  # noqa: E402

MAX_SUMMARY = 200
SUMMARY_FIELDS = ("command", "file_path", "path", "pattern", "url", "query", "subagent_type", "description")
HIDDEN = "[REDACTED credential]"
# Generic credential shapes, used only here: widening secret_patterns would change the commit scan (ADR-0005).
SECRET_WORD = r"(?:pass(?:word)?|passwd|pwd|secret(?:[_-]?key)?|token|api[_-]?key|access[_-]?key|private[_-]?key)"
VALUE = r"(?!['\"]?\[REDACTED)(\"[^\"]*\"|'[^']*'|[^\s'\"&]+)"   # a quoted value whole, else up to a space
CREDENTIALS = (
    (re.compile(r"(?i)(\bauthorization\s*[:=]\s*(?:(?:basic|bearer|token)\s+)?)(?!(?:basic|bearer|token)\s)" + VALUE), rf"\1{HIDDEN}"),
    (re.compile(r"(?i)(\bbearer\s+)" + VALUE), rf"\1{HIDDEN}"),
    # the key must END in the secret word (--passes=3 stays); a closing quote may follow it ({"password": …})
    (re.compile(r"(?i)(\b[\w.-]*" + SECRET_WORD + r"['\"]?\s*[=:]\s*)" + VALUE), rf"\1{HIDDEN}"),
    (re.compile(r"(://[^/\s:@]+:)([^/\s@]+)(@)"), rf"\1{HIDDEN}\3"),
)


def redact_credentials(text: str) -> str:
    for pattern, replacement in CREDENTIALS:
        text = pattern.sub(replacement, text)
    return text


def summarise(tool_input: object) -> str:
    if not isinstance(tool_input, dict):
        return ""
    value = next((str(tool_input[k]) for k in SUMMARY_FIELDS if tool_input.get(k)), "")
    return strip_invisible(redact_credentials(redact(value))).replace("\n", " ")[:MAX_SUMMARY]


def append(project: Path, record: dict) -> None:
    """Add one record, stamped with the time, to today's log."""
    now = dt.datetime.now(dt.timezone.utc)
    log = project / f".cursor/logs/events-{now:%Y-%m-%d}.jsonl"
    log.parent.mkdir(parents=True, exist_ok=True)
    with log.open("a", encoding="utf-8") as handle:
        handle.write(json.dumps({"ts": f"{now:%Y-%m-%dT%H:%M:%SZ}", **record}, ensure_ascii=False) + "\n")
