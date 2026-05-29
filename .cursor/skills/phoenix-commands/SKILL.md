---
name: phoenix-commands
description: Maps user intent to **agents**, **skills**, and remaining **operational** command docs under `.cursor/commands/` (HandsOff checklist, branch switch, Swagger refresh, git/sync helpers). Duplicative slash-command markdown files were removed — route workflows via Task/subagents or natural language.
---

# Phoenix routing (agents / skills / operational commands)

Use this skill when the user asks **which workflow or agent** applies.

**Canonical behavior:** procedural detail lives in **`.cursor/agents/*.md`** and **`.cursor/skills/*/SKILL.md`**.

**Remaining `.cursor/commands/*.md`:** operational procedures only — notably **`hands-off.md`** (Rule 37 checklist), **`switch-phoenix-branches.md`** (+ `.ps1`), **`update-swagger-specs.md`**, **`send-playwright-results-slack.md`**, and git/sync helpers (`sync-*`, `pull-energots`, `push-energots`, `update-*`).

Rules load first (**Rule 0.0**) from **`.cursor/rules/`**.

## When to Apply

- User asks how to run Phoenix Q&A, bug validation, test cases, HandsOff, reports, or EnergoTS tests.
- User mentions an agent name or workflow keyword (`bug-validator`, `cross-dependency-finder`, HandsOff, etc.).
- Need to align intent with the correct **Task/subagent** or skill.

## Intent → canonical agent / skill

| User intent | Route to |
|-------------|-----------|
| Phoenix question (endpoints, logic, docs) | `.cursor/agents/phoenix-qa.md` — PhoenixExpert (**Rule 0.2**) |
| Consult before a Phoenix-related task | **Rule 8** + `.cursor/skills/phoenix-agent-workflow/SKILL.md`; PhoenixExpert patterns like **`phoenix-qa`** |
| Saved Chat report | `.cursor/skills/phoenix-reporting/SKILL.md`, `.cursor/agents/report-generator.md` — user **`/report`** or explicit save (**Rule 0.6**) |
| Session feedback file | **`phoenix-reporting`** skill — **Feedback saves**; **`report-generator`** |
| Bug validation | `.cursor/agents/bug-validator.md`, **`phoenix-bug-validation`** skill (**Rule 32**) |
| Jira bug text (Experiments only) | **`jira-bug`** subagent, **`jira-bug-template`** skill (**Rule JIRA.0**) |
| Production DB read-only analysis | **`production-data-reader`** agent + skill (**Rule PDR.0**) |
| Resolve environment for Phoenix workflows | **`environment-resolver`** subagent — `.cursor/agents/environment-resolver.md` |
| Cross-dependencies / impact | **`cross-dependency-finder`** subagent + skill (**Rules 35, 35a**) |
| Test cases (.md, Backend + Frontend) | **`cross-dependency-finder`** then **`test-case-generator`** — `.cursor/skills/test-case-generator/SKILL.md` |
| Run EnergoTS Playwright | **`energo-ts-run`** — `.cursor/agents/energo-ts-run.md`, skill (**Rules 36, ENERGOTS.0**) |
| Author EnergoTS `.spec.ts` | **`energo-ts-test`** — `.cursor/agents/energo-ts-test.md` (**Rule 0.8.1**, Swagger **Rule 41**) |
| Full HandsOff pipeline | **`.cursor/commands/hands-off.md`** + **`.cursor/agents/hands-off.md`**; triggers **`/HandsOff`** / **`!HandsOff`** + Jira (**Rule 37**) |

## Section notes

### Phoenix Q&A

Confluence (MCP, fresh) → codebase → answer as PhoenixExpert. Optional disk reports only per **Rule 0.6** / **`report-generator`**.

### Bug validation

Rule **32**: **`bug-validator`** subagent; evidence from **Confluence (mandatory full wiki URL per decision page in chat + Slack)**, mandatory **`update-swagger-specs.ps1`**, aligned Phoenix code, reproduce steps + diagrams per skill; **no** automatic test-case or Playwright pipeline inside Rule 32. **Env gate:** if Jira Environment is empty, parent must **not** delegate with a pre-filled env — subagent asks user (six options) before alignment/DB. Chat + Slack **`bug-validation`**; no disk unless **`/report`** or explicit save.

### HandsOff

No separate HandsOff skill file — orchestrator follows **`hands-off.md`** checklist end-to-end.

## Summary

- Route duplicated workflows through **agents/skills**, not deleted `.md` stubs.
- **HandsOff**, branch switching, Swagger refresh, and git/sync docs remain under **`.cursor/commands/`** as runnable/checklist references.
