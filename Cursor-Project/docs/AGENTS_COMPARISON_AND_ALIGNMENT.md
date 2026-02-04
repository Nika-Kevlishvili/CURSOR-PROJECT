# Agents Comparison: Cursor Subagents vs Python Agents — Alignment with Project Idea

This document compares the two "agents" layers in the project and evaluates how well they align with the project's idea (Phoenix multi-agent system, rules, consultation, reporting, read-only).

---

## 1. Two Layers of "Agents"

| Aspect | **Cursor Subagents** (`.cursor/agents/*.md`) | **Python Agents** (`Cursor-Project/agents/`) |
|--------|-----------------------------------------------|-----------------------------------------------|
| **What** | Declarative instructions for Cursor's AI when it delegates a task | Executable Python code: logic, API calls, MCP, reporting |
| **Where** | Workspace root `.cursor/agents/` | `Cursor-Project/agents/` (Main, Support, Core, Services, Adapters, Utils) |
| **Format** | Markdown + YAML frontmatter (name, description) | Python modules, classes, `get_*_agent()` |
| **Who runs them** | Cursor's main agent delegates to a subagent (separate context) | Code: `get_phoenix_expert()`, `get_bug_finder_agent()`, etc.; or AgentRouter routes and invokes them |
| **Role** | Tell the delegated AI *what to do* and *how* (rules, workflow, output format) | Actually *do* it: call Confluence MCP, codebase search, run tests, save reports |

**Summary:** Subagents are "prompts + rules" for delegation; Python agents are the implementation. They are meant to work together: when Cursor delegates (e.g. "validate this bug"), the subagent instructions say "follow BugFinderAgent workflow and rules"; when running inside the project, Python code can call `get_bug_finder_agent()` and the same rules apply.

---

## 2. Mapping: Subagent ↔ Python Agent / Service / Rule

| # | Cursor Subagent (`.cursor/agents/`) | Python / Rule (`Cursor-Project/agents/` or rules) | Match |
|---|-------------------------------------|----------------------------------------------------|-------|
| 1 | phoenix-qa.md | PhoenixExpert (Main) | ✅ 1:1 |
| 2 | bug-validator.md | BugFinderAgent (Main) + Rule 32 | ✅ 1:1 |
| 3 | test-runner.md | TestAgent (Main) + Rule 8, 17 | ✅ 1:1 |
| 4 | test-case-generator.md | TestCaseGeneratorAgent (Main) | ✅ 1:1 |
| 5 | report-generator.md | ReportingService (Services) + Rule 0.6 | ✅ 1:1 |
| 6 | database-query.md | database_workflow.mdc (Rule DB.0–DB.5) | ✅ Workflow ↔ rule |
| 7 | git-sync.md | GitLabUpdateAgent (Support) + git_sync_workflow.mdc | ✅ 1:1 + rule |
| 8 | environment-access.md | EnvironmentAccessAgent (Support) + Rule 10 | ✅ 1:1 |
| 9 | postman-collection.md | PostmanCollectionGenerator (Services) + Rule 8, 17 | ✅ 1:1 |

**Python side with no subagent (by design):**

- **Core:** AgentRegistry, AgentRouter, IntegrationService, GlobalRules — infrastructure; not delegation targets.
- **Adapters:** Implement Agent interface for the main agents; not separate user-facing roles.
- **Utils:** rules_loader, logger_utils, reporting_helper, ai_response_logger — shared utilities.

So: every *user-facing* Python agent or workflow has a corresponding Cursor subagent. The project idea is reflected in both layers.

---

## 3. Alignment with Project Idea

The project idea (from rules and docs) can be summarized as:

- **Phoenix as single source of truth:** Backend, Confluence, codebase — one coherent system.
- **Strict roles:** PhoenixExpert for Q&A, BugFinderAgent for bug validation, TestAgent for tests, etc.
- **Mandatory flow:** IntegrationService before task, PhoenixExpert consultation where needed, report after task (Rule 0.6).
- **Safety:** Read-only for GitLab/Confluence/code where specified; no code edits in Phoenix unless explicitly allowed; credentials never in logs.

### 3.1 How well the two layers support this

