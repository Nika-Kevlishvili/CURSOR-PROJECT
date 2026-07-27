---
name: bug-validation
description: Validate bug reports with evidence (Rule 32). READ-ONLY. No automatic TC pipeline.
---

# Bug validation

1. Resolve environment via environment-resolver + `project-config.json` (ask if empty).
2. Load Jira issue (MCP → REST). Read linked Confluence same pass.
3. Optional OpenAPI under `config/swagger/`.
4. Search code under `codeRoot`.
5. Optional DB (same env, read-only).
6. Verdict in chat: VALID | NOT VALID | NEEDS CLARIFICATION | NEEDS APPROVAL (or equivalent clear labels).
7. Findings + Confidence + Agents involved. Disk report only if user asks.
