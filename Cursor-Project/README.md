# Cursor Project

Workspace with **Cursor rules/subagents/skills**, Phoenix Java projects (read-only), EnergoTS Playwright tests, Postman collections, and supporting tooling. The Python `agents/` package is **not** shipped — see `docs/HISTORICAL_PYTHON_AGENTS_PACKAGE.md`.

## Project Structure

```
Cursor-Project/
├── .cursor/               # Cursor IDE config (agents, rules, skills, commands, hooks)
├── config/                # Swagger specs, Jira/Confluence scripts, Playwright generation, templates
├── cross_dependencies/    # Cross-dependency-finder JSON outputs (date_JIRA-KEY.json)
├── Cursor Setup/          # MCP config template, env.example for new machine setup
├── docs/                  # Active documentation (archived Python-era docs in docs/_archive/)
├── EnergoTS/              # Playwright test project (submodule, locked to cursor branch)
├── menu_data/             # Phoenix UI menu structure snapshots
├── Phoenix/               # Phoenix Java projects (submodules, read-only — Rule 0.8 Tier A)
├── postman/               # Postman collections
├── reports/               # Chat reports, Chat reports (ScopedPlaywright), Feedback (see reports/README.md)
├── scripts/               # Validation, git hooks
├── test_cases/            # Backend/ and Frontend/ test case files
└── User story/            # User stories and flow documentation
```

## Quick Start

### New machine (recommended)

1. Clone this repo from GitHub; open it in Cursor.
2. Install **Node.js/npm**, **PowerShell** extension, and **Playwright Test (Modified)** manually.
3. Run:

```powershell
.\.cursor\commands\setup-new-machine.ps1
```

The script clones all submodules, copies `.env` from `Cursor Setup/env.example`, writes MCP config to `%USERPROFILE%\.cursor\mcp.json`, installs marketplace extensions, and verifies layout.

Details: [`docs/QUICK_START.md`](docs/QUICK_START.md) · [`.cursor/commands/setup-new-machine.md`](../.cursor/commands/setup-new-machine.md) · templates in `Cursor Setup/`.

### Git hooks (recommended)

```powershell
git config core.hooksPath Cursor-Project/scripts/git-hooks
```

This enables:
- **commit-msg** — enforces conventional commit format (`feat:`, `fix:`, `docs:`, etc.)
- **pre-commit** — runs `validate-cursor-rules.ps1` when `.cursor/rules/**/*.mdc` files change

### Phoenix submodules

URLs in `.gitmodules` use `git.domain.internal`. Prefer `setup-new-machine.ps1` (optional `-GitLabToken`). Manual alternative:

```bash
git submodule update --init --recursive
```

### Java/Gradle (Phoenix)

```bash
cd Phoenix/phoenix-core-lib
./gradlew build
```

Requires Java 17+. Gradle wrapper included.

## Documentation

- **[Agent / subagent map](docs/AGENT_SUBAGENT_MAP.md)** — canonical paths for all subagents
- [Cursor subagents](docs/CURSOR_SUBAGENTS.md) — how `.cursor/agents/` works
- [Commands reference](docs/COMMANDS_REFERENCE.md)
- [Rules loading](docs/RULES_LOADING_SYSTEM.md)
- [Workspace patterns](docs/WORKSPACE_PATTERNS.md)
- [Agents model (current vs historical)](docs/AGENTS_COMPARISON_AND_ALIGNMENT.md)
- [Phoenix project analysis](docs/PHOENIX_PROJECT_ANALYSIS.md)
- [Historical Python agents docs](docs/HISTORICAL_PYTHON_AGENTS_PACKAGE.md) — index to `docs/_archive/`

## Important Notes

1. **Secrets**: API keys, tokens, passwords belong in environment variables or MCP config — do NOT commit `.env` to Git
2. **Phoenix is read-only**: Cursor AI must never modify files under `Phoenix/` (Rule 0.8 Tier A)
3. **EnergoTS branch lock**: EnergoTS stays on `cursor` branch only (Rule ENERGOTS.0)

---

**Last Updated**: 2026-07-27
