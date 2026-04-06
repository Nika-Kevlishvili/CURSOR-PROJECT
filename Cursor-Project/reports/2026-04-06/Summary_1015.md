# Summary — Jira issues mentioning Prefix (2026-04-06)

## Request
Find Jira tasks related to "Prefix" / "Prefixes".

## Method
Jira MCP `searchJiraIssuesUsingJql` on cloud `oppa-support.atlassian.net`.

**JQL:**
```
summary ~ "Prefix" OR summary ~ "prefix" OR summary ~ "Prefixes" ORDER BY updated DESC
```

**Scope:** `maxResults` 50 (newest by `updated`).

## Findings
- **50 issues** returned; the majority show **Done** in the sample.
- **Notable open (non-done) issue:** **PDT-2737** — "Invoice reversal of Reconnection - incorrect prefix of the credit note", status **Needs Approval**, Bug, Highest.
- Projects represented include **PHX**, **PHN**, **PDT**, **GB** (prefix nomenclature, invoice/document prefixes, POD prefixes, templater, mass import, etc.).

## Follow-up
For open-only views, refine JQL with project and/or `statusCategory` / explicit status filters per team convention.

Agents involved: PhoenixExpert (Jira read via MCP), Summary reporter.
