# Bulk Bug Validation

Validate multiple bug reports in one run using BulkBugValidatorAgent (Rule 38).

## ALWAYS Use BulkBugValidatorAgent:
ALL `/bug-validate-bulk` requests MUST be handled by BulkBugValidatorAgent — NO EXCEPTIONS.

## Invocation syntax:

**Mode A — explicit Jira keys:**
```
/bug-validate-bulk REG-101 REG-102 REG-205
```

**Mode B — Jira filter:**
```
/bug-validate-bulk --project=EXP --label=customer-feedback --status=Open
/bug-validate-bulk --project=EXP --type=Bug --status="In Progress"
```

Auto-detect: if the first argument matches `[A-Z]+-\d+` → Mode A (explicit keys). If arguments start with `--` → Mode B (Jira filter, build JQL).

---

## Mandatory Workflow:

### Phase 1: Intake

**Mode A:** Parse provided Jira keys. Fetch each ticket (title, description, environment field, comments) via Jira MCP.

**Mode B:** Build JQL from flags. Fetch matching issues via Jira MCP. Cap at 20 per run; if more match, ask user to narrow the filter or confirm proceeding.

**Environment resolution:**
- All bugs share one environment → align once, reuse for all (Rule PHOENIX-SWITCH.0 §7a).
- Bugs span multiple environments → align per unique environment encountered.
- Ambiguous environment for a bug → run `environment-resolver` (Rule CONF.0; never default silently).

Announce batch before starting: "Starting bulk validation for N bugs: [KEY list]".

---

### Phase 2: Per-bug validation (Rule 32 pipeline — each bug independently)

For each bug run the full Rule 32 sequence:

1. **Step 0a:** Resolve environment + align Phoenix branches (reuse if same env already aligned, Rule PHOENIX-SWITCH.0 §7a).
2. **Step 0b:** Recovery intake for incomplete tickets (`production_bug_patterns.json`).
3. **Step 1:** Extract expected behavior.
4. **Step 2:** Confluence validation (evidence strength).
5. **Step 3:** Code validation + **ALREADY_FIXED detection** (mandatory — see below).
6. **Step 4:** Reproducibility pipeline: Swagger refresh → cross-dep → test cases → Playwright → run.
7. **Step 4a:** Hard gate enforcement — pipeline must complete before any verdict.
8. **Step 5:** Apply 6-verdict decision matrix.
9. **Step 6a:** Collect pipeline evidence section.

Announce progress after each bug: "REG-101: VALID ✓ | REG-102: in progress…".

**Collect from each bug for grouping:**
`jira_key`, `title`, `verdict`, `confluence_evidence_strength`, `code_references` (file + line range), `playwright_run_result`, `already_fixed_candidate`.

---

### ALREADY_FIXED detection (Step 3, mandatory for each bug)

During code analysis: if the faulty code path **no longer exhibits** the reported behavior in the currently aligned branch:
1. Flag verdict candidate as `ALREADY_FIXED`.
2. Cross-check Confluence to confirm the removal was intentional.
3. If Confluence confirms → set verdict `ALREADY_FIXED`.
4. If Confluence is silent → use `NEEDS CLARIFICATION` instead.

---

### Phase 3: Grouping pass (after all individual validations)

Compare every pair using:

**Signals (in order of weight):**
1. Code path overlap (same file + overlapping line range) → strong `DUPLICATE` / `CAUSES` signal.
2. Confluence page overlap (same page ID cited) → `RELATED` / `DUPLICATE`.
3. Expected behavior text similarity (≥3 shared domain terms) → `LIKELY_SAME`.
4. Verdict cluster (two `VALID` bugs in same service) → inspect for relationship.
5. Entity/data dependency (Bug A creates entity that Bug B reads) → `CAUSES` / `CAUSED_BY`.

**Similarity score (0–100):**
- +40 same file in code references
- +25 overlapping line range
- +20 same Confluence page cited
- +15 expected behavior texts share ≥3 key domain terms

**Relationship labels:**
| Label | Score threshold | Meaning |
|-------|----------------|---------|
| `DUPLICATE` | ≥ 80 | Same root cause, same code path |
| `LIKELY_SAME` | 60–79 | Similar symptoms, same area, not confirmed identical |
| `RELATED` | 30–59 | Shared code path or data dependency, distinct bugs |
| `CAUSES` / `CAUSED_BY` | any | Bug A creates condition that triggers Bug B |
| `INDEPENDENT` | < 30 | No detectable relationship |

**Non-blocking:** if grouping fails or only 1 bug, deliver individual reports with `Grouping: skipped — [reason]`.

---

### Phase 4: Save run file

Write JSON to `Cursor-Project/config/bug_validation/bulk_validation_runs/` after every completed run.

**File name:** `YYYY-MM-DD_HHMM_<label>.json`
- Mode A: `REG101-and-2-more`
- Mode B: `EXP-customer-feedback`

