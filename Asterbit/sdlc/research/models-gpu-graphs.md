# მოდელები, GPU, typesafe.ai და გრაფები — ვებ-კვლევა

> **ჩანაცვლებულია (2026-10-08):** მფლობელის გადაწყვეტილებით ძრავის როლებს მხოლოდ Claude-ის მოდელები ასრულებს ([ADR-0007](../../docs/decisions/0007-claude-only-orchestration.md)). OpenAI-ისა და ღია მოდელების ნაწილი ისტორიად რჩება; ცოდნის გრაფის ნაწილი (ADR-0006) ძალაშია.

> **თარიღი:** 2026-10-07. ფასები და მოდელების სახელები ამ დღისაა და სწრაფად იცვლება — გამოყენებამდე თავიდან გადაამოწმე. **მეთოდი:** აგენტმა (Sonnet 5.5) პირველადი წყაროები წაიკითხა: ოფიციალური დოკუმენტაცია, ფასების გვერდები, მოდელების ბარათები, GitHub. ის, რაც პირველად წყაროში ვერ დადასტურდა, მონიშნულია როგორც **UNVERIFIED**. ბენჩმარკები მწარმოებლების მიერ გამოქვეყნებულია, ანუ დამოუკიდებლად გაზომილი არ არის. რეპოების კვლევა: [repo-study.md](repo-study.md).

## 1. typesafe.ai

**რა არის.** TypeSafe AI 2026 წელს დაარსებული სან-ფრანცისკოს ლაბორატორიაა. მისი ერთადერთი პროდუქტია **Jev** — „System One" ტიპის მოდელი. ის ტექსტს არ წერს. მას აწვდი მდგომარეობას (state) და ტიპიზებულ კითხვებს, ის კი ყოველ კითხვაზე ტიპიზებულ პასუხს და მის ალბათობას აბრუნებს. კითხვის სამი ტიპი არსებობს: არჩევანი (Choice), ქულა (Score) და კი/არა-ს ალბათობა 0-დან 1-მდე. კითხვები ერთმანეთისგან იზოლირებულად და პარალელურად მუშავდება. სხვა სიტყვებით, ეს კოდის დამწერი მოდელი კი არ არის, არამედ სწრაფი და იაფი კლასიფიკატორი, რომელიც გადაწყვეტილებას იღებს.

**წვდომა.** API არის `POST https://api.typesafe.ai/v1/systemone` გასაღებით. აქვს Python-ისა და JavaScript-ის SDK, ხოლო დოკუმენტაცია Claude Code-ისთვის მზა skill-ს ახსენებს. CLI-სა და MCP-ის ხსენება ვერ ვიპოვეთ. ფასი 42$ ერთ მილიარდ შემავალ ტოკენზეა, სტატუსი კი „early access". playground-ის გვერდი (`console.typesafe.ai/playground`) HTTP 403-ს აბრუნებს, ამიტომ წაკითხვა ვერ მოხერხდა. პრესის ცნობები (დაარსება 15 სექტემბერს, 40 მლნ$ seed დაფინანსება, დამფუძნებელი OpenAI-დან, დაყოვნება 70–500 ms) მხოლოდ ძიების ფრაგმენტებში ჩანს და UNVERIFIED-ია. მონაცემების შენახვისა და უსაფრთხოების პოლიტიკა ვერ ვიპოვეთ.

**სად შეიძლება ჩაჯდეს.** Evaluate-სა და Secure ეტაპებზე ის იაფ, მრჩეველ სორტირებას შეძლებდა, მაგალითად: „ეს ცვლილება დაცულ ფაილს ეხება?" ან „ეს ლოგის ხაზი უჩვეულოა?". Observe ეტაპზე მოვლენების სიმძიმის კლასებად დახარისხებას შეძლებდა. Spec-სა და კოდირების ციკლში თითქმის არ გამოდგება, რადგან არც მსჯელობს და არც წერს.

**რისკი.** ის ალბათობებს აბრუნებს, ჩვენი წესით კი კარიბჭე დეტერმინისტული უნდა იყოს. ამიტომ მხოლოდ მრჩევლის როლს შეასრულებდა. გარდა ამისა, კოდის ან ლოგების გაგზავნა ახალ სტარტაპთან, რომელსაც მონაცემების შენახვის პოლიტიკა არ აქვს გამოქვეყნებული, რეალური რისკია.

