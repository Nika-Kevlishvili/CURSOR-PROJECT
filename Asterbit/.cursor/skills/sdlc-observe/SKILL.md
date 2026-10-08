---
name: sdlc-observe
description: Phase 9 — build AI observability: what the agent (the Cursor agent and/or the product agent) did, why, at what cost, and whether it worked — through structured logs, traces, dashboards and control-band alerts. Use when setting up logging or monitoring, when investigating what an agent did, or before release.
---
# Phase 9 — AI observability

Read `sdlc/design/ai-observability.md` first — it holds the event schema and the stack options.

## Already in the engine (ADR-0006, ADR-0008)
The builder — the Cursor agent itself — is observed from day one: `.cursor/hooks/cursor_adapter.py` writes one redacted line per tool call and per subagent (with the model it ran on) to `.cursor/logs/events-<date>.jsonl`, and `python3 sdlc/checks/agent_report.py [--date …] [--session …]` summarises calls, failures, changed files and the models that ran. Cost comes from Cursor's usage page. This phase adds observability for the **product** and the control bands.

## Steps
1. Agree with the owner which questions must be answerable later — for example: What did the agent do? Why? How much did it cost? Did it break anything? Who approved it?
2. Choose the stack from the design doc's options (the AskQuestion tool, recommendation first) and record an ADR.
3. Instrument: product-agent traces following the event schema in the design doc; for the builder, extend the event log above. Never log secrets or personal data.
4. Define control bands for 1–3 metrics (e.g. eval pass rate, cost per day, error rate) and the response tier for each breach: log → diagnose read-only → propose a fix as a new intent.
5. Drill: emit a known event and find it in the store; breach a band on purpose and see the alert.
6. `auditor` (gate: observability — review loop per AGENTS.md, loop id `gate-observe`) → owner approval → phase 9 `approved` + a gate-log line.

## Done when
Every question from step 1 is answerable from the store, logs are checked free of secrets and personal data, and at least one band alert was drilled end to end.
