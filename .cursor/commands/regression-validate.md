# /regression-validate

**Usage:** `/regression-validate PDT-XXXX`

**Purpose:** Run Dev-to-Dev2 regression validation for a single Jira ticket. Compares code presence on Dev vs Dev2 branches, scores deployment confidence, and produces a structured assessment with Senior QA Findings.

## What it does

1. Reads the Jira ticket (MCP or REST fallback)
2. Maps the ticket to code via git history and keyword search
3. Aligns Phoenix repos to `dev` branch, analyzes relevant code
4. Aligns Phoenix repos to `dev2` branch, checks if the same code exists
5. Produces a 4-section report with deployment verdict and confidence score

## Delegation

Route to **`regression-validator`** subagent with:
- `subagent_type: "regression-validator"`
- Prompt: load `.cursor/skills/regression-validator/SKILL.md` and execute Steps 0-7 for `{TICKET_KEY}`

## Constraints

- **No Confluence** -- this workflow explicitly excludes Confluence reads
- **READ-ONLY** -- no Phoenix edits (Rule 0.8)
- **Senior QA active** -- Findings for Dev vs Dev2 mismatches
- **Confidence scored** -- Three-Zone (GO/CAUTION/STOP) per `confidence_scoring_matrix.mdc`

## Example

```
/regression-validate PDT-2971
```

Produces: standalone **`## Verdict`** block (label + Reason + Evaluation criteria table + Evidence basis), deployment status (DEPLOYED / NOT DEPLOYED / PARTIALLY DEPLOYED / UNKNOWN), Dev vs Dev2 score comparison, and actionable Findings.
