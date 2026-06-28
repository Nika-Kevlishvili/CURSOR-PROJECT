---
name: phoenix-file-organization
description: Places new files in the correct project directories (docs, User story, reports, config, test_cases, .cursor). Use when creating or saving files or when the user asks where to put a file.
---

# Phoenix File Organization

Ensures new files are saved in the correct directories (Rule 31 under `workspace/file_organization_rules.mdc`). Reports go under **`Cursor-Project/reports/`** subfolders (`Chat reports/`, `HandsOff reports/`, `Feedback/`); stories and flows go in `Cursor-Project/User story/`.

## When to Apply

- User asks to create or save a file.
- User mentions user story, flow diagram, report, documentation, or config.
- Unsure where a new file should go.

## Directory Map

| Content | Directory | Notes |
|---------|-----------|--------|
| Cursor subagent specs | **`.cursor/agents/*.md`** | Workspace root `.cursor/` |
| Cursor rules | **`.cursor/rules/**/*.mdc`** | Thematic folders: `main/`, `safety/`, etc. |
| Cursor skills / commands | **`.cursor/skills/`**, **`.cursor/commands/`** | |
| Config, templates, swagger | `Cursor-Project/config/` | Templates: `config/template/` |
| Documentation | `Cursor-Project/docs/` | |
| User stories, flows | **`Cursor-Project/User story/`** | **Mandatory** for story/flow files |
| Reports | **`Cursor-Project/reports/`** → `Chat reports/`, `HandsOff reports/`, `Feedback/` | See `reports/README.md` |
| Test cases | **`Cursor-Project/test_cases/Backend/<Topic>.md`** always; **`Frontend/<Topic>.md`** when TC-FRONTEND scope includes UI | Per `test_cases_structure.mdc` |
| Phoenix code | `Cursor-Project/Phoenix/**` | AI: read-only (Rule 0.8) |

**Optional Python agent package:** If `Cursor-Project/agents/` is reintroduced later, organize per project owner — it is **not** present in this workspace now.

## Reports

- **Chat reports:** only when **`/report`** or explicit save — e.g. `Summary_{HHMM}.md`; `BugValidation_*.md` only if the user asks to persist after bug validation (Rule 32 default = chat only).
- **HandsOff reports:** only `{JIRA_KEY}.md` (Rule 37).
- **Feedback:** when **`/feedback`** runs or the user explicitly asks to save feedback under **Feedback** (e.g. `Feedback_{HHMM}.md`).

## User Stories and Flows

- All user stories, flow diagrams, story docs → **`Cursor-Project/User story/`**.
- Persisted story content in **English** (Rule 0.7).

## Examples

- ✅ `Cursor-Project/reports/Chat reports/2026/april/12/Summary_1200.md` (per `reports/README.md`; day = real save date)
- ✅ `Cursor-Project/reports/Feedback/2026/april/12/Feedback_1430.md` (via **`/feedback`** or explicit save-feedback request)
- ✅ `Cursor-Project/User story/MY_USER_STORY.txt`
- ✅ `.cursor/agents/phoenix-qa.md`
- ❌ Report in workspace-only `reports/` instead of `Cursor-Project/reports/<area>/YYYY/<english-month>/<DD>/` per **`Cursor-Project/reports/README.md`**

Full rules: `.cursor/rules/workspace/file_organization_rules.mdc`.
