# Hooks, agents, and skills audit (2026-03-28)

## Hooks (`.cursor/hooks/` + `.cursor/hooks.json`)

| Script | Registered in `hooks.json` | Notes |
|--------|----------------------------|--------|
| `block-phoenix-code-requests.ps1` | `beforeSubmitPrompt` | OK |
| `block-jira-phoenix-delivery.ps1` | `beforeSubmitPrompt` | OK; cites `jira_bug_agent.mdc` path correctly |
| `block-energots-branch-requests.ps1` | **`beforeSubmitPrompt`** (added this audit) | Was **missing** from `hooks.json` while `energots_branch_lock.mdc` claimed it was active |
| `warn-phoenix-code-edit.ps1` | `afterFileEdit` | OK |
| `block-confluence-write.ps1` | `beforeMCPExecution` | OK |
| `control-database-write.ps1` | `beforeMCPExecution` | OK |
| `block-energots-branch-switch.ps1` | **`beforeShellExecution`** (added this audit; runs **before** `control-git-push`) | Was **missing** from `hooks.json` |
| `control-git-push.ps1` | `beforeShellExecution` | OK |
| **`protect-phoenix-code.ps1`** | **Not registered** | Orphan relative to current `hooks.json`; `beforeFileEdit` is not used in this config. Rely on `block-phoenix-code-requests` + `warn-phoenix-code-edit` + Rule 0.8. Optional: wire if Cursor adds a matching hook event. |

**Change applied:** Updated **`.cursor/hooks.json`** so EnergoTS hooks match **Rule ENERGOTS.0** documentation.

## Agents (`.cursor/agents/*.md`)

- **Count:** 16 agent specs + `README.md`.
- **Rule path references:** No obsolete flat `.cursor/rules/<file>.mdc` (without thematic folder) found in agent bodies (prior grep).
- **README fixes:** Added **hands-off**, **production-data-reader**, **playwright-test-validator**; corrected **bug-validator** description (in-chat Rule 32, not Python BugFinderAgent); replaced outdated **`Cursor-Project/agents/`** Python tree claim with **Rule 34** / removed package note; added **`commands/`** to thematic rules list in README.

**Optional drift (not broken links):** Several agent files still mention `IntegrationService` / Python imports — align with **`main/core_rules.mdc` Rule 0.3** when editing those docs.

## Skills (`.cursor/skills/**/SKILL.md`)

- **Count:** 11 skills with YAML `name` + `description` frontmatter; all present.
- **Rule references:** Use thematic paths (e.g. `integrations/…`, `workflows/…`, `commands/commands_rules.mdc`) — consistent with rules layout.
- **README fixes:** Added **energo-ts-run** row; expanded **phoenix-commands** row; **Source** section now points to **`.cursor/agents/`** instead of removed `Cursor-Project/agents/`.

**Mapping gap (informational):** There is no dedicated skill for **environment-access**; the **database-query** / **postman** workflows are covered by agents + `phoenix-database` / command docs — acceptable.

## Conclusion

- **Critical fix:** EnergoTS branch hooks are now **registered** in **`hooks.json`**, matching **`energots_branch_lock.mdc`**.
- **Docs:** Agent and skill READMEs updated for removed Python package and missing agent rows.
- **Follow-up:** Consider removing or registering **`protect-phoenix-code.ps1`**; optionally soften **phoenix-agent-workflow** skill text (still mentions AgentRouter / IntegrationService as historical pattern).

Agents involved: None (direct audit and edits)
