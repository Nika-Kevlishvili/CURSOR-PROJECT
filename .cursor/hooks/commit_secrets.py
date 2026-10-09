"""Staged-secret scan for `git commit` (ADR-0005, ADR-0008).

.cursor/hooks/cursor_adapter.py calls check() before every shell command. Two scanners:
the built-in patterns (always run, no dependencies) and gitleaks when it is installed;
the answer names the scanners that actually ran. A scan that cannot run raises, and the
adapter blocks — no evidence is not a pass. A commit that would include anything not yet
staged (`git add … && git commit`, `-a`, `-i`, `-o`, `-p`, file names) is blocked too,
because the scan only sees what is already staged.
"""
from __future__ import annotations

import os
import shutil
import subprocess
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from guard import git_calls, segments_of  # noqa: E402
from secret_patterns import find_secrets, is_secret_file  # noqa: E402

MARKER = "ASTERBIT-SECRETS"
STAGING_SUBCOMMANDS = {"add", "rm", "mv", "stage"}
BYPASS_LONG = {"--all", "--include", "--only", "--interactive", "--patch", "--pathspec-from-file"}
VALUE_LONG = {"--message", "--file", "--author", "--date", "--template", "--reuse-message", "--reedit-message",
              "--fixup", "--squash", "--trailer", "--cleanup"}
VALUE_SHORT = set("mFCct")
BYPASS_SHORT = set("aiop")


def commit_bypasses_index(args: list[str]) -> str | None:
    """Why this commit would include changes that are not staged yet, or None."""
    i = 0
    while i < len(args):
        arg = args[i]
        if arg == "--":
            return "it names files to commit" if i + 1 < len(args) else None
        if arg.startswith("--"):
            name = arg.split("=", 1)[0]
            if name in BYPASS_LONG:
                return f"{name} commits changes that are not staged"
            i += 2 if name in VALUE_LONG and "=" not in arg else 1
            continue
        if arg.startswith("-") and len(arg) > 1:
            for k, letter in enumerate(arg[1:]):
                if letter in BYPASS_SHORT:
                    return f"-{letter} commits changes that are not staged"
                if letter in VALUE_SHORT:
                    i += 1 if k == len(arg) - 2 else 0  # value is the next token only if the letter ends the cluster
                    break
            i += 1
            continue
        return "it names files to commit"
    return None


def git(cwd: str, *args: str) -> str:
    return subprocess.run(["git", "-C", cwd, *args], capture_output=True, text=True, check=True, timeout=30).stdout


def builtin_scan(cwd: str) -> list[str]:
    findings = [f"{name}: secret file is staged" for name in git(cwd, "diff", "--cached", "--name-only").splitlines()
                if is_secret_file(name)]
    added = "\n".join(line[1:] for line in git(cwd, "diff", "--cached", "-U0", "--no-color").splitlines()
                      if line.startswith("+") and not line.startswith("+++"))
    return findings + [f"staged change, {item}" for item in find_secrets(added)]


def gitleaks_scan(cwd: str) -> tuple[str, list[str]]:
    if shutil.which("gitleaks") is None:
        return "gitleaks not installed", []
    result = subprocess.run(["gitleaks", "git", "--pre-commit", "--staged", "--redact", "--no-banner", cwd],
                            capture_output=True, text=True, timeout=60)
    if result.returncode == 0:
        return "gitleaks", []
    if result.returncode == 1:
        return "gitleaks", ["gitleaks found a leak in the staged changes (run it yourself to see where)"]
    raise RuntimeError(f"gitleaks exited {result.returncode}")


def check(command: str, cwd: str) -> tuple[int, str]:
    """(0, "") when the command makes no commit; (0, scanners used) when the staged changes are clean;
    (2, reason) when the commit is blocked. Raises when the scan cannot run — the caller blocks."""
    calls = [c for t in segments_of(command) for c in git_calls(t)]
    commits = [args for sub, args in calls if sub == "commit"]
    if not commits:
        return 0, ""
    reason = next(filter(None, map(commit_bypasses_index, commits)), None)
    if reason or any(sub in STAGING_SUBCOMMANDS for sub, _ in calls):
        why = reason or "files are staged in the same command"
        return 2, (f"{MARKER}: blocked — {why}, so the secret scan cannot see them. Stage with `git add <files>` "
                   "first, then run `git commit -m …` (or `-F file`) as a separate command.")
    findings = builtin_scan(cwd)
    gitleaks_label, gitleaks_findings = gitleaks_scan(cwd)
    scanners = f"built-in patterns + {gitleaks_label}"
    findings += gitleaks_findings
    if findings:
        return 2, "\n  ".join([f"{MARKER}: blocked — possible secret in the commit ({scanners}):", *findings])
    return 0, f"{MARKER}: staged changes are clean ({scanners})."