| Project idea | Cursor Subagents | Python Agents | Together |
|--------------|-------------------|---------------|----------|
| Phoenix Q&A only via PhoenixExpert | phoenix-qa.md enforces Confluence + codebase, READ-ONLY, "Expert: PhoenixExpert" | PhoenixExpert implements Q&A logic | ✅ Same role and constraints in both |
| Bug validation only via BugFinderAgent (Rule 32) | bug-validator.md enforces Confluence → code → report, READ-ONLY | BugFinderAgent implements workflow | ✅ Same workflow in both |
| Consultation before task (Rule 8) | test-runner, postman-collection, etc. say "consult PhoenixExpert first" | AgentRouter/Registry and agents call consult_best_agent() | ✅ Same rule in both |
| IntegrationService before task (Rule 11) | Subagent prompts say "call IntegrationService.update_before_task()" | Python agents call it in code | ✅ Same requirement in both |
| Report after task (Rule 0.6) | report-generator.md + others say "save report + summary" | ReportingService.save_agent_report(), save_summary_report() | ✅ Same outcome in both |
| Read-only GitLab/Confluence/code where required | git-sync (no push), phoenix-qa/bug-validator (no code edit) | Hooks + safety_rules + core_rules | ✅ Subagents state it; Python + hooks enforce it |
| Correct env for DB (Dev/Test/Prod) | database-query.md says "use exact env user asked" | database_workflow.mdc + MCP | ✅ Same rule in both |

**Conclusion:** The two layers are **aligned**: subagents describe the same roles, workflows, and constraints that the Python agents and rules implement. Cursor chat delegation (subagents) and in-project execution (Python agents) both follow the same project idea.

### 3.2 Gaps and nuances

- **Execution path:** When the user works *inside Cursor chat only*, the subagent runs in Cursor's context and may not execute Python (e.g. `get_bug_finder_agent()`). The subagent then instructs "use MCP + codebase search" and "follow Rule 32"; the *behavior* matches BugFinderAgent even if the Python class is not called. When the user or automation runs *Python* (e.g. script or AgentRouter), the Python agents run. So:
  - **Chat-only:** Subagents + rules + skills drive behavior; Python may not run.
  - **Python run:** Python agents + rules drive behavior; subagents are the "same role" description for when Cursor delegates.
- **Skills:** `.cursor/skills/phoenix-*` tell the main agent *when* to use which workflow (e.g. phoenix-bug-validation, phoenix-reporting). Subagents are *who* does the work when delegated. So: Skills = "when/what"; Subagents = "how/who" for that task. Together they fit the project idea.
- **Core/Adapters/Utils:** Intentionally not subagents; they orchestrate and support the main agents. No gap.

---

## 4. Summary Table: "How much does this match the project idea?"

| Dimension | Match | Note |
|-----------|--------|-----|
| **1:1 coverage of user-facing agents** | ✅ 100% | Every Main/Support agent and Reporting/Postman has a subagent; DB has a workflow subagent. |
| **Same rules in both layers** | ✅ High | Rule 0.2, 0.6, 8, 11, 32, DB.0–DB.5, read-only, consultation, reporting are reflected in subagent prompts and in Python/rules. |
| **Same workflows** | ✅ High | Bug validation (Confluence → code → report), Phoenix Q&A (Confluence + codebase), test run (consult → run), report (save agent + summary), git sync (fetch/update/checkout only). |
| **Safety (read-only, no credentials)** | ✅ High | Subagents say READ-ONLY where needed; Python + hooks + safety_rules enforce it. |
| **Structure (Main/Support/Services)** | ✅ Reflected | Subagents don't duplicate folder structure but map cleanly: Main agents → phoenix-qa, bug-validator, test-runner, test-case-generator; Support → git-sync, environment-access; Services → report-generator, postman-collection; workflow → database-query. |

**Overall:** The two agent layers **match each other and the project idea very well**. Declarative subagents (`.cursor/agents/*.md`) and programmatic agents (`Cursor-Project/agents/`) describe and implement the same Phoenix-centric, rule-driven, consultation-and-reporting workflow; gaps are minor (execution path in chat vs Python) and by design.

---

## 5. Recommendation

- **Keep both layers** and keep the current mapping.
- **When adding a new user-facing Python agent:** add a corresponding `.cursor/agents/<name>.md` subagent and update `.cursor/agents/README.md` and `docs/CURSOR_SUBAGENTS.md`.
- **When changing a rule (e.g. Rule 32 or DB.0):** update both the `.mdc` rule and the relevant subagent prompt so Cursor chat and Python execution stay aligned.

This keeps "Phoenix multi-agent system with strict rules, consultation, and reporting" consistent whether the user works via Cursor chat (subagents + skills + rules) or via Python (agents + rules + hooks).
