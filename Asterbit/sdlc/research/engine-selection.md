# ძრავის შერჩევა — AI Pulse Georgia-ს კოლექციიდან

> წყარო: [awesome-ai-pulse-georgia](https://github.com/tornikebolokadze1-cyber/awesome-ai-pulse-georgia) (ბოლოს განახლდა 2026-10-05). განხილული სექციები: „Claude Code პლაგინები და უნარები", „მეხსიერება, RAG და Vector DB", „კოდბაზის გრაფები და კონტექსტი", „ტოკენების დაზოგვა და ხარჯი". რეპოების აქტიურობა GitHub API-ით შემოწმდა 2026-10-06-ს.

## 1. კრიტერიუმები

1. ერგება შენს სასიცოცხლო ციკლს და playbook-ის არტეფაქტების ჯაჭვს (intent → spec → plan → კოდი).
2. იყენებს Claude Code-ის მშობლიურ ნაწილებს (skills, subagents, hooks) — ჩაკეტვის (lock-in) გარეშე.
3. გასაგები და მსუბუქია მარტოხელა მფლობელისთვის, რომელიც კოდს არ კითხულობს.
4. ცოცხალია: ბოლო 12 თვეში განახლდა და archived არ არის.
5. თუ შესაძლებელია, უკვე დაყენებულია.

## 2. „ძრავის" კანდიდატები (პროცესის ჩარჩოები)

| კანდიდატი | რა არის | ★ / აქტიურობა | შეფასება |
|---|---|---|---|
| **Superpowers** (obra) | skills-ზე აგებული მეთოდოლოგია: brainstorming → plans → TDD → debugging → verification | ≈296k · push 2026-10-06 | ✅ **არჩეულია მეთოდების ბიბლიოთეკად.** დაყენებულია; რადგან skills-ია, ნაწილ-ნაწილ ერგება ჩვენს ეტაპებს |
| Spec Kit (GitHub) | spec-driven development: `/specify`, `/plan`, `/tasks`, `/implement` | ≈140k · push 2026-10-05 | ⚠ კარგი იდეები (ყოველ ცვლილებას ცალკე spec საქაღალდე), მაგრამ საკუთარი ბრძანებები და საქაღალდეები მეორე ჭეშმარიტების წყაროს შექმნიდა |
| GSD (Get Shit Done) | context engineering + spec-driven სისტემა | ≈64k · **archived 2026-05-31** | ❌ აღარ ვითარდება. იდეა ავიღეთ: ერთი STATE ფაილი → `docs/sdlc-state.md` |
| BMAD-METHOD | analyst / PM / architect / dev / QA აგენტების გუნდი | ≈54k | ⚠ ერთი ადამიანისთვის ძალიან ბევრი ცერემონიაა |
| Everything Claude Code | უზარმაზარი კოლექცია: skills, memory, security, „instincts" | ≈273k | ⚠ შენს გლობალურ hook-ებსა და წესებს ფარავს → კონფლიქტი და კონტექსტის გადატვირთვა |
| oh-my-claudecode | ორკესტრაციის რეჟიმები (autopilot, team, ralph) | ≈40k · დაყენებულია | ⚠ ძლიერია, მაგრამ მძიმე; მხოლოდ საჭიროებისას |
| Karpathy Skills | ერთი CLAUDE.md კოდირების ქცევის წესებით | ≈217k · დაყენებულია (karpathy-guidelines) | ✅ ქცევის წესებად, არა პროცესად |
| Agent Skills (Addy Osmani) | 20+ engineering workflow skill | ≈101k | ⚪ Superpowers-ს ჰგავს; დუბლირებას ვერიდებით |
| Matt Pocock Skills | პირადი skills, TypeScript/React-ზე ორიენტირებული | ≈277k | ⚪ საჭიროების მიხედვით, თუ სტეკი TS/React იქნება |
| შენი claude-code-setup | 17 წესი, 7 hook, `/setup` ბრძანება | 13★ | ⚪ შენს გლობალურ `~/.claude`-ს ფარავს; hook-ების წყაროდ გამოგვადგება |

## 3. ეტაპების დამატებები — შემოდის მხოლოდ შესაბამისი ეტაპის ADR-ით

| ეტაპი | ინსტრუმენტი | რატომ |
|---|---|---|
| 1 Intent | PM Skills (phuryn) — დაყენებულია `pm-product-discovery`-ად | ინტერვიუს სკრიპტები, დაშვებების რუკა |
| 2 Architecture | Archify (≈78k) | სისტემის დიაგრამები Before / Delta / After შედარებით (სურვილისამებრ) |
| 3 Harness, 8 Secure | SkillSpector (NVIDIA, ≈19k) | skill-ების შემოწმება ინსტალაციამდე: prompt injection, მონაცემების გატანა, supply chain |
| 8 Secure | Defending Code Reference Harness (Anthropic) | threat modeling → სკანირება → triage → patch-ის ნიმუში (სასწავლოა, აღარ ვითარდება) |
| 6–7 Build / review, ეტაპი 2 | Codex Plugin (OpenAI, ≈34k) — დაყენებულია | სხვა მომწოდებლის reviewer Claude Code-ის შიგნიდან |
| მეხსიერება | Obsidian Skills (kepano, ≈49k) | Obsidian-ის სწორი markdown, Bases, Canvas — თუ Obsidian-ს ავირჩევთ |
| მეხსიერება | QMD / MemPalace / claude-mem / agentmemory / Graphiti | ინტერვიუ → ADR-0004 |
| 9 Observe | agentsview (≈6k) | ლოკალური დაფა: სესიები, ტოკენების ხარჯი |
| კოდის კონტექსტი (Build-ის შემდეგ) | Graphify (დაყენებულია) / codebase-memory-mcp | კოდბაზის გრაფი, როცა კოდი გაიზრდება |

## 4. დასკვნა

ძრავა = საკუთარი თხელი ჩარჩო + Superpowers, როგორც მეთოდების ბიბლიოთეკა ([ADR-0002](../../docs/decisions/0002-engine-base-native-plus-superpowers.md)).

რატომ არა ერთი მზა ჩარჩო: თითოეულს საკუთარი არტეფაქტების სახელები, საქაღალდეები და ნაკადი მოაქვს, ჩვენ კი ორი ჭეშმარიტების წყარო გვექნებოდა. გარდა ამისა, შენი სასიცოცხლო ციკლი ნებისმიერ ცალკეულ ჩარჩოზე სრულია: არცერთს არ აქვს ცალკე harness-ის, უსაფრთხოებისა და დაკვირვების ეტაპები ისე, როგორც შენ აღწერე. ამ ეტაპზე ახალი ინსტალაცია არ გვჭირდება.

## 5. შენიშვნა კოლექციისთვის

GSD კოლექციაში აქტიურ პროექტადაა აღწერილი, მაგრამ რეპო 2026-05-31-დან archived-ია. ღირს README-ში მისი მონიშვნა.
