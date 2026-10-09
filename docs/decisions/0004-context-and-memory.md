---
id: "0004"
title: Context and memory layers
status: accepted
scope: engine
date: 2026-10-06
deciders: owner (interview, 2026-10-06), Claude (drafted)
supersedes: —
superseded_by: —
review_by: when memory/ holds 150+ archived files, or Claude fails twice to find a past fact
tags: [memory, context, harness]
---
# 0004 — Context and memory layers

## Context (კონტექსტი)
Owner's requirements (2026-10-06): compaction at 60–70%; an automatic handoff before every compaction, following agreed principles; the 4–5 handoffs of a long session merged instead of piling up; a session summary at the end of every session, saved; long-term protection against context overflow through memory layers (graph — e.g. Obsidian; RAG; episodic) that also reveal decisions the owner changed over time. Design and measurements: `sdlc/design/memory-and-context.md`.

## Options considered (განხილული ვარიანტები)
Asked in the owner interview on 2026-10-06:
- Compaction threshold: 60% / **65%** / 70% of the 1M-token window.
- Graph layer: **Obsidian vault** / Obsidian + Graphify / none yet.
- Retrieval (RAG): **grep now, QMD at a measured trigger** / QMD now / MemPalace (repair) / claude-mem.
- Changed-decision detection: **ADR + git + wrap-time check** / the same + Graphiti later / MemPalace knowledge graph.
Trade-offs per option: `sdlc/design/memory-and-context.md` §8.

## Decision (გადაწყვეტილება)
1. **Compaction at 65%:** `autoCompactWindow: 650000` in the project's `.claude/settings.json` (overrides the global 620000 for this repo).
2. **Handoff = the compaction summary.** Its shape comes from CLAUDE.md "Compact instructions" (documented mechanism) plus the existing global PreCompact instructions. A project PostCompact hook writes `compact_summary` to `memory/episodic/handoffs/<session>.md` — one rolling file per session; the previous version moves to `memory/archive/handoffs/`. A SessionStart hook (sources `compact` and `resume`) injects `memory/now.md` + the current handoff.
3. **Session summary:** `/sdlc-wrap` at the end of every session. If the previous session has no summary, `/sdlc-status` (and the SessionStart hook) asks for it first.
4. **Graph:** the repository is an Obsidian vault (Obsidian is installed). Links stay standard relative Markdown links, so files read the same in Obsidian, VS Code and GitHub.
5. **Retrieval:** grep/ripgrep now. Trigger for QMD (local hybrid search, a derived index exposed through MCP): 150+ archived files, or Claude fails twice to find a past fact → a new ADR.
6. **Changed decisions:** ADR supersession + git history (decision date in front matter, record date in the commit) + the drift check in `/sdlc-wrap`. A temporal knowledge graph is not adopted.
7. **Budgets:** session-start injection stays under ~3k tokens (CLAUDE.md <200 lines, now.md ≤120 lines, current handoff). Consolidation every 10 sessions or at 30+ summaries.
8. **Never in memory:** secrets, other people's personal data, verbatim untrusted text (memory poisoning).

## Rationale (რატომ)
No new installs now; everything stays git-versioned and readable by the owner; every index can be built later from the files; one source of truth, as the playbook recommends.

## Consequences (შედეგები)
- Positive: memory survives any tool change; the history of changed minds is free and auditable.
- Negative: keyword search only until the QMD trigger; the drift check works only if `/sdlc-wrap` is run.
- Still open (decide in harness v0): transcript retention `cleanupPeriodDays` (recommendation 365 — a privacy trade-off); the owner's handoff and summary principles in `memory/README.md`.

## Verification (როგორ შევამოწმებთ) — to run in harness v0
- Manual `/compact` drill: `compact_summary` follows the template and lands in `memory/episodic/handoffs/<session>.md`; a second compaction replaces it and archives the first.
- SessionStart drill: a session after compaction shows now.md + the handoff in context.
- Budget gate: a deterministic check fails when memory/now.md exceeds 120 lines (clean control passes; a seeded 121-line file fails).
- The undocumented PreCompact-stdout behaviour is drilled before anything relies on it; CLAUDE.md compact instructions are the documented fallback.
None of these exist yet.

## Links
- sdlc/design/memory-and-context.md
- memory/README.md
