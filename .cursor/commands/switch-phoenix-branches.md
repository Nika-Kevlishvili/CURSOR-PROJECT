# Switch Phoenix repos to an environment branch

Switch every repository under `Cursor-Project/Phoenix/` to the branch that maps to a target
environment, fetch the latest tip from `origin`, **discard any uncommitted local changes**,
and force-reset the local branch to `origin/<branch>`.

This command implements **Rule PHOENIX-SWITCH.0** in
[`.cursor/rules/integrations/phoenix_branch_switching.mdc`](.cursor/rules/integrations/phoenix_branch_switching.mdc).
It is the only sanctioned way for Cursor agents to switch Phoenix repos to a different
environment-aligned branch before answering Phoenix questions, validating bugs, generating
test cases, or running cross-dependency analysis.

## Branch mapping (canonical, lowercase)

| Environment   | Remote branch        |
|---------------|----------------------|
| `dev`         | `origin/dev`         |
| `dev2`        | `origin/dev2`        |
| `test`        | `origin/test`        |
| `preprod`     | `origin/preprod`     |
| `prod`        | `origin/prod`        |
| `experiments` | `origin/experiments` |

No other environment names are accepted. The script validates the parameter against this set.

## What the script does (per repo, in order)

1. Detect & abort any in-progress git operation (`merge`, `rebase`, `cherry-pick`, `revert`).
2. `git fetch origin --prune`. Failures are categorized so the agent can react:
   - `no-origin` — repo has no `origin` remote at all.
   - `network-failure` — DNS / VPN / TLS / connection issue.
   - `auth-failure` — credentials missing or rejected.
   - `fetch-failed` — any other fetch error.
3. Verify `origin/<branch>` exists; otherwise mark the repo `missing-remote` and skip.
4. **Idempotent fast-path:** if HEAD is already at `origin/<branch>` and the working tree is
   clean, mark `already-aligned` and skip the destructive steps.
5. **Discard uncommitted local changes** if any: `git reset --hard HEAD` + `git clean -fd`.
   - Per workspace policy: local Phoenix edits are NOT preserved during a switch.
6. `git checkout -B <branch> origin/<branch>` (creates / re-points local tracking branch).
7. `git reset --hard origin/<branch>` (force-align local branch to the latest remote tip).

## Concurrency lock

While the script runs, it writes a lock file at
`.cursor/.phoenix_switch.lock` containing the PID and start timestamp. If another invocation
is started while a previous one is still running (and that PID is still alive), the second
invocation exits with code `1` and a clear message. Stale locks (PID no longer running) are
removed automatically on the next invocation.

## Per-repo statuses

| Status              | Meaning                                                                 |
|---------------------|-------------------------------------------------------------------------|
| `ok`                | Aligned to `origin/<branch>` after a fresh fetch.                       |
| `already-aligned`   | HEAD was already at `origin/<branch>` with a clean working tree.        |
| `dry-run-ok`        | Dry-run preview only; no git state changed.                             |
| `missing-remote`    | The repo has no `origin/<branch>` ref. Skipped, left untouched.         |
| `no-origin`         | The repo has no `origin` remote configured.                             |
| `network-failure`   | Fetch failed because of network / VPN / DNS / TLS.                      |
| `auth-failure`      | Fetch failed because of missing or rejected credentials.                |
| `fetch-failed`      | Fetch failed for another reason (see `Detail` column).                  |
| `checkout-failed`   | `git checkout` failed (uncommon).                                       |
| `reset-failed`      | `git reset --hard origin/<branch>` failed (uncommon).                   |
| `error`             | Unhandled exception in the script.                                      |

## Usage

PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File .cursor/commands/switch-phoenix-branches.ps1 -Environment dev
powershell -ExecutionPolicy Bypass -File .cursor/commands/switch-phoenix-branches.ps1 -Environment test
powershell -ExecutionPolicy Bypass -File .cursor/commands/switch-phoenix-branches.ps1 -Environment experiments
```

**Prod requires explicit confirmation** (extra safety because aligning to `origin/prod` is
destructive on local Phoenix edits, just like every other environment, but prod is special):

```powershell
powershell -ExecutionPolicy Bypass -File .cursor/commands/switch-phoenix-branches.ps1 -Environment prod -ConfirmProd
```

If `-Environment prod` is passed without `-ConfirmProd` (and not in dry-run mode), the script
exits with code `1` and prints a clear message. Cursor agents MUST surface the prod
confirmation request to the user (Rule CONF.0) **before** running with `-ConfirmProd`.

Dry-run (no destructive operations, prints the planned actions per repo):

```powershell
powershell -ExecutionPolicy Bypass -File .cursor/commands/switch-phoenix-branches.ps1 -Environment dev -DryRun
```

## Exit codes

- `0` — every repo reached `ok` / `already-aligned` (or `dry-run-ok` in dry mode).
- `1` — setup error (Phoenix root missing, prod without `-ConfirmProd`, lock conflict).
- `2` — partial failure: at least one repo succeeded, others failed. The summary table
  identifies each repo.
- `3` — every repo failed (catastrophic; usually network/VPN down).

Cursor agents MUST check the exit code AND the per-repo summary table before continuing
with environment-sensitive Phoenix code reading. If the script returned `2` or `3`, the
agent MUST surface that fact in the chat answer and clearly label any Phoenix conclusions
as "based on possibly stale / mixed-environment local state" until the user resolves the
underlying connectivity / auth / branch-availability problem.

## When Cursor agents must run this command

Cursor agents MUST run this command (or the equivalent dry-run during planning) **before**
they read Phoenix code for environment-sensitive answers, in the cases listed in
[Rule PHOENIX-SWITCH.0](.cursor/rules/integrations/phoenix_branch_switching.mdc):

- **Phoenix Q&A** when the user names an environment in scope (e.g. "in test", "on prod").
- **Bug validation** (Rule 32) — switch to the environment of the bug ticket before code analysis.
- **Cross-dependency analysis** (Rule 35a) — switch to the environment of the Jira/task before
  reading code; this is **not** local merge-history archaeology, it is just aligning the working
  copy with the environment under analysis.
- **Test case generation** (Rule 35) — same alignment as cross-dep step.
- **HandsOff** (Rule 37) — applied as part of step (1) when the ticket binds an environment.
- **Postman collection generation** (`/postman-collection`) — when the collection targets a
  specific environment's Phoenix endpoints.
- **Test runner** (`/test-runner`) — when the run targets Phoenix unit/integration tests for a
  specific environment.

If the user did not specify an environment for an environment-sensitive task, the agent MUST
ask which environment to use **before** running the script (Rule CONF.0). Do not silently
default.

### Subagent reuse

If the parent agent has already aligned Phoenix to the same environment in this chat session
(and no environment switch has happened since), child subagents (e.g. `cross-dependency-finder`
and `test-case-generator` when invoked back-to-back by `hands-off`) MUST NOT re-run the
script. Re-alignment is needed only when:

- the parent did not align yet, or
- the environment changes (e.g. Q&A on `test`, then bug validation on `prod`), or
- a previous alignment ended with exit code `2` or `3` (stale state), or
- the user explicitly asks for a fresh alignment.

### Multi-environment comparisons

For requests that compare two environments (e.g. "what is different between dev and prod"),
agents MUST run the script twice in series: first for env A, capture the relevant code reads
in chat memory, then for env B, capture again, then compare. Do not interleave reads across
environments without an explicit alignment in between.

## Out of scope

- This command does **not** touch `Cursor-Project/EnergoTS/` (that path is locked to the
  `cursor` branch by Rule ENERGOTS.0).
- This command does **not** modify Phoenix source code (Rule 0.8 Tier A still applies — only
  branch state changes, no `.java` edits).
- This command does **not** push, commit, or open MRs against Phoenix.
