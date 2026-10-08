---
name: sdlc-intent
description: Phase 1 — shape a raw idea into an approved docs/intent.md through a structured interview, enriched with Claude's own ideas. Use when the owner brings a new project idea, feature, bug or incident to turn into intent, or says "იდეა მაქვს", "ახალი პროექტი", "let's start".
---
# Phase 1 — Intent

Goal: the owner's idea, in the owner's own terms, committed as an intent file the next phase can act on. You are the analyst: you ask, propose and write; the owner decides.

Technique: brainstorming (one topic at a time, multiple choice where possible, alternatives offered) — but the output goes to the intent file, never to docs/plans/.

## Before you start
- Read `docs/sdlc-state.md`. If phase 1 is already `approved`, this is a new change: use `docs/changes/NNNN-<slug>/intent.md` (next free number) and leave docs/intent.md untouched.
- Copy `sdlc/templates/intent.md` to the target path (status: draft). Set phase 1 to `in-progress` in docs/sdlc-state.md.

## Interview
Ask in rounds of at most 4 questions (the AskQuestion tool when the options are clear, plain questions when they are not). After each round, reflect back in 2–3 Georgian sentences what you understood and let the owner correct it.
1. Problem — what is hard or impossible today? Who feels it, how often, and how do they cope now?
2. Outcome — what does "better" look like? What would the owner show a friend?
3. Users — who uses it, how tech-savvy are they, which language and device?
4. Success — how will we know it worked? Push for observable, measurable signs.
5. Constraints — budget, deadline, data and privacy, tools wanted or refused, hosting.
6. Scope — what is explicitly NOT in v1?
7. Risks — what could make this fail or cause harm?

## Enrich — Claude's ideas
After round 2, offer 3–5 ideas the owner did not mention: a simpler v1, a risk mitigation, an existing tool that already solves part of the problem (verify it exists and is maintained before naming it), a measurable success signal. Each idea = one sentence of value + one sentence of cost. The owner marks each one accepted / parked / rejected; record all three kinds.

## Close
1. Fill every section. Unknowns go to "Open questions" — never invent answers.
2. Run the `auditor` subagent (gate: intent) on the file, then the review loop per AGENTS.md (loop id `gate-intent`): fix only what it locates with evidence — two fix rounds and a final review at most, then the owner decides what is left.
3. Give the owner a short Georgian summary plus the auditor's verdict and ask for explicit approval.
4. On approval: front matter `status: accepted`, `accepted: <date>`; docs/sdlc-state.md → phase 1 `approved` (who, when) + a gate-log line; commit `docs(intent): accept <title>`.
5. Tell the owner the next step: `/sdlc-architecture`.

## Done when
The intent file is committed with status accepted, every section is filled or explicitly "none", the success criteria are measurable, and the gate log names who approved and when.
