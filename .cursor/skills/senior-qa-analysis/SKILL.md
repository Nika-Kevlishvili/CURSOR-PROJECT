# Senior QA Analysis

Product-quality-first analysis: code defects, documentation gaps, and code↔documentation mismatches. READ-ONLY.

**Governing rules:** `.cursor/rules/main/senior_qa_product_quality.mdc` (Rule QA.0–QA.4), `evidence_only_project_answers.mdc`, `core_rules.mdc` Rule 0.5.

---

## When to use

- User asks for QA audit, gap analysis, spec vs implementation, or “Senior QA” mode
- Phoenix Q&A where quality issues may hide in conflicts
- Bug validation (Rule 32) — **add** Findings section to standard workflow
- Test case generation — validate expected results against Confluence **and** flag code divergence
- Any Phoenix-scoped task unless user says “runtime only, no findings”

---

## Step 0: Scope and environment

1. Resolve **environment** via `environment-resolver` when work is env-sensitive (DB, Phoenix branch, Swagger path).
2. Align Phoenix repos: `switch-phoenix-branches.ps1` (Rule PHOENIX-SWITCH.0).
3. Define **scope**: ticket key, feature/domain, or Confluence page IDs.

---

## Step 1: Gather spec evidence (product intent)

1. **Jira** (if ticket): load `jira-evidence` SKILL — full issue, links, attachments.
2. **Confluence**: MCP first; REST fallback per `confluence_rest_fallback.mdc`. For bug validation, follow `phoenix-bug-validation` Step 2 breadth.
3. **User story / diagrams** under `User story/` or `config/Diagrams/` when in scope.
4. Extract **expected behaviors** as bullet list (cite page ID + URL per item).

**Doc gap signal:** behavior is user-visible but no Confluence/ticket statement → record **Finding: Doc gap**.

---

## Step 2: Gather implementation evidence (runtime)

1. **Phoenix code** search (semantic + grep) after branch alignment.
2. **Swagger** (refresh if Rule 32/41 applies): `config/swagger/<env>/swagger-spec.json`.
3. **DB** (optional, SELECT-only): same env; `phoenix-database` SKILL.

Extract **actual behaviors** with file:line citations.

**Code defect signal:** logic error, missing validation, inconsistent state — even when docs are silent.

---

## Step 3: Crosswalk (mandatory)

For each expected behavior from Step 1:

| Match? | Action |
|--------|--------|
| Code + Confluence agree | Note as **aligned** (brief) |
| Confluence only, code missing | **Finding: Code defect** or **Doc gap** (if feature not implemented) |
| Code only, Confluence silent | **Finding: Doc gap** |
| Confluence says X, code does Y | **Finding: Code↔Doc mismatch** — cite both |
| Swagger says X, code does Y | **Finding: Swagger↔Code mismatch** |

**Never** report mismatch as resolved by picking code alone.

---

## Step 4: Prioritize

Sort Findings by **user impact** (High first):

- **High:** wrong billing, data loss risk, security, blocking workflow, incorrect customer-facing outcome
- **Medium:** partial feature, workaround exists, wrong non-critical field
- **Low:** cosmetic, internal-only, doc typo with no behavior change

---

## Step 5: Output

**Chat (default):**

1. **Summary** — counts by Finding type
2. **Aligned areas** (short, optional)
3. **Findings** — each per Rule QA.2 template
4. **Recommendations** — ordered next steps for PO/dev/doc owner
5. **Confidence** block (Rule CONF.1)
6. **Agents involved:** Senior QA Tester, PhoenixExpert (if Phoenix code/Confluence used), others as applicable

**Disk:** only on `/report` or explicit save (Rule 0.6) → `Cursor-Project/reports/Chat reports/YYYY/<month>/<DD>/QA_Findings_<Topic>_<HHMM>.md`

---

## Integration with Rule 32 (bug validation)

Run standard `phoenix-bug-validation` SKILL, then **append**:

`### Quality Findings (Senior QA)`

List mismatches and doc gaps **in addition to** the 5-verdict matrix. A NOT VALID bug can still expose **doc outdated** or **Swagger drift**.

---

## READ-ONLY

No Phoenix edits. No Confluence writes. Recommendations only.
