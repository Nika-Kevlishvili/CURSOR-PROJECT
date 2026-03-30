# Summary — Project rules review (user request: explain rules in detail)

**Date:** 2026-03-28  
**Scope:** Reviewed all 13 `.mdc` files under `.cursor/rules/` and produced a structured explanation for the user (response in English per Rule 0.7).

**Update 2026-03-28:** Rules were reorganized into thematic subfolders (`main/`, `safety/`, `agents/`, `workflows/`, `workspace/`, `integrations/`). Index: `.cursor/rules/main/phoenix.mdc`; map: `.cursor/rules/README.md`. See `Summary_RulesThematicReorg.md`.

## Files covered

- core_rules.mdc, agent_rules.mdc, safety_rules.mdc, workflow_rules.mdc  
- test_cases_structure.mdc, handsoff_playwright_report.mdc  
- git_sync_workflow.mdc, energots_branch_lock.mdc  
- database_workflow.mdc, production_data_reader.mdc  
- file_organization_rules.mdc, jira_bug_agent.mdc, phoenix.mdc (index)

## Key takeaways

1. Core rules 0.x: agents disclosure, English docs, reports under `Cursor-Project/reports/YYYY-MM-DD/`, Phoenix read-only for AI edits, EnergoTS tests only via EnergoTSTestAgent workflow.  
2. GitLab/Confluence read-only; Confluence edit tools forbidden.  
3. Specialized workflows: BugFinder (32), test cases with cross-deps (35/35a), Playwright run on EnergoTS `cursor` only (36), HandsOff (37), production DB reader (PDR.0), Jira bugs Experiments only (JIRA.0).

**Agents involved:** PhoenixExpert (synthesis), Reporting (Rule 0.6 file write)

---

## Addendum 2026-03-28 — Duplicate / overlapping rules

User asked whether rules are repeated. **Answer:** Yes, many policies appear in multiple `.mdc` files (reinforcement + thematic self-containment). `phoenix.mdc` duplicates summaries as an index. No intentional contradictions identified; see chat for the overlap table.
