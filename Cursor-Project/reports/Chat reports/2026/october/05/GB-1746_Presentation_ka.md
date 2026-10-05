# GB-1746 — პრეზენტაცია

**თასქი:** [GB-1746](https://oppa-support.atlassian.net/browse/GB-1746) — `[ AI ] Read Confluence page comments`  
**აუდიტორია:** თიმი (~5–7 წთ)  
**Commit:** `a83b76f4`  
**შენიშვნა:** იმპლემენტაცია გაკეთებულია და შემოწმებულია; Jira შეიძლება ჯერაც In Progress იყოს, სანამ Done-ზე არ გადავა.

---

## სლაიდი 1 — სათაური

**GB-1746**  
AI ახლა კითხულობს Confluence-ის კომენტარებსაც

არა მხოლოდ გვერდის ტექსტს — იმავე გვერდის footer + inline comments

---

## სლაიდი 2 — პრობლემა

**რა იყო არასწორი**

- წესებში უკვე ეწერა: წაიკითხე Confluence გვერდის კომენტარები
- MCP tool-ები (`getConfluencePageFooterComments` / `getConfluencePageInlineComments`) რეალურ MCP-ში **არ იყო**
- აგენტი კითხულობდა მხოლოდ **page body**-ს (სტატიას)

**შედეგი:** გარკვევები, რომლებიც მხოლოდ კომენტარშია — AI-სთვის უხილავი იყო

**ერთი წინადადება:**  
> დოკუმენტაცია კარგია, მაგრამ ხშირად მნიშვნელოვანი ნაწილი კომენტარშია — AI-მ ეს ვერ ხედავდა.

---

## სლაიდი 3 — რა გავაკეთეთ

**გადაწყვეტა**

1. **REST helper:** `Cursor-Project/config/confluence/get-confluence-page-comments-rest.ps1`  
   - იღებს footer + inline კომენტარებს page ID-ით  
   - თუ list ცარიელ body-ს აბრუნებს — დამატებით ტვირთავს კომენტარის ტექსტს
2. **ჩავრთეთ სავალდებულოდ** როცა Confluence გვერდი evidence-ად გამოიყენება:
   - Jira ticket ანალიზი (Rule 44) — `jira-evidence`
   - Bug validation
   - Cross-dependency finder
   - Phoenix Q&A / agent workflow
   - Confluence REST fallback + safety rules
3. თუ კომენტარი არაა → `confluence_comments: none_or_unavailable` (არ იგონებს)

**სკოპი:** მხოლოდ იმავე Confluence გვერდი — არა მთელი wiki-ს ძებნა, არა Jira კომენტარები, არა კომენტარის დაწერა.

---

## სლაიდი 4 — დემო / მტკიცებულება

**შემოწმება — PASS**

მაგალითი: [get product list](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/779517953/get+product+list) (`779517953`)

| შემოწმება | შედეგი |
|---|---|
| Page body | წაიკითხა |
| Footer comments | 0 |
| Inline comments | 1 — „Product“ |
| Body hydration | OK |

**როგორ გავიმეოროთ (live):**

```text
powershell -ExecutionPolicy Bypass -File "Cursor-Project/config/confluence/get-confluence-page-comments-rest.ps1" -PageId "779517953"
```

ან ახალ Cursor ჩატში ჩააგდე Confluence გვერდის ლინკი და სთხოვე, რომ პასუხში კომენტარებიც ჩართოს.

---

## სლაიდი 5 — რატომ აქვს მნიშვნელობა

- უფრო სრული evidence bug validation / HandsOff / Phoenix Q&A-ში
- ნაკლები შეცდომა, როცა მნიშვნელოვანი ინფო მხოლოდ კომენტარშია
- სხვა AI თასქებსაც ეხმარება — უკეთესი წყარო = უკეთესი პასუხი

---

## სლაიდი 6 — სტატუსი / შემდეგი

**გაკეთებულია**

- REST სკრიპტი + skill/წესებში ჩართვა
- შემოწმება რეალურ გვერდზე
- Jira-ზე Done summary კომენტარი (GB-1746)
- Git commit: `a83b76f4` — *Enable Confluence page comment reads for agents (GB-1746).*

**ოფციონალური შემდეგი**

- თუ MCP დაამატებს comment tool-ებს → ჯერ MCP, REST რჩება fallback
- Jira გადაიყვანე **Done**-ზე, თუ ჯერ არ არის

---

## სალაპარაკო ტექსტი (30 წამი)

> ადრე AI Confluence-ს კითხულობდა, მაგრამ კომენტარებს — არა. MCP-ში tool არ იყო. დავამატეთ REST და ჩავრთეთ workflow-ებში. შევამოწმეთ რეალურ გვერდზე — კომენტარი წაიკითხა. ახლა evidence უფრო სრულია.

---

## ფაილები (Q&A-სთვის)

| არე | გზა |
|---|---|
| REST helper | `Cursor-Project/config/confluence/get-confluence-page-comments-rest.ps1` |
| Skills | `jira-evidence`, `phoenix-bug-validation`, `cross-dependency-finder`, `phoenix-agent-workflow`, `phoenix-safety-readonly` |
| Agent | `.cursor/agents/phoenix-qa.md` |
| Rules | `confluence_rest_fallback.mdc`, `safety_rules.mdc` |
