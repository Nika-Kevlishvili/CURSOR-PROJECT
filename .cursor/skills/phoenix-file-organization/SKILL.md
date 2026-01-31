---
name: phoenix-file-organization
description: Places new files in the correct project directories (agents, docs, User story, reports, config, postman, Phoenix). Use when creating or saving files, user stories, flows, reports, or when the user asks where to put a file.
---

# Phoenix File Organization

Ensures new files are saved in the correct directories (Rule 31). Reports use current date; stories and flows go in User story.

## When to Apply

- User asks to create or save a file.
- User mentions user story, flow diagram, report, documentation, or config.
- Unsure where a new file should go.

## Directory Map

| Content | Directory | Notes |
|---------|-----------|--------|
| Agent code, services, utils | `agents/` (with subdirs Main, Support, Core, etc.) | Never in agents root |
| Example/demo scripts | `examples/` | |
| Config, env, specs | `config/` | |
| Documentation | `docs/` | |
| User stories, flows | `User story/` | **Mandatory** for all story/flow files |
| Reports (any agent/task) | `reports/YYYY-MM-DD/` | Use **current** date dynamically |
| Postman collections | `postman/` | |
| Phoenix Java/TS code | `Phoenix/` | |

Project root for paths above is `Cursor-Project/` (e.g. `Cursor-Project/reports/...`, not workspace root `reports/...`).

## Reports

- Path: `Cursor-Project/reports/YYYY-MM-DD/` with **current** date: `datetime.now().strftime('%Y-%m-%d')`.
- Naming: `{AgentName}_{HHMM}.md` (e.g. `PhoenixExpert_1430.md`), `Summary_{HHMM}.md`, `BugValidation_[Name].md`.

## User Stories and Flows

- All user stories, flow diagrams, story docs → `User story/` at project root.
- Types: .txt, .drawio, .md (stories/flows).
- User story content (acceptance criteria, business rules, technical text) must be in English.

## Examples

- ✅ `agents/Main/new_agent.py` (in correct subdir)
- ✅ `Cursor-Project/reports/2026-01-31/Summary_1200.md`
- ✅ `User story/MY_USER_STORY.txt`
- ❌ New agent in `agents/new_agent.py` (must be in Main/Support/Core/etc.)
- ❌ Report in workspace root `reports/` instead of `Cursor-Project/reports/YYYY-MM-DD/`
- ❌ Story file in `docs/` or root instead of `User story/`

Full rules: `Cursor-Project/.cursor/rules/file_organization_rules.mdc`.
