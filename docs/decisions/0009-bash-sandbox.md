---
id: "0009"
title: OS sandbox so a script cannot read secret files — option still open
status: proposed
scope: engine
date: 2026-10-08
deciders: owner (the goal — 2026-10-08: „ოქეია, რომ ვერ წაიკითხავს"), Claude (drafted)
supersedes: —
superseded_by: —
review_by: at the Phase 0 gate
tags: [security, harness]
---
# 0009 — OS sandbox so a script cannot read secret files

## Context (კონტექსტი)
Pull request #10 numbered this ADR 0008. On this branch 0008 is Cursor support, so the sandbox decision is 0009. The options below name `.claude/settings.json`, which this branch has deleted; nothing is turned on here, and a later yes would be a Cursor sandbox, not that file.

ახლა საიდუმლო ფაილებს (`.env*`, `*.pem`, `*.key`) ორი რამ იცავს: `settings.json`-ის `deny` წესები და `guard.py`. ორივე Claude-ის ხელსაწყოებსა და ცნობილ ბრძანებებს ხედავს, მაგრამ ვერ ხედავს, რას კითხულობს გაშვებული პროგრამა: Python-ის ან Node-ის სკრიპტი `.env`-ს თავისუფლად წაიკითხავს. ეს არის ძრავის შეფასების B3 (`sdlc/research/engine-assessment-2026-10.md`). Anthropic-ის რჩევაა, ჩაირთოს ოპერაციული სისტემის sandbox (macOS-ზე Seatbelt): ის shell-ის ყოველ ბრძანებას და მის შვილ-პროცესებს იზოლირებულ გარემოში უშვებს (`sdlc/research/anthropic-guidance-2026-10.md` §7). მფლობელმა მიზანი დაადასტურა: სკრიპტმაც ვერ უნდა წაიკითხოს საიდუმლო ფაილი.

პარამეტრების სახელები 2026-10-08-ს ამ Mac-ზე დაყენებულ Claude Code-ში (2.1.293) ინტერნეტის გარეშე შემოწმდა: `sandbox.enabled`, `sandbox.failIfUnavailable`, `sandbox.filesystem.denyRead` / `allowWrite`, `sandbox.network.allowedDomains`, `sandbox.excludedCommands`, `sandbox.credentials.files`. იქვე ჩანს ის, რაც გადაწყვეტილებას ართულებს: sandbox მარტო წაკითხვას არ ზღუდავს. ბრძანებას ჩაწერა მხოლოდ პროექტის საქაღალდეში შეუძლია, ინტერნეტთან კავშირი კი მხოლოდ ნებადართულ მისამართებზე.

## Options considered (განხილული ვარიანტები)
### A — სრული sandbox ახლავე (რეკომენდებული)
- What it is: `.claude/settings.json`-ში `"sandbox": {"enabled": true, "failIfUnavailable": true, "filesystem": {"denyRead": ["~/.ssh", "~/.aws", "<პროექტი>/.env*"]}, "network": {"allowedDomains": ["github.com", "api.github.com", "*.githubusercontent.com"]}}`.
- Pros: საიდუმლოს ვერცერთი პროგრამა ვეღარ წაიკითხავს; თუ sandbox ვერ ჩაირთო, Claude Code არ ჩაირთვება და ჩუმად დაუცველად არ იმუშავებს.
- Cons: Bash ვეღარ ჩაწერს პროექტის გარეთ — მაგალითად, მეზობელ სამუშაო ასლში (`~/Asterbit-*`), რომელსაც ძრავა გრძელ სამუშაოზე იყენებს. სხვა საიტებთან კავშირი (pip, npm, curl) ყოველ ჯერზე დასტურს ითხოვს. sandbox-ში შეიძლება ჩავარდეს სკრიპტი, რომელიც დროებით ფაილს სხვაგან წერს (მაგ. წვრთნის `tempfile`) — ეს ჯერ გამოსაცდელია.
- Cost (money + learning): ფული — არა; სწავლა — ახალი ტიპის „Operation not permitted" შეცდომები.
- Risk: სამუშაო პროცესი (worktree, CI-ის ლოკალური გაშვება) შეიძლება გაფუჭდეს, სანამ `allowWrite`-ს არ მოვარგებთ.
- Reversibility: ადვილი — ერთი ბლოკი `settings.json`-ში.
- Source checked: Claude Code 2.1.293-ის ჩაშენებული ტექსტები (ზემოთ); `sdlc/research/anthropic-guidance-2026-10.md` §7.
### B — sandbox-ის გარეშე, მხოლოდ არსებული დაცვა
- What it is: რჩება `deny` წესები, `guard.py` და `commit_secrets.py`.
- Pros: არაფერი იცვლება, არაფერი ფუჭდება.
- Cons: B3-ის ხვრელი რჩება — სკრიპტი საიდუმლოს წაიკითხავს.
- Reversibility: —
### C — sandbox პროდუქტის კოდირებამდე (Phase 6)
- What it is: A, მაგრამ მოგვიანებით, როცა პროდუქტის კოდი გაჩნდება.
- Pros: ძრავის სამუშაოს ახლა არ შეაწუხებს.
- Cons: საიდუმლოები უკვე ახლა შეიძლება იყოს მანქანაზე; შეფასება „ნამდვილ პროდუქტამდე" ჩართვას ურჩევდა.

## Decision (გადაწყვეტილება)
მიზანი მფლობელისაა (2026-10-08): სკრიპტმაც ვერ უნდა წაიკითხოს საიდუმლო. ვარიანტს — A, B თუ C — მფლობელი ირჩევს, როცა ჩაწერისა და ინტერნეტის შეზღუდვასაც დაინახავს. ამიტომ `settings.json` ამ ADR-ით არ შეცვლილა.

## Rationale (რატომ)
A ერთადერთია, რომელიც B3-ის ხვრელს ხურავს, და `failIfUnavailable`-ით ჩუმი გამორთვა შეუძლებელია: დაცვა, რომელიც შეუმჩნევლად ითიშება, მხოლოდ დაცვის იერია. ჩაწერისა და ქსელის შეზღუდვა მისი ფასია, ამიტომ ჯერ ცალკე branch-ზე უნდა გამოიცადოს.

## Consequences (შედეგები)
- Positive: საიდუმლოს დაცვას სისტემა უზრუნველყოფს და არა ინსტრუქცია.
- Negative: Bash ვეღარ ჩაწერს პროექტის გარეთ; უცნობ მისამართზე კავშირი დასტურს ითხოვს.
- What becomes harder: მეზობელ სამუშაო ასლში მუშაობა — დასჭირდება `allowWrite` ან სამუშაო ასლის პროექტის შიგნით გადატანა.

## Verification (როგორ შევამოწმებთ)
A-ს არჩევის შემდეგ, ცალკე branch-ზე და ახალ სესიაში: (1) `python3 -c "open('.env')"` ჩავარდება, ჩვეულებრივი ფაილის წაკითხვა კი გაივლის (სუფთა კონტროლი); (2) `check_structure.py`, `drill_hooks.py` (ARMED) და სტაბით (DEAD) sandbox-ში გაეშვება; (3) `git push` და `gh pr view` იმუშავებს; (4) `denyRead`-ის ზუსტი ჩანაწერი პროექტის `.env*`-ისთვის (შაბლონი თუ სრული გზა) ამ გამოცდით დადასტურდება — ჯერ დაუდასტურებელია. სანამ ეს ოთხივე არ გაივლის, ჩართვა `main`-ში არ შევა.

## Links
- [sdlc/research/engine-assessment-2026-10.md](../../sdlc/research/engine-assessment-2026-10.md) — B3
- [sdlc/research/anthropic-guidance-2026-10.md](../../sdlc/research/anthropic-guidance-2026-10.md) — §7