**რეკომენდაცია.** ახლა არ ვიყენებთ. ის, რისი გაკეთებაც შეუძლია, ჩვენთან Python სკრიპტი ან regex უფასოდ და დეტერმინისტულად აკეთებს. ხელახლა განვიხილავთ მხოლოდ მაშინ, თუ რომელიმე კარიბჭეს დიდი მოცულობით „ბუნდოვანი" კლასიფიკაცია დასჭირდება, და მაშინაც მხოლოდ არასენსიტიურ ტექსტზე.

## 2. ღია წონების (open-weight) კოდირების მოდელები — 2026 წლის ოქტომბერი

„ღია წონები" ნიშნავს, რომ მოდელის ფაილები გადმოსაწერია და მისი გაშვება საკუთარ სერვერზე შეიძლება.

| მოდელი | პარამეტრები (სულ / აქტიური) | ლიცენზია | კონტექსტი | კოდირების ქულა (მწარმოებლის) | მეხსიერება გასაშვებად |
|---|---|---|---|---|---|
| [DeepSeek-V4-Pro](https://huggingface.co/deepseek-ai/DeepSeek-V4-Pro) | 1.6T / 49B | MIT | 1M | SWE-bench Verified 80.6 | ~0.8–1 TB, ანუ 8×H200 დონე (შეფასება, UNVERIFIED) |
| [MiniMax-M3](https://huggingface.co/MiniMaxAI/MiniMax-M3) | ~428B / ~23B | MiniMax Community | 1M | SWE-bench Verified 80.5 | ~215 GB 4-ბიტზე, 2–3×H200 (შეფასება, UNVERIFIED) |
| [Kimi K3](https://huggingface.co/moonshotai/Kimi-K3) | 2.8T / 104B | Kimi K3 License (უფასო 20 მლნ$ წლიურ შემოსავლამდე) | 1M | Terminal-Bench 2.1: 88.3 | რამდენიმე სერვერი |
| [GLM-5.2](https://huggingface.co/zai-org/GLM-5.2) | 753B | MIT | 1M | SWE-bench Pro 62.1 | მინიმუმ ~4×H200 |
| [Qwen3.6-27B](https://huggingface.co/Qwen/Qwen3.6-27B) | 27B | Apache 2.0 | 262K (1M YaRN-ით) | SWE-bench Verified 77.2 | ~16–20 GB 4-ბიტზე — ერთი 24–32 GB ბარათი (შეფასება) |
| [Qwen3.8-27B](https://huggingface.co/Qwen/Qwen3.8-27B) | 27B | Apache 2.0 | 262K (1M) | SWE-bench Pro 61.7 | იგივე |
| [Qwen3-Coder-Next](https://huggingface.co/Qwen/Qwen3-Coder-Next) | 80B / 3B | Apache 2.0 | 262K | SWE-bench Verified 70.6 | ~46 GB 4-ბიტზე — ერთი 96 GB ბარათი |

ყველაზე ძლიერი სამი (DeepSeek V4-Pro, MiniMax M3, Kimi K3) ერთ ნაქირავებ ბარათზე ვერ ეტევა. ერთ ბარათზე გასაშვებ მოდელებს შორის საუკეთესოა Qwen-ის 27B მოდელები და Qwen3-Coder-Next. დამოუკიდებელი შეფასებით, ერთ ბარათზე გაშვებადი ღია მოდელები SWE-bench Verified-ზე დაახლოებით 60–72 ქულას იღებენ, მაშინ როცა ღრუბლოვანი წამყვანი მოდელები 80–95-ს.

**როგორ ეშვება.** მოდელების ბარათები vLLM-სა და SGLang-ს გვირჩევს. ორივე OpenAI-თავსებად API-ს იძლევა. Claude Code Anthropic-ის ფორმატზე საუბრობს, ამიტომ საკუთარ vLLM-თან დასაკავშირებლად შუამავალი, მაგალითად LiteLLM, სავარაუდოდ დაგვჭირდება (UNVERIFIED). DeepSeek-ის საკუთარი API უკვე Anthropic-თავსებადია.

**API თუ GPU?** ნაქირავები 96 GB ბარათი ~2$ საათში, თვეში 88 საათისთვის (დღეში 4 საათი × 22 დღე), დაახლოებით 175$ ჯდება. იმავე ფულით OpenRouter-ზე Qwen3-Coder-Next-ის ~880 მილიონი შემავალი ტოკენის ყიდვა შეიძლება, ანუ დღეში ~40 მილიონის. ეს გამოთვლა ჩვენია. კოდირების აგენტი cache-ით ამდენს იშვიათად ხარჯავს. დიდი მოდელების შემთხვევაში (8×H200 ≈ 30$ საათში) API მრავალჯერ იაფია. GPU-ს ქირაობას აზრი მხოლოდ მაშინ აქვს, როცა კოდი სხვის სერვერზე არ უნდა წავიდეს, მონაცემების ადგილმდებარეობა მნიშვნელოვანია ან ექსპერიმენტს ვატარებთ.

| მოდელი | API ფასი 1M ტოკენზე (შემავალი / გამავალი) | წყარო |
|---|---|---|
| DeepSeek V4-Pro | 0.66–1.32$ / 1.98–3.96$ (cache 0.022–0.044$) | [DeepSeek](https://api-docs.deepseek.com/quick_start/pricing) |
| Kimi K3 | 3$ / 15$ | [Moonshot](https://platform.kimi.ai/docs/pricing/chat) |
| GLM-5.2 | 1.4$ / 4.4$ | [Z.ai](https://docs.z.ai/guides/overview/pricing) |
| Qwen3-Coder-Next | 0.12–0.30$ / 0.80–1.50$ | [OpenRouter](https://openrouter.ai/api/v1/models/qwen/qwen3-coder-next/endpoints) |

**რეკომენდაცია.** „იაფი მეორე ტვინისთვის" ჯერ API ვცადოთ: DeepSeek V4-Pro ან Qwen3-Coder-Next. ყველაზე უარეს შემთხვევაშიც ეს დღეში რამდენიმე დოლარი იქნება. GPU მხოლოდ მაშინ ვიქირაოთ, თუ შენ გადაწყვეტ, რომ კოდი სხვის API-ზე არ უნდა გავიდეს. ასეთ შემთხვევაში ერთ ბარათზე Qwen3-Coder-Next ან Qwen-ის 27B მოდელი ვუშვებთ.

## 3. GPU-ს ქირაობა

| პლატფორმა | ჩვენთვის საინტერესო ბარათები და ფასი | გადახდა | ევროპა | მარტივია არა-პროგრამისტისთვის? |
|---|---|---|---|---|
| [RunPod](https://www.runpod.io/pricing) | RTX PRO 6000 (96 GB) 1.64–2.09$/სთ; H100 1.99–2.89$; H200 3.59–4.59$; 4090 0.34–0.74$ | წამობრივი; serverless, გამოუყენებლად 0-მდე ეცემა | Secure Cloud-ს ევროპული რეგიონები აქვს (რომელი — UNVERIFIED) | კარგი: vLLM-ის მზა თარგები |
| [Vast.ai](https://vast.ai/pricing) | ბაზარი, 68+ ტიპის ბარათი; RTX PRO 6000 ~1.42$/სთ (მეორადი წყარო) | წამობრივი; შეწყვეტადი 50%+ იაფია | 40+ მონაცემთა ცენტრი, ხარისხი განსხვავებულია | საშუალო |
| [Lambda](https://lambda.ai/pricing) | H100 3.99–4.29$; B200 6.69–6.99$; A100 1.99–2.79$ | წუთობრივი | გაურკვეველი | კარგი, მაგრამ ძვირი და ხშირად არ არის თავისუფალი |
| [Modal](https://modal.com/pricing) | 0.000164$/წმ (T4)-დან 0.001972$/წმ-მდე (B300); 30$ უფასო კრედიტი | წამობრივი serverless | გაურკვეველი | Python სჭირდება |
| [Hetzner GEX131](https://www.hetzner.com/dedicated-rootserver/gex131/) | RTX PRO 6000 96 GB; 889€/თვე ან ~1.42€/სთ | ვალდებულების გარეშე; ინსტალაციის საფასური არ ჩანს (UNVERIFIED) | **გერმანია, ფინეთი** | SSH და Linux სჭირდება |

Hyperbolic-ის ფასების გვერდმა 404 დააბრუნა, ხოლო Together-ის გამოყოფილი სერვერები არ შეგვიმოწმებია.

**რეკომენდაცია.** მთავარ ვარიანტად RunPod-ს გირჩევთ: ერთი RTX PRO 6000 (96 GB) Secure Cloud-ზე, ქსელურ დისკთან ერთად. ის Qwen3-Coder-Next-ს 4-ბიტზე იტევს და კონტექსტისთვისაც რჩება ადგილი. დღეში 4 საათი × 22 დღე ≈ 88 სთ × 2$ ≈ 175$ + 5–10$ დისკი, ანუ **დაახლოებით 185$ თვეში** (ჩვენი გამოთვლა). სათადარიგო ვარიანტია Hetzner GEX131 (~1.42€/სთ): ეს ნამდვილი ევროპული, გამოყოფილი სერვერია, იმავე 88 საათისთვის დაახლოებით 125€ თვეში, თუ საათობრივი გადახდა ისე მუშაობს, როგორც წერია. ⚠ სერვერი მუშაობის დასრულებისას აუცილებლად უნდა გამორთო, თორემ უწყვეტად დაგერიცხება — დაახლოებით 1500$ თვეში.

## 4. OpenAI-ის მოდელი სხვა მომწოდებლის შემმოწმებლად

„GPT-6 Astra" ნამდვილად არსებობს. მისი API ID არის `gpt-6-astra` ([model page](https://developers.openai.com/api/docs/models/gpt-6-astra)). ფასი: 10$ შემავალი, 1$ cache-დან, 50$ გამავალი 1M ტოკენზე. კონტექსტი 1.05M-ია, მაქსიმალური პასუხი 128K. 272 ათას ტოკენზე მეტ კონტექსტზე ფასი იზრდება (მეორადი წყარო). **ჩვენს ADR-0003-ში ეწერა „ChatGPT 6.1 Astra", რაც OpenAI-ის კატალოგს არ ემთხვევა.** რეალური ხაზია `gpt-6-astra`, `gpt-6.1-sol` (2$ / 10$) და `gpt-6-luna` (0.10$ / 0.50$) ([models](https://developers.openai.com/api/docs/models)). Codex-ის დოკუმენტაციით, Astra რთული სამუშაოსთვისაა, Sol კი „Astra-სთან ახლო შედეგს იაფად" იძლევა.

Claude Code-იდან ოფიციალური გზაა plugin [openai/codex-plugin-cc](https://github.com/openai/codex-plugin-cc) (Apache-2.0, ბოლო push 2026-07-08; შენს კომპიუტერზე უკვე დაყენებულია). ბრძანებებია `/codex:review`, `/codex:adversarial-review` და `/codex:rescue`. სჭირდება Node 18.18+ და ChatGPT-ის გეგმა ან OpenAI API გასაღები. მოდელი `.codex/config.toml`-ში ირჩევა (README-ს მაგალითი მოძველებულია). Astra-ს კოდირების ბენჩმარკები პირველად წყაროში ვერ ვიპოვეთ.

**რეკომენდაცია.** ორკესტრაციის მეორე ეტაპზე სხვა მომწოდებლის ნაგულისხმევი შემმოწმებელი იყოს `/codex:adversarial-review` მოდელით `gpt-6.1-sol`, ხოლო `gpt-6-astra` მხოლოდ ეტაპის (milestone) აუდიტზე ჩაერთოს. ADR-0003-ში სახელი უნდა გასწორდეს.

## 5. ცოდნის გრაფები — დოკუმენტებისთვის და კოდისთვის

ვარსკვლავები და ბოლო push GitHub API-ით შემოწმდა 2026-10-07-ს.

| ინსტრუმენტი | რას ინდექსირებს | ლოკალური? | MCP | ლიცენზია | ★ / ბოლო push | რამდენად მძიმეა |
|---|---|---|---|---|---|---|
| **Obsidian** (გრაფის ხედი) | markdown ბმულები | კი, აპლიკაცია | ჩაშენებული არა | საკუთრებითი | — | ნული: რეპო უკვე vault-ია |
| [Graphify](https://github.com/safishamsi/graphify) | კოდი (37 ენა, tree-sitter, LLM-ის გარეშე), დოკუმენტები, PDF | კი | კი | Apache-2.0 / MIT | 124.5k · 2026-10-06 | `uv tool install graphifyy`; `/graphify` skill შენთან უკვე დაყენებულია |
| [Neo4j + ოფიციალური MCP](https://github.com/neo4j/mcp) | რასაც ჩატვირთავ | მონაცემთა ბაზის სერვერი | კი | გვერდზე არ წერია | 296 · 2026-10-05 | მძიმე: ბაზა და სქემის დიზაინი |
| [codebase-memory-mcp](https://github.com/DeusData/codebase-memory-mcp) | კოდი, 162 ენა | კი, ერთი ფაილი | 17 ხელსაწყო | MIT | 45.9k · 2026-10-07 | ერთი ინსტალაციის სკრიპტი |
| [Serena](https://github.com/oraios/serena) | კოდი, language server-ებით | კი | კი | GPL-3.0 + MIT | 30.1k · 2026-10-06 | `uv tool install` |
| [GitNexus](https://github.com/abhigyanpatwari/GitNexus) | კოდი | კი | 19 ხელსაწყო | **PolyForm Noncommercial** | 47.8k · 2026-10-07 | `npx`; ლიცენზია პროდუქტში გამოყენებას კრძალავს |
| [Graphiti](https://github.com/getzep/graphiti) | აგენტის მეხსიერება, ფაქტები დროში | Neo4j/FalkorDB + LLM გასაღები | კი | Apache-2.0 | 31.5k · 2026-10-06 | მძიმე |
| [Cognee](https://github.com/topoteretes/cognee) | დოკუმენტები, კოდი, სესიების გაკვეთილები | კი ან Docker | კი | Apache-2.0 | 31.5k · 2026-10-07 | მსუბუქი–საშუალო |
| [LightRAG](https://github.com/HKUDS/LightRAG) | დოკუმენტები (graph RAG) | სერვერი, LLM სჭირდება | არ შემოწმდა | MIT | 40.0k · 2026-10-03 | საშუალო |

**⚠ ADR-0004-თან კავშირი.** 2026-10-06-ის ინტერვიუში გრაფის ფენაზე სამი ვარიანტი იყო: „Obsidian vault / Obsidian + Graphify / ჯერ არაფერი". შენ აირჩიე **მხოლოდ Obsidian vault**, და ეს `accepted` გადაწყვეტილებაა. მაშინ საუბარი მეხსიერებასა და დოკუმენტებზე იყო, კოდი კი ჯერ არ არსებობდა. ამიტომ Graphify კოდისთვის ახალი, ვიწრო გადაწყვეტილება იქნება, რომელსაც ცალკე შენი არჩევანი და ADR სჭირდება. ქვემოთ მოცემული რეკომენდაცია მხოლოდ შეთავაზებაა.

**რეკომენდაცია — თხელი გზა.** დოკუმენტებისა და თეორიისთვის **ახლა** Obsidian ვიყენოთ, რადგან რეპო უკვე vault-ია (ADR-0004). კოდისა და დოკუმენტების გრაფისთვის **Graphify** დავამატოთ. skill უკვე დაყენებულია, მაგრამ ხელსაწყოს დაყენება (`uv tool install graphifyy`) შენს თანხმობას საჭიროებს. მისი გრაფი წარმოებული ინდექსია, ამიტომ git-ში არ ინახება და ნებისმიერ დროს თავიდან აიგება. **codebase-memory-mcp** მხოლოდ მაშინ დავამატოთ, როცა კოდში ~50 წყარო-ფაილი იქნება და აგენტი ერთსა და იმავე ფაილებს განმეორებით კითხულობს. Neo4j-ს, Graphiti-სა და Cognee-ს გამოვტოვებთ, სანამ მეხსიერება markdown-ს არ გადააჭარბებს. ამ ზღვარს ADR-0004-ის QMD-ის წესი უკვე აკვირდება.

## 6. კონტექსტის პროცენტი — „შეჯამება შეკუმშვამდე"-სთვის

- **statusline იღებს:** მის შემავალ JSON-ში არის `context_window.used_percentage`, `remaining_percentage` და `context_window_size` ([statusline docs](https://code.claude.com/docs/en/statusline)). ⚠ შენს გლობალურ პარამეტრებში უკვე გაქვს საკუთარი statusline (`~/.claude/statusline-rich.js`). პროექტის statusline მას ამ რეპოში ჩაანაცვლებდა, ამიტომ ამ გზას არ გირჩევთ.
- **ჩვეულებრივი hook-ები პროცენტს არ იღებენ** ([hooks docs](https://code.claude.com/docs/en/hooks)). ყველა hook იღებს `transcript_path`-ს, PreCompact — `trigger`-ს და `custom_instructions`-ს, PostCompact კი `compact_summary`-ს. PreCompact-ს შეკუმშვის შეჩერებაც შეუძლია (exit 2).
- **ჩვენი გზა (ადგილზე შემოწმდა 2026-10-07):** სესიის ჩანაწერის ყოველ პასუხს `usage` ველი აქვს: `input_tokens + cache_read_input_tokens + cache_creation_input_tokens`. მიმდინარე სესიაში ეს ჯამი ~281 ათასი იყო, ანუ 1M ფანჯრის ~28%. hook-ს ამ რიცხვის დათვლა თავად შეუძლია, statusline-ის შეცვლის გარეშე. ჩანაწერის ფორმატი ოფიციალურად დოკუმენტირებული არ არის, ამიტომ ახალ hook-ს წვრთნა (drill) და მკაფიო „ვერ დავთვალე" შედეგი სჭირდება.

## წყაროები

- typesafe.ai: https://typesafe.ai · https://docs.typesafe.ai/ · https://docs.typesafe.ai/llms.txt · https://docs.typesafe.ai/api.md · https://letsdatascience.com/news/typesafe-ai-launches-jev-decision-model-889a38c0 · https://console.typesafe.ai/playground (403)
- მოდელები: https://huggingface.co/deepseek-ai/DeepSeek-V4-Pro · https://huggingface.co/MiniMaxAI/MiniMax-M3 · https://huggingface.co/moonshotai/Kimi-K3 · https://huggingface.co/zai-org/GLM-5.2 · https://huggingface.co/Qwen/Qwen3.6-27B · https://huggingface.co/Qwen/Qwen3.8-27B · https://huggingface.co/Qwen/Qwen3-Coder-Next · https://www.eweek.com/news/alibaba-qwen3-8-27b-license-apac-china/ · https://www.digitalapplied.com/blog/best-open-weight-coding-models-self-host-hardware-match-2026
- API ფასები: https://api-docs.deepseek.com/quick_start/pricing · https://platform.kimi.ai/docs/pricing/chat · https://docs.z.ai/guides/overview/pricing · https://openrouter.ai/api/v1/models/qwen/qwen3-coder-next/endpoints
- GPU: https://www.runpod.io/pricing · https://computeprices.com/providers/runpod/gpus/rtx-pro-6000 · https://vast.ai/pricing · https://lambda.ai/pricing · https://modal.com/pricing · https://www.hetzner.com/dedicated-rootserver/gex131/
- OpenAI: https://developers.openai.com/api/docs/models · https://developers.openai.com/api/docs/models/gpt-6-astra · https://learn.chatgpt.com/docs/models · https://github.com/openai/codex-plugin-cc
- გრაფები: https://github.com/safishamsi/graphify · https://github.com/DeusData/codebase-memory-mcp · https://github.com/oraios/serena · https://github.com/abhigyanpatwari/GitNexus · https://github.com/getzep/graphiti · https://github.com/topoteretes/cognee · https://github.com/HKUDS/LightRAG · https://github.com/neo4j/mcp
- Claude Code: https://code.claude.com/docs/en/statusline · https://code.claude.com/docs/en/hooks
