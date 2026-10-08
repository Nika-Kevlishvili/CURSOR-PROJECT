# Project Progress — პროექტის პროგრესი

> ახლდება ყოველი ეტაპის, კარიბჭისა და milestone-ის ბოლოს და სესიის დასაწყისში იკითხება, რომ კონტექსტი არ დაიკარგოს.
> ჟურნალი მხოლოდ ივსება: ძველი ჩანაწერები არ იცვლება. ფაილების როლები: [PROCESS.md](PROCESS.md) §4.

## Current State (მიმდინარე მდგომარეობა)
- **Phase:** 0 — Engine setup — IN PROGRESS (ძრავა v1 და v2, `AGENTS.md`, `env.example`, შეფასება, A1–A6 და `commit_secrets`-ის საქაღალდის გასწორება `main`-შია — PR #2–#8)
- **Branch:** `main` (`566fd00`, PR #8); `engine/cursor-support` — Cursor-ზე გადასვლა, Claude Code-ის ფაილების წაშლა და PR #10-ის გადმოტანა, **მხოლოდ ლოკალურად** (GitHub-ზე არ ატვირთულა) · **GitHub:** `tornikebolokadze1-cyber/Asterbit` — **public** (2026-10-08-ს, ამ სესიის გარეთ გახდა საჯარო; დილით private იყო). gitleaks-მა მთელი ისტორია შეამოწმა (`--all`, 34 commit): „no leaks found" · **გუნდი:** მფლობელი + თანამშრომელი lashavamleti (წერის უფლება, მოწვევა მიღებულია)
- **Decisions:** ADR-0004 accepted; ADR-0001…0003 და ADR-0005…0009 proposed (0008 — მხოლოდ Cursor; 0009 — sandbox, ვარიანტი ღიაა. PR #10 sandbox-ს 0008-ად ნომრავდა)
- **Owner's decisions (2026-10-08, PR #10, აქ ლოკალურად გადმოტანილია):** საცდელი გაშვება არ ტარდება — Phase 0-ის კარიბჭე auditor-ის შემოწმებითა და მფლობელის დამტკიცებით დაიხურება; ყოველ `rm`-სა და `git rm`-ზე hook ჯერ კითხულობს; გრძელი handoff-ის თავი და ბოლო რჩება; Codex იშლება, CodeRabbit-ს მფლობელი მოგვიანებით განიხილავს
- **Not built yet:** Phase 0-ის კარიბჭე; პროდუქტის იდეა არ არის არჩეული

## Completed (დასრულებული)

### 2026-10-06 — Phase 0.1: კვლევა
- Anthropic-ის „AI-native SDLC playbook" (2026-08-21) სრულად წავიკითხე → `sdlc/research/playbook-notes.md`
- awesome-ai-pulse-georgia-ს ოთხი სექცია შევაფასე, რეპოების აქტიურობა GitHub API-ით გადავამოწმე (GSD archived-ია) → `sdlc/research/engine-selection.md`
- Claude Code-ის დოკუმენტაციით (VS Code-ში 2.1.289) გადავამოწმე: `autoCompactWindow`, PreCompact/PostCompact, advisor tool, effort, subagent-ებისა და skill-ების front matter
- შენს კომპიუტერზე აღმოვაჩინე: გლობალური handoff hook-ები უკვე მუშაობს; MemPalace-ის hook ჩუმად ვერ სრულდება; სესიების ჩანაწერები 30 დღეში იშლება; ტერმინალის CLI 2.1.92-ია

### 2026-10-06 — Phase 0.2: დირექტორია (scaffold)
- git ჩაირთო; branch `engine/v0-scaffold`; commits `3dc2036`, `095d2da`, `85731b3`
- შეიქმნა: README, CLAUDE.md, 11 skill, 3 აგენტი, 7 თარგი, 5 დიზაინ-დოკუმენტი, ADR-0001…0004, `memory/` ფენები
- შემოწმება: 92/92 გადის; drill-მა 3 ჩადებული შეცდომიდან 3 დაიჭირა

### 2026-10-06 — Phase 0.3: მეხსიერების ინტერვიუ
- ADR-0004 accepted: შეკუმშვა 65% · Obsidian vault · ჯერ grep, QMD ზღვარზე · ADR + git + wrap — commit `62b1008`
- Obsidian-ში ჩაირთო ჩვეულებრივი markdown ბმულები (`.obsidian/app.json`)

### 2026-10-06 — Phase 0.4: PROCESS.md და PROGRESS.md
- სამუშაო შეთანხმება ერთ დოკუმენტში ჩაიწერა ([PROCESS.md](PROCESS.md)), პლუს ეს ჟურნალი
- CLAUDE.md-ში დაემატა წესი: PROGRESS.md ყოველი ეტაპის, კარიბჭისა და milestone-ის ბოლოს ივსება; `/sdlc-wrap`-ს შესაბამისი ნაბიჯი დაემატა
- გაკვეთილი ჩაიწერა `tasks/lessons.md`-ში: ეს ფაილები თავიდანვე უნდა შექმნილიყო

### 2026-10-06 — Phase 0.5: GitHub
- `git add -A`-მა plugin-ის დროებითი ფაილი (`.omc/state/idle-notif-cooldown.json`) commit-ში შეიტანა; თვალყურის დევნება შეწყდა, `.omc/` → .gitignore, გაკვეთილი ჩაიწერა — commit `5e41fad`
- საიდუმლოებების შემოწმება ატვირთვამდე: gitleaks (მთელი git ისტორია) და scan_secrets.py — exit 0; ორივემ ჩადებული ყალბი გასაღები დაიჭირა — exit 1
- შეიქმნა private რეპო https://github.com/tornikebolokadze1-cyber/Asterbit; აიტვირთა `main` და `engine/v0-scaffold`; GitHub-ზე 49 ფაილია
- გაიხსნა PR #1 (`engine/v0-scaffold` → `main`): მისი merge = ძრავის v0-ის დამტკიცება

### 2026-10-07 — Phase 0.6: თანამშრომელი და დამოუკიდებელი პროექტი
- მფლობელის მოთხოვნა: პროექტი „ცალკე პროექტი" იყოს, რომ თანამშრომელმა ჩამოტვირთოს და შეცვალოს. შემოწმებამ ოთხი ხარვეზი აჩვენა: `main` ცარიელი იყო; CLAUDE.md მფლობელის კომპიუტერის გლობალურ წესებს ეყრდნობოდა; Claude ყველას მფლობელად ჩათვლიდა; Superpowers plugin სავალდებულოდ ეწერა
- lashavamleti მოწვეულია წერის უფლებით (GitHub invitation, 2026-10-07 07:55 UTC) — მიღებას ელოდება
- ახალი: `CONTRIBUTING.md` (თანამშრომლის გზამკვლევი), `docs/FILES.md` (ყველა ფაილის რუკა)
- CLAUDE.md და PROCESS.md: მფლობელი და თანამშრომლები; კარიბჭეებსა და `main`-ს მხოლოდ მფლობელი ამტკიცებს; უსაფრთხოების წესები რეპოშია და ყველა კომპიუტერზე მოქმედებს; Superpowers სურვილისამებრ; ფაილის დამატებისას `docs/FILES.md` ახლდება
- მფლობელის გადაწყვეტილებით PR #1 `main`-ში merge commit-ით ერთიანდება, როგორც საერთო საფუძველი; ეს ADR-0001…0003-სა და PROCESS.md-ს არ ამტკიცებს — ისინი `proposed` რჩება. merge commit-ი ინარჩუნებს ამ ჟურნალში ნახსენებ ყველა commit-ს
- private რეპოში `main`-ის დაცვა (branch protection) GitHub Pro-ს მოითხოვს (API: HTTP 403), ამიტომ „მხოლოდ PR-ით" ჯერ შეთანხმებაა და არა ტექნიკური აკრძალვა

### 2026-10-07 — Phase 0.7: harness v0 (branch `engine/v1`)
- მფლობელის გადაწყვეტილებები: დაბალანსებული ავტონომია; სესიების ჩანაწერები 30 დღე (ნაგულისხმევი, პარამეტრი არ იცვლება); handoff-ისა და შეჯამების რვა პრინციპი (Claude-ის შემოთავაზება); CI ყოველ PR-ზე → [ADR-0005](docs/decisions/0005-harness-v0.md) (proposed)
- `.claude/settings.json`: Sonnet 5.5 high, Opus 5.5 advisor, `autoCompactWindow: 650000`, allow / ask / deny უფლებები; ჩაწერისთანავე advisor tool ამ სესიაში გამოჩნდა — ესე იგი, ის ამ ანგარიშზე მუშაობს
- hook-ები `.claude/hooks/`-ში (Python, მხოლოდ სტანდარტული ბიბლიოთეკა): `guard.py`, `commit_secrets.py`, `secret_patterns.py`, `memory_handoff.py`, `memory_context.py`
- შემოწმებები `sdlc/checks/`-ში: `check_structure.py`, `drill_hooks.py`; CI: `.github/workflows/checks.yml` (actions/checkout ზუსტ commit-ზე, gitleaks 8.30.1 checksum-ით)
- ცოცხალი გამოცდა: საიდუმლო ფაილის (`.env.probe`) ჩაწერა პროექტის hook-მა დაბლოკა (ASTERBIT-GUARD); verifier აგენტის `touch` დაიბლოკა „read-only agent" შეტყობინებით და ფაილი არ შეიქმნა
- დოკუმენტები: CLAUDE.md და PROCESS.md (push სამუშაო branch-ზე თავისუფლად, `main`-ში — არასდროს; hook-ები; შემოწმებები), CONTRIBUTING.md (Python 3, დაცვა), docs/FILES.md, README.md, `memory/README.md`-ში რვა პრინციპი, ADR-0002-ში Superpowers-ის შენიშვნა, `/sdlc-status` PROGRESS.md-საც კითხულობს
- auditor-ის პირველმა შემოწმებამ harness ჩააჭრა (FAIL): `main`-ზე ყოფნისას `git push origin HEAD` არ იბლოკებოდა; პლუს რვა საშუალო ხარვეზი (`bash -c`-ში დამალული ბრძანება, commit ფაილის სახელით სკანირებას გვერდს უვლიდა, `switch -f`/`branch -D` უკითხავად, handoff-ის ფაილი CI-ს გააწითლებდა და სხვ.). ყველა გასწორდა და თითოეულს წვრთნის შემთხვევა დაემატა. მეორე რაუნდმა კიდევ ერთი გზა იპოვა (`git push origin 2>&1` — გადამისამართება branch-ის სახელად ითვლებოდა); გასწორდა და წვრთნით შემოწმდა. მესამე აუდიტი არ ჩატარებულა (ორი რაუნდის ლიმიტი), ამიტომ ბოლო გასწორებას მხოლოდ წვრთნა ადასტურებს
- PR #2 (`engine/v1` → `main`): CI მწვანეა — 7 ნაბიჯი, მათ შორის „უმოქმედო" hook-ებით DEAD და gitleaks (https://github.com/tornikebolokadze1-cyber/Asterbit/actions/runs/37601321945). ცოცხალი გამოცდები საბოლოო hook-ზე გამეორდა და გავიდა
- გზად: lashavamleti-მ მოწვევა მიიღო (2026-10-07, GitHub API: role `write`)
- PR #2 `main`-ში გაერთიანდა (merge commit `c934e3b`)

### 2026-10-07 — Phase 0.8: ძრავა v2 — კვლევა (branch `engine/v2`)
- მფლობელმა თავდაპირველი პრომპტი ხელახლა გაგზავნა და სთხოვა, ძრავში ყველაფერი დაემატოს, რაც აკლია. რეპოებიდან აღებული ფრაგმენტები კი ჯერ მასთან უნდა შეთანხმდეს
- მფლობელის გადაწყვეტილებები: შეკუმშვა 65%-ზე რჩება (ADR-0004 უცვლელია, თუმცა პრომპტში 70% ეწერა); PRD და TRD ცალკე ფაილებია; Sonnet↔Opus ციკლი ეტაპის ბოლოს და რისკიან დავალებებზე ტრიალებს
- ცხრა რეპო და AI Pulse კოლექცია კოდის დონეზე შეისწავლა ორმა აგენტმა, ვებ-კვლევა კი მესამემ ჩაატარა → `sdlc/research/repo-study.md` (18 ფრაგმენტი), `sdlc/research/models-gpu-graphs.md`, `sdlc/research/engine-v2-gaps.md` (შედარება და პაკეტები P1–P6) — commit `b6c393c`; `check_structure.py`: 283 შემოწმება, PASS, exit 0
- გზად აღმოჩნდა: ADR-0003 და სხვა ოთხი ფაილი არარსებულ მოდელს წერს („ChatGPT 6.1 Astra"; რეალური ID-ებია `gpt-6-astra` და `gpt-6.1-sol`); ADR-0004-ის Verification ამბობს „None of these exist yet", თუმცა `now.md ≤120` შემოწმება უკვე არსებობს; Graphify კოდისთვის ADR-0004-ის არჩევანს („მხოლოდ Obsidian") ეხება, ამიტომ ცალკე კითხვაა
- მრჩევლის (Opus 5.5) შენიშვნით შეთავაზება შესწორდა: კონტექსტის monitor PostToolUse-ზე მუშაობს და ზღვარს settings-იდან კითხულობს; ჟურნალი `.claude/logs/`-შია; გაკვეთილების ზღვარი 2 რჩება; F2 „ან მფლობელის მკაფიო მოთხოვნას" მოიცავს; პაკეტები ცალკე ეტაპებად აიწყობა, სამი PR-ით

### 2026-10-07 — Phase 0.9: ძრავა v2 — P1 (PRD/TRD) და P2 (Sonnet↔Opus ციკლი), branch `engine/v2`
- მფლობელმა ექვსივე პაკეტი აირჩია; კოდის გრაფი — კოდის გაჩენამდე გადადებულია ([ADR-0006](docs/decisions/0006-engine-v2.md), proposed)
- P1: PRD და TRD თარგები, Phase 2 = PRD → ADR-ები → TRD, „ცივი მკითხველის" ტესტი, `[NEEDS CLARIFICATION]` ნიშანი და მისი შემოწმება დამტკიცებულ დოკუმენტებში, მოთხოვნიდან დავალებამდე კვალის შემოწმება
- P2: ციკლი შემოწმება → გასწორება → შემოწმება → გასწორება → ბოლო შემოწმება → მფლობელი; რაუნდებს `sdlc/checks/review_rounds.py` ითვლის ჟურნალში `tasks/review-log.jsonl`
- ციკლი პირველად თავად ამ ცვლილებაზე გაეშვა (loop `ENG-v2-P1P2`): review 1 — FAIL (1 HIGH, 6 MEDIUM: მაგ., თარგების დამხმარე ტექსტი ახალ შემოწმებას აჩერებდა) → fix 1 `26edffd` → review 2 — 0 HIGH, 1 MEDIUM → fix 2 `e17d4cc` → review 3 — 0 HIGH, 0 MEDIUM, 2 LOW → `status` exit 0. LOW-ები runtime branch-ზე სწორდება
- შემოწმებები (`e17d4cc`): სტრუქტურა PASS (exit 0); წვრთნა 106 შემთხვევა ARMED (exit 0, ზედიზედ რამდენიმე გაშვება); „უმოქმედო" hook-ებით DEAD (exit 1); ხუთი ცალკეული mutant ასლში — თითოეული დაიჭირა (exit 1)
- გზად აღმოჩნდა: ძველი წვრთნის შემთხვევა (`commit_secrets`, gitleaks-ის გასაღები) 4 გაშვებიდან ერთხელ ჩავარდა — გადადებულია `tasks/todo.md`-ში; P3–P5-ის ნახევრად მზა სამუშაო ცალკე branch-ზე `engine/v2-runtime` გადავიდა, რომ ციკლს მხოლოდ გასწორება შეეხედა

### 2026-10-07 — Phase 0.10: ძრავა v2 — P3–P6, branch `engine/v2-runtime`
- P3 (მეხსიერება v2): `context_monitor.py` (PostToolUse) ზომავს კონტექსტს სესიის ჩანაწერიდან და შეკუმშვამდე 100 ათასი token-ით ადრე handoff-ის ჩაწერას ითხოვს; ზღვარს `autoCompactWindow`-იდან კითხულობს. `untrusted.py` მეხსიერებას შემთხვევითი ნიშნით (nonce) ღობავს და „ინსტრუქციის ჩანაცვლების" ნიმუშებს ეძებს; handoff-ი `injection_flags` ველს იღებს. `memory_hygiene.py` (ძველი/გადაბერილი ფაილები, exit 0/3/2); მეხსიერების ფენების ცხრილი `memory/README.md`-ში; `/sdlc-wrap` ნაბიჯები 4 და 7
- P4 (თვითგანვითარება): `tasks/lessons.md`-ს „Kind" სვეტი დაემატა; `lessons_graduate.py` ორ სხვადასხვა დღეს განმეორებულ გაკვეთილს ეძებს და შემოწმების აწყობას სთავაზობს — რეალურ ფაილზე უკვე იპოვა 2026-10-06-ის `git add -A` გაკვეთილი
- P5 (კარიბჭეები და ჟურნალი): `run_gates.py` (`docs/gates.json` Phase 7a-ში შეიქმნება; PASS / FAIL / INCONCLUSIVE ცალ-ცალკე, „0 ტესტი" მწვანე არასდროსაა); `event_log.py` (PostToolUse / PostToolUseFailure → `.claude/logs/`, git-ში არ შედის); `agent_report.py`
- P6 (მეორე ეტაპის ჩანაწერები, არაფერი ჩართულა): OpenAI-ის სწორი ID-ები `gpt-6-astra` / `gpt-6.1-sol`; ღია მოდელი API-ით; GPU — RunPod, სათადარიგოდ Hetzner (`sdlc/design/orchestration-and-loop.md` §7)
- ყველა კარიბჭის skill ახლა საერთო ციკლს მიუთითებს (loop id `gate-<phase>`); წვრთნის ასლები ახალ git საცავს ქმნის, ამიტომ worktree-დანაც ეშვება
- ცოცხალი მტკიცებულება ამ სესიიდან: `event_log` 6 ხაზი ჩაწერა, მათ შორის auditor ქვე-აგენტი; `context_monitor`-მა ნამდვილი ჩანაწერი გაზომა (<55%)
- ENG-v2-P1P2-ის ბოლო ორი LOW აქ გასწორდა: `[NEEDS CLARIFICATION: <question>]` გამონაკლისია მხოლოდ `>` ხაზზე, შემოწმება ასოების ზომას აღარ უყურებს (ორი ახალი წვრთნის შემთხვევა); ADR-0006-ში რიცხვები დაზუსტდა
- შემოწმებები: სტრუქტურა 328, PASS (exit 0); წვრთნა 126 შემთხვევა, ARMED (exit 0); „უმოქმედო" hook-ებით DEAD (exit 1)
- გზად აღმოჩნდა: `commit_secrets.py` სესიის საქაღალდეს ამოწმებს და არა იმას, სადაც ბრძანება `cd`-ით ან `git -C`-ით გადადის — worktree-დან გაკეთებული commit-ები ხელით შემოწმდა (gitleaks, built-in ნიმუშები: არაფერი). გასწორება hook-ს ცვლის, ამიტომ მფლობელის „კი" სჭირდება → `tasks/todo.md`
- აუდიტის ციკლი `ENG-v2-P3P6` PR #4-ზე ეშვება
- ციკლი `ENG-v2-P3P6`: review 1 — 0 HIGH, 6 MEDIUM, 8 LOW (მაგ., კონტექსტის მზომი ქვე-აგენტებზეც ეშვებოდა; ჩატვირთვისას handoff-ში შენახულ „ინექციის ნიშანს" ენდობოდა; ჟურნალი მხოლოდ ცნობილი სერვისების გასაღებებს ფარავდა) → fix 1 `3b077a7` → review 2 — 0 HIGH, 2 MEDIUM, 4 LOW → fix 2 `fc4f48d`. მესამე, ბოლო შემოწმება არ ჩატარებულა — მფლობელმა ციკლი შეაჩერა (ქვემოთ). fix 2 მხოლოდ მანქანური შემოწმებებით დადასტურდა: სტრუქტურა 329 PASS, წვრთნა 131 ARMED, „უმოქმედო" hook-ებით DEAD, 3 mutant დაიჭირა, CI. auditor-ის ერთი შემოწმება 7–12 წუთს გრძელდებოდა
- მფლობელის გადაწყვეტილება (2026-10-07): ძრავი Phase 0-ის კარიბჭემდე მსუბუქად შენდება — აუდიტორის ციკლის, mutant-ებისა და ციკლის ჟურნალის გარეშე; რჩება უსაფრთხოების წესები, `check_structure.py`, hook-ის ცვლილებისას წვრთნა და CI. ძრავით გაშვებული სამუშაო (საცდელი გაშვება, პროდუქტი) სრულ ციკლს იყენებს → CLAUDE.md, PROCESS.md, ADR-0006, `tasks/lessons.md`
- გზად: დისკი გაივსო (117 MB დარჩა) და სესიის დროებითი საქაღალდე გასუფთავდა — worktree და fix 1-ის შეუნახავი ნამუშევარი დაიკარგა. commit-ები GitHub-ზე იყო, ამიტომ worktree თავიდან შეიქმნა `~/Asterbit-runtime`-ში და review 1-ის ჩანაწერი ხელახლა ჩაიწერა. ამ ჩანაწერში სტრუქტურის რიცხვი 328 ერთით ნაკლებია — now.md-ის ბმულის შემდეგ 329-ია

### 2026-10-07 — Phase 0.11: ძრავა v2 `main`-შია; `AGENTS.md` და `env.example` (branch `engine/agents-env`)
- მფლობელის „კი"-ს შემდეგ PR #3 (`a0414c8`) და PR #4 (`d1b66c7`) `main`-ში გაერთიანდა; ორივეზე CI მწვანე იყო, `main`-ზე `check_structure.py` — 330 შემოწმება, PASS. ADR-ები `proposed` რჩება Phase 0-ის კარიბჭემდე
- მფლობელის საქაღალდე `~/Asterbit` `main`-ზე გადავიდა; `settings.json`-ის ორი ცარიელი ხაზი (Claude-ს არ ჩაუწერია) `git stash`-ში გადაიდო და არ წაშლილა. გადასვლისთანავე ახალი hook-ები ამ სესიაში ჩაირთო — `.claude/logs/`-ში მოვლენების ჟურნალი და კონტექსტის მზომის ფაილი გაჩნდა
- მფლობელმა იპოვა ორი ხარვეზი: არც `AGENTS.md` იყო (წესები Codex / GPT, Cursor, Kilo-სთვის — მათ `CLAUDE.md` არ წაუკითხავთ) და არც საიდუმლო პარამეტრების ნიმუში. ორივე დაემატა; `AGENTS.md` `check_structure.py`-ის სავალდებულო ფაილებშია
- ნიმუში `env.example` ჰქვია და არა `.env.example`: პროექტის დაცვის წესი `Read(./.env.*)` Claude-ს `.env.example`-ის ჩაწერასაც უკრძალავს. მფლობელმა სახელის შეცვლა აირჩია, დაცვის წესი უცვლელია

### 2026-10-08 — Phase 0.12: Anthropic-ის რჩევების კვლევა, ძრავის დამოუკიდებელი შეფასება, ADR-0007 (branch `engine/guidance-2026-10`)
- მფლობელმა ითხოვა Anthropic-ის უახლესი SDLC-რჩევების სიღრმისეული კვლევა და ძრავის ობიექტური შეფასება. გზად შეასწორა: პირველი ორკესტრაცია მხოლოდ Claude-ის მოდელებით უნდა იყოს (Fable 5.1, Opus 5.5, Sonnet 5.5, Haiku 5.5), სხვა მოდელები სრულად გამოირიცხება
- R1 (Sonnet, 65 ოფიციალური გვერდი) → `sdlc/research/anthropic-guidance-2026-10.md`, 241 პუნქტი; R2 (Sonnet) → `sdlc/research/claude-models-2026-10.md`. Haiku 5.5 ნამდვილია (`claude-haiku-5-5`), მაგრამ Claude Code ≥ 2.1.293-ს ითხოვს, აქ კი 2.1.292 დგას. მთავარი ფაქტები მთავარმა სესიამ თავადაც გადაამოწმა სამ ოფიციალურ გვერდზე (models overview, advisor, model-config)
- auditor-მა (Opus, სუფთა კონტექსტი — ძრავის ავტორს საკუთარი ნამუშევარი არ შეუფასებია) დაწერა `sdlc/research/engine-assessment-2026-10.md`: „READY FOR DRY RUN: no — 6 blocking items" (A1–A6), 11 რეკომენდებული, 8 კითხვა საცდელი გაშვებისთვის. ADR-0007-ის ერთი რიცხვი გასწორდა (B11)
- ADR-0007 (proposed): მხოლოდ Claude — მფლობელის გადაწყვეტილება; როლების სამი ვარიანტი, რეკომენდებულია B (auditor Fable 5.1-ზე); Haiku 5.5-ის `scout` `claude update`-ის შემდეგ; Codex-ის ჩაკეტვა ამ რეპოში. ყველაფერს მფლობელის „კი" სჭირდება; settings და აგენტები უცვლელია
- ცოცხლად ნაპოვნი: (1) კონტექსტის მზომი advisor-იან ნაბიჯზე ორმაგად ითვლის (117% vs ნამდვილი ≈59%); (2) ქვეაგენტის (R1-ის) შეკუმშვამ ამ სესიის handoff-ი ორჯერ გადაწერა — ხელით აღდგა (ცოცხალი → `v5`, `v3` → ცოცხალი); (3) შეჯამებაში ჩაწერილმა ბმულის მაგალითმა სტრუქტურის შემოწმება ჩააგდო — `v5`-ში ერთი ჰარით გაუვნებელყოფილია; (4) `compact`-ზე SessionStart-მა წინა handoff-ი ჩატვირთა; (5) სესია Opus-ზე მუშაობდა, რადგან `/model` პროექტის პარამეტრს ჯობნის
- ნამდვილი შეკუმშვის ჯაჭვი დამტკიცდა: 23:00-ზე checkpoint-ი არქივში გადავიდა (`v2`), ახალ handoff-ს `compaction: 3` და `injection_flags` აქვს, მეხსიერება nonce-იანი ღობით ჩაიტვირთა

### 2026-10-08 — Phase 0.13: საცდელ გაშვებამდე ექვსი ხარვეზის გასწორება (branch `engine/dryrun-blockers`)
- მფლობელის არჩევანი: ADR-0007-ის ვარიანტი A (Sonnet 5.5 კოდერი; Opus 5.5 მრჩეველი, არქიტექტორი და აუდიტორი; Sonnet verifier; Haiku 5.5 `scout`); Codex და CodeRabbit ჯერ რჩება; A1–A6-ის გასწორებასა და `claude update`-ზე — „კი"; PR #6 `main`-შია (`ac5f639`)
- `claude update`: CLI 2.1.292 → 2.1.293; VS Code-ის extension → 2.1.294 (VS Code-ის გადატვირთვისას ჩაირთვება)
- A1 — საცდელი გაშვების წესი CLAUDE.md-ში, PROCESS.md-ში და `sdlc-harness`-ში. A2 — ქვეაგენტის შეკუმშვა სესიის handoff-ს აღარ გადაწერს. A3 — შეკუმშვის შემდეგ ძველი handoff აღარ ჩაიტვირთება, დაწყებისას კი უახლესი დროით ირჩევა. A4 — კონტექსტი advisor-იან ნაბიჯზეც სწორად ითვლება (`transcript_usage.py`). A5 — მეხსიერების ფაილებში ბმულები აღარ მოწმდება. A6 — მოდელისა და ვერსიის ჩაწერა საცდელი გაშვების წესშია; ძრავის აგენტებს მოდელი გამოძახებისას არ გადაეცემა
- ყოველი ახალი შემოწმება ჯერ ძველ კოდზე (`origin/main`) გაეშვა და იქ ჩავარდა (7/7 MISSED), ახალ კოდზე კი მუშაობს. საბოლოო შემოწმება: სტრუქტურა — 380 შემოწმება, PASS (92 ფაილი); წვრთნა — 138 შემთხვევა, ARMED; „უმოქმედო" hook-ებით — DEAD; ახალი სესიის დასაწყისი — 8 405 სიმბოლო, მოჭრის გარეშე
- ADR-0007-ში მფლობელის არჩევანი ჩაიწერა; დაემატა `scout`-ი, `AGENT_MODELS`-ში — `claude-haiku-5-5`. OpenAI-ისა და ღია მოდელების „მეორე ეტაპის" ხსენებები (README, PROCESS, todo, ორკესტრაციის დიზაინი, models-gpu-graphs) ჩანაცვლებულად მოინიშნა
- ახალი სესია დაწყებისას ამაღამინდელი გრძელი handoff-ის თავს მიიღებდა (12 000 სიმბოლოზე მოჭრილს, ძველი „შემდეგი ნაბიჯით"). ამიტომ ის არქივში გადავიდა (`v6`), ცოცხალი handoff კი მოკლე, მიმდინარე ჩანაწერია

### 2026-10-08 — Phase 0.14: Cursor-ის მხარდაჭერა (branch `engine/cursor-support`)
- მოთხოვნა (Cursor-ის სესია, თანამშრომლის კომპიუტერი): პროექტი Cursor-ზე მოერგოს
- ცოცხალი შემოწმება Cursor 3.23.23-ში — დროებითი, მხოლოდ ჩამწერი hook რეპოს გარეთ: Cursor `.claude/settings.json`-ის hook-ებს უშვებს, მაგრამ ბრძანებას `Shell`-ად აწვდის, ამიტომ `guard.py` და `commit_secrets.py` Cursor-ში არაფერს ბლოკავდა; აგენტების `tools:` ველს Cursor არ კითხულობს, ანუ read-only აგენტებს წერა შეეძლოთ; შეჯამებას hook ვერ ხედავს, `transcript_path` ცარიელია და ჩანაწერში ტოკენები არ წერია; ორი წყაროს hook-ები ერთსა და იმავე მოქმედებაზე ორჯერ ეშვება
- აიწყო: `.cursor/hooks.json` + `.cursor/hooks/cursor_adapter.py` (იგივე ფუნქციები `.claude/hooks/`-იდან), `.claude/hooks/hook_host.py` (Claude-ის hook-ები Cursor-ის მოვლენაზე ჩუმდება), `.cursor/agents/` (ოთხი აგენტი იგივე ინსტრუქციით + `advisor`), `.cursor/rules/cursor-runtime.mdc`, `.cursor/cli.json`, Cursor-ის შემოწმებები `check_structure.py`-ში, `sdlc/checks/drill_cursor.py` → [ADR-0008](docs/decisions/0008-cursor-support.md) (proposed)
- შემოწმებები: სტრუქტურა — 470, PASS (104 ფაილი); წვრთნა — 218 შემთხვევა, ARMED (80 ახალი); „უმოქმედო" hook-ებით — DEAD
- ცოცხლად Cursor-ში: force-push, `rm -rf` და `.env`-ის ჩაწერა დაიბლოკა; ქვეაგენტის გამოძახებები მისი სახელითა და მოდელით ჩაიწერა
- გზად: Cursor-მა აგენტს `.cursorignore`-ის ჩაწერა არ მისცა (ადამიანს დარჩა); სატესტო hook-ის ფაილის წაშლის შემდეგ Cursor-მა ძველი კონფიგურაცია შეინახა და ფანჯრის გადატვირთვამდე ყველა ხელსაწყო დაბლოკა → `tasks/lessons.md`

### 2026-10-08 — Phase 0.15: მხოლოდ Cursor — Claude Code-ის ფაილები წაიშალა (branch `engine/cursor-support`, ლოკალურად)
- მოთხოვნა (იგივე სესია): „კლოდის ფაილები წაშალე, თუ მათი ალტერნატივა კურსორისთვის მზადაა"; შემდეგ: GitHub-ზე არაფერი ატვირთო, ლოკალური commit-ები შეიძლება
- მზადყოფნის შემოწმება: მზად იყო `.claude/agents/` (`.cursor/agents/`), `.claude/settings.json`-ის hook-ები და deny (`.cursor/hooks.json`, hook-ები, `.cursor/cli.json`) და Claude-ის მეხსიერების hook-ები (შეკუმშვის შემდეგ checkpoint). მზად **არ** იყო: შემოწმებები `.claude/hooks/`-ში (Cursor-ის ადაპტერი მათ იყენებდა), ბრძანებები `.claude/skills/`-ში, წესები `CLAUDE.md`-ში და settings-ის „იკითხე" სია (პროგრამის დაყენება, `curl`/`wget`, `gh pr merge`)
- მზად გახდა, მერე წაიშალა: შემოწმებები გადავიდა `.cursor/hooks/`-ში, ბრძანებები `.cursor/skills/`-ში (ტექსტი Cursor-ის ტერმინებით), წესები `AGENTS.md`-ში; guard ახლა დასტურს ითხოვს პროგრამის დაყენებაზე, `curl`/`wget`-ზე და `gh pr merge`-ზე. ამის შემდეგ `git rm`: `CLAUDE.md`, `.claude/settings.json`, `.claude/agents/`, `memory_handoff.py`, `context_monitor.py`, `transcript_usage.py`, `hook_host.py`, `.cursor/rules/cursor-runtime.mdc`; ყველაფერი აღდგენადია commit `ba06fac`-იდან
- წვრთნა Cursor-ის ფორმატზე გადაიწერა: Claude-ის ეპოქის ყველა ბლოკის, კითხვის, საიდუმლოების, მეხსიერებისა და რედაქციის შემთხვევა ადაპტერით მოწმდება; Claude-ის მექანიზმების შემთხვევები (PostCompact, კონტექსტის მზომი, advisor-ის ტოკენები) მათთან ერთად წავიდა
- შემოწმებები: სტრუქტურა — 398, PASS (93 ფაილი); წვრთნა — 166 შემთხვევა, ARMED (1 გამოტოვებული — gitleaks აქ არ დგას); „უმოქმედო" hook-ით — DEAD (hook-ის ყველა შემთხვევა ჩავარდა)
- ცნობილი ხარვეზი: Cursor-ის hook ფაილის ცვლილებაზე „იკითხე"-ს ვერ ამბობს, ამიტომ `.cursor/hooks.json`-ის, `.cursor/hooks/`-ისა და CI-ის შეცვლამდე თანხმობა წესზე და PR-ის განხილვაზე დგას → [ADR-0008](docs/decisions/0008-cursor-support.md), `tasks/todo.md`

### 2026-10-08 — Phase 0.16: PR #10 ლოკალურად, Cursor-ის ძრავაზე (branch `engine/cursor-support`, ატვირთვის გარეშე)
- მოთხოვნა: PR #10-ის ყველა ცვლილება აქ განხორციელდეს; GitHub-ზე არაფერი ატვირთოს; საჭიროების შემთხვევაში მხოლოდ ლოკალური commit
- `main` უკვე `566fd00`-ზეა (PR #8). PR #10 ამაზე ზის და Claude-ის ფაილებს ცვლის; ეს branch ის ფაილები აღარ აქვს, ამიტომ ქცევა `.cursor/hooks/`-ში გადმოვიდა
- გადმოვიდა: ყოველი `rm` და `git rm` ჯერ კითხულობს (`grep rm` არა); read-only აგენტს `git stash list`/`show` შეუძლია, `git stash` და `pop` კი არა; გრძელ handoff-ს მხოლოდ შუა ეჭრება; `review_rounds.py` ნამდვილ commit-ებს ამოწმებს; `run_gates.py`-ის ცუდი კონფიგი INCONCLUSIVE-ა; `agent_report.py` გაფუჭებულ ხაზს გამოტოვებს, ტოკენების გარეშე ჩანაწერზე კი INCONCLUSIVE-ს ამბობს; წვრთნის ასლი ჩაუწერელ, არაიგნორირებულ ფაილსაც იღებს (B5); gitleaks-ის შემთხვევა ფიქსირებულ მნიშვნელობას იყენებს
- საცდელი გაშვება წესებიდან ამოვიდა. Codex იშლება, CodeRabbit მოგვიანებით. რეპო public-ია. sandbox — [ADR-0009](docs/decisions/0009-bash-sandbox.md) (PR #10-ში 0008), ვარიანტი ღიაა, არაფერი ჩართულა
- არ გადმოვიდა, რადგან Cursor-ში აღარ არსებობს: `context_monitor.py`-ის ცრუ განგაშის გასწორება (ფაილი Claude-ის ეპოქასთან ერთად წაიშალა; Cursor ტოკენებს არ წერს) და `.claude/settings.json`-ის ორი ცარიელი ხაზი
- შემოწმებები: სტრუქტურა — 402, PASS (94 ფაილი); წვრთნა — 191 შემთხვევა, ARMED (1 გამოტოვებული — gitleaks აქ არ დგას); „უმოქმედო" hook-ით — DEAD
- ატვირთვა არ მომხდარა

## In Progress (მიმდინარე)
- `engine/cursor-support` (ლოკალური branch) — მხოლოდ Cursor (ADR-0008) და PR #10-ის გადმოტანა; მფლობელის განხილვას ელოდება; ატვირთვა და PR — მხოლოდ თანხმობით

## Next Steps (შემდეგი ნაბიჯები)
1. მფლობელი: `engine/cursor-support`-ის განხილვა — ის Claude Code-ის მხარდაჭერას მთელი გუნდისთვის აუქმებს; თანხმობის შემთხვევაში ატვირთვა და PR. PR #10 GitHub-ზე კვლავ ღიაა და ამ branch-ში არ შერწყმულა
2. Cursor-ში `Asterbit` საქაღალდედ გახსნა → ახალი ჩატი Claude Sonnet 5.5-ით → ASTERBIT-RUNTIME ხაზის ჩანაწერი აქ; პირველ სესიაში `tasks/todo.md`-ის ხუთი Cursor-ის შემოწმება
3. მფლობელი: `/sdlc-wrap`-ის ლოგიკისა (B8) და მეხსიერების ცრუ განგაშის გასწორების (B6) შეთანხმება; ADR-0009-ის ვარიანტის არჩევა
4. Phase 0-ის კარიბჭე: auditor მთელ ძრავაზე → ADR-0001…0003, ADR-0005…0009 და PROCESS.md-ის დამტკიცება; მერე, როცა მფლობელი იდეას აირჩევს — `/sdlc-intent`. C1–C8 პირველ ნამდვილ ეტაპებზე პასუხდება (`sdlc/research/engine-assessment-2026-10.md`)
