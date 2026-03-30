# Cross-dependency timing: comparing iterations

**Workflow pattern:** [`CROSS_DEPENDENCY_WORK_PATTERN.md`](CROSS_DEPENDENCY_WORK_PATTERN.md) — **no** local merge/git for cross-dep (Rule 35a).

If you instrument runs with UTC marker files (`t0`…`t4`), phases measure wall-clock for:

1. Cross-dependency (Jira + codebase + shallow Confluence)
2. Test-case `.md` authoring
3. README / index updates
4. Reporting (Rule 0.6)

Older notes that referenced **`CrossDependency_GitSnapshot.ps1`** are **obsolete**; that script was removed from the cross-dependency pattern.
