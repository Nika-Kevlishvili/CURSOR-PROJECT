# PhoenixExpert — Opinion: separate folders per environment/branch

**Date:** 2026-03-31

**Topic:** User asked whether to split the Phoenix folder tree into per-environment (or per-branch) directories.

**Summary:** Reasonable for isolation and clarity, especially with multiple Phoenix repos; trade-offs are disk usage and sync surface area (`N environments × many repos`). Alternative: `git worktree` per repo for lighter duplication. Recommend 2–3 clear environment roots with documented default branches and sync policy if adopting full folder split.

**Agents:** PhoenixExpert
