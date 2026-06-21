# Regression Validator SKILL

**Agent:** `.cursor/agents/regression-validator.md`
**Scoring:** `.cursor/rules/scoring/confidence_scoring_matrix.mdc` — section "Regression Validation"
**Report template:** `Cursor-Project/config/templates/regression-report-template.md`

---

## Overview

Single-ticket Dev-to-Dev2 regression validation. Reads a Jira ticket, locates related code on the Dev branch, then verifies the same code exists on the Dev2 branch. Produces a scored assessment with Senior QA Findings for mismatches.

**Explicitly out of scope:** Confluence reads, DB queries (unless user requests), test case generation, Playwright.

---

## Step 0: Pre-flight Validation

**Purpose:** Verify infrastructure readiness before ticket work.

**Actions:**

### 0a. Discover Phoenix repositories
The branch-switching script (`switch-phoenix-branches.ps1` line 129) discovers repos the same way:
```powershell
Get-ChildItem -Path "Cursor-Project/Phoenix" -Directory | Where-Object { Test-Path (Join-Path $_.FullName '.git') }
```
Run this and store the list of repo names. These are the repos used in all subsequent steps for `git log`, `git diff`, and code reads. If 0 repos found: PROCESS BLOCKED.

### 0b. Verify Jira connectivity
Attempt Atlassian MCP `search` with a simple query (e.g. the ticket key), or verify REST script exists at `Cursor-Project/config/jira/get-jira-issue-rest.ps1`.

### 0c. Verify branch switching script
Confirm `.cursor/commands/switch-phoenix-branches.ps1` exists and is readable.

### Step 0 Validator

| Check | Method | Pass | Fail |
|-------|--------|------|------|
| Phoenix repos discovered | PowerShell command returns >= 1 git repo | Store repo list; continue | PROCESS BLOCKED |
| Jira access | MCP responds or REST script exists | Continue | PROCESS BLOCKED |
| Switch script | File exists | Continue | PROCESS BLOCKED |

**On any PROCESS BLOCKED:** Stop and report which prerequisite failed. Do not proceed.

**Output of Step 0:** `$phoenixRepos` — list of repo directory names (e.g. `phoenix-core`, `phoenix-core-lib`, etc.). All subsequent steps iterate over this list.

---

## Step 1: Jira Ticket Analysis

**Purpose:** Read ticket data and map it to code locations.

**Actions:**

### 1a. Fetch ticket
1. Use Atlassian MCP `search` with the ticket key → `fetch` with returned ARI
2. If MCP fails after 2 retries: run `get-jira-issue-rest.ps1` with the ticket key
3. Extract: summary, description, bug description (`customfield_10217`), acceptance criteria (`customfield_10048`), comments, linked issues, status, resolution, issue type
4. If REST fallback used, add disclosure: `Jira source: REST fallback (MCP unavailable or failed after retries).`

### 1b. Ticket-to-code mapping (ordered fallback)

**Strategy 1 — Git history (most reliable):**
For each repo in `$phoenixRepos` from Step 0:
```powershell
git -C "Cursor-Project/Phoenix/$repoName" log --all --oneline --grep="<TICKET_KEY>"
```
If commits found, extract changed files:
```powershell
git -C "Cursor-Project/Phoenix/$repoName" diff-tree --no-commit-id --name-only -r <commit_hash>
```
This gives the exact files modified for the ticket.

**Strategy 2 — Keyword extraction + code search:**
From Jira description/summary, extract:
- Endpoint paths (e.g. `/api/v1/disconnection-request`)
- Class or entity names (e.g. `DisconnectionRequest`, `Invoice`, `Contract`)
- Error messages or specific field names
- Service/controller/validator names mentioned

Use Grep and SemanticSearch on `Cursor-Project/Phoenix/` to find relevant files.

**Strategy 3 — Linked issues trail:**
If ticket has issuelinks (blocks, is-blocked-by, relates-to), note them. The linked tickets may reference the same code area. Do not fetch linked tickets in full — just note them for context.

**Fallback — UNKNOWN:**
If none of the above yields results, set `CODE_MAPPING: UNKNOWN`. This is not a failure — it means manual verification is needed. Confidence takes a -15 penalty.

### Step 1 Validator

| Check | Method | Pass | Fail |
|-------|--------|------|------|
| Ticket data non-empty | summary is not null/empty | Continue | PROCESS BLOCKED |
| Has description content | at least one of: description, bug description, acceptance criteria | Rich data (+20) | Sparse data (+10) |
| Code mapping determined | At least one strategy returned results, OR explicitly UNKNOWN | Continue | Set UNKNOWN, -15 |

