#!/usr/bin/env python3
"""Drills for the engine's check scripts (not hooks): clean controls pass, seeded defects are caught.

Imported by drill_hooks.py, which owns the Drill harness, the report and the exit code
(ADR-0006). Every verdict is tied to the checked script's own exit code and output marker.
"""
from __future__ import annotations

import json
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CHECKS = ROOT / "sdlc/checks"


def tracked_copy(dst: Path, root: Path = ROOT) -> Path:
    """A hermetic copy of the working tree as git sees it, in a fresh repository.

    Tracked files (with their working-tree content) and untracked files that are not ignored:
    a tracked file may already link to a new file that is not committed yet, and leaving that
    file out made a clean control fail on a dirty tree (engine assessment B5). Ignored files —
    a local .env, logs — never leak into a drill. Works from a git worktree too (there .git is
    a file, not a folder).
    """
    files = subprocess.run(["git", "-C", str(root), "ls-files", "--cached", "--others", "--exclude-standard"],
                           capture_output=True, text=True, check=True).stdout
    for rel in files.splitlines():
        if (root / rel).is_file():
            (dst / rel).parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(root / rel, dst / rel)
    subprocess.run(["git", "init", "-q", str(dst)], check=True)
    return dst


def drill_tracked_copy(drill, scratch: Path) -> None:
    """B5: the drill copy mirrors the working tree — a new untracked file comes along, an ignored one never does."""
    source, secret_file = scratch / "copy-source", "." + "env"
    source.mkdir()
    subprocess.run(["git", "init", "-q", str(source)], check=True)
    (source / ".gitignore").write_text(secret_file + "*\n", encoding="utf-8")
    (source / "FILES.md").write_text("- [new](new.md)\n", encoding="utf-8")
    subprocess.run(["git", "-C", str(source), "add", ".gitignore", "FILES.md"], check=True)
    (source / "new.md").write_text("# new, not committed yet\n", encoding="utf-8")
    (source / secret_file).write_text("A=1\n", encoding="utf-8")
    copy = tracked_copy(scratch / "copy-target", source)
    drill.expect_bool("tracked_copy", "a new untracked file that a tracked file links to is copied (B5)", (copy / "new.md").is_file())
    drill.expect_bool("tracked_copy", "an ignored local secret file is never copied", not (copy / secret_file).exists())


def commits(repo: Path, count: int) -> list[str]:
    """A scratch repository with `count` real commits in one line of history; their full SHAs, oldest first."""
    subprocess.run(["git", "init", "-q", str(repo)], check=True)
    identity = ["-c", "user.name=drill", "-c", "user.email=drill@example.invalid", "-c", "core.hooksPath=/dev/null"]
    shas = []
    for number in range(count):
        subprocess.run(["git", "-C", str(repo), *identity, "commit", "-q", "--allow-empty", "-m", f"step {number}"], check=True)
        shas.append(subprocess.run(["git", "-C", str(repo), "rev-parse", "HEAD"], capture_output=True, text=True, check=True).stdout.strip())
    return shas


def rounds(ledger: Path, repo: Path, *args: str) -> subprocess.CompletedProcess:
    command = [sys.executable, str(CHECKS / "review_rounds.py"), *args, "--ledger", str(ledger), "--repo", str(repo)]
    return subprocess.run(command, capture_output=True, text=True, timeout=30)


def play(ledger: Path, repo: Path, loop: str, steps: list[tuple]) -> list[int]:
    """Steps: ("review", base, head, high, medium[, low]) or ("fix", head). Returns the exit codes."""
    codes = []
    for step in steps:
        if step[0] == "review":
            low = str(step[5]) if len(step) > 5 else "0"
            args = ["review", loop, "--base", step[1], "--head", step[2], "--high", str(step[3]), "--medium", str(step[4]), "--low", low]
        else:
            args = ["fix", loop, "--head", step[1]]
        codes.append(rounds(ledger, repo, *args).returncode)
    return codes


def text_of(path: Path) -> str:
    return path.read_text(encoding="utf-8") if path.exists() else ""


