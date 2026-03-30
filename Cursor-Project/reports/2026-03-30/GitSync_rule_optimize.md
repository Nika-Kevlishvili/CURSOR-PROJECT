# Git sync workflow rule — optimization

- **File:** `.cursor/rules/integrations/git_sync_workflow.mdc`
- **Before:** ~1226 lines (duplicated PowerShell/Bash blocks, long troubleshooting, token embedded).
- **After:** ~118 lines; procedural spec + short credential snippets only.
- **alwaysApply:** set to `false` with `description` for agent/request routing (matches prior rules review).
- **Security:** GitLab PAT removed from the rule; use **`GIT_READONLY_TOKEN`** in the environment. If the old token was ever committed or shared, rotate it in GitLab.
- **Agent:** `.cursor/agents/git-sync.md` updated to reference env var.

Agents involved: None (direct tool usage)
