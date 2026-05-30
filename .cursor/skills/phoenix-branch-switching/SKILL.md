---
name: phoenix-branch-switching
description: "Align Cursor-Project/Phoenix/* repos to origin/<branch> for environment-sensitive work (Rule PHOENIX-SWITCH.0). Covers env resolution, script invocation, exit codes, failure handling, prod safety gate, subagent reuse."
---

# Phoenix Branch Switching Skill (Rule PHOENIX-SWITCH.0)

Align every `Cursor-Project/Phoenix/*` repo to the target environment's `origin/<branch>` before reading Phoenix code for env-sensitive tasks (Q&A, bug validation, cross-dep, test cases, HandsOff). Phoenix files remain **READ-ONLY** (Rule 0.8 Tier A).

Does **not** apply to `EnergoTS/` (locked to `cursor` per Rule ENERGOTS.0).
Does **not** constitute merge-history archaeology (Rule 35a safe).

## When to Apply

Before reading Phoenix code when the task targets a specific environment:
- Phoenix Q&A with env reference
- Bug validation (Rule 32)
- Cross-dependency analysis (Rule 35a)
- Test case generation (Rule 35)
- HandsOff (Rule 37)

**Not required** for documentation-only answers with no env-specific code dependency.

## 1. Branch mapping (canonical, lowercase)

| Environment | `-Environment` value | Remote branch |
|-------------|---------------------|---------------|
| Dev | `dev` | `origin/dev` |
| Dev2 | `dev2` | `origin/dev2` |
| Test | `test` | `origin/test` |
| PreProd | `preprod` | `origin/preprod` |
| Prod | `prod` | `origin/prod` |
| Experiments | `experiments` | `origin/experiments` |

## 2. Environment resolution

Use **`environment-resolver`** (or AskQuestion with six options) before alignment. Priority:
1. Explicit user env in current chat
2. Jira ticket evidence
3. Prior confirmed env in this session

No silent defaults (Rule CONF.0).

## 3. How to run (sanctioned script ONLY)

```powershell
# Standard
powershell -ExecutionPolicy Bypass -File .cursor/commands/switch-phoenix-branches.ps1 -Environment <env>

# Prod (after explicit user ack — see §4)
powershell -ExecutionPolicy Bypass -File .cursor/commands/switch-phoenix-branches.ps1 -Environment prod -ConfirmProd

# Dry-run (planning)
powershell -ExecutionPolicy Bypass -File .cursor/commands/switch-phoenix-branches.ps1 -Environment <env> -DryRun
```

Do **not** run ad-hoc multi-repo `git checkout`/`git reset` directly.

### Script behavior

For each `Cursor-Project/Phoenix/*` git repo:
1. Abort any in-progress merge/rebase/cherry-pick/revert
2. `git fetch origin --prune`
3. `git reset --hard HEAD` + `git clean -fd` (always — discard local edits)
4. `git checkout -B <branch> origin/<branch>` + `git reset --hard origin/<branch>`

### Exit codes

| Code | Meaning | Agent action |
|------|---------|-------------|
| `0` | All repos aligned | Proceed |
| `1` | Setup error (missing root, prod without `-ConfirmProd`, lock) | Fix and retry |
| `2` | Partial failure (some repos ok, some failed) | Proceed; flag mixed-state in chat |
| `3` | All repos failed (network/VPN) | **STOP** — ask user to fix connectivity |

## 4. Prod safety gate

When env = `prod`:
1. Warn user: local Phoenix edits will be discarded, force-reset to `origin/prod`
2. Wait for explicit user acknowledgement
3. Only then run with `-ConfirmProd`

Do **not** pass `-ConfirmProd` preemptively.

## 5. Local-changes policy (DESTRUCTIVE)

- All uncommitted Phoenix edits are **discarded** on every switch
- Untracked files **removed** (`git clean -fd`)
- No stash-and-reapply; no skip-cleanup
- Report which repos had dirty state

## 6. Reporting in chat

After script, report (one short block):
- Selected environment + target branch
- Per-repo result (ok / already-aligned / missing-remote / failure type)
- Exit code
- Note if any repos had local changes discarded

## 7. Subagent reuse (do not re-align unnecessarily)

Skip re-alignment when:
- Parent already aligned same env in this session AND
- Previous exit code was `0`

Re-align when: parent did not align, env changed, previous exit `2`/`3`, or user asks.
Parent passes resolved env in subagent prompt.

## 8. Multi-environment comparisons

Run alignment twice in series — once per env. Capture code reads between switches. Do not interleave reads across environments without alignment.

## 9. Coverage for other agents

Same requirement applies to `postman-collection`, `test-runner`, `PhoenixExpert` consultation when code reads are env-sensitive. Out of scope: `production-data-reader`, `database-query` (DB only), `energo-ts-*` (locked to `cursor`).

## 10. Interaction with other rules

- Rule 0.8 Tier A unchanged — branch state change ≠ edit
- Rule 0.2/0.5 unchanged — PhoenixExpert + Confluence still apply
- Rule 35a safe — alignment is not merge archaeology
- Rule CONF.0 — ask env if ambiguous

Violation (answering env-sensitive Phoenix questions without alignment, or ad-hoc multi-repo checkout) is a **BLOCK**.