def line_count(path: Path) -> int:
    return len(text_of(path).splitlines())


def drill_review_rounds(drill, scratch: Path) -> None:
    gate, ledger, repo = "review_rounds", scratch / "review-log.jsonl", scratch / "rounds-repo"
    a, b, c, d = commits(repo, 4)  # real commits: the ledger now checks that a range is one line of history
    def run(*args: str) -> subprocess.CompletedProcess:
        return rounds(ledger, repo, *args)
    def steps(loop: str, items: list[tuple]) -> list[int]:
        return play(ledger, repo, loop, items)
    clean = steps("M1", [("review", a, b, 1, 2), ("fix", c), ("review", b, c, 0, 0, 3)])
    status = run("status", "M1")
    drill.expect_bool(gate, "clean control: review → fix → review with only LOW → done (exit 0)",
                      clean == [0, 0, 0] and status.returncode == 0 and "done" in status.stdout)
    before = line_count(ledger)
    early = run("fix", "M2", "--head", a)
    drill.expect_bool(gate, "fix before any review → refused, nothing written",
                      early.returncode == 1 and "not allowed" in early.stdout and line_count(ledger) == before)
    empty = run("review", "M3", "--base", a, "--head", a, "--high", "1", "--medium", "0", "--low", "0")
    drill.expect_bool(gate, "review with base == head → refused", empty.returncode == 1 and "empty diff range" in empty.stdout)
    full = steps("M4", [("review", a, b, 3, 2), ("fix", c), ("review", b, c, 2, 1), ("fix", d), ("review", c, d, 1, 0)])
    before = line_count(ledger)
    third = run("fix", "M4", "--head", a)
    final = run("status", "M4")
    drill.expect_bool(gate, "findings after the final review → 3rd fix refused, nothing written, status escalates (exit 1)",
                      full == [0] * 5 and third.returncode == 1 and "not allowed" in third.stdout and line_count(ledger) == before
                      and final.returncode == 1 and "final review" in final.stdout)
    steps("M6", [("review", a, b, 1, 0), ("fix", c)])
    before = line_count(ledger)
    shifted = run("review", "M6", "--base", a, "--head", c, "--high", "0", "--medium", "0", "--low", "0")
    drill.expect_bool(gate, "review 2 not covering exactly the fix (base ≠ previous head) → refused, nothing written",
                      shifted.returncode == 1 and "must cover exactly the fix" in shifted.stdout and line_count(ledger) == before)
    steps("M7", [("review", a, b, 1, 0)])
    no_commit = run("fix", "M7", "--head", b)
    drill.expect_bool(gate, "fix with no new commit → refused", no_commit.returncode == 1 and "no new commit" in no_commit.stdout)
    negative = run("review", "M8", "--base", a, "--head", b, "--high", "1", "--medium", "-1", "--low", "0")
    drill.expect_bool(gate, "negative finding count → rejected, nothing written",
                      negative.returncode == 2 and "0 or more" in negative.stderr and "M8" not in text_of(ledger))
    steps("M5", [("review", a, b, 0, 2), ("fix", c), ("review", b, c, 1, 1)])
    stuck = run("status", "M5")
    drill.expect_bool(gate, "HIGH+MEDIUM did not fall → escalates early (exit 1)",
                      stuck.returncode == 1 and "did not fall" in stuck.stdout)
    before = line_count(ledger)
    backwards = run("review", "M9", "--base", c, "--head", a, "--high", "1", "--medium", "0", "--low", "0")
    ghost = run("review", "M10", "--base", a, "--head", "0" * 40, "--high", "1", "--medium", "0", "--low", "0")
    steps("M11", [("review", a, c, 1, 0)])
    after_review = line_count(ledger)
    behind = run("fix", "M11", "--head", b)
    drill.expect_bool(gate, "base after head, a SHA that is no commit, a fix that does not build on the reviewed head → all refused, nothing written",
                      backwards.returncode == 1 and "not an ancestor" in backwards.stdout
                      and ghost.returncode == 1 and "does not exist" in ghost.stdout
                      and behind.returncode == 1 and "not an ancestor" in behind.stdout
                      and after_review == before + 1 and line_count(ledger) == after_review)
    elsewhere = rounds(ledger, scratch, "review", "M12", "--base", a, "--head", b, "--high", "1", "--medium", "0", "--low", "0")
    drill.expect_bool(gate, "commits cannot be checked (not a git repository) → INCONCLUSIVE (exit 2), never recorded",
                      elsewhere.returncode == 2 and "INCONCLUSIVE" in elsewhere.stdout and "M12" not in text_of(ledger))
    bad_lines = {"not json": "not json", "a list": "[]", "an unknown step": json.dumps({"loop": "M1", "step": "Review"}),
                 "a negative count": json.dumps({"loop": "M1", "step": "review", "high": 1, "medium": -1, "low": 0})}
    for label, line in bad_lines.items():
        broken = scratch / "broken-log.jsonl"
        broken.write_text(line + "\n", encoding="utf-8")
        unreadable = rounds(broken, repo, "status", "M1")
        drill.expect_bool(gate, f"ledger line with {label} → INCONCLUSIVE (exit 2), never escalate or pass",
                          unreadable.returncode == 2 and "INCONCLUSIVE" in unreadable.stdout)


