---
id: "0002"
title: Engine base — thin native Claude Code engine, Superpowers as the method library
status: proposed
scope: engine
date: 2026-10-06
deciders: owner (to approve), Claude (drafted)
supersedes: —
superseded_by: —
review_by: at the Phase 0 gate
tags: [engine, tooling]
---
# 0002 — Engine base

## Context (კონტექსტი)
The owner asked Claude to pick the relevant engine from the AI Pulse Georgia collection (awesome-ai-pulse-georgia, section "Claude Code plugins and skills"). The full evaluation is in `sdlc/research/engine-selection.md`. The lifecycle itself is already defined by the owner (intent → ADR → harness → spec → plan → build → evaluate → secure → observe) and matches the artifact chain of Anthropic's AI-native SDLC playbook.

## Options considered (განხილული ვარიანტები)
### A — Adopt a complete framework as the chassis (Spec Kit, BMAD-METHOD, GSD)
- Pros: ready-made commands and folders.
- Cons: each brings its own artifact names, folders and flow → a second source of truth next to the owner's lifecycle. BMAD is heavy for a solo owner. GSD is archived since 2026-05-31 (GitHub API, checked 2026-10-06).
### B — Thin native engine (CLAUDE.md + sdlc-* skills + 3 subagents + hooks), Superpowers as the method library — recommended
- Pros: uses exactly the primitives the playbook recommends (CLAUDE.md, skills, subagents, hooks); each phase maps 1:1 to the owner's lifecycle; Superpowers (≈296k★, pushed 2026-10-06, MIT) is already installed and supplies proven techniques — brainstorming, writing-plans, test-driven-development, systematic-debugging, verification-before-completion. No new installs.
- Cons: we maintain our own skills (11 short files).
### C — All-in-one collections (Everything Claude Code, oh-my-claudecode)
- Pros: very broad capability.
- Cons: their hooks and rules overlap the owner's existing global setup → conflicts and context bloat; hard for a non-coder to reason about.

## Decision (გადაწყვეტილება)
Option B. Borrow patterns, not the chassis:
- per-change spec folders (Spec Kit) → `docs/changes/NNNN-<slug>/`;
- one state file (GSD's STATE.md idea) → `docs/sdlc-state.md`;
- fresh-context checkers (playbook's verifier) → `verifier` and `auditor` subagents.
Phase add-ons (Codex plugin, Obsidian skills, SkillSpector, agentsview, memory tools…) enter only through the ADR of the phase that needs them.

## Rationale (რატომ)
One source of truth and low context cost matter more for a solo, non-coding owner than breadth of features. Every part stays replaceable.

Note added 2026-10-07 (still `proposed`): "No new installs" holds for the owner's machine. For collaborators Superpowers is optional — the repo lists its marketplace but does not enable it, and our skills name a technique only as a hint (CLAUDE.md, Methods; ADR-0005).

## Consequences (შედეგები)
- Positive: clear, small, auditable engine.
- Negative: skill quality depends on us → the `auditor` reviews the whole engine at the Phase 0 gate, and the first real phases are watched for the questions C1–C8 in `sdlc/research/engine-assessment-2026-10.md`. The owner cancelled the engine dry run on a toy intent (2026-10-08).

## Verification (როგორ შევამოწმებთ)
Phase 0 gate: the auditor's review of the whole engine, then the first real phases with every gate exercised; result logged in tasks/todo.md. Not done yet.

## Links
- sdlc/research/engine-selection.md
- https://github.com/tornikebolokadze1-cyber/awesome-ai-pulse-georgia
