---
id: "0001"
title: Record significant decisions as ADRs in docs/decisions/
status: proposed
scope: engine
date: 2026-10-06
deciders: owner (asked for ADRs on 2026-10-06), Claude (drafted)
supersedes: —
superseded_by: —
review_by: —
tags: [process, memory]
---
# 0001 — Record significant decisions as ADRs

## Context (კონტექსტი)
The owner asked that architecture decisions be chosen from real alternatives and saved as ADRs. The owner's global rule 12 already places ADRs in `docs/decisions/`. Decisions are also the long-lived "what we decided and why" layer of the memory system, and the owner wants decisions that changed over time to stay detectable.

## Options considered (განხილული ვარიანტები)
### A — One ADR file per decision in docs/decisions/ (Markdown + front matter)
- Pros: readable in VS Code, Obsidian and GitHub; diffable; front matter is machine-readable; matches the owner's global convention.
- Cons: the index needs to be kept current.
### B — One growing DECISIONS.md log
- Pros: a single file.
- Cons: grows without bound; no per-decision status; superseding is messy.
### C — Decisions only inside a memory tool (e.g. a knowledge graph)
- Pros: queryable.
- Cons: not reviewable in git; tool lock-in; invisible to the owner.

## Decision (გადაწყვეტილება)
Option A. Template `sdlc/templates/adr.md`; file names `NNNN-<slug>.md`; index and timeline in `docs/decisions/README.md`; `scope: engine | product` separates engine decisions from product decisions.

## Rationale (რატომ)
Git-versioned Markdown is the single source of truth (ADR-0004 principle). Superseding instead of editing keeps the full history of changed minds.

## Consequences (შედეგები)
- Positive: every choice is auditable; `/sdlc-wrap` can compare new statements against active ADRs.
- Negative: a small writing overhead per decision.

## Verification (როგორ შევამოწმებთ)
The auditor checks at the architecture gate that each listed decision has an ADR with ≥2 real options. A deterministic check (front matter valid, index matches files) is planned for the harness step — it does not exist yet.

## Links
- docs/decisions/README.md
- sdlc/research/playbook-notes.md (committed artifacts as the audit trail)