PRD = "---\ntitle: drill\nstatus: approved\n---\n# PRD\n| ID | Feature | Journeys | Priority | Why |\n|---|---|---|---|---|\n" \
      "| P-1 | add a note | J-1 | must | core |\n| P-2 | export | J-1 | could | later |\n"
SPEC = "---\ntitle: drill\nstatus: approved\n---\n# Spec\n- FR-1 — add a note. Source: P-1\n- NFR-1 — fast. Source: TR-1\n"
PLAN = "---\ntitle: drill\nstatus: approved\n---\n# Plan\nM1 covers FR-1 and NFR-1.\n"
TODO = "# Tasks\n- [ ] T1 — drill task · proof: `true` → exit 0\n"  # the copy's own todo: the live one may mention any ID


def approved_from_template(name: str, fill: tuple[str, str] = ("", "")) -> str:
    text = (ROOT / f"sdlc/templates/{name}.md").read_text(encoding="utf-8").replace("status: draft", "status: approved", 1)
    return text.replace(*fill, 1) if fill[0] else text


def drill_artifact_checks(drill, scratch: Path) -> None:
    gate = "check_structure"
    real = tracked_copy(scratch / "templates")
    docs = {"prd": approved_from_template("prd"), "trd": approved_from_template("trd"),
            "spec": approved_from_template("spec", ("Source: P-n / J-n", "Source: P-1")),
            "plan": approved_from_template("plan") + "\nM1 covers FR-1 and NFR-1.\n"}
    for name, text in docs.items():
        (real / f"docs/{name}.md").write_text(text, encoding="utf-8")
    (real / "tasks/todo.md").write_text(TODO, encoding="utf-8")
    with (real / "docs/FILES.md").open("a", encoding="utf-8") as handle:
        handle.write("\n- [prd](prd.md) · [trd](trd.md) · [spec](spec.md) · [plan](plan.md)\n")
    from_templates = subprocess.run([sys.executable, str(real / "sdlc/checks/check_structure.py"), str(real)],
                                    capture_output=True, text=True, timeout=120)
    drill.expect_clean(gate, "clean control: approved PRD/TRD/spec/plan built from the real templates",
                       from_templates.returncode == 0 and "PASS" in from_templates.stdout)
    copy = tracked_copy(scratch / "artifacts")
    for name, text in (("prd", PRD), ("spec", SPEC), ("plan", PLAN)):
        (copy / f"docs/{name}.md").write_text(text, encoding="utf-8")
    (copy / "tasks/todo.md").write_text(TODO, encoding="utf-8")
    with (copy / "docs/FILES.md").open("a", encoding="utf-8") as handle:
        handle.write("\n- [prd](prd.md) · [spec](spec.md) · [plan](plan.md)\n")
    check = [sys.executable, str(copy / "sdlc/checks/check_structure.py"), str(copy)]
    clean = subprocess.run(check, capture_output=True, text=True, timeout=120)
    drill.expect_clean(gate, "clean control: approved PRD → spec → plan, fully traced", clean.returncode == 0 and "PASS" in clean.stdout)
    (copy / "docs/prd.md").write_text(PRD + "| P-3 | shared tags | J-1 | must | core |\n\nLanguage: [NEEDS CLARIFICATION: which?]\n", encoding="utf-8")
    (copy / "docs/spec.md").write_text(SPEC + "- FR-2 — export to CSV. Source: P-2\n", encoding="utf-8")
    seeded = subprocess.run(check, capture_output=True, text=True, timeout=120)
    expected = ("docs/prd.md: approved but still has [NEEDS CLARIFICATION]", "must-priority feature P-3 reaches no requirement line",
                "requirement FR-2 reaches no plan.md / todo.md")
    drill.expect_bool(gate, "approved artifacts: open marker, untraced must-feature, untraced requirement all reported",
                      seeded.returncode == 1 and all(e in seeded.stdout for e in expected))
    (copy / "docs/spec.md").write_text("---\ntitle: drill\nstatus: Approved  # x\n---\n# Spec\nRequirements are in a table elsewhere.\n", encoding="utf-8")
    (copy / "docs/plan.md").write_text("---\ntitle: drill\nstatus: final\n---\n# Plan\n", encoding="utf-8")
    empty = subprocess.run(check, capture_output=True, text=True, timeout=120)
    drill.expect_bool(gate, "approved spec with no requirement, and an unknown status, are reported — never a silent pass",
                      empty.returncode == 1 and "no FR-n/NFR-n requirement found" in empty.stdout and "unknown status 'final'" in empty.stdout)
    (copy / "docs/prd.md").write_text(PRD + "\n> [!question] Language [NEEDS CLARIFICATION: Georgian or English?]\n", encoding="utf-8")
    callout = subprocess.run(check, capture_output=True, text=True, timeout=120)
    drill.expect_bool(gate, "open question hidden in an Obsidian callout of an approved PRD → reported",
                      callout.returncode == 1 and "docs/prd.md: approved but still has [NEEDS CLARIFICATION]" in callout.stdout)
    for label, line in (("an unfilled placeholder outside the guidance", "Language: [NEEDS CLARIFICATION: <question>]"),
                        ("a lower-case marker", "Language: [needs clarification: Georgian or English?]")):
        (copy / "docs/prd.md").write_text(PRD + f"\n{line}\n", encoding="utf-8")
        variant = subprocess.run(check, capture_output=True, text=True, timeout=120)
        drill.expect_bool(gate, f"approved PRD with {label} → reported",
                          variant.returncode == 1 and "docs/prd.md: approved but still has [NEEDS CLARIFICATION]" in variant.stdout)


