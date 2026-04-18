"""
Environment detection + Phoenix repo branch switcher for Bug Validator.

Reads bug summary + description, detects which environment the bug is reported on
(dev / dev2 / test / preprod / prod), and switches every Phoenix repo under
PHOENIX_LOCAL_ROOT to the matching branch BEFORE the codebase scan runs.

Rules (per user request):
- Bug mentions "dev"      -> checkout `dev`-style branch
- Bug mentions "dev2"     -> checkout `dev2`-style branch
- Bug mentions "test"     -> checkout `test`-style branch (incl. TestRelease_*)
- Bug mentions "preprod"  -> checkout `preprod`-style branch (incl. PreProdRelease_*)
- Bug mentions "prod"     -> checkout `prod`-style branch
- No environment found    -> default to PROD branch

Safety:
- Never pushes. Only `git stash` + `git checkout` + (optional) `git fetch`.
- If a repo is dirty, changes are stashed with a labelled message; not popped
  back automatically (the next run can resume; we never want to corrupt a scan).
- If git is missing or the dir has no .git, the repo is skipped silently.
"""

from __future__ import annotations

import os
import re
import shutil
import subprocess
from pathlib import Path
from typing import Iterable, Optional

EnvName = str  # one of: "dev", "dev2", "test", "preprod", "prod"

DEFAULT_ENV: EnvName = "prod"

ALL_ENVS: tuple[EnvName, ...] = ("dev", "dev2", "test", "preprod", "prod")


_ENV_PATTERNS: list[tuple[EnvName, re.Pattern[str]]] = [
    (
        "dev2",
        re.compile(
            r"(?:\bdev[\s\-_]?2\b|\bdev2\b|დევ\s*[\-_]?\s*2|დევ\s*ორ)",
            re.IGNORECASE,
        ),
    ),
    (
        "preprod",
        re.compile(
            r"(?:\bpre[\s\-_]?prod(?:uction)?\b|\bpreprod\b|პრე[\s\-_]?პროდ)",
            re.IGNORECASE,
        ),
    ),
    (
        "test",
        re.compile(
            r"(?:\btest(?:ing|release)?\b|ტესტ(?:ი|ის|ზე|ში)?)",
            re.IGNORECASE,
        ),
    ),
    (
        "dev",
        re.compile(
            r"(?:\bdev(?:elopment|elop)?\b|დევ(?:ი|ის|ზე|ში)?|девелопер|разработ)",
            re.IGNORECASE,
        ),
    ),
    (
        "prod",
        re.compile(
            r"(?:\bprod(?:uction)?\b|პროდ(?:ი|ის|ზე|ში|უქცი)?|прод(?:акшн|акшен)?)",
            re.IGNORECASE,
        ),
    ),
]


# Ordered priority per env. Entries are either:
#   - str: a literal branch name (matched exactly)
#   - re.Pattern: a regex matched against a branch name; when multiple branches
#                 match, the "latest" wins (sorted by extracted digit tokens).
_BRANCH_PRIORITY: dict[EnvName, list[object]] = {
    "dev": ["dev", "dev-fix"],
    "dev2": [
        "dev2",
        re.compile(r"^Dev2Release[_/].+$"),
        re.compile(r"^Dev2Update[_/].+$"),
    ],
    "test": [
        "test",
        re.compile(r"^TestRelease[_/].+$"),
    ],
    "preprod": [
        "preprod",
        "pre-prod",
        re.compile(r"^PreProdRelease[_/].+$"),
    ],
    "prod": [
        "prod",
        re.compile(r"^ProdRelease[_/].+$"),
        "main",
        "master",
    ],
}


def detect_environment(text: str) -> Optional[EnvName]:
    """
    Detect environment mentioned in the bug text.

    Order is important: dev2 / preprod must be checked BEFORE dev / prod
    so they are not swallowed by the shorter pattern.

    Returns None if no environment keyword is found (caller should default to prod).
    """
    if not text:
        return None
    for env, pattern in _ENV_PATTERNS:
        if pattern.search(text):
            return env
    return None


def resolve_environment(bug_text: str) -> tuple[EnvName, bool]:
    """
    Resolve the environment to use, returning (env, was_explicit).

    When the bug doesn't mention any environment, defaults to prod.
    """
    detected = detect_environment(bug_text or "")
    if detected:
        return detected, True
    return DEFAULT_ENV, False


# --------------------------- git helpers ---------------------------


def _git_available() -> bool:
    return shutil.which("git") is not None


