# Tasks — <milestone>

> Source: docs/plan.md. One task = one small, testable change. A box is ticked only with evidence.

## M1 — <name>
- [ ] T1 — <task> · proof: `<command>` → <expected result>
- [ ] T2 — <task> `[parallel-ok]` · proof: `<command>` → <expected result>
- [ ] T3 — <task> `[high-risk]` · proof: `<command>` → <expected result>

> `[parallel-ok]` — can run alongside other tasks. `[high-risk]` — touches authentication, payments, deletion of data, security controls or an irreversible step: the Sonnet↔Opus review loop runs on this task alone before it is ticked (ADR-0006).

## Parked (გადადებული იდეები)
- <idea that appeared during build — not in scope now>

## Done log
- YYYY-MM-DD — T1 ✓ — evidence: <commit sha / decisive output line>