def script(name: str, *args: str) -> subprocess.CompletedProcess:
    return subprocess.run([sys.executable, str(CHECKS / name), *args], capture_output=True, text=True, timeout=60)


def drill_memory_hygiene(drill, scratch: Path) -> None:
    gate, project = "memory_hygiene", scratch / "hygiene"
    (project / "memory/episodic/handoffs").mkdir(parents=True)
    (project / "docs/decisions").mkdir(parents=True)
    subprocess.run(["git", "init", "-q", str(project)], check=True)
    handoff = project / "memory/episodic/handoffs/2026-10-01-abcd1234.md"
    handoff.write_text("---\nupdated: 2026-10-01T10:00:00Z\n---\n# Handoff\n", encoding="utf-8")
    clean = script("memory_hygiene.py", str(project), "--today", "2026-10-03")
    drill.expect_clean(gate, "clean control: a 2-day-old handoff, nothing else → exit 0", clean.returncode == 0)
    (project / "docs/decisions/0009-x.md").write_text("---\nstatus: accepted\nreview_by: 2026-10-02\n---\n", encoding="utf-8")
    (project / "app.py").write_text("x = 1  # " + "asterbit" + "-debt: naive loop · limit: 10k rows\n", encoding="utf-8")
    seeded = script("memory_hygiene.py", str(project), "--today", "2026-10-20")
    drill.expect_bool(gate, "stale handoff, overdue ADR review and a debt marker without revisit all listed (exit 3)",
                      seeded.returncode == 3 and "archive" in seeded.stdout and "0009-x.md" in seeded.stdout and "app.py:1" in seeded.stdout)


