# Cross-dependency work pattern (saved reference)

**Purpose:** Stable record of how **cross-dependency discovery** and **test-case handoff (Rule 35)** run in this workspace: **Jira + codebase + shallow Confluence** — **no** local merge/git archaeology on disk (e.g. `D:` clones are not a mandatory source of truth).

**Rules:** `.cursor/rules/workflows/workflow_rules.mdc` — **Rule 35** and **Rule 35a** (rewritten: no local merge/git for cross-dep).

---

## 1. When this applies

- **Cross-dependencies**, “what could break”, or **test cases** (Rule 35: cross-dependency-finder **before** test-case-generator).
- User gives a **Jira/bug/task key** → **Rule 35a**: anchor on **Jira MCP + codebase** (+ shallow Confluence); **not** `git log` / merge lists by default.

---

## 2. What we removed from the pattern

- **`CrossDependency_GitSnapshot.ps1`** (removed) and **`technical_details.git_snapshot`** / per-ticket JSON in `cross_dependencies/cache/` as part of the workflow.
- **Merge-first**, **conditional sync**, and **merge-derived technical_details** as mandatory steps for CrossDependencyFinderAgent.

Optional **GitLab MR (read-only)** only if the **user explicitly** asks.

---

## 3. Confluence — shallow only (cross-dependency)

- **Primary evidence:** **Jira + codebase**; Confluence is **light** (one search/CQL, snippets/titles; optional single page if clearly the owning spec).
- **Do not:** deep wiki walks, descendant trees, many pages.

---

## 4. Output and handoff to test cases

Structured **`cross_dependency_data`**:

- `scope`, `entry_points`, `upstream`, `downstream`, `shared`, `data_entities`, `integration_points`, **`what_could_break`**, **`technical_details`** (Jira + codebase notes — not mandatory merge/MR lists).

Save optionally to **`Cursor-Project/cross_dependencies/YYYY-MM-DD_<scope_slug>.json`**.

---

## 5. Source files to preserve

| Role | Path |
|------|------|
| Skill | `.cursor/skills/cross-dependency-finder/SKILL.md` |
| Subagent | `.cursor/agents/cross-dependency-finder.md` |
| Command | `.cursor/commands/cross-dependency-finder.md` |
| Design | `Cursor-Project/docs/CROSS_DEPENDENCY_FINDER_AGENT.md` |
| Timing note (optional) | `Cursor-Project/docs/CROSS_DEPENDENCY_TIMING_ITERATIONS.md` |
| **This pattern** | `Cursor-Project/docs/CROSS_DEPENDENCY_WORK_PATTERN.md` |

`cross_dependencies/cache/` may remain empty or for **non-git** optional artifacts only.

---

## 6. READ-ONLY and safety

- **Phoenix** code: no AI edits (Rule 0.8).
- **GitLab / Confluence:** read-only (Rule 1).
- Persisted project text in **English** (Rule 0.7).
