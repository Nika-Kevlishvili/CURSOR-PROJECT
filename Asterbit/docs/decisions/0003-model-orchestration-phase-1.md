---
id: "0003"
title: Model orchestration, stage 1 — Sonnet 5.5 coder (high effort), Opus 5.5 advisor and auditor
status: proposed
scope: engine
date: 2026-10-06
deciders: owner (instructed on 2026-10-06), Claude (drafted)
supersedes: —
superseded_by: —
review_by: when stage 2 is requested
tags: [models, orchestration]
---
# 0003 — Model orchestration, stage 1

## Context (კონტექსტი)
Owner's instruction (2026-10-06): start simple — Sonnet 5.5 is the main coder at high effort; Opus 5.5 is the main advisor and auditor. A later stage (not now): Opus 5.5 and Sonnet 5.5 code; Fable 5.1 and ChatGPT 6.1 Astra handle architecture decisions and review. (Corrected 2026-10-07: OpenAI's real API IDs are `gpt-6-astra` and `gpt-6.1-sol`; there is no "6.1 Astra" — ADR-0006, `sdlc/research/models-gpu-graphs.md` §4.)

Verified in the Claude Code docs (the VS Code extension bundles v2.1.289; checked 2026-10-06):
- The advisor tool (`advisorModel` setting, `/advisor`, `--advisor`) lets the main model consult a stronger model at key moments; "Sonnet main + Opus advisor" is a documented pairing. It is experimental and needs the Anthropic API (subscription accounts work).
- If the API refuses an advisor pairing, Claude Code silently resends the request without the advisor.
- Per-model effort lives in `modelSettings.<model>.effortLevel`; Sonnet 5.5 and Opus 5.5 default to `medium`.
- Subagent front matter accepts `model` and `effort`.

## Options considered (განხილული ვარიანტები)
### A — Opus main session, coding delegated to a Sonnet subagent
- Cons: the expensive model runs every turn; the coding context is split across subagents.
### B — `opusplan` (Opus in plan mode, Sonnet in execution)
- Pros: built in.
- Cons: switches only at the plan-mode boundary; no independent audit; plan mode cannot write the artifacts the interviews produce.
### C — Sonnet main + Opus advisor tool + Opus auditor subagent — recommended, matches the instruction
- Pros: Sonnet carries the volume; Opus is consulted exactly at decision points; the auditor runs in a fresh context, so the author never grades its own work (playbook: separation of duties).
- Cons: the advisor can drop silently → CLAUDE.md makes the coder name the advisor's verdict at each decision point; a missing verdict is an auditor finding.

## Decision (გადაწყვეტილება)
Option C. Settings to apply in the harness step — **not applied yet**:

```json
{
  "model": "claude-sonnet-5-5",
  "modelSettings": { "claude-sonnet-5-5": { "effortLevel": "high" } },
  "advisorModel": "claude-opus-5-5"
}
```

Subagents (already defined in `.claude/agents/`): `architect` and `auditor` run `claude-opus-5-5` at effort `high`; `verifier` runs `claude-sonnet-5-5` at effort `medium`. Full model IDs are pinned so models change only through a new ADR, never silently with a Claude Code update.

Loop limits: 3 fix attempts per failing check; 2 auditor fix rounds, then report UNVERIFIED. Refined by ADR-0006 (2026-10-07): review → fix → review → fix → final review → the owner, counted by `sdlc/checks/review_rounds.py`.

## Stage 2 — documented, NOT active
- Coding: Opus 5.5 for tasks tagged high-risk or complex, Sonnet 5.5 for the rest.
- Architecture and review: Fable 5.1 as advisor/architect (documented pairing "Sonnet main + Fable advisor"; needs Fable access and may bill to usage credits); an OpenAI model as a cross-vendor reviewer through the Codex plugin (openai/codex-plugin-cc, installed): `gpt-6.1-sol` with `/codex:adversarial-review` by default, `gpt-6-astra` for milestone audits (IDs and prices verified 2026-10-07, ADR-0006). A local open-weight model (DeepSeek V4-Pro or Qwen3-Coder-Next) is tried through a hosted API first; a GPU is rented only if code must not leave our control.
- Activation: a new ADR that supersedes this one, on the owner's decision.

## Consequences (შედეგები)
- Positive: spend goes where judgment matters; every gate gets an independent audit.
- Negative: Opus at every gate costs tokens — accepted by design.

## Verification (როგორ შევამოწმებთ)
After the harness step: `/model` shows Sonnet 5.5 at high; session start shows the advisor notification; a test task shows an `Advising` line in the transcript; the auditor's transcript shows Opus 5.5. Not done yet.

## Links
- sdlc/design/orchestration-and-loop.md
- https://code.claude.com/docs/en/advisor
