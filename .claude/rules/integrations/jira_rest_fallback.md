---
description: "Jira reads use official ACLI only. Jira MCP and hand-written issue GET are not the path. See atlassian_zero_cost.md."
paths:
  - ".claude/agents/energo-ts-test.md"
  - ".claude/skills/energo-ts-test/SKILL.md"
  - ".claude/agents/bug-validator.md"
  - ".claude/agents/cross-dependency-finder.md"
  - ".claude/agents/test-case-generator.md"
  - ".claude/agents/environment-resolver.md"
  - ".claude/agents/jira-bug.md"
  - ".claude/skills/phoenix-bug-validation/**"
  - ".claude/skills/cross-dependency-finder/**"
  - ".claude/skills/jira-bug-template/**"
  - ".claude/skills/test-case-generator/**"
  - ".claude/skills/environment-resolver/**"
---

# JIRA READ PATH — OFFICIAL ACLI [CRITICAL]

## Rule JIRA.1: Official `acli` is the only Jira read path [CRITICAL]

This file used to say “Jira MCP first, REST fallback.” That path is retired.

Follow **`.claude/rules/integrations/atlassian_zero_cost.md`** (Rules ATLASSIAN.0–ATLASSIAN.3):

- One issue: `acli jira workitem view <KEY> --json --fields "<project field list>"`
- JQL: `acli jira workitem search --jql "<jql>" --json --fields "key,summary,status,assignee" --limit 25` (max 50, no `--paginate`)
- Bytes of attachments: `Cursor-Project/config/jira/download-jira-attachments.ps1` only
- Bug-reporter create/edit: `acli jira workitem create` / `edit` after approval

Do **not** call Jira MCP. Do **not** run `acli rovodev`, `acli rovo`, or `twg`. Do **not** invent a Jira issue GET when `acli` fails — stop and ask the user to fix `acli` auth.

**Chat disclosure:** `Jira source: official ACLI (acli jira workitem view|search).`

Field completeness stays in **`.claude/skills/jira-evidence/SKILL.md`**. Changelog expand, rendered HTML, and createmeta are not official `acli` commands. Say so and continue with the `view` payload. Bug-reporter issue types and fix versions come from `acli jira project view --key KEY --json`, not from createmeta and not from a hand-written project GET.
