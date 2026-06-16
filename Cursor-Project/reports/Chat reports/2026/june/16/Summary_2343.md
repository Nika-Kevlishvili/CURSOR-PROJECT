# Chat Report — Cursor AI Caching, Pricing, and Usage Dashboard

**Date:** 2026-06-16  
**Topic:** Cursor IDE AI model caching, pricing, local storage, and Cache Write = 0 on usage dashboard  
**Language:** English (on-disk artifact)

---

## Session summary

The user asked a series of questions about how Cursor AI models handle caching, pricing, refresh/invalidation, local storage, whether caching can be disabled, provider architecture, and why the usage dashboard often shows **Cache Write: 0** while **Cache Read** is high.

This report consolidates the chat answers into a single reference document.

---

## 1. Three different “cache” concepts in Cursor

| Type | Where it lives | Purpose | Affects billing? |
|------|----------------|---------|------------------|
| **Prompt cache** | AI provider servers (Anthropic, OpenAI, Google, etc.) | Reuses stable conversation prefix (rules, files, prior turns) | Yes — Cache Read / Cache Write token classes |
| **Codebase index** | Cursor servers (embeddings) + local retrieval metadata | Semantic search (`@codebase`, Agent search) | No (infrastructure; Teams may pay Cursor Token Rate on agent tokens) |
| **Local IDE cache** | `%AppData%\Cursor`, `%LocalAppData%\Cursor` | Electron/Chromium performance (UI, chat DB) | No |

**Critical:** LLM prompt cache is **not stored on the user's PC**.

---

## 2. How prompt caching works

1. Cursor builds prompts in a cache-friendly order and sends them to the provider.
2. The **provider's cache** decides cache hits vs. full re-seed.
3. There is **no user setting** to enable or disable prompt caching.

**Token classes (usage dashboard):**

| Class | Meaning |
|-------|---------|
| **Input** | New content for this step (latest message, new tool output) |
| **Cache write** | Context first stored in the provider's temporary cache |
| **Cache read** | Reuse of an exact matching prefix (typically ~10–25% of input price) |
| **Output** | Model response — always full output pricing |

**Prefix invalidation** (even inside TTL): switching models, editing earlier messages, toggling tools/rules/MCP, switching provider.

**Sources:** [Understanding Write Cache](https://forum.cursor.com/t/understanding-write-cache/156915), [Understanding LLM Token Usage](https://forum.cursor.com/t/understanding-llm-token-usage/120673), [Models & Pricing](https://cursor.com/docs/models-and-pricing.md)

---

## 3. Pricing related to caching

Official pricing: [cursor.com/docs/models-and-pricing](https://cursor.com/docs/models-and-pricing.md)

- **Cache read** is cheaper than full input (often ~10% for Claude).
- **Cache write** (Anthropic): ~1.25× input on first seed; enables cheaper reads later.
- **Auto model** fixed rates: Input + Cache Write $1.25/MTok; Cache Read $0.25/MTok; Output $6.00/MTok.

Usage breakdown: [cursor.com/dashboard/usage](https://cursor.com/dashboard/usage)

---

## 4. Refresh and invalidation

| Cache type | Refresh / TTL |
|------------|---------------|
| **Prompt cache (Claude in Cursor)** | ~5 minute **sliding** TTL; each hit extends; idle >5 min → full re-seed |
| **Codebase index** | ~5 minute sync for changed files; **6 weeks** inactivity → server-side delete |
| **Local IDE cache** | Manual clear or reinstall |

---

## 5. Can caching be disabled?

**No.** There is no Cursor setting to disable Cache Read or Cache Write. Providers manage caching internally.

**Indirect ways to avoid reusing cache** (not the same as disabling):

- Start a **new chat**
- **Switch models** mid-thread
- **Edit** an earlier message
- **Auto mode** may route to providers with different caching visibility on the dashboard

**Source:** [How to disable Cache Write and Cache Read?](https://forum.cursor.com/t/how-to-disable-cache-write-and-cache-read/118864)

---

## 6. Provider architecture

Each AI provider runs its **own API infrastructure**:

```
User (Cursor IDE)
       ↓
Cursor servers (routing, indexing, billing)
       ↓
External providers (each with own servers + own prompt cache)
  • Anthropic (Claude)
  • OpenAI (GPT)
  • Google (Gemini)
  • xAI (Grok)
  • ...
```

Prompt cache is **per provider**, not a single unified Cursor AI server.

---

## 7. Local storage on Windows

**Prompt cache:** not on local disk.

**Local paths:**

| Path | Purpose |
|------|---------|
| `C:\Users\<user>\AppData\Roaming\Cursor` | Chat history (`state.vscdb`), Chromium cache, retrieval metadata |
| `C:\Users\<user>\AppData\Local\Cursor` | GPUCache, Code Cache (ephemeral) |
| `C:\Users\<user>\.cursor\` | Project rules, skills, MCP config |

---

## 8. Why Cache Write = 0 while Cache Read is high

Observed on user's dashboard (`auto`, `composer-2.5-fast`): e.g. Cache Read 200,155; Cache Write 0; Input 117,553; Output 5,154.

**This is usually normal, not a bug.**

| Reason | Explanation |
|--------|-------------|
| **Cache already warm** | Write occurred on an **earlier** request (same chat or another chat within TTL). Current row only reads. |
| **Per-request view** | Dashboard line = one API call. Agent turns may include multiple internal calls; write may appear on a different line. |
| **Non-Anthropic models** | Explicit **Cache Write** line is mainly **Anthropic-style**. `auto` / `composer` may fold write into **Input** or only show **Cache Read**. |
| **Cross-chat reuse** | Provider cache can reuse prefix from recent activity in the same repo within ~5 min TTL. |

**When to worry:** Cache Read = 0 **and** Cache Write = 0 with high Input on every request (especially on Auto) — may indicate routing to non-caching paths.

**To see Cache Write:** new chat + manually select **Claude Sonnet** + first large prompt.

**Sources:** [Why absurd cache read tokens?](https://forum.cursor.com/t/why-does-cursor-consume-an-absurd-amount-of-cache-read-tokens/151439), [Auto mode prompt caching](https://forum.cursor.com/t/auto-mode-prompt-caching-not-working/154654)

---

## 9. Cost optimization tips

- One chat per focused task; new chat for new topics.
- Use `@` mentions instead of broad context when possible.
- Keep rules concise; disable unused MCP tools.
- Monitor [dashboard/usage](https://cursor.com/dashboard/usage).
- If Auto shows inconsistent caching, try a specific Claude model.

---

## Prompt → answer (chat context)

| User question | Answer (short) |
|---------------|--------------|
| How does Cursor cache work, pricing, refresh, where stored? | Three cache types; prompt cache on provider; Claude ~5 min sliding TTL; local AppData for IDE/chat only. |
| Can caching be disabled? | No user setting; providers manage it. |
| Does each AI have its own server? | Yes — Cursor routes to separate provider APIs; prompt cache is per provider. |
| Why Cache Write always 0? | Usually normal: cache already warm, per-request view, non-Anthropic reporting, or write on earlier line. |

---

## Confidence

**High** — Based on Cursor official docs (models/pricing, indexing, semantic search), Cursor staff forum posts, and user's usage dashboard screenshot pattern. Non-Anthropic provider TTL details are not fully documented by Cursor.

---

Agents involved: Report Generator
