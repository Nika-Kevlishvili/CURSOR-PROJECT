---
name: jira-evidence
description: Complete Jira issue reads including custom fields, links, attachments, linked Confluence.
---

# Jira evidence

1. MCP get issue with expand names when possible.
2. On failure → `config/scripts/get-jira-issue-rest.ps1` + disclose REST.
3. Process issuelinks, attachments metadata, comments as needed.
4. If Confluence URLs present → read them same pass (Rule 44).
