# GB-1746 — Presentation (copy into PowerPoint / Google Slides)

**Ticket:** [GB-1746](https://oppa-support.atlassian.net/browse/GB-1746) — `[ AI ] Read Confluence page comments`  
**Audience:** team (~5–7 min)  
**Commit:** `a83b76f4`  
**Status note:** Implementation delivered and verified; Jira may still show In Progress until transitioned to Done.

---

## Slide 1 — Title

**GB-1746**  
AI now reads Confluence page comments

Not only the page body — footer + inline comments on the same page

---

## Slide 2 — Problem

**What was wrong**

- Rules already said: read Confluence page comments
- MCP tools named in rules (`getConfluencePageFooterComments` / `getConfluencePageInlineComments`) were **not available** in the current MCP
- Agents read **page body only**

**Result:** Clarifications that live only in comments were invisible to AI

**One-liner:**  
> Documentation is good, but important details often sit in comments — AI could not see them.

---

## Slide 3 — What we delivered

**Solution**

1. **REST helper:** `Cursor-Project/config/confluence/get-confluence-page-comments-rest.ps1`  
   - Reads footer + inline comments for a given page ID  
   - Hydrates comment body when the list response is empty
2. **Wired as mandatory** when a Confluence page is used as evidence:
   - Jira ticket analysis (Rule 44) — `jira-evidence`
   - Bug validation
   - Cross-dependency finder
   - Phoenix Q&A / agent workflow
   - Confluence REST fallback + safety rules
3. If there are no comments → note `confluence_comments: none_or_unavailable` (never invent)

**Scope:** Same Confluence page only — not whole-wiki search, not Jira comments, not writing comments.

---

## Slide 4 — Demo / proof (main slide)

**Verification — PASS**

Example page: [get product list](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/779517953/get+product+list) (`779517953`)

| Check | Result |
|---|---|
| Page body | Read |
| Footer comments | 0 |
| Inline comments | 1 — “Product” |
| Body hydration | OK |

**How to re-demo (live):**

```text
powershell -ExecutionPolicy Bypass -File "Cursor-Project/config/confluence/get-confluence-page-comments-rest.ps1" -PageId "779517953"
```

Or in a new Cursor chat, paste a Confluence page URL and ask to include page comments in the answer.

---

## Slide 5 — Why it matters

- Fuller evidence for bug validation, HandsOff, Phoenix Q&A
- Fewer wrong answers when the critical rule is only in a comment
- Better input for other AI workflows — better source → better answer

---

## Slide 6 — Status / next

**Delivered**

- REST script + skills/rules wiring
- Smoke / agent verification on a real page
- Jira Done summary comment on GB-1746
- Git commit: `a83b76f4` — *Enable Confluence page comment reads for agents (GB-1746).*

**Optional next**

- If MCP later exposes comment tools → prefer MCP; keep REST as fallback
- Close Jira to **Done** if not already transitioned

---

## Speaker notes (30 seconds)

> Previously AI could read Confluence pages but not their comments. The MCP tools in our rules were missing. We added a REST helper and made comment reads mandatory in the main workflows. We verified on a real page that an inline comment was returned. Evidence is more complete now.

---

## Files touched (for Q&A)

| Area | Path |
|---|---|
| REST helper | `Cursor-Project/config/confluence/get-confluence-page-comments-rest.ps1` |
| Skills | `jira-evidence`, `phoenix-bug-validation`, `cross-dependency-finder`, `phoenix-agent-workflow`, `phoenix-safety-readonly` |
| Agent | `.cursor/agents/phoenix-qa.md` |
| Rules | `confluence_rest_fallback.mdc`, `safety_rules.mdc` |
