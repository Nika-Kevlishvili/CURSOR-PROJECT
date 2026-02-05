---
name: phoenix-safety-readonly
description: Enforces read-only use of GitLab and Confluence, forbids code modification and Confluence edit tools, and forbids logging credentials. Use when the user asks what is allowed or forbidden, about permissions, safety, Confluence editing, or code changes.
---

# Phoenix Safety and Read-Only Rules

Summarizes what must never happen and what is read-only. Prevents Confluence/GitLab writes, code edits, and credential leakage.

## When to Apply

- User asks what is allowed, forbidden, or about safety/permissions.
- User mentions editing Confluence, pushing to GitLab, or changing code.
- Clarifying read-only vs write operations.

## GitLab and Confluence: READ-ONLY (Rule 1)

- **No** commits, pushes, merges, or page edits.
- **Only** IntegrationService may update GitLab pipelines and Jira tickets (not Confluence).
- Confluence **edit tools forbidden:** updateConfluencePage, createConfluencePage, createConfluenceFooterComment, createConfluenceInlineComment.
- Confluence **allowed (read):** getConfluencePage, getPagesInConfluenceSpace, getConfluenceSpaces, search, searchConfluenceUsingCql, getConfluencePageDescendants, getConfluencePageFooterComments, getConfluencePageInlineComments.

## Code Modification: FORBIDDEN (Rule 0.8 / 7)

- Do **not** modify, edit, or alter any code (Java, Python, TS, etc.).
- Only read, analyze, search, recommend. When issues are found: analysis and recommendations only, no implementation.
- Phoenix folder is protected; hooks block edit attempts.

## Agent Mode: READ-ONLY (Rule 14)

- READ-ONLY: explore code, Confluence, documentation.
- Do not modify, commit, push, merge, delete, or execute unless the user explicitly commands a write.

## Credentials and Secrets (Rule 28)

- Never log or expose credentials, passwords, API keys, or tokens.
- Use safe logging and sanitize outputs.

## Key "Never" Rules

- Never work without required PhoenixExpert consultation (Rule 18).
- Never bypass AgentRouter; use `route_query()` (Rule 19).
- Never skip IntegrationService.update_before_task() (Rule 20).
- Never modify GitLab or Confluence (Rule 21).
- Never answer Phoenix questions without PhoenixExpert (Rule 22).
- Never skip "Agents involved" at end of response (Rule 24).
- Never run destructive operations without explicit user command (Rule 25).
- Never skip report generation after a task (Rule 30).

Full list: `.cursor/rules/safety_rules.mdc` and `core_rules.mdc` (Rule 0.8).
