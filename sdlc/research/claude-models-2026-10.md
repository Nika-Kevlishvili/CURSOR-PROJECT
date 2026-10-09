---
title: Claude-ის მოდელები და ორკესტრაცია Claude Code-ში (2026 წლის ოქტომბერი)
date: 2026-10-07
type: research
sources: მხოლოდ ოფიციალური Anthropic-ის გვერდები (platform.claude.com/docs, code.claude.com/docs, anthropic.com, support.claude.com) და ამ მანქანის ტრანსკრიპტების usage/model ველები
status: კვლევის შენიშვნა, არა დამტკიცებული არტეფაქტი; ფასები და ვერსიები სწრაფად იცვლება
---

# Claude-ის მოდელები და ორკესტრაცია Claude Code-ში

> **როგორ ვკითხულობთ.** ყველა მონაცემი 2026-10-07-ს არის ამოღებული ოფიციალური გვერდიდან; ბმული სტრიქონთან ან სექციის ბოლოს წერია. გვერდებიდან მიღებული ტექსტი მონაცემად მივიჩნიეთ და არა ინსტრუქციად. `docs.claude.com` ახლა `platform.claude.com/docs`-ზე გადამისამართდება. სადაც ორი ოფიციალური გვერდი ერთმანეთს ეწინააღმდეგება, ორივე მნიშვნელობა მივუთითეთ (§8). „**ლოკალური დაკვირვება**" ამ მანქანის ტრანსკრიპტებიდანაა გაზომილი და Anthropic-ის მტკიცება არ არის.

