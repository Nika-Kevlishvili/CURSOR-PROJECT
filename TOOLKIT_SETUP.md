# Cursor QA Toolkit — Setup & Adaptation Guide

A portable **Cursor QA agent framework**: rules, subagents, skills, hooks, commands,
docs, scripts, and config templates. Drop it into any project to get the same
QA / test-case / bug-validation / HandsOff workflows used in the source workspace.

This branch (`qa-cursor-toolkit`) is a **cleaned template** — all real project data,
internal hosts, dependency blobs, and source submodules have been removed. You wire it
to *your* project during setup.

---

## 1. What is included vs removed

**Included (the reusable framework):**

| Path | What it is |
|------|------------|
| `.cursor/rules/**` | Rule set (core, safety, workflows, scoring, integrations) |
| `.cursor/agents/**` | Subagent specifications |
| `.cursor/skills/**` | Skill procedures (SKILL.md) |
| `.cursor/commands/**` | Operational command docs + scripts |
| `.cursor/hooks/**`, `.cursor/hooks.json` | Guard hooks (path protection, env gate, confidence format) |
| `Cursor-Project/docs/**` | Framework documentation |
| `Cursor-Project/scripts/**` | Validation + git hooks |
| `Cursor-Project/config/{template,playwright,playwright_generation,confidence}` | Templates, playwright instructions pack, scoring matrix |
| `Cursor-Project/config/{jira,confluence,slack,swagger}` | REST/MCP helper scripts + READMEs (no cached data) |
| `Cursor Setup/` | MCP config + `env.example` for new-machine setup |
| `Cursor-Project/{reports,test_cases,cross_dependencies,User story,menu_data}` | Empty skeletons + READMEs |

**Removed for portability (you add your own):**

- Source submodules (`Cursor-Project/Phoenix/**`, `Cursor-Project/EnergoTS`) and `.gitmodules`.
- All real test cases, reports, cross-dependency outputs, user stories, menu snapshots.
- All cached Jira/Confluence/Swagger/diagram data and bug-validation patterns.
- Dependency / archive blobs (`__MACOSX`, slack `mcp-file-upload` node_modules).
- Project-specific example scripts.
- Real internal hosts/IPs (replaced with `<...>` placeholders).

---

## 2. Quick setup on a new project

1. **Copy** `.cursor/`, `Cursor-Project/`, and `Cursor Setup/` into your project root (or open this branch as the workspace).
2. **MCP & env:** follow `Cursor Setup/` — set up `env.example` → real env, and MCP servers (Jira, Confluence, Slack, PostgreSQL per environment) for *your* systems.
3. **Swagger hosts:** edit `Cursor-Project/config/swagger/environments.json` — replace `<DEV_HOST>`/`<PORT>` etc. with your API hosts.
4. **Git host (optional submodules):** if you want source repos under `Cursor-Project/`, recreate `.gitmodules` with your hosts and `git submodule add` them. Otherwise leave them out.
5. **Git hooks (recommended):** `git config core.hooksPath Cursor-Project/scripts/git-hooks`.
6. **Validate rules:** run `Cursor-Project/scripts/validate-cursor-rules.ps1`.

---

## 3. Reference domain note (IMPORTANT)

The rules/agents/skills still use **Phoenix** (a Java backend) and **EnergoTS**
(a Playwright suite) as the **reference implementation domain**, plus example
ticket prefixes (`PDT-`, `PHN-`) and Atlassian hosts. This is intentional: the
concrete examples make the methodology readable.

If you only need the *workflows*, you can use the toolkit as-is and simply point the
MCP servers / hosts at your project — the Phoenix references act as worked examples.

If you want a **fully domain-agnostic** toolkit (no Phoenix/EnergoTS naming), perform
the rename pass in section 4.

---

## 4. Optional: full domain-agnostic rename pass

This is a deeper, interdependent refactor (~160 files still mention the reference
domain). Do it deliberately and verify cross-references after.

### 4.1 File / folder renames (`git mv`)

| From | Suggested generic name |
|------|------------------------|
| `.cursor/rules/main/phoenix.mdc` | `project-expert.mdc` |
| `.cursor/rules/integrations/phoenix_branch_switching.mdc` | `repo_branch_switching.mdc` |
| `.cursor/rules/integrations/energots_branch_lock.mdc` | `e2e_branch_lock.mdc` |
| `.cursor/skills/phoenix-*` | `app-*` / `project-*` |
| `.cursor/skills/energo-ts-*` | `e2e-*` |
| `.cursor/agents/phoenix-qa.md` | `app-qa.md` |
| `.cursor/agents/energo-ts-*.md` | `e2e-*.md` |
| `.cursor/commands/switch-phoenix-branches.*` | `switch-repo-branches.*` |
| `.cursor/hooks/protect-phoenix-code.ps1`, `warn-phoenix-code-edit.ps1` | `protect-source-code.ps1`, `warn-source-code-edit.ps1` |
| `.cursor/hooks/protect-energots-writes.ps1`, `block-energots-*.ps1` | `protect-e2e-writes.ps1`, `block-e2e-*.ps1` |
| `.cursor/hooks/block-jira-phoenix-delivery.ps1` | `block-jira-delivery-board.ps1` |

### 4.2 Content token replacement (review each — do not blind-replace)

| Token | Replace with |
|-------|--------------|
| `Phoenix`, `PhoenixExpert` | `<App>`, `<App>Expert` |
| `EnergoTS` | `<E2E>` |
| `PDT-`, `PHN-` | `<TICKET->` |
| `git.domain.internal` | `<YOUR_GIT_HOST>` |
| `asterbit.atlassian.net` | `<YOUR_CONFLUENCE_HOST>` |
| `oppa-support.atlassian.net` | `<YOUR_JIRA_HOST>` |
| `PostgreSQLDev/Dev2/Test/PreProd/Prod/experiments` | your MCP server names |

### 4.3 After renaming — fix all cross-references

These files index or reference the names above and **must be updated together**:

- `.cursor/hooks.json` (hook script paths)
- `Cursor-Project/docs/RULES_CANONICAL_INDEX.md`
- `Cursor-Project/docs/AGENT_SUBAGENT_MAP.md`
- `.cursor/README.md`, `.cursor/agents/README.md`, `.cursor/skills/README.md`, `.cursor/rules/README.md`
- Any rule/agent/skill that points to a renamed path.

### 4.4 Verify

```powershell
git ls-files | Select-String -Pattern "phoenix|energo"          # expect: none (names)
# spot-check remaining content tokens:
git grep -i -l "phoenix|energots|PDT-|git.domain.internal"
```

Then re-run `Cursor-Project/scripts/validate-cursor-rules.ps1`.
