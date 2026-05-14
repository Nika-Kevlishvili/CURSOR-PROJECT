---
name: jira-ticket-analysis
description: Routes Jira ticket analysis requests — ensures full payload, custom fields, attachments, and linked Confluence pages are covered. Use when user asks to analyze, triage, or summarize a Jira ticket.
---

# Jira Ticket Analysis Skill

Routes Jira ticket analysis to ensure thorough, evidence-based results.

## When to Apply

- User asks to analyze, triage, summarize, or retrieve a Jira ticket.
- User provides a Jira key and wants full understanding.
- User mentions "analyze ticket", "ticket details", "what does this ticket say".

## Action

1. Fetch full issue via Jira MCP (or REST fallback after retries).
2. Ensure these are all checked:
   - `description` (may be null — check custom fields too)
   - Custom fields (per mapping in `evidence_only_answers.mdc`)
   - Linked issues
   - Attachments (metadata + content if needed)
   - Comments
   - Linked Confluence pages (fetch and merge into response)
3. If the ticket is a bug, offer to run **bug-validator** (Pipeline 2).
4. If the ticket is a task, offer to run **test-case-generator** (Pipeline 1).

## Do NOT

- Conclude "no information" just because `description` is empty.
- Skip linked Confluence pages when they are present.
- Skip attachments metadata.

## Reference

- Rule: `rules/main/evidence_only_answers.mdc` (Jira completeness section)
- Scripts: `scripts/jira/fetch-issue.ps1`, `scripts/jira/download-attachments.ps1`
- Orchestrator: `agents/qa-workflow.md` (Pipeline 4)