LESSONS_HEAD = "| Date | What went wrong | Rule | Kind | Check that guards it | In AGENTS.md? |\n|---|---|---|---|---|---|\n"


def drill_lessons(drill, scratch: Path) -> None:
    gate, table = "lessons_graduate", scratch / "lessons.md"
    table.write_text(LESSONS_HEAD + "| 2026-10-01 | Commit swept runtime state | Stage explicit paths | mechanical | guard blocks it | No |\n"
                     "| 2026-10-02 | Answer was too technical | Gloss every term | judgement | — | No |\n", encoding="utf-8")
    clean = script("lessons_graduate.py", str(table))
    drill.expect_clean(gate, "clean control: unrelated lessons, mechanical one already checked → exit 0", clean.returncode == 0)
    table.write_text(table.read_text(encoding="utf-8")
                     + "| 2026-10-05 | Commit swept runtime state again | Stage explicit paths only | mechanical | none | No |\n", encoding="utf-8")
    seeded = script("lessons_graduate.py", str(table))
    drill.expect_bool(gate, "lesson repeated on two dates + mechanical lesson without a check → both proposed (exit 3)",
                      seeded.returncode == 3 and "promote to AGENTS.md" in seeded.stdout and "build a check" in seeded.stdout)
    table.write_text(LESSONS_HEAD + "| 2026-10-03 | Pushed from a dirty tree | Commit before push | mechanical |  | No |\n"
                     "| 2026-10-04 | Port number drifted between files | Read the port from one config | mechanical | n/a | No |\n",
                     encoding="utf-8")
    blank = script("lessons_graduate.py", str(table))
    drill.expect_bool(gate, "mechanical lessons with a blank and an 'n/a' Check cell → both proposed as checks (exit 3)",
                      blank.returncode == 3 and "build a check (2026-10-03" in blank.stdout and "build a check (2026-10-04" in blank.stdout)
    missing = script("lessons_graduate.py", str(scratch / "no-lessons.md"))
    drill.expect_bool(gate, "missing lessons file → INCONCLUSIVE (exit 2)", missing.returncode == 2 and "INCONCLUSIVE" in missing.stdout)


def gate(gate_id: str, code: str, count: bool = False) -> dict:
    entry = {"id": gate_id, "command": [sys.executable, "-c", code]}
    return {**entry, "count_pattern": r"(\d+) passed"} if count else entry


def drill_run_gates(drill, scratch: Path) -> None:
    name, gates = "run_gates", scratch / "gates.json"
    def run(*entries: dict) -> subprocess.CompletedProcess:
        gates.write_text(json.dumps(list(entries)), encoding="utf-8")
        return script("run_gates.py", "--gates", str(gates))
    clean = run(gate("ok", "print('7 passed')", count=True), gate("lint", "pass"))
    drill.expect_clean(name, "clean control: counted gate + plain gate → PASS (exit 0)", clean.returncode == 0 and "PASS" in clean.stdout)
    zero = run(gate("ok", "print('0 passed')", count=True))
    drill.expect_bool(name, "'0 passed' → INCONCLUSIVE, never green (exit 2)", zero.returncode == 2 and "INCONCLUSIVE" in zero.stdout)
    failed = run(gate("ok", "print('3 passed')", count=True), gate("broken", "import sys; sys.exit(4)"))
    drill.expect_bool(name, "a failing gate → FAIL (exit 1)", failed.returncode == 1 and "FAIL " in failed.stdout)
    none = script("run_gates.py", "--gates", str(scratch / "no-gates.json"))
    drill.expect_bool(name, "no gates file → INCONCLUSIVE (exit 2)", none.returncode == 2 and "INCONCLUSIVE" in none.stdout)
    for label, broken in (("an invalid regex", {"count_pattern": "(\\d+ passed"}),
                          ("a pattern without a group", {"count_pattern": "\\d+ passed"}),
                          ("a timeout that is not a number", {"timeout": "soon"})):
        config = run({**gate("ok", "print('3 passed')"), **broken})
        drill.expect_bool(name, f"gate config with {label} → INCONCLUSIVE (exit 2), never FAIL",
                          config.returncode == 2 and "bad gate config" in config.stdout)


