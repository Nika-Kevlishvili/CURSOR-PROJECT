# CrossDependencyFinderAgent Report — shallow Confluence policy (2026-03-30 22:43)

## Change

User requested that cross-dependency discovery **not search Confluence deeply** — only **surface-level** information tied to the topic.

## Updates (English, Rule 0.7)

- `.cursor/skills/cross-dependency-finder/SKILL.md` — explicit **Confluence shallow only** rules: one search/CQL, snippets/titles, at most one `getPage` if clearly primary; no descendants/related-page walks; optional note when skipped.
- `.cursor/agents/cross-dependency-finder.md` — same policy in agent workflow.
- `.cursor/commands/cross-dependency-finder.md` — command step 4 aligned.
- `Cursor-Project/docs/CROSS_DEPENDENCY_FINDER_AGENT.md` — design table updated.
- `Cursor-Project/docs/CROSS_DEPENDENCY_TIMING_ITERATIONS.md` — Confluence bullet aligned with shallow policy.

## Principle

**Jira + git snapshot + codebase** remain primary; Confluence is **supplementary, shallow** context for CrossDependencyFinderAgent.

Agents involved: CrossDependencyFinderAgent, PhoenixExpert
