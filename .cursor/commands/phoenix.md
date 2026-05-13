# Phoenix Expert Query

Route ALL Phoenix-related questions to PhoenixExpert agent (Rule 0.2 - ABSOLUTE PRIORITY).

## Mandatory Workflow:

1. **Rule 0.3** — No Python `IntegrationService` here; follow MCP/Jira when needed.
2. **Phoenix branch alignment (Rule PHOENIX-SWITCH.0)** — When the question is environment-sensitive (mentions or implies `dev`, `dev2`, `test`, `preprod`, `prod`, or `experiments`), **MANDATORY resolver call:** run `/environment-resolve` (EnvironmentResolverAgent) and use its resolved output.
   - **Prod safety gate (§1a):** if the env is `prod`, FIRST tell the user that local Phoenix edits will be discarded and force-reset to `origin/prod`, wait for explicit ack, then add `-ConfirmProd`.
   - **Subagent reuse (§7a):** if a previous step in this chat session already aligned to the same env and exited `0`, do NOT re-run the script — reuse it.
   - Otherwise run: `powershell -ExecutionPolicy Bypass -File .cursor/commands/switch-phoenix-branches.ps1 -Environment <env>` (add ` -ConfirmProd` for `prod` only).
   - Aligns every `Cursor-Project/Phoenix/*` repo to `origin/<branch>` (latest tip).
   - Local Phoenix edits are discarded; Phoenix code stays READ-ONLY (Rule 0.8 Tier A).
   - Inspect exit code: `0` proceed; `2` proceed but flag mixed-state; `3` STOP and ask user to fix VPN / credentials.
   - If ambiguity remains, EnvironmentResolverAgent MUST ask the user via questionnaire first (Rule CONF.0). Skip alignment only for clearly environment-agnostic doc questions.
3. **Confluence Check** - Search Confluence via MCP tools (FRESH, no cache)
4. **Codebase Check** - Search Phoenix codebase (primary source) using the working copy aligned in step 2
5. **PhoenixExpert Response** - Provide answer with full context, including the environment and target branch used

## Source Priority:
1. Codebase (highest - always wins)
2. Confluence (secondary)
3. General knowledge (fallback)

## Response Requirements:
- State "**Expert:** PhoenixExpert" at beginning
- Provide comprehensive answer
- End with: "Agents involved: PhoenixExpert"

## Reports (Rule 0.6 — optional):
- **Default:** answer in chat only; do **not** create files under **`Cursor-Project/reports/`** unless the user asks for a saved report or uses **`/report`** (paths per **`Cursor-Project/reports/README.md`**).