def _run_git(repo: Path, args: list[str], timeout: int = 60) -> tuple[int, str, str]:
    try:
        proc = subprocess.run(
            ["git", *args],
            cwd=str(repo),
            capture_output=True,
            text=True,
            timeout=timeout,
            check=False,
        )
        return proc.returncode, proc.stdout.strip(), proc.stderr.strip()
    except (OSError, subprocess.TimeoutExpired) as exc:
        return 1, "", f"git error: {exc}"


def _list_local_branches(repo: Path) -> list[str]:
    code, out, _ = _run_git(repo, ["branch", "--format=%(refname:short)"])
    if code != 0:
        return []
    return [b.strip() for b in out.splitlines() if b.strip()]


def _list_remote_branches(repo: Path) -> list[str]:
    code, out, _ = _run_git(
        repo, ["for-each-ref", "--format=%(refname:short)", "refs/remotes/origin/"]
    )
    if code != 0:
        return []
    branches: list[str] = []
    for line in out.splitlines():
        line = line.strip()
        if not line or line == "origin/HEAD" or line.endswith("/HEAD"):
            continue
        if line.startswith("origin/"):
            branches.append(line[len("origin/"):])
    return branches


def _current_branch(repo: Path) -> Optional[str]:
    code, out, _ = _run_git(repo, ["rev-parse", "--abbrev-ref", "HEAD"])
    if code != 0 or not out or out == "HEAD":
        return None
    return out


def _is_dirty(repo: Path) -> bool:
    code, out, _ = _run_git(repo, ["status", "--porcelain"])
    return code == 0 and bool(out)


def _branch_recency_key(branch: str) -> tuple:
    """
    Sort key that prefers the "latest" date-versioned release branch.

    Extracts numeric tokens from the branch name and uses them in order; this
    correctly compares names like `PreProdRelease/2026-04-02` vs
    `PreProdRelease_2025_12_01` regardless of separator.
    """
    nums = tuple(int(n) for n in re.findall(r"\d+", branch))
    return (nums, branch)


def pick_branch_for_env(
    env: EnvName,
    local_branches: Iterable[str],
    remote_branches: Iterable[str],
) -> Optional[str]:
    """
    Choose the best branch name (without `origin/` prefix) for the given env.

    Walks the env's ordered priority list. For literal entries (e.g. "dev",
    "main") the first one present wins. For regex entries the matching branch
    with the most recent date-like suffix wins. The first non-empty step is
    returned, so e.g. for `prod` we prefer `prod` -> `ProdRelease_<latest>` ->
    `main` -> `master`.
    """
    local_set = {b.strip() for b in local_branches if b}
    remote_set = {b.strip() for b in remote_branches if b}
    available = local_set | remote_set

    for entry in _BRANCH_PRIORITY.get(env, []):
        if isinstance(entry, str):
            if entry in available:
                return entry
            continue

        # entry is a compiled regex pattern
        matches = [b for b in available if entry.match(b)]
        if matches:
            matches.sort(key=_branch_recency_key, reverse=True)
            return matches[0]

    return None


def _merge_origin(repo: Path, branch: str) -> tuple[bool, str]:
    """
    Merge origin/<branch> into current branch to pull in remote updates.
    Returns (success, message).
    """
    code, out, err = _run_git(repo, ["merge", f"origin/{branch}", "--ff-only"])
    if code == 0:
        if "Already up to date" in out or "Already up-to-date" in out:
            return True, "up to date"
        return True, "merged updates from origin"
    # --ff-only failed: might be diverged
    return False, f"merge failed (diverged?): {err or out or 'unknown'}"


def _checkout_and_update(repo: Path, branch: str) -> tuple[bool, str]:
    """
    Checkout the branch and merge from origin/<branch> to get latest updates.
    If branch doesn't exist locally, create a tracking branch from origin/<branch>.
    Returns (success, message).
    """
    local_branches = _list_local_branches(repo)
    remote_branches = _list_remote_branches(repo)
    has_remote = branch in remote_branches

    if branch in local_branches:
        code, _, err = _run_git(repo, ["checkout", branch])
        if code != 0:
            return False, f"checkout failed: {err or 'unknown error'}"
        msg = f"checked out '{branch}'"
        # Pull updates from origin if remote branch exists
        if has_remote:
            merge_ok, merge_msg = _merge_origin(repo, branch)
            if merge_ok:
                msg += f"; {merge_msg}"
            else:
                msg += f"; WARNING: {merge_msg}"
        return True, msg

    if has_remote:
        # Create local tracking branch directly from origin (already up to date)
        code, _, err = _run_git(
            repo, ["checkout", "-B", branch, f"origin/{branch}"]
        )
        if code == 0:
            return True, f"created tracking branch from origin/{branch}"
        return False, f"create-track failed: {err or 'unknown error'}"

    return False, f"branch '{branch}' missing locally and on origin"


