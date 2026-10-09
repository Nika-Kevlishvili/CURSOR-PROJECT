---
name: sdlc-architecture
description: Phase 2 — turn the accepted intent into docs/prd.md (product requirements), architecture decisions the owner chooses from verified alternatives (one ADR each in docs/decisions/), and docs/trd.md (technical requirements). Use after the intent is approved, or whenever a stack, tool, library, hosting or model choice comes up.
---
# Phase 2 — PRD, architecture decisions, TRD

Goal: the product requirements are written down, every significant choice is made by the owner from real alternatives and recorded as an ADR, and the technical frame that follows from those choices is written down. Order: PRD → ADRs → TRD (ADR-0006).

## Before you start
- Gate: phase 1 in docs/sdlc-state.md must be `approved`. Read docs/intent.md fully, and docs/decisions/README.md.
- Set phase 2 to `in-progress`.

## 1. PRD — what and why
1. Copy sdlc/templates/prd.md → docs/prd.md and write it from the intent: users, journeys (J-n), features with priorities (P-n), metrics with target numbers. No technology. Unknowns stay inline as `[NEEDS CLARIFICATION: …]` and are listed under Open questions.
2. Ask the owner about every `[NEEDS CLARIFICATION]` (the AskQuestion tool, at most 4 questions per round, recommendation first) and replace each marker with the answer or a parked item.
3. Reader test: spawn a fresh `generalPurpose` subagent on Claude Sonnet 5.5, told to read ONLY docs/prd.md and nothing else, to change no file, and to answer 5–10 questions a builder would ask (who is the main user, what must v1 do, what is out of scope, how is success measured…). Compare its answers with the truth. Every wrong or "not stated" answer is an ambiguity: fix the PRD and record the row in its Reader test table. Re-run once if you changed more than a sentence.

## 2. List the decisions
Derive the list from the intent and the PRD — typically: product form and platform; language and framework; data storage; hosting and deployment; AI model and agent framework (if any); integrations; authentication (if any). Show it in plain Georgian and let the owner add or remove items. Put decisions that constrain others first.

## 3. For each decision
1. Spawn the `architect` subagent with the decision, docs/intent.md, docs/prd.md and the accepted ADRs. It returns 2–3 verified options with pros, cons, cost, risk, reversibility and a recommendation.
2. Consult the `advisor` agent on the recommendation and name its verdict.
3. Present the options with the AskQuestion tool: recommended option first with "(Recommended)" in the label, a one-sentence trade-off per option, in Georgian. Show a short comparison snippet in the chat first when a stack or code choice needs one. Gloss any term the owner may not know.
4. After the owner chooses, write `docs/decisions/NNNN-<slug>.md` from sdlc/templates/adr.md (next free number, `scope: product`, `status: accepted`) and add its row to docs/decisions/README.md.
5. If the choice contradicts an earlier accepted ADR: write a new ADR with `supersedes:` and set the old one to `superseded` with `superseded_by:`.

## 4. TRD — the technical frame
1. Copy sdlc/templates/trd.md → docs/trd.md. Fill the Technical context table from the accepted ADRs (one source per row), then components, data model, interfaces, and technical requirements TR-n — each with its source (ADR or P-n) and how it will be checked. Never re-argue a decision; link the ADR.
2. Resolve every `[NEEDS CLARIFICATION]` with the owner, as in step 1.2.
3. Reader test as in step 1.3, on docs/trd.md alone (questions such as: which language and version, where is data stored, what must never happen with secrets, how is it deployed).

## 5. Close
1. Write the one-paragraph architecture overview in docs/decisions/README.md (what we build with and why); add a mermaid diagram if it helps the owner see the parts.
2. Run the `auditor` (gate: architecture — review loop per AGENTS.md, loop id `gate-architecture`) on docs/prd.md, the new ADRs and docs/trd.md: consistency with the intent, unverified claims, missing decisions, reader tests recorded, no marker left.
3. Owner approval → docs/prd.md and docs/trd.md `status: approved`, phase 2 `approved` + a gate-log line; commit `docs(architecture): approve PRD, ADRs NNNN–MMMM and TRD`.
4. Next step: `/sdlc-harness`.

## Done when
docs/prd.md and docs/trd.md are approved with their reader tests recorded and no `[NEEDS CLARIFICATION]` left; every decision on the list has an accepted ADR with at least two real options considered; the index is current; the auditor reports no open HIGH finding.