**Confidence factors from this step:**
- `+20` ticket fetched with rich data (description + acceptance criteria)
- `+10` ticket fetched but sparse (summary only)
- `+10` git log found commits referencing ticket
- `-5` per assumption about code location
- `-20` both MCP and REST failed → PROCESS BLOCKED

---

## Step 2: Align Phoenix to Dev + Code Analysis

**Purpose:** Switch to Dev branch and analyze the code related to the ticket.

**Actions:**

### 2a. Branch alignment
```powershell
powershell -ExecutionPolicy Bypass -File .cursor/commands/switch-phoenix-branches.ps1 -Environment dev
```

### 2b. Verify alignment
For each Phoenix repo:
```powershell
git -C Cursor-Project/Phoenix/<repo> branch --show-current
git -C Cursor-Project/Phoenix/<repo> log -1 --format="%H %ci"
```
Confirm: correct branch, HEAD is recent (not a stale detached state).

### 2c. Code analysis
Using file paths from Step 1b:
- Read each relevant file
- Document what the code does in relation to the ticket
- Note the specific lines/methods that implement the fix/feature
- If CODE_MAPPING = UNKNOWN: attempt broad SemanticSearch using ticket summary keywords; if still nothing, record "No code located on Dev" and continue

### Step 2 Validator

| Check | Method | Pass | Fail |
|-------|--------|------|------|
| Script exit code | Check $LASTEXITCODE | 0 = full, 2 = partial | 3 = PROCESS BLOCKED |
| Branch correct | `git branch --show-current` = `dev` per repo | Continue | -5 per misaligned repo |
| HEAD freshness | `git log -1 --format="%ci"` within last 7 days | Continue | Warning in report |
| Code found | At least one file read with relevant content | +15 | -15 (no code found) |

**Confidence factors from this step:**
- `+5` Dev environment aligned (exit 0)
- `-5` partial alignment (exit 2)
- `+15` relevant code found and cited (file:line)
- `-15` no code found for ticket scope

**Senior QA checkpoint:** Analyze the Dev code — does it look like a correct fix/implementation for what the ticket describes? Note concerns.

---

## Step 3: Dev Assessment

**Purpose:** Score how well the Dev code matches the Jira ticket description.

**Actions:**
1. Compare Jira description (Step 1) against Dev code (Step 2)
2. Evaluate alignment:
   - Does the code change address what the ticket describes?
   - Are acceptance criteria (if present) satisfied by the code?
   - Are there obvious gaps?
3. Produce a brief Dev assessment narrative

### Step 3 Validator

| Check | Method | Pass | Fail |
|-------|--------|------|------|
| Assessment has code citation | At least one `file:line` reference | Continue | Cap at CAUTION zone |
| CODE_MAPPING = UNKNOWN | No code was found in Step 1/2 | Cap score at CAUTION | N/A |

**Confidence factors from this step:**
- `+10` Dev code clearly matches Jira description
- `+5` Dev code partially matches (some aspects unclear)
- `-10` Dev code does not obviously match ticket description

**Senior QA role:** Primary judgment step for Dev. Structured assessment:
- What the ticket says should be fixed/implemented
- What the Dev code actually does
- Gap analysis

---

## Step 4: Align Phoenix to Dev2 + Deployment Check

**Purpose:** Switch to Dev2 branch and determine if the ticket's code was deployed.

**Actions:**

### 4a. Branch alignment
```powershell
powershell -ExecutionPolicy Bypass -File .cursor/commands/switch-phoenix-branches.ps1 -Environment dev2
```

### 4b. Verify alignment (same as Step 2b but for Dev2)

### 4c. Deployment check (ordered strategies)

**Strategy 1 — Git log on Dev2:**
```powershell
git -C Cursor-Project/Phoenix/<repo> log origin/dev2 --oneline --grep="<TICKET_KEY>"
```
If the commit exists on Dev2: ticket was deployed.