def switch_phoenix_repos_to_env(
    phoenix_root: Path,
    env: EnvName,
    fetch: bool = True,
) -> dict:
    """
    Walk every direct child of `phoenix_root` that is a git repo and switch it
    to the branch matching `env`. Stash dirty trees first, then fetch + checkout
    + merge origin/<branch> to get the latest code.

    Args:
        phoenix_root: directory containing one Phoenix repo per subfolder.
        env: target environment ("dev"/"dev2"/"test"/"preprod"/"prod").
        fetch: if True (default), run `git fetch origin` before resolving branches
               so that the latest remote refs are available.

    Returns a dict suitable for embedding in the validation report:
        {
            "env": "...",
            "git_available": bool,
            "phoenix_root": str,
            "repos": [
                {
                    "repo": "phoenix-core",
                    "before": "main",
                    "after": "dev",
                    "status": "switched" | "already_on_branch" | "skipped" | "failed",
                    "message": "...",
                },
                ...
            ],
        }
    """
    summary: dict = {
        "env": env,
        "git_available": _git_available(),
        "phoenix_root": str(phoenix_root),
        "fetch_attempted": bool(fetch),
        "repos": [],
    }

    if not summary["git_available"]:
        summary["error"] = "git executable not found in PATH"
        return summary

    if not phoenix_root.exists() or not phoenix_root.is_dir():
        summary["error"] = f"Phoenix root '{phoenix_root}' does not exist"
        return summary

    for child in sorted(phoenix_root.iterdir()):
        if not child.is_dir():
            continue
        if not (child / ".git").exists():
            continue

        repo_entry: dict = {
            "repo": child.name,
            "before": _current_branch(child),
            "after": None,
            "status": "skipped",
            "message": "",
        }

        if fetch:
            code, _, err = _run_git(child, ["fetch", "origin"], timeout=120)
            if code != 0:
                repo_entry["message"] = f"fetch failed: {err or 'unknown'}; "

        local_branches = _list_local_branches(child)
        remote_branches = _list_remote_branches(child)

        target = pick_branch_for_env(env, local_branches, remote_branches)
        if not target:
            repo_entry["status"] = "skipped"
            repo_entry["message"] += (
                f"no branch matching env '{env}' "
                f"(local={len(local_branches)}, remote={len(remote_branches)})"
            )
            summary["repos"].append(repo_entry)
            continue

        repo_entry["after"] = target

        # Stash dirty tree before any branch operations
        if _is_dirty(child):
            stash_label = f"bug-validator: switching to {env}"
            code, _, err = _run_git(child, ["stash", "push", "-u", "-m", stash_label])
            if code != 0:
                repo_entry["status"] = "failed"
                repo_entry["message"] += (
                    f"could not stash dirty tree: {err or 'unknown'}"
                )
                summary["repos"].append(repo_entry)
                continue
            repo_entry["message"] += "stashed local changes; "

        if repo_entry["before"] == target:
            # Already on target branch — just pull updates from origin
            remote_branches = _list_remote_branches(child)
            if target in remote_branches:
                merge_ok, merge_msg = _merge_origin(child, target)
                repo_entry["status"] = "updated" if merge_ok else "failed"
                repo_entry["message"] += f"already on '{target}'; {merge_msg}"
            else:
                repo_entry["status"] = "already_on_branch"
                repo_entry["message"] += f"already on '{target}' (no remote to pull)"
            summary["repos"].append(repo_entry)
            continue

        ok, msg = _checkout_and_update(child, target)
        repo_entry["status"] = "switched" if ok else "failed"
        repo_entry["message"] += msg
        if ok:
            repo_entry["after"] = _current_branch(child) or target

        summary["repos"].append(repo_entry)

    return summary


def format_switch_summary(summary: dict) -> list[str]:
    """Format the switch summary as a list of lines for logging or Markdown."""
    lines: list[str] = []
    env = summary.get("env", "?")
    lines.append(f"Environment selected: '{env}'")
    if not summary.get("git_available"):
        lines.append("git not available — branch switching skipped.")
    if summary.get("error"):
        lines.append(f"Error: {summary['error']}")
        return lines
    for repo in summary.get("repos", []):
        before = repo.get("before") or "?"
        after = repo.get("after") or "?"
        status = repo.get("status", "?")
        msg = repo.get("message", "")
        lines.append(
            f"- {repo.get('repo')}: {before} -> {after} [{status}] {msg}".rstrip()
        )
    return lines


__all__ = [
    "ALL_ENVS",
    "DEFAULT_ENV",
    "detect_environment",
    "resolve_environment",
    "pick_branch_for_env",
    "switch_phoenix_repos_to_env",
    "format_switch_summary",
]
