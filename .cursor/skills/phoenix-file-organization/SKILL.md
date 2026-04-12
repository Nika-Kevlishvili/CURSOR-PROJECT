---
name: phoenix-file-organization
description: Places new files in the correct project directories (docs, User story, reports, config, postman, test_cases, .cursor). Use when creating or saving files or when the user asks where to put a file.
---

# Phoenix File Organization

Ensures new files are saved in the correct directories (Rule 31 under `workspace/file_organization_rules.mdc`). Reports go under **`Cursor-Project/reports/`** subfolders (`Chat reports/`, `HandsOff reports/`, `Feedback/`); stories and flows go in `User story/`.

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
| Example/demo scripts | `Cursor-Project/examples/` | |
| Config, templates, swagger | `Cursor-Project/config/` | Templates: `config/template/` |
| Documentation | `Cursor-Project/docs/` | |
| User stories, flows | **`User story/`** (project root) | **Mandatory** for story/flow files |
| Reports | **`Cursor-Project/reports/`** → `Chat reports/`, `HandsOff reports/`, `Feedback/` | See `reports/README.md` |
| Postman | `Cursor-Project/postman/` | |
| Test cases | **`Cursor-Project/test_cases/Backend/<Topic>.md`** + **`test_cases/Frontend/<Topic>.md`** (two-folder layout) | Per `test_cases_structure.mdc` |
| Phoenix code | `Cursor-Project/Phoenix/**` | AI: read-only (Rule 0.8) |

**Optional Python agent package:** If `Cursor-Project/agents/` is reintroduced later, organize per project owner — it is **not** present in this workspace now.

## Reports

- **Chat reports:** optional session files — `{AgentName}_{HHMM}.md`, `Summary_{HHMM}.md`, `BugValidation_[Name].md`.
- **HandsOff reports:** only `{JIRA_KEY}.md` (Rule 37).
- **Feedback:** only when the user asks to save feedback here.

## User Stories and Flows

- All user stories, flow diagrams, story docs → **`User story/`** at project root.
- Persisted story content in **English** (Rule 0.7).

## Examples

- ✅ `Cursor-Project/reports/Chat reports/2026/april/12/Summary_1200.md` (per `reports/README.md`; day = real save date)
- ✅ `User story/MY_USER_STORY.txt`
- ✅ `.cursor/agents/phoenix-qa.md`
- ❌ Report in workspace-only `reports/` instead of `Cursor-Project/reports/<area>/YYYY/<english-month>/<DD>/` per **`Cursor-Project/reports/README.md`**

Full rules: `.cursor/rules/workspace/file_organization_rules.mdc`.
