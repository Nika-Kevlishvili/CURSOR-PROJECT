# SDLC state — ფაზების ჟურნალი

> ერთადერთი ადგილი, სადაც წერია, სად ვართ. Claude ამ ფაილს კითხულობს ნებისმიერი ეტაპის დაწყებამდე და აახლებს ყოველ კარიბჭეზე (gate).
> Status values: `not-started` · `in-progress` · `awaiting-approval` · `approved` · `skipped`

| # | Phase | Status | Artifact | Approved by / date |
|---|---|---|---|---|
| 0 | Engine setup | in-progress | sdlc/, .cursor/, AGENTS.md, PROCESS.md, PROGRESS.md, CONTRIBUTING.md, docs/FILES.md, docs/decisions/0001–0009 | — |
| 1 | Intent | not-started | docs/intent.md | — |
| 2 | Architecture | not-started | docs/prd.md, docs/decisions/, docs/trd.md | — |
| 3 | Harness | not-started | .cursor/hooks.json, .cursor/hooks/, .cursor/cli.json, ADR | — |
| 4 | Spec | not-started | docs/spec.md | — |
| 5 | Plan | not-started | docs/plan.md, tasks/todo.md | — |
| 6 | Build | not-started | code + tests | — |
| 7 | Evaluate | not-started | gates + evals | — |
| 8 | Secure | not-started | threat model, controls, drills | — |
| 9 | Observe | not-started | log schema, telemetry, control bands | — |

## Current focus (მიმდინარე ფოკუსი)
ეს სამუშაო სივრცე QA პროექტია Phoenix-სა და EnergoTS-ისთვის (მფლობელი, 2026-10-09). ყოველდღიური QA ფაზის კარიბჭეს არ ელოდება. ფაზები მხოლოდ ძრავის დოკუმენტებს ეხება. ძრავის სრულყოფა გრძელდება (მფლობელი, 2026-10-08). საცდელი გაშვება არ ტარდება. შემდეგია Phase 0-ის კარიბჭე: `auditor` მთელ ძრავას ამოწმებს, მერე მფლობელი ამტკიცებს ADR-0001…0003-ს, ADR-0005…0009-სა და PROCESS.md-ს.

## Gate log (კარიბჭეების ჟურნალი)
- 2026-10-06 — Engine scaffold created on branch `engine/v0-scaffold`. Not approved yet.
- 2026-10-06 — ADR-0004 (context & memory) accepted by the owner through the interview: 65% · Obsidian vault · grep → QMD at trigger · ADR + git + wrap check.
- 2026-10-07 — Owner decision: the scaffold becomes the shared base in `main` (PR #1, merge commit). This is not acceptance of ADR-0001…0003 or PROCESS.md — they stay `proposed`. Collaborator lashavamleti invited with write access; only the owner approves gates and merges into `main`.
- 2026-10-07 — Owner choices for harness v0: balanced autonomy, 30-day transcript retention (default), Claude's eight handoff principles, CI on every pull request → ADR-0005 (proposed until the Phase 0 gate).
- 2026-10-08 — Owner choices: Claude-only models, ADR-0007 option A (proposed until the Phase 0 gate); PR #7 merged (`3113985`); PR #8 merged (`566fd00`).
- 2026-10-08 — Owner decisions (PR #10, brought onto `engine/cursor-support` locally, not pushed): no engine dry run; this repo's rules win over user-level rules (B1); every deletion asks first (B2); the sandbox goal is accepted, its option is still open (ADR-0009; PR #10 numbered it 0008).
