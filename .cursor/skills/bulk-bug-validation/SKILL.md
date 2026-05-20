---
name: bulk-bug-validation
description: Validates multiple bug reports in one run (Rule 38). Accepts explicit Jira keys or a Jira filter, delegates each bug to the Rule 32 single-bug pipeline, then runs a cross-bug grouping pass (duplicate detection, causal chains, similarity scoring). Saves a gitignored JSON run file for cross-session persistence. Use when the user invokes /bug-validate-bulk, provides multiple Jira keys to validate together, or wants to validate all bugs matching a filter.
---

# Bulk Bug Validation Skill

Orchestrates batch bug validation per **Rule 38** in `.cursor/rules/workflows/workflow_rules.mdc`.

## When to Apply

- User invokes `/bug-validate-bulk` with any arguments.
- User provides 2 or more Jira keys alongside a validation request (e.g. "validate REG-101, REG-102 and REG-205 together").
- User wants to validate all open bugs for a project/label/status in one pass.
- User says "find duplicate bugs", "group these bugs", "which bugs are related", or similar grouping intent.

## Quick reference — invocation modes

```
/bug-validate-bulk REG-101 REG-102 REG-205
/bug-validate-bulk --project=PDT --label=customer-feedback --status=Open
/bug-validate-bulk --project=PDT --type=Bug --status="In Progress"
```

Auto-detect:
- Arguments matching `[A-Z]+-\d+` → explicit keys mode.
- Arguments starting with `--` → Jira filter mode; build JQL and fetch via Jira MCP.

## Architecture

```
/bug-validate-bulk
  ├── Phase 1: Intake (fetch tickets, resolve environments)
  ├── Phase 2: Per-bug validation × N  ←── delegates to Rule 32 pipeline per bug
  │   ├── Step 0a: env align (Rule PHOENIX-SWITCH.0)
  │   ├── Step 0b: recovery intake
  │   ├── Step 1–3: expected behavior + Confluence + code (+ ALREADY_FIXED check)
  │   ├── Step 4: reproducibility pipeline (hard gate)
  │   └── Step 5: 6-verdict decision matrix
  ├── Phase 3: Grouping pass (cross-bug comparison)
  ├── Phase 4: Save JSON run file (gitignored)
  └── Phase 5: Deliver results (chat + local .md report file; no Slack)
```

## Key behaviors

### ALREADY_FIXED detection (per bug, Step 3)
If the faulty code path no longer exhibits the reported behavior in the aligned branch:
1. Flag as `ALREADY_FIXED` candidate.
2. Confirm with Confluence that the removal was intentional.
3. If confirmed → verdict `ALREADY_FIXED`. If Confluence silent → `NEEDS CLARIFICATION`.

### Grouping pass (Phase 3)
Runs after **all** individual validations complete. Non-blocking — if it fails, individual reports are delivered as-is.

**Signals used (highest to lowest weight):**
1. Same file + overlapping line range in code references → `DUPLICATE` / `CAUSES`
2. Same Confluence page cited → `RELATED` / `DUPLICATE`
3. Expected behavior text similarity (≥3 shared domain terms) → `LIKELY_SAME`
4. Verdict clustering (two `VALID` bugs in same service)
5. Entity/data dependency (Bug A creates entity consumed by Bug B) → `CAUSES`

**Similarity scoring (0–100):**
- +40 same file in code refs
- +25 overlapping line range
- +20 same Confluence page
- +15 shared ≥3 domain terms

**Labels:** `DUPLICATE` (≥80) / `LIKELY_SAME` (60–79) / `RELATED` (30–59) / `CAUSES`/`CAUSED_BY` (any, data dep) / `INDEPENDENT` (<30)

### Persistence (Phase 4)
After every completed run, save:
```
Cursor-Project/config/bug_validation/bulk_validation_runs/YYYY-MM-DD_HHMM_<label>.json
```
File is gitignored (local only). Contains: run metadata, per-bug verdict summaries, grouping analysis.

To reload in a new session: "compare with last run" or "compare with [filename]".

### Cap
Maximum 20 bugs per run. If Jira filter returns more, ask user to narrow or confirm.

## 6-Verdict matrix (applies to each bug)

| Verdict | Condition | Action |
|---------|-----------|--------|
| `VALID` | Exact Confluence + code confirms faulty behavior | Fix the bug |
| `NEEDS CLARIFICATION` | Contextual Confluence + code confirms behavior; OR code changed but Confluence silent | Get product clarification |
| `NEEDS APPROVAL` | No Confluence + code confirms behavior | Get product approval |
| `NOT VALID` | Confluence contradicts expected + code follows Confluence | Close as working as designed |
| `ALREADY_FIXED` | Code no longer shows faulty pattern + Confluence confirms | Close as fixed |
| `INSUFFICIENT EVIDENCE` | Confluence/code unavailable or too weak | Resolve evidence gap, rerun |

## Hard gate (inherited from Rule 32)
`VALID`, `NEEDS CLARIFICATION`, `NEEDS APPROVAL`, `NOT VALID`, `ALREADY_FIXED` MUST NOT be issued for any bug before `/energo-ts-run` executes for that bug. `PROCESS BLOCKED` applies per-bug.

## Rules referenced

- **Rule 38** (`.cursor/rules/workflows/workflow_rules.mdc`) — bulk validation
- **Rule 32** — single-bug pipeline delegated per bug
- **Rule PHOENIX-SWITCH.0** — branch alignment per bug's environment
- **Rule CONF.0** — ask when environment is ambiguous
- **Rule 0.6** — bulk is an exception: always writes `BugValidation_Bulk_[label].md` locally after every run (no Slack); single-bug is chat only unless `/report`
- **Rule 0.8 Tier A** — Phoenix READ-ONLY

## Reference files

- `.cursor/agents/bulk-bug-validator.md` — full subagent spec
- `.cursor/commands/bug-validate-bulk.md` — command spec with complete phase details
- `.cursor/agents/bug-validator.md` — single-bug subagent (delegated to per bug)
- `.cursor/commands/bug-validate.md` — single-bug command
- `Cursor-Project/config/bug_validation/bulk_validation_runs/` — run persistence store