**Strategy 2 — File-level comparison:**
For each file identified in Step 2c:
```powershell
git diff origin/dev origin/dev2 -- <filepath>
```
- No diff for the file = code is identical on both branches (deployed)
- Diff exists = code differs (check if the ticket's specific changes are in the diff)

**Strategy 3 — Content comparison:**
If git diff shows differences, read the file on Dev2 and compare the specific functions/methods from Step 2c. Determine if the ticket's changes are present even if other parts of the file differ.

**Deployment verdict:**
- `DEPLOYED` — ticket commit on Dev2, OR all relevant files identical, OR ticket-specific code confirmed present
- `NOT DEPLOYED` — ticket commit absent from Dev2 AND relevant code missing or different
- `PARTIALLY DEPLOYED` — some files/changes present, others missing

### Step 4 Validator

| Check | Method | Pass | Fail |
|-------|--------|------|------|
| Dev2 alignment | Script exit code 0 or 2 | Continue | 3 = PROCESS BLOCKED |
| Branch correct | `git branch --show-current` = `dev2` | Continue | -5 per repo |
| HEAD freshness | Recent commit date | Continue | Warning |
| Git diff ran | No git errors for diff commands | Continue | -5 per error |
| Deployment status determined | One of: DEPLOYED / NOT DEPLOYED / PARTIALLY DEPLOYED | Continue | Set UNKNOWN |

**Confidence factors from this step:**
- `+5` Dev2 environment aligned (exit 0)
- `+15` ticket commit found on Dev2 (clearly deployed)
- `+10` code matches but no explicit commit (likely deployed via merge)
- `-15` commit missing from Dev2 and code differs (NOT deployed)
- `+0` PARTIAL — mixed state

**Senior QA checkpoint:** If PARTIALLY DEPLOYED, document exactly which changes are present and which are missing — this is a critical Finding.

---

## Step 5: Dev2 Code Verification + Scoring

**Purpose:** Analyze Dev2 code quality relative to the ticket and produce Dev2 score.

**Actions:**
1. On Dev2 branch, read the same files analyzed in Step 2
2. Compare Dev2 code against Jira description (same analysis as Step 3)
3. Compare Dev2 code against Dev code — catalog differences
4. For each difference, determine: is it expected (different feature on Dev2), a gap (ticket not deployed), or a concern (code divergence)?

### Step 5 Validator

| Check | Method | Pass | Fail |
|-------|--------|------|------|
| Files read from Dev2 | Confirm current branch is dev2 before reading | Continue | -10 (wrong branch) |
| Comparison made | At least one Dev vs Dev2 comparison documented | Continue | -5 |

**Confidence factors from this step:**
- `+15` Dev2 code matches Dev for ticket scope (full deployment confirmed)
- `+5` Dev2 code partially matches
- `-10` significant differences exist
- `-15` code missing entirely on Dev2

**Senior QA role (most important checkpoint):** Produce explicit Findings for any Dev vs Dev2 mismatch:

```
### Finding: Dev vs Dev2 mismatch for {TICKET_KEY}
- **Type:** Environment mismatch
- **Impact:** High | Medium | Low
- **Dev code:** [file:line — what exists on Dev]
- **Dev2 code:** [file:line — what exists on Dev2, or "MISSING"]
- **Gap:** [what is different and why it matters]
- **Recommendation:** Merge to Dev2 | Investigate | Already handled differently
```

---

## Step 6: Grouped Assessment Report

**Purpose:** Produce the final structured report.

**Template:** Use `Cursor-Project/config/templates/regression-report-template.md`

**4 mandatory sections:**

### Section 1 — Ticket Content
- Key, type, summary, status, resolution
- Clear explanation of what was fixed/implemented (from Jira, not from code)

### Section 2 — Deployment Status
- `DEPLOYED` / `NOT DEPLOYED` / `PARTIALLY DEPLOYED`
- Git commits on each branch
- File-level match table

### Section 3 — Dev vs Dev2 Score Comparison
- Dev confidence score with factors
- Dev2 confidence score with factors
- Delta and explanation

### Section 4 — Jira vs Dev2 Alignment
- Does Dev2 satisfy the original ticket? YES / NO / PARTIAL / UNKNOWN
- If NO or PARTIAL: what specifically is missing?

### Quality Findings (Senior QA)
All Findings from Steps 2-5 collected here.

### Step 6 Validator

| Check | Method | Pass | Fail |
|-------|--------|------|------|
| All 4 sections populated | Each section has non-placeholder content | Continue | -5 per empty section |
| Findings included | If mismatches detected in Steps 4-5, Findings present | Continue | -10 (hidden mismatch) |

---

## Step 7: Final Confidence Score

**Purpose:** Compute the final confidence score using evidence factor table.

### 7a. Compute Dev sub-score and Dev2 sub-score separately

Each sub-score starts at base **40** and applies only the factors from its own steps.

**Dev sub-score factors (Steps 0-3 only):**

| Factor | Points | Source step |
|--------|--------|------------|
| Jira ticket fetched + rich data | +20 | Step 1 |
| Jira ticket fetched + sparse | +10 | Step 1 (mutually exclusive with rich) |
| Git commit found referencing ticket | +10 | Step 1 |
| Dev environment aligned (exit 0) | +5 | Step 2 |
| Dev alignment partial (exit 2) | -5 | Step 2 (mutually exclusive with exit 0) |
| Dev code found + cited | +15 | Step 2 |
| No code found for ticket scope | -15 | Step 2 (mutually exclusive with found) |
| Dev code matches Jira description | +10 | Step 3 |
| Dev code partially matches | +5 | Step 3 (mutually exclusive with full match) |
| Dev code does not match | -10 | Step 3 (mutually exclusive with match/partial) |
| Assumption (per assumption) | -5 | Any |
| Step Validator failure | -3 to -10 | Per severity |

**Dev2 sub-score factors (Steps 4-5 only):**

Start at base **40**, inherit ticket quality from Step 1 (same +20/+10), then:

| Factor | Points | Source step |
|--------|--------|------------|
| Jira ticket quality (inherited) | +20 or +10 | Step 1 |
| Dev2 environment aligned (exit 0) | +5 | Step 4 |
| Dev2 alignment partial (exit 2) | -5 | Step 4 (mutually exclusive with exit 0) |
| Ticket commit found on Dev2 | +15 | Step 4 |
| Ticket commit NOT on Dev2 | -15 | Step 4 (mutually exclusive with found) |
| Dev2 code matches Dev for ticket scope | +15 | Step 5 |
| Dev2 code partially matches | +5 | Step 5 (mutually exclusive with full match) |
| Code missing entirely on Dev2 | -20 | Step 5 (mutually exclusive with match/partial) |
| Significant differences | -10 | Step 5 (mutually exclusive with match) |
| Mismatch documented as Finding | +5 | Step 5 |
| Mismatch present but no Finding | -10 | Step 5 |
| Assumption (per assumption) | -5 | Any |
| Step Validator failure | -3 to -10 | Per severity |

### 7b. Compute final (overall) score

**Formula:** `Final = MIN(Dev sub-score, Dev2 sub-score)`

Rationale: same as HandsOff aggregation — the pipeline is only as strong as its weakest link. A perfect Dev score is meaningless if Dev2 is missing the code.

### 7c. Three-Zone routing

| Zone | Range | Meaning for regression |
|------|-------|----------------------|
| **GO** | >= 85% | Code deployed and verified on Dev2 with high confidence |
| **CAUTION** | 55-84% | Likely deployed but gaps exist; recommend manual spot-check |
| **STOP** | < 55% | Cannot confirm deployment; manual verification required |

### 7d. Output format

```
**Confidence: XX% (ZONE)** [aggregation: minimum-of-subscores]
- Dev: XX% (ZONE)
- Dev2: XX% (ZONE) ← limiting factor (if lower)
Evidence: [+factor1, +factor2, -factor3]
Reason: <which sub-score limited and why>
```

**CAUTION zone** additionally requires:
```
Assumptions: [numbered list]
Recommend user verify: [item → method]
```

**STOP zone** additionally requires:
```
BLOCKED: Cannot deliver reliable result. Missing: [evidence list]
Action needed: [specific steps or questions]
```

---

## Error Handling

| Step | Error | Action | Confidence impact |
|------|-------|--------|-------------------|
| 0 | Phoenix repos missing | PROCESS BLOCKED | N/A |
| 0 | Jira MCP auth failure | Try REST fallback | -5 (fallback used) |
| 1 | Ticket not found | PROCESS BLOCKED | -20 |
| 1 | Empty description | Continue with sparse data | +10 instead of +20 |
| 1 | All code mapping fails | Set CODE_MAPPING: UNKNOWN | -15 |
| 2 | Branch switch exit 3 | PROCESS BLOCKED | N/A |
| 2 | Branch switch exit 2 | Continue with warning | -5 |
| 2 | File read fails | Skip file, note in report | -5 per file |
| 4 | Dev2 switch exit 3 | PROCESS BLOCKED for Dev2 | N/A |
| 4 | Git diff huge (>500 lines for one file) | Summarize key sections; note "large diff" | -5 |
| 5 | Dev2 code analysis timeout | Report partial results | -10 |

---

## Integration with existing workspace

- **Jira reads:** follows Rule 42 / JIRA.1 (MCP first, REST fallback)
- **Phoenix branch switching:** follows Rule PHOENIX-SWITCH.0 via `switch-phoenix-branches.ps1`
- **Phoenix code:** READ-ONLY (Rule 0.8 Tier A)
- **Senior QA:** Rule QA.0 Finding format for mismatches
- **Confidence:** Rule CONF.1 Three-Zone with evidence factors from `confidence_scoring_matrix.mdc`
- **Reports:** Rule 0.6 — chat first; disk only when user asks
- **Disclosure:** Rule 0.1 — `Agents involved: RegressionValidator, Senior QA`