**Haiku 5.5 დადასტურებულია.** ID: `claude-haiku-5-5`; გამოშვება: **2026-10-07**, ანუ კვლევის დღეს. ეს ოთხმა ოფიციალურმა წყარომ დაადასტურა: [models overview](https://platform.claude.com/docs/en/models/overview), [Haiku 5.5-ის გვერდი](https://platform.claude.com/docs/en/models/haiku-5-5/overview), [deprecations-ის ცხრილი](https://platform.claude.com/docs/en/about-claude/model-deprecations) და [announcement](https://www.anthropic.com/claude-haiku-5-5). ეს უახლესი Haiku-ა; წინა — `claude-haiku-4-5-20251001` (Active, გაუქმება „არაუადრეს 2026-10-15"). Claude Code-ში Haiku 5.5-ს **v2.1.293 ან უფრო ახალი** სჭირდება. ამ მანქანაზე v2.1.292 დგას (CLI-ც და VS Code extension-იც), ამიტომ ჯერ `claude update` გჭირდება; მანამდე `haiku` alias Haiku 4.5-ს ნიშნავს ([version history](https://code.claude.com/docs/en/model-config)).

## 1. Four models (ოთხი მოდელი)

| მოდელი | API ID (Claude Code alias) | გამოშვება | კონტექსტი / max output | $ / 1M ტოკენი: input / output | წყარო |
|---|---|---|---|---|---|
| Claude Fable 5.1 | `claude-fable-5-1` (`fable`) | 2026-09-01 | 1M / 128K | 10 / 50 | [model page](https://platform.claude.com/docs/en/models/fable-5-1/overview) |
| Claude Opus 5.5 | `claude-opus-5-5` (`opus`) | 2026-09-22 | 1M / 128K | 4 / 20 | [model page](https://platform.claude.com/docs/en/models/opus-5-5/overview) |
| Claude Sonnet 5.5 | `claude-sonnet-5-5` (`sonnet`) | 2026-09-28 | 1M / 128K | 2 / 10 | [model page](https://platform.claude.com/docs/en/models/sonnet-5-5/overview) |
| Claude Haiku 5.5 | `claude-haiku-5-5` (`haiku`) | 2026-10-07 | 1M / 128K | 0.10 / 0.50 (prompt ≤ 100K); 0.50 / 2.50 (prompt > 100K) | [model page](https://platform.claude.com/docs/en/models/haiku-5-5/overview) |

| მოდელი | cache write 5 წთ / 1 სთ | cache read | effort-ის დონეები | default effort: API / Claude Code | Claude Code, მინ. ვერსია | წყარო |
|---|---|---|---|---|---|---|
| Fable 5.1 | 12.50 / 20 | 0.25 | low, medium, high, xhigh, max | high / high | v2.1.257 | [pricing](https://platform.claude.com/docs/en/about-claude/pricing), [effort](https://platform.claude.com/docs/en/build-with-claude/effort) |
| Opus 5.5 | 5 / 8 | 0.20 | იგივე ხუთი | medium / medium | v2.1.280 | იგივე |
| Sonnet 5.5 | 2.50 / 4 | **0.10** (2026-10-07-დან, იხ. §8) | იგივე ხუთი | high / **medium** | v2.1.284 | იგივე |
| Haiku 5.5 | 0.125 / 0.20 (≤ 100K); 0.625 / 1 (> 100K) | 0.01 (≤ 100K); 0.05 (> 100K) | იგივე ხუთი | medium / medium | v2.1.293 | იგივე + [model-config](https://code.claude.com/docs/en/model-config) |

| მოდელი | Anthropic-ის სიტყვებით | გეგმები და Claude Code | წყარო |
|---|---|---|---|
| Fable 5.1 | „For demanding reasoning and long-horizon agentic work". Claude Code-ის დოკუმენტი: „the most capable models in Claude Code, suited to tasks larger than a single sitting". Mythos 5.1 იგივე მოდელია სხვა safeguards-ით და მხოლოდ verified ორგანიზაციებისთვისაა | ყველა ფასიან გეგმაზე. Max-ზე და premium seat-ებზე — weekly limit-ის 50%-მდე დამატებითი ფასის გარეშე; Pro-ზე და standard seat-ებზე — მხოლოდ pay-as-you-go usage credits; API/usage-based Enterprise — API ტარიფით. არც ერთ გეგმაზე default არ არის: `/model fable` | [support](https://support.claude.com/en/articles/15424964-claude-fable-models-on-your-plan), [announcement](https://www.anthropic.com/claude-fable-and-mythos-5-1) |
| Opus 5.5 | „For long-running agentic coding and knowledge work". „It performs at the level of Claude Fable 5.1 on most work and costs 40% less to run than Opus 5"; „delegates to subagents far more effectively and checks its own work in creative ways" | Claude Code-ის default Pro, Max, Team, Enterprise და API-ზე. Fast mode (research preview): $8 / $40 | [announcement](https://www.anthropic.com/claude-opus-5-5), [model-config](https://code.claude.com/docs/en/model-config) |
| Sonnet 5.5 | „The best combination of speed and intelligence". „A faster, lower-cost complement to Claude Opus 5.5 … strongest at well-scoped everyday tasks, fixing bugs, and creating polished documents, slides, and spreadsheets" | 1M ფანჯარა ყველა გეგმაზე, Pro-ზეც; usage credits არ სჭირდება | [announcement](https://www.anthropic.com/claude-sonnet-5-5), [model-config](https://code.claude.com/docs/en/model-config) |
| Haiku 5.5 | „For high-volume, latency-sensitive tasks such as classification, extraction, and routing". „The cheapest, fastest, and most capable small model we've ever released"; „pairs well with Opus 5.5 and Sonnet 5.5 as a subagent on coding work"; „our first Haiku-class model to come with an adjustable effort setting" | 1M ფანჯარა ყველა გეგმაზე. Claude Code-ში გრძელი prompt (> 100K) უფრო ძვირია | [announcement](https://www.anthropic.com/claude-haiku-5-5), [model-config](https://code.claude.com/docs/en/model-config) |

შენიშვნები. თარიღის გარეშე ID-ები თავადვე „pinned snapshot"-ია. Batch API −50%; batch-ზე max output 300K ცალკე beta header-ით. გაუქმება „არაუადრეს": 2027-09-01 (Fable 5.1), 2027-09-22 (Opus 5.5), 2027-09-28 (Sonnet 5.5), 2027-10-07 (Haiku 5.5). Thinking ოთხივეზე adaptive-ია და Claude Code-ში მის გამორთვას ვერ შეძლებ ([costs](https://code.claude.com/docs/en/costs)). Fable 5.1-ის [announcement](https://www.anthropic.com/claude-fable-and-mythos-5-1) ამბობს: „Fable 5.1 defaults to High effort in Claude Code".

## 2. (a) Main session model: who wins (მთავარი სესიის მოდელი)

1. **რიგი** (მაღლიდან დაბლა, [model-config](https://code.claude.com/docs/en/model-config), „Setting your model"): (1) `/model <alias|name>` სესიაში ან `/model`-ის picker; (2) `claude --model`; (3) `ANTHROPIC_MODEL`; (4) `model` ველი settings-ფაილში; (5) `ANTHROPIC_DEFAULT_MODEL` — მხოლოდ თუ არაფერი სხვა ირჩევს. ცალკე შრეებია: ორგანიზაციის default (Enterprise; „override user selection" რეჟიმში user, project და local ფაილების `model`-ს ჯობნის) და ანგარიშის default — Pro, Max, Team, Enterprise, API-ზე **Opus 5.5**.
2. **ფაილებს შორის** (`model` ველისთვის, [settings](https://code.claude.com/docs/en/settings), „Settings precedence"): managed > `claude --settings` > `.claude/settings.local.json` > `.claude/settings.json` (პროექტის) > `~/.claude/settings.json` (მომხმარებლის). ცვლადები ამ რიგში არ შედის: `ANTHROPIC_MODEL` ყველა ფაილის `model`-ს ჯობნის, `--model` კი ორივეს. managed `model` მხოლოდ საწყისი მნიშვნელობაა, ჩაკეტვა `availableModels`/`deniedModels`-ია.
3. **`/model` ინახავს არჩევანს მომხმარებლის ფაილში** (`Enter` — შენახვა; `s` — მხოლოდ ამ სესიისთვის). რადგან პროექტის ფაილი მომხმარებლისას ჯობნის, ჩვენს რეპოში შენახული არჩევანი ახალ სესიაზე არ მოქმედებს; დოკუმენტი ასე ამბობს: „Your `/model` choice is still saved; it's outranked". `/advisor` იგივენაირად ინახება `~/.claude/settings.json`-ში.
4. **განახლებული სესია** (`--resume`, `--continue`) ინარჩუნებს ტრანსკრიპტის მოდელს, თუ ახალ გაშვებაზე `--model` ან `ANTHROPIC_MODEL` არ მიგითითებია.
5. **VS Code** ([vs-code](https://code.claude.com/docs/en/vs-code)): picker იგივეა, რაც `/model` (დააჭირე მოდელის სახელს prompt-ის ქვემოთ; v2.1.284-დან `/model`-იც ხსნის). extension-ის დოკუმენტირებულ settings-ცხრილში მოდელის ველი არ არის; extension იზიარებს `~/.claude/settings.json`-ს; `environmentVariables` Claude პროცესს ცვლადებს აწვდის (იქ დაყენებული `ANTHROPIC_MODEL` ფაილებს გადაფარავდა — ეს დასკვნაა და პირდაპირ დოკუმენტში არ წერია).
6. **Alias-ები:** `default`, `best` (Fable, თუ გაქვს; სხვაგვარად `opus`), `fable`, `opus`, `sonnet`, `haiku`, `opusplan`, `sonnet[1m]`/`opus[1m]` (Sonnet 5.5-ზე და Opus 5.5-ზე უკვე 1M არის), ქვეაგენტებისთვის `inherit`. Alias მოძრავია: `fable`→5.1 (v2.1.257-დან), `opus`→5.5 (v2.1.280), `sonnet`→5.5 (v2.1.284), `haiku`→5.5 (v2.1.293). დასამაგრებლად — სრული ID ან `ANTHROPIC_DEFAULT_*_MODEL`.

**ლოკალური დაკვირვება.** პროექტის ფაილში `model: claude-sonnet-5-5` 2026-10-07T09:32Z-ზე გაჩნდა (commit `31573a1`). ამის შემდეგ დაწყებული სესიებიდან `e3bc42f4` (VS Code, `/model`-ის გარეშე) Sonnet 5.5-ზე იმუშავა, ხოლო `df3e2417` (10:35Z) და `31c2c6fd` (11:20Z) — Opus 5.5-ზე; ორივეში `/model` პირველ პასუხამდე გაიხსნა (ტრანსკრიპტში არჩეული მნიშვნელობა არ ინახება). ეს დოკუმენტს ემთხვევა: სესიაში `/model` პროექტის ფაილს ჯობნის. მომხმარებლის ფაილში ახლა `model: "opus"` და `modelSettings."claude-opus-5-5".effortLevel: "xhigh"` წერია, ამიტომ Opus 5.5 ამ რეპოში `xhigh`-ზე მუშაობს; `/effort max` ერთ სესიაზე მოქმედებს. რომელი მოდელი რეალურად მუშაობს, `/status`-ში ჩანს (გაშვების header-ი პროექტის ფაილს ასახელებს). ამ shell-ში `ANTHROPIC_MODEL`, `ANTHROPIC_DEFAULT_MODEL`, `CLAUDE_CODE_SUBAGENT_MODEL`, `DISABLE_TELEMETRY` დაყენებული არ არის; VS Code-ის `environmentVariables` არ შემოგვიმოწმებია.

წყარო (2026-10-07): [model-config](https://code.claude.com/docs/en/model-config), [settings](https://code.claude.com/docs/en/settings), [settings-reference](https://code.claude.com/docs/en/settings-reference), [vs-code](https://code.claude.com/docs/en/vs-code).

## 3. (b) Advisor (მრჩეველი მოდელი, რომელსაც მთავარი მოდელი რთულ მომენტში ეკითხება)

**ჩართვა.** `/advisor` (picker; `/advisor off` გამორთავს), `"advisorModel"` settings-ში, ან `claude --advisor <model>` ერთი სესიისთვის; სრულად გამორთვა — `CLAUDE_CODE_DISABLE_ADVISOR_TOOL=1`. `advisorModel` იღებს alias-ებს `fable`, `opus`, `sonnet` ან სრულ ID-ს; `haiku` alias არ არის, Haiku 5.5-ისთვის `claude-haiku-5-5` გჭირდება. API-ში ეს beta tool-ია (`advisor_20260301`), Claude Code-ში — experimental, მხოლოდ Anthropic API-ზე (არა Bedrock, Vertex, Foundry, Claude Platform on AWS). ჩვენი პროექტის ფაილი (`advisorModel: claude-opus-5-5`) დასაშვებია.

**დასაშვები წყვილები** (advisor მთავარზე არანაკლებ ძლიერი უნდა იყოს):

| მთავარი მოდელი | დასაშვები advisor |
|---|---|
| Fable 5.1 | მხოლოდ Fable 5.1 (Opus ან Sonnet advisor-ს Claude Code Fable-ზე არ ამაგრებს) |
| Opus 5.5 / Opus 5 | Fable; Opus 5 და უფრო ახალი |
| Sonnet 5.5 | Fable; Opus 5 და უფრო ახალი; Sonnet 5.5 |
| Haiku 5.5 / Sonnet 5 | Fable; Opus 4.7 და უფრო ახალი; Sonnet 5 და უფრო ახალი; Haiku 5.5 |
| ძველი (Haiku 4.5, Sonnet 4.6, Opus 4.6–4.8) | იხ. [advisor](https://code.claude.com/docs/en/advisor#choose-an-advisor-model); Haiku 4.5 თვითონ advisor ვერ იქნება |

ე.ი.: Sonnet 5.5 + Opus 5.5 ✔; Sonnet 5.5 + Fable 5.1 ✔ (საჭიროა Fable-ის წვდომა, Pro/standard seat-ზე — ერთჯერადი usage-credits თანხმობა `/model fable`-ით); Haiku 5.5 + Opus 5.5 ან Fable ✔; Opus 5.5 + Opus 5.5 ✔ („a second Opus reviews the first"); Sonnet 5.5 + Haiku 5.5 ✘; Opus 5.5 + Sonnet ✘; Fable + Opus/Sonnet ✘. API-ის დონეზეც იგივეა (+ Mythos); Sonnet 5.5 executor-ზე Opus 4.8, Opus 4.7 და Sonnet 5 advisor-ებს API 400-ით აბრუნებს. ქვეაგენტები დაყენებულ advisor-ს იღებენ და წყვილს თავისი მოდელით ამოწმებენ.

**როდის ითიშება უხმაუროდ** (შეცდომის გარეშე): API-მ წყვილი უარყო (Claude Code იმავე მოთხოვნას advisor-ის გარეშე ხელახლა აგზავნის); advisor მთავარზე სუსტია (მხოლოდ notification); `availableModels` მას გამორიცხავს; feature-flag-ების წამოღება გამორთულია (მაგ. `DISABLE_TELEMETRY`); Fable-ის თანხმობა მიცემული არ არის; provider არ არის Anthropic API (ან gateway მოთხოვნას უცვლელად არ აწვდის). თუ advisor-ის ზარი ჩავარდა, სესიის ეკრანზე ჩანს `Advisor unavailable (<error_code>)` და მთავარი მოდელი ანგარიშს აგრძელებს (API-ში ეს `advisor_tool_result_error`-ია).

**ხარჯი.** advisor-ის ტოკენები advisor-ის ტარიფით იწერება: API-ზე — მისი input/output ფასით; გამოწერაზე — იმავე plan limit-ში (Fable advisor — usage credits-ში, სადაც Fable-იც credits-ით იწერება). advisor ყოველ ზარზე მთელ ტრანსკრიპტს თავიდან კითხულობს და Claude Code-ში **ქეშირებული არ არის**; tool-ის განმარტება ≈1000 prompt-ტოკენს ამატებს ყოველ მოთხოვნას. ზარების რაოდენობის შეზღუდვის ან იძულების პარამეტრი Claude Code-ში არ არსებობს. advisor-ის ჩართვა/გამორთვა მთავარი მოდელის ქეშს არ აბათილებს; მოდელის გადართვა (`/model`) კი აბათილებს და შემდეგი მოთხოვნა ქეშის გარეშე იკითხება.

**ლოკალური დაკვირვება (ჩვენი არითმეტიკა list ფასებით).** ერთი advisor-ის ზარი ≈590K ტოკენიან კონტექსტზე: 590,220 ქეშის გარეშე input + 9,037 output = ≈ $2.54 Opus 5.5-ზე (2.36 + 0.18). იმავე ბიჯზე მთავარი მოდელის ორი წრე ≈ $0.27 (Opus 5.5 executor) ან ≈ $0.13 (Sonnet 5.5 executor): გრძელ კონტექსტზე ერთი advisor-ის ზარი 10–19-ჯერ ძვირია. ტრანსკრიპტშიც advisor-ის პასუხი `advisor_redacted_result`-ია (≈5,000 სიმბოლო `encrypted_content`): Opus 5.5, Fable 5.1, Sonnet 5.5 და Haiku 5.5 advisor-ები პასუხს დაშიფრულად აბრუნებენ, მთავარი მოდელი მას სერვერის მხარეს გაშიფრულს ხედავს, კლიენტი — არა. ამიტომ მფლობელი advisor-ის ტექსტს ვერ ხედავს, მხოლოდ მთავარი მოდელის გადმოცემას; ტრანსკრიპტიდან ზარის ფაქტი მოწმდება, შინაარსი — არა.

**გაზომილი სარგებელი** ([optimizing-for-cost-and-intelligence](https://platform.claude.com/docs/en/about-claude/models/optimizing-for-cost-and-intelligence)): Sonnet 5.5-მა და Haiku 5.5-მა Opus 5.5 advisor „on none of the 198 questions" არ გამოიძახეს (system prompt-ის გარეშე, რომელიც advisor-ის გამოძახებას სთხოვს), ამიტომ შედეგი executor-ის მარტო შედეგისგან noise-ის ფარგლებში იყო, tool-ის განმარტებამ კი ხარჯი 25%-ით (Sonnet 5.5) და 12%-ით (Haiku 5.5) გაზარდა. გამოძახება prompt-ით იზრდება: „one call before substantive work and one before finishing, about two to three calls per task". კოდინგის წყვილი Opus 5.5 `high` + Fable 5.1 advisor: 90.1%, $2.92 ცდაზე — Opus 5.5 მარტო `high`-ზე +1.7 პუნქტი (noise-ის ზღვარზე) ≈2.1× ფასად; Opus 5.5 მარტო `xhigh`: 91.1%, $4.11. `low` effort-ზე executor შეიძლება advisor-ს აღარ დაუძახოს. დასკვნა: „Whatever the pairing, first price the advisor's model alone at low effort; that is the baseline to beat." ქვეაგენტებზე დელეგაცია კი მხოლოდ დიდ, დამოუკიდებელ ნაწილებზე ან ერთ კონტექსტში ჩაუტევად მოცულობაზე იხდის; ერთ დამოკიდებულ ჯაჭვზე „the coordinator's model alone at lower effort came out ahead".

წყარო (2026-10-07): [advisor (Claude Code)](https://code.claude.com/docs/en/advisor), [advisor tool (API)](https://platform.claude.com/docs/en/agents-and-tools/tool-use/advisor-tool), [errors](https://code.claude.com/docs/en/errors), [costs](https://code.claude.com/docs/en/costs).

## 4. (c) Subagents: front matter `model` and `effort` (ქვეაგენტები)

- **`model`:** `sonnet`, `opus`, `haiku`, `fable`, სრული ID (`claude-opus-5-5`; იგივე მნიშვნელობები, რაც `--model`) ან `inherit`. გამოტოვებულია → იხ. რიგი. **`effort`:** `low`, `medium`, `high`, `xhigh`, `max` (დონეები მოდელზეა დამოკიდებული); frontmatter-ის effort სესიის დონეს გადაფარავს, `CLAUDE_CODE_EFFORT_LEVEL` ცვლადს — არა ([sub-agents](https://code.claude.com/docs/en/sub-agents)).
- **რიგი** (მაღლიდან): per-invocation `model` პარამეტრი → frontmatter `model` (`inherit` = მთავარი მოდელი) → `CLAUDE_CODE_SUBAGENT_MODEL` → მთავარი სესიის მოდელი. v2.1.251-მდე ცვლადი პირველი იყო. `CLAUDE_CODE_SUBAGENT_MODEL_FORCE=1` (v2.1.257-დან) ყველას ერთ მოდელზე აიძულებს; frontmatter და per-invocation მნიშვნელობები მაშინ იგნორირდება (fork-ები და `inherit`-იანი skill-ები გამონაკლისია).
- **Family alias-ის წესი:** თუ ქვეაგენტის `model: opus` და მთავარი მოდელიც Opus-ია, ქვეაგენტი მთავარის ზუსტ მოდელზე გაივლის (`[1m]`-ითაც); სხვა შემთხვევაში alias → მისი ახლანდელი ვერსია (`opus` = Opus 5.5). `CLAUDE_CODE_SUBAGENT_MODEL`-ის alias ყოველთვის alias-ის ვერსიაზე გადადის.
- **Built-in:** Explore — მთავარის მოდელი (Fable-ზე გამოწერით/Console-ით: Opus); Plan — მემკვიდრეობით; general-purpose — `CLAUDE_CODE_SUBAGENT_MODEL`, სხვაგვარად მთავარის მოდელი; claude-code-guide — Haiku; statusline-setup — Sonnet. `Explore` სახელის საკუთარი ფაილი built-in-ს ჩაანაცვლებს.
- Thinking მთავარი სესიისგან მემკვიდრეობით გადადის (v2.1.198+), ცალკე პარამეტრი არ არსებობს. თუ `availableModels` მოდელს ბლოკავს: alias → უახლესი ნებადართული ვერსია, სხვა მნიშვნელობა → მემკვიდრეობითი მოდელი (გაფრთხილებით). ქვეაგენტის მოთხოვნები იმავე plan limit-ში ითვლება. `/tasks` აჩვენებს, რომელ მოდელზე მუშაობს ქვეაგენტი.
- **ჩვენი `.claude/agents/`:** architect და auditor — `claude-opus-5-5`, `effort: high`; verifier — `claude-sonnet-5-5`, `medium`. ყველა დასაშვები ფორმაა. `model: haiku` ამ მანქანაზე (v2.1.292) Haiku 4.5-ია, `claude-haiku-5-5` კი ახალ ვერსიამდე მოგცემს შეცდომას „does not support this model; version … or newer is required" ([errors](https://code.claude.com/docs/en/errors)).

წყარო (2026-10-07): [sub-agents](https://code.claude.com/docs/en/sub-agents), [model-config](https://code.claude.com/docs/en/model-config), [errors](https://code.claude.com/docs/en/errors).

## 5. (d) `opusplan` and other hybrids (ჰიბრიდები)

- **`opusplan`:** plan mode-ში `opus`, შესრულებაში `sonnet`; Anthropic API-ზე ახლა Opus 5.5 → Sonnet 5.5. ცვლადები `ANTHROPIC_DEFAULT_OPUS_MODEL`/`ANTHROPIC_DEFAULT_SONNET_MODEL` მათ გადააყენებს; `/model opusplan[1m]` — v2.1.265-დან (უფრო ძველზე — `--model` ან `model` ველი). დოკუმენტი advisor-ს ასე ადარებს: `opusplan` მოდელს plan-ის საზღვარზე ცვლის, advisor-ს Claude თავად იძახებს შუა ამოცანაში ([model-config](https://code.claude.com/docs/en/model-config)). თითო მოდელს საკუთარი prompt cache აქვს, ამიტომ გადართვის შემდეგ პირველი მოთხოვნა ქეშის გარეშე იკითხება.
- **სხვა:** `best` = Fable, თუ ხელმისაწვდომია, სხვაგვარად `opus`. Haiku სესია plan mode-ში Sonnet-ზე „ადის" (ეს მხოლოდ `availableModels`-ის განყოფილებაშია ნახსენები). `fallbackModel` / `--fallback-model sonnet,haiku`: overload-ის დროს, მაქს. 3 მოდელი, მხოლოდ მიმდინარე ბიჯზე. ავტომატური safety-fallback: Fable 5.1 და Opus 5.5 — bio → Opus 5, cyber → Opus 4.8; Sonnet 5.5 — cyber → Sonnet 5, bio → უარი.

წყარო (2026-10-07): [model-config](https://code.claude.com/docs/en/model-config) („`opusplan` model setting", „Fallback model chains", „Automatic model fallback"), [advisor](https://code.claude.com/docs/en/advisor#compare-with-related-features).

## 6. (e) Per-model effort (მოდელზე მიბმული effort)

- **`modelSettings.<model>.effortLevel`** (v2.1.251-დან): `low`, `medium`, `high` ან `xhigh`; `max` settings-ში არ მიიღება. გასაღები მოდელის canonical სახელია (`claude-opus-5-5`); Claude Code alias-საც, თარიღიან, `[1m]` და provider-ის ID-ებსაც ამოიცნობს. იმავე ობიექტში: `maxEffortLevel`, `autoCompactWindow` (v2.1.288-დან) ([settings-reference](https://code.claude.com/docs/en/settings-reference)).
- **რიგი:** `CLAUDE_CODE_EFFORT_LEVEL` > `--effort` / `/effort` > settings (მოდელის `modelSettings` > ზედა დონის `effortLevel` იმავე ფაილში; ფაილებს შორის — უმაღლესი ფაილი, თითო მოდელზე ცალკე) > მოდელის default. `max` მხოლოდ ერთი სესიისთვისაა (გარდა ცვლადისა). `ultrathink` სიტყვა prompt-ში ერთი მოთხოვნისთვის ამძაფრებს მსჯელობას.
- **მახე:** ზედა დონის `effortLevel` მომხმარებლის ფაილში ძველი ფორმაა; ის Opus 5-ზე და უფრო ძველ მოდელებზე მუშაობს, **Opus 5.5-ზე და მის შემდეგ გამოსულებზე (Sonnet 5.5, Haiku 5.5) უგულებელყოფილია**. პროექტის, local და managed ფაილებში იგივე გასაღები ყველა მოდელზე მოქმედებს.
- **Anthropic-ის რჩევა Sonnet 5.5-ზე:** „Start with `high` unless your workload is agentic or latency-sensitive. For agentic coding … start with `medium` for well-specified tasks and move to `high` for harder or longer ones". Opus 5.5: `medium` ნაგულისხმევია და, Anthropic-ის ტესტით, `medium`-ზე Opus 5.5 ტოლია ან სჯობს Opus 5-ს `high`-ზე. Haiku 5.5: „Start with `medium`"; `low` გრძელ agent-prompt-ებზე ზოგჯერ ბიჯს ან შემოწმებას აკლებს.
- **ჩვენი პროექტის ფაილი:** `modelSettings."claude-sonnet-5-5".effortLevel = "high"` — დასაშვებია და Claude Code-ის default-ს (`medium`) ამაღლებს.

წყარო (2026-10-07): [model-config](https://code.claude.com/docs/en/model-config) („Adjust effort level"), [settings-reference](https://code.claude.com/docs/en/settings-reference) (`modelSettings`, `effortLevel`, `maxEffortLevel`), [effort (API)](https://platform.claude.com/docs/en/build-with-claude/effort), [sub-agents](https://code.claude.com/docs/en/sub-agents).

## 7. (f) Reading `usage` and `usage.iterations` (ტრანსკრიპტის usage-ის წაკითხვა)

**API-ს დოკუმენტი** ([advisor tool, „Usage and billing"](https://platform.claude.com/docs/en/agents-and-tools/tool-use/advisor-tool)): `usage.iterations[]` ყოველ „წრეს" ცალკე აღწერს — `type:"message"` მთავარი (executor) მოდელის ზარია, `type:"advisor_message"` (+ `model`) — advisor-ის. ციტატები: „Top-level `usage` fields reflect executor tokens only. Advisor tokens are not rolled into the top-level totals because they are billed at a different rate." და „Every top-level `usage` field is the sum of that field across all executor iterations … Because each executor iteration re-sends the growing conversation … summed `input_tokens` exceeds the size of any single prompt. Use `usage.iterations` for a full per-iteration breakdown."

**Claude Code-ის დოკუმენტი:** statusline-ის `context_window.current_usage` არის „ბოლო API ზარის" ტოკენები, `used_percentage` = `input_tokens + cache_creation_input_tokens + cache_read_input_tokens`, output-ის გარეშე ([statusline](https://code.claude.com/docs/en/statusline)); `iterations`-ზე და advisor-ზე იქ არაფერია. [Agent SDK cost-tracking](https://code.claude.com/docs/en/agent-sdk/cost-tracking): ერთი API პასუხი რამდენიმე assistant message-ად ჩაიწერება, ყველა ერთსა და იმავე `id`-ს და usage-ს ატარებს (ერთხელ უნდა ჩაითვალოს), per-step `output_tokens` კი placeholder-ია. [sessions](https://code.claude.com/docs/en/sessions): JSONL-ტრანსკრიპტის ფორმატი „internal to Claude Code and changes between versions", ანუ პირდაპირ გაპარსვა ნებისმიერ რელიზზე შეიძლება გატყდეს.

**ლოკალური გადამოწმება** (v2.1.292, ამ მანქანის ტრანსკრიპტი). 1,175,330 = (2 + 583,214 + 3,682) + (2 + 586,896 + 1,534) = 586,898 + 588,432 — ორი executor-ის წრე, თითო ≈587K. advisor-ის წრე (590,220 input, cache 0) ჯამში არ შედის. **მთავარი მოდელის რეალური კონტექსტი 588,432-ია** (≈58.8% 1M-დან, ≈90.5% ჩვენი 650K compaction-ფანჯრიდან), არა 1,175,330. იგივე სურათია ორი სესიის ყველა 7 advisor-იან ბიჯზე: top-level ჯამი ≈2 × ბოლო წრე (მაგ. 402,294 და 204,196). ეს დაკვირვებაა და არა დოკუმენტის გარანტია.

**წესი** (დოკუმენტის ციტატებიდან გამოყვანილი დასკვნა, არა პირდაპირი ციტატა): მთავარი მოდელის რეალური prompt-ის ზომა = ბოლო `type:"message"` წრის `input_tokens + cache_read_input_tokens + cache_creation_input_tokens` (პასუხის შემდეგ — `+ output_tokens`). თუ `iterations` არ არის, top-level უკვე ერთი ზარია. `advisor_message` წრეები მთავარი მოდელის კონტექსტში არასოდეს ითვლება; ხარჯში ითვლება advisor-ის ტარიფით. ერთი `message.id` ტრანსკრიპტში მრავალ ხაზზე მეორდება — დუბლირება `id`-ით მოხსენი.

**რეპოს პრობლემა (დაკვირვებული, არ გამოგვისწორებია).** `.claude/hooks/context_monitor.py` ხაზი 49 ჯამავს top-level `usage`-ს, ამიტომ advisor-იან ბიჯზე კონტექსტს ≈2×-ით აჭარბებს (1,175,330 vs 588,432). ეს რეალურ სესიაში დაკვირვებული ჩავარდნაა, ანუ CLAUDE.md-ის „engine additions need an observed failure" სტანდარტს აკმაყოფილებს. მისი გამოსწორება და drill-ი ცალკე სამუშაოა.

წყარო (2026-10-07): [advisor tool (API)](https://platform.claude.com/docs/en/agents-and-tools/tool-use/advisor-tool), [statusline](https://code.claude.com/docs/en/statusline), [agent-sdk/cost-tracking](https://code.claude.com/docs/en/agent-sdk/cost-tracking), [sessions](https://code.claude.com/docs/en/sessions); ლოკალური ნაწილი — `~/.claude/projects/-Users-tornikebolokadze-Asterbit/*.jsonl` (მხოლოდ `usage`/`model` ველები).

## 8. Official pages that disagree (სადაც ოფიციალური გვერდები ერთმანეთს ეწინააღმდეგება)

1. **Sonnet 5.5, cache read.** $0.10 — [Haiku 5.5 announcement](https://www.anthropic.com/claude-haiku-5-5) („starting today, we're lowering the price of cache reads on Claude Sonnet 5.5 … $0.10 per million tokens rather than $0.20"), [what's new](https://platform.claude.com/docs/en/models/sonnet-5-5/whats-new-sonnet-5-5) და [pricing](https://platform.claude.com/docs/en/about-claude/pricing)-ის ტექსტი (0.05×). $0.20 — pricing-ის ცხრილის სტრიქონი, [Sonnet 5.5-ის გვერდი](https://platform.claude.com/docs/en/models/sonnet-5-5/overview) და 2026-09-28-ის announcement. ავიღეთ $0.10 (2026-10-07-დან); $0.20 მოძველებულია.
2. **Sonnet 5.5, default effort.** API-ზე `high` ([effort](https://platform.claude.com/docs/en/build-with-claude/effort)), Claude Code-ში `medium` ([model-config](https://code.claude.com/docs/en/model-config)). ეს ორი სხვადასხვა კლიენტია, ნამდვილი წინააღმდეგობა არაა.
3. **Fable 5.1, მინ. ვერსია.** v2.1.257 (model-config, advisor) და v2.1.255 ([support](https://support.claude.com/en/articles/15424964-claude-fable-models-on-your-plan)). უსაფრთხო ზღვარი 2.1.257-ია.

## 9. Unverified / not found (ვერ დადასტურდა)

- VS Code picker მოდელს როგორ გადასცემს: `--model`-ად (ფაილებზე მაღლა) თუ მხოლოდ `model`-ად მომხმარებლის ფაილში? დოკუმენტი ამას effort-ზე ამბობს, მოდელზე — არა. `claudeCode.selectedModel`-ის მსგავსი ველი დოკუმენტირებულ settings-ცხრილში არ არის.
- რას აირჩევდა `/model` picker-ში `df3e2417` და `31c2c6fd` სესიებში: ტრანსკრიპტი არჩეულ მნიშვნელობას არ ინახავს. ის კი ზუსტად ჩანს, რომ მომდევნო პასუხები `claude-opus-5-5`-ზე იყო; რომ მიზეზი სწორედ `/model`-ია, დასკვნაა (დროითი დამთხვევა). დასტური `/status` ან გაშვების header-ია.
- ცხადდება თუ არა `/advisor` VS Code extension-შიც: დოკუმენტი ასახელებს ტერმინალს, `-p`-ს, SDK-ს, desktop-ს და Remote Control-ს, extension-ს — არა. `advisorModel` გასაღები საერთო ფაილშია.
- statusline-ის `current_usage` advisor-იან ბიჯზე ორმაგად ითვლის თუ არა: დოკუმენტი ამაზე დუმს. ტრანსკრიპტის ფორმატი დოკუმენტით არასტაბილურია.
- plan limit-ების რიცხვები Opus/Sonnet/Haiku-სთვის გეგმების მიხედვით: ოფიციალურ გვერდებზე მხოლოდ Fable-ის წესია (50%) და usage credits. claude.com/pricing-ის შეჯამებას არ ვენდობით (შეცდომებს შეიცავდა), ამიტომ გეგმების ფასებს არ ვაცხადებთ.
- announcement-ების benchmark-ები: fetch-ინსტრუმენტი შეჯამებას აბრუნებდა და სიტყვასიტყვით არ გადავამოწმეთ, ამიტომ არ შევიტანეთ. შეტანილია მხოლოდ სიტყვასიტყვით ციტირებული წინადადებები.
- „Haiku → Sonnet plan mode-ში" ქცევა: ცალკე განყოფილება არ მოიძებნა, მხოლოდ `availableModels`-ის ტექსტში ხვდება.
- რომელი ზუსტი ვერსიიდან მუშაობს `claude-haiku-5-5` სრული ID-ით `--model`-ზე: დოკუმენტი ამბობს „v2.1.293 or later"; ჩვენ ამ ვერსიაზე არ გაგვიშვია.
