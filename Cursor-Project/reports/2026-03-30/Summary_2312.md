# Summary — cross-dependency without local merge/git (2026-03-30 23:12)

Cross-dependency discovery no longer mandates **local git merge history**, **CrossDependency_GitSnapshot.ps1**, or **conditional sync** for a Jira key. Rule **35a** now anchors on **Jira MCP + Phoenix codebase (READ-ONLY) + shallow Confluence**. GitLab MR review remains **optional** and only when the user explicitly requests it.

Agents involved: PhoenixExpert