**JSON structure:**
```json
{
  "run_id": "YYYY-MM-DD_HHMM_<label>",
  "timestamp": "ISO-8601",
  "mode": "explicit_keys | jira_filter",
  "filter": "<jql or key list>",
  "environment": "<resolved env or 'mixed'>",
  "bug_count": 3,
  "bugs": [
    {
      "jira_key": "REG-101",
      "title": "...",
      "verdict": "VALID",
      "confluence_evidence_strength": "exact match",
      "code_references": [{"file": "path/File.java", "lines": "123-145"}],
      "playwright_run_result": "passed",
      "already_fixed_candidate": false
    }
  ],
  "grouping": {
    "pairs": [
      {
        "bug_a": "REG-101",
        "bug_b": "REG-102",
        "relationship": "DUPLICATE",
        "score": 94,
        "evidence": "same file Foo.java:45, same Confluence page P-123"
      }
    ],
    "clusters": [
      {
        "label": "Payment processing issues",
        "bugs": ["REG-101", "REG-102"],
        "relationship": "DUPLICATE",
        "recommendation": "Merge into one ticket, fix once"
      }
    ],
    "causal_chains": [
      {
        "from": "REG-102",
        "to": "REG-205",
        "relationship": "CAUSES",
        "evidence": "REG-102 corrupts entity state consumed by REG-205"
      }
    ]
  }
}
```

---

### Phase 5: Deliver results (chat + local report file)

**Chat output:**
```markdown
## Bug Validation Batch Report
**Mode:** [explicit keys / Jira filter: <jql>]
**Bugs processed:** N
**Environment:** [resolved env or mixed]
**Run file:** Cursor-Project/config/bug_validation/bulk_validation_runs/<filename>.json
**Report file:** Cursor-Project/reports/Chat reports/YYYY/<english-month>/<DD>/BugValidation_Bulk_<label>.md

---

### REG-101: [title]
[Standard Rule 32 Steps 1–5 report]

### REG-102: [title]
[...]

---

## Bug Group Analysis

### Summary table
| Bug A | Bug B | Relationship | Score | Evidence |
|-------|-------|--------------|-------|----------|
| REG-101 | REG-102 | DUPLICATE | 94 | Same path Foo.java:45, same Confluence P-123 |
| REG-102 | REG-205 | CAUSES | — | REG-102 corrupts entity state consumed by REG-205 |

### Clusters
**Cluster 1: [label]**
- REG-101, REG-102 — DUPLICATE (score 94)
- Recommendation: merge into one ticket, fix once

### Causal chains
- REG-102 → REG-205 (CAUSES): REG-102 corrupts entity state consumed by REG-205

### Verdict summary
| Key | Title | Verdict |
|-----|-------|---------|
| REG-101 | ... | VALID |
| REG-102 | ... | ALREADY_FIXED |
| REG-205 | ... | NEEDS_CLARIFICATION |
```

**Report file (mandatory):** Write `BugValidation_Bulk_[label].md` under **Chat reports** (`Cursor-Project/reports/Chat reports/YYYY/<english-month>/<DD>/` per `Cursor-Project/reports/README.md`) after every completed run. Persistent local artifact alongside the JSON run file. No Slack delivery — results are delivered locally only.

---

## Cross-session comparison

If user says "compare with last run" or names a previous file:
1. Read the file (or most recent by timestamp) from `bulk_validation_runs/`.
2. Report previous verdicts for matching bugs.
3. Highlight verdict changes (e.g. `VALID` → `ALREADY_FIXED`).
4. Re-run grouping across old + new bugs if requested.

---

## Hard gate (Rule 32 inherited)

Verdicts `VALID`, `NEEDS CLARIFICATION`, `NEEDS APPROVAL`, `NOT VALID`, and `ALREADY_FIXED` MUST NOT be issued for any bug before `/energo-ts-run` has executed for that bug. `PROCESS BLOCKED` applies per-bug if the pipeline cannot complete after retries.

---

## 6-Verdict Decision Matrix:

- **VALID**: Exact Confluence match + code confirms reported faulty behavior → Fix the bug
- **NEEDS CLARIFICATION**: Contextual Confluence match + code confirms reported behavior → Get product clarification
- **NEEDS APPROVAL**: No Confluence match + code confirms reported behavior → Get product approval
- **NOT VALID**: Confluence contradicts expected behavior + code follows Confluence → Close as "working as designed"
- **ALREADY_FIXED**: Code no longer exhibits the faulty pattern AND Confluence confirms intentional → Close as fixed; if Confluence silent, use NEEDS CLARIFICATION
- **INSUFFICIENT EVIDENCE**: Confluence/code evidence unavailable or too weak → resolve evidence gap and rerun

---

## Response Must End With:
"Agents involved: BulkBugValidatorAgent, BugFinderAgent, PhoenixExpert, CrossDependencyFinderAgent, TestCaseGeneratorAgent, EnergoTSTestAgent, PlaywrightTestValidatorAgent, EnergoTSRunAgent" (omit agents that did not participate)