def drill_agent_report(drill, scratch: Path) -> None:
    name, logs = "agent_report", scratch / "logs"
    logs.mkdir()
    empty = script("agent_report.py", "--logs", str(logs))
    drill.expect_bool(name, "empty log → INCONCLUSIVE (exit 2), never 'nothing happened'", empty.returncode == 2 and "INCONCLUSIVE" in empty.stdout)
    events = [{"session": "s1", "agent": "main", "tool": "Write", "summary": "src/a.py", "outcome": "ok"},
              {"session": "s1", "agent": "auditor", "tool": "Shell", "summary": "git diff", "outcome": "failed"}]
    (logs / "events-2026-10-07.jsonl").write_text("".join(json.dumps(e) + "\n" for e in events), encoding="utf-8")
    full = script("agent_report.py", "--logs", str(logs))
    drill.expect_clean(name, "clean control: two events → counts, the failure and the changed file reported",
                       full.returncode == 0 and "2 tool calls" in full.stdout and "1 failed" in full.stdout and "src/a.py" in full.stdout)
    main_round = {"type": "message", "input_tokens": 2, "cache_read_input_tokens": 299_998, "cache_creation_input_tokens": 0, "output_tokens": 100}
    advisor = {"type": "advisor_message", "model": "claude-opus-5-5", "input_tokens": 300_000, "output_tokens": 900}
    usage = {"input_tokens": 4, "cache_read_input_tokens": 599_996, "cache_creation_input_tokens": 0, "output_tokens": 200,
             "iterations": [main_round, advisor, main_round]}
    transcript = scratch / "advisor-transcript.jsonl"
    transcript.write_text(2 * (json.dumps({"type": "assistant", "message": {"id": "msg_drill", "usage": usage}}) + "\n"), encoding="utf-8")
    tokens = script("agent_report.py", "--logs", str(logs), "--transcript", str(transcript))
    drill.expect_bool(name, "advisor turn written as two lines → context = last main-model round (300,000), output counted once (200)",
                      tokens.returncode == 0 and "context now 300,000; output so far 200" in tokens.stdout)
    with (logs / "events-2026-10-07.jsonl").open("a", encoding="utf-8") as handle:
        handle.write("{not json\n")
    corrupt = script("agent_report.py", "--logs", str(logs))
    drill.expect_bool(name, "one corrupt log line → skipped and counted; the other events are still reported (exit 0)",
                      corrupt.returncode == 0 and "2 tool calls" in corrupt.stdout and "skipped 1 unreadable" in corrupt.stdout)
    bare = scratch / "no-usage-transcript.jsonl"
    bare.write_text(json.dumps({"type": "user", "message": {"content": "hi"}}) + "\n", encoding="utf-8")
    blind = script("agent_report.py", "--logs", str(logs), "--transcript", str(bare))
    drill.expect_bool(name, "transcript without token usage → INCONCLUSIVE (exit 2), never 'context now 0'",
                      blind.returncode == 2 and "tokens: INCONCLUSIVE — no token usage" in blind.stdout and "context now 0" not in blind.stdout)


def drill_engine_checks(drill, scratch: Path) -> None:
    drill_tracked_copy(drill, scratch)
    drill_review_rounds(drill, scratch)
    drill_artifact_checks(drill, scratch)
    drill_memory_hygiene(drill, scratch)
    drill_lessons(drill, scratch)
    drill_run_gates(drill, scratch)
    drill_agent_report(drill, scratch)
