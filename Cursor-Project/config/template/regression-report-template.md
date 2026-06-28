# Regression Validation Report — {PRIMARY_TICKET_KEY}

**Source env:** dev | **Target env:** dev2 | **Date:** {YYYY-MM-DD}

---

## Related tickets (include when `issuelinks` exist)

| Ticket | Layer | Relation | Jira status | Deployment (Dev→Dev2) | Dev2 satisfies ticket? |
|--------|-------|----------|-------------|------------------------|-------------------------|
| {KEY} | FE / BE / UNKNOWN | primary / clones / relates | {status} | DEPLOYED / NOT DEPLOYED / PARTIALLY DEPLOYED / UNKNOWN | YES / NO / PARTIAL / UNKNOWN |

**Bundle confidence:** MIN(per-ticket scores) = {XX}% ({ZONE})

---

## Verdict — {TICKET_KEY}

`DEPLOYED` | `NOT DEPLOYED` | `PARTIALLY DEPLOYED` | `UNKNOWN`

**Reason:** {2–4 sentences — what was confirmed, what was missing, why the label is not stronger}

**Evaluation criteria:**

| Criterion | Result | Notes |
|-----------|--------|-------|
| Jira / ticket scope understood | ✅ / ⚠️ / ❌ | |
| Ticket example scenario checked | ✅ / ⚠️ / ❌ | |
| Code mapping (git log / search) | ✅ / ⚠️ / ❌ | |
| Dev branch aligned | ✅ / ⚠️ / ❌ | |
| Dev code matches ticket | ✅ / ⚠️ / ❌ | |
| Dev2 branch aligned | ✅ / ⚠️ / ❌ | |
| Ticket commit on Dev2 | ✅ / ⚠️ / ❌ | |
| Dev2 code matches Dev (ticket scope) | ✅ / ⚠️ / ❌ | |
| Ops / env-only step (if ticket implies) | ✅ / ⚠️ / ❌ / N/A | |

**Evidence basis:** {Jira MCP | REST fallback | Phoenix code | git log | git diff | commit ancestry — list only what was used}

---

## Section 1 — Ticket Content

- **Key / type / status / resolution:**
- **What was fixed or implemented (from Jira):**

## Section 2 — Deployment Status

- **Deployment label:** (repeat from Verdict for traceability)
- **Commits on Dev / Dev2:**
- **File-level match table:**

| File | Dev | Dev2 | Match |
|------|-----|------|-------|
| | | | |

## Section 3 — Dev vs Dev2 Score Comparison

- **Dev:** XX% (ZONE) — factors
- **Dev2:** XX% (ZONE) — factors
- **Final:** MIN(Dev, Dev2) = XX% (ZONE)
- **Limiting factor:**

## Section 4 — Jira vs Dev2 Alignment

- **Does Dev2 satisfy the ticket?** YES | NO | PARTIAL | UNKNOWN
- **If NO or PARTIAL — what is missing:**

## Quality Findings (Senior QA)

{Findings per Rule QA.2 — mandatory when Dev vs Dev2 mismatch detected}

---

**Confidence: XX% (ZONE)** [aggregation: minimum-of-subscores]

Agents involved: RegressionValidator, Senior QA
