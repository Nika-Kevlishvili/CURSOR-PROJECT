# Summary — Cursor subagent Background (all agents enabled)

**Date:** 2026-04-04  
**Topic:** User asked what happens if Background is enabled for all agents.

## Answer (stored for audit)

- Background runs subagent work less synchronously with the main chat; more tasks may overlap.
- Trade-offs: less blocking, but potentially interleaved results, harder linear follow-up, parent workflows still need ordered handoffs.
- Resource usage and API/model calls may increase with parallel background runs.
- Background does not change read-only or safety rules by itself.

## Agents involved

None (direct explanation to user in chat; Georgian).
